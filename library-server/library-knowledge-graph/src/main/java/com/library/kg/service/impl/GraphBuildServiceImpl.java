package com.library.kg.service.impl;

import com.library.ai.llm.LlmService;
import com.library.ai.llm.LlmUnavailableException;
import com.library.ai.nlp.NlpService;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.core.entity.Book;
import com.library.core.entity.Category;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.kg.config.KnowledgeGraphProperties;
import com.library.kg.dto.BookEntityList;
import com.library.kg.dto.BookEntityList.Entity;
import com.library.kg.repository.Neo4jRepository;
import com.library.kg.service.GraphBuildService;
import com.library.kg.service.TopicNetworkBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识图谱构建服务实现.
 * <p>
 * NER（实体识别）→ MERGE（Neo4j 写入）→ 实体对齐。
 * LLM（DeepSeek API）优先，不可用时降级为本地 HanLP 分词 + 规则。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class GraphBuildServiceImpl implements GraphBuildService {

    private final Neo4jRepository neo4jRepository;
    private final NlpService nlpService;
    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;
    private final KnowledgeGraphProperties kgProperties;
    private final LlmService llmService;
    /**
     * 主题网络构建器（通过 ObjectProvider 延迟解析）.
     * <p>
     * TopicNetworkBuilderImpl 仅依赖 {@link Neo4jRepository} / {@link KnowledgeGraphProperties}，
     * 当前不存在循环依赖；改用 {@link ObjectProvider} 延迟注入以保持解耦，并在未来若上游
     * （如召回流水线）反向依赖 GraphBuildService 时避免循环依赖风险。
     */
    private final ObjectProvider<TopicNetworkBuilder> topicNetworkBuilderProvider;

    public GraphBuildServiceImpl(
            Neo4jRepository neo4jRepository,
            NlpService nlpService,
            BookMapper bookMapper,
            CategoryMapper categoryMapper,
            KnowledgeGraphProperties kgProperties,
            @Autowired(required = false) LlmService llmService,
            ObjectProvider<TopicNetworkBuilder> topicNetworkBuilderProvider) {
        this.neo4jRepository = neo4jRepository;
        this.nlpService = nlpService;
        this.bookMapper = bookMapper;
        this.categoryMapper = categoryMapper;
        this.kgProperties = kgProperties;
        this.llmService = llmService;
        this.topicNetworkBuilderProvider = topicNetworkBuilderProvider;
    }

    @Override
    // 事务边界说明：@Transactional 仅管理 MySQL 事务。本方法无 MySQL 写操作（仅 selectById 读取），
    // 故 Spring 事务实际为空；Neo4j 写入通过 Driver 独立 Session auto-commit（见 Neo4jRepository.execute），
    // 不纳入此事务，无法借 @Transactional 回滚。图谱一致性依赖 MERGE 幂等语义 + KgBuildListener 重试
    // 保证最终一致。保留 @Transactional 以备未来在方法内引入 MySQL 写操作时提供事务保护。
    @Transactional(rollbackFor = Exception.class)
    public void buildGraph(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getDeleted() != null && book.getDeleted() == 1) {
            throw new BizException(ErrorCode.KG_ENTITY_NOT_FOUND, "bookId=" + bookId);
        }

        try {
            // 1. 实体识别
            BookEntityList entities = recognizeEntities(book);

            // 2. 写入 Neo4j
            writeToNeo4j(book, entities);

            log.info("知识图谱构建成功: bookId={}, title={}", bookId, book.getTitle());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("知识图谱构建失败: bookId={}, error={}", bookId, e.getMessage(), e);
            throw new BizException(ErrorCode.KG_BUILD_FAILED, "bookId=" + bookId + ", " + e.getMessage());
        }
    }

    @Override
    // 事务边界同 buildGraph：@Transactional 仅管理 MySQL（本方法仅 selectList 读取，事务为空），
    // Neo4j 写入不在此事务内、不可回滚；单本构建失败由下方 catch BizException 跳过，
    // 不会回滚整批已写入的 Neo4j 数据（MERGE 幂等，全量重跑安全）。
    @Transactional(rollbackFor = Exception.class)
    public int rebuildAll() {
        int built = 0;
        int pageSize = 100;
        long lastId = 0;
        while (true) {
            List<Book> books = bookMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                            .gt(Book::getId, lastId)
                            .eq(Book::getDeleted, 0)
                            .orderByAsc(Book::getId)
                            .last("LIMIT " + pageSize));
            if (books.isEmpty()) break;
            for (Book book : books) {
                try {
                    buildGraph(book.getId());
                    built++;
                } catch (BizException e) {
                    log.warn("全量重建跳过 bookId={}: {}", book.getId(), e.getMessage());
                }
                lastId = book.getId();
            }
        }
        log.info("全量重建完成: 成功 {} 本", built);
        // 全量重建后自动触发主题网络（RELATED_TO + PageRank）重建
        try {
            TopicNetworkBuilder builder = topicNetworkBuilderProvider.getIfAvailable();
            if (builder != null) {
                builder.buildTopicNetwork();
            } else {
                log.warn("TopicNetworkBuilder 不可用，跳过主题网络重建");
            }
        } catch (Exception e) {
            log.warn("主题网络重建失败（不影响全量重建结果）: {}", e.getMessage());
        }
        return built;
    }

    // ---- NER ----

    private BookEntityList recognizeEntities(Book book) {
        // LLM 路径
        if (llmService != null) {
            try {
                String prompt = buildNerPrompt(book);
                BookEntityList result = llmService.chat(prompt, BookEntityList.class);
                if (result != null && hasAnyEntity(result)) {
                    return result;
                }
            } catch (LlmUnavailableException e) {
                log.warn("LLM NER 失败，降级本地 NLP: {}", e.getMessage());
            } catch (Exception e) {
                log.warn("LLM NER 异常，降级本地 NLP: {}", e.getMessage());
            }
        }
        // 本地 NLP 降级
        return fallbackNer(book);
    }

    private BookEntityList fallbackNer(Book book) {
        BookEntityList result = new BookEntityList();

        // 作者：按逗号/分号/顿号切分；西方全名含空格整体保留（不按空格切分）
        List<Entity> authors = new ArrayList<>();
        if (StringUtils.hasText(book.getAuthor())) {
            String[] parts = book.getAuthor().split("[,，;；、]+");
            for (String part : parts) {
                String name = part.trim();
                if (!name.isEmpty()) {
                    authors.add(new Entity(name, 0.8));
                }
            }
        }
        result.setAuthors(authors);

        // 关键词：HanLP TextRank 提取 top 20
        List<Entity> keywords = new ArrayList<>();
        String text = buildExtractionText(book);
        if (StringUtils.hasText(text)) {
            List<String> extracted = nlpService.extractKeywords(text, 20);
            for (String kw : extracted) {
                if (!kw.isEmpty()) {
                    keywords.add(new Entity(kw, 0.7));
                }
            }
        }
        // 补充原始 keywords 字段中的词
        if (StringUtils.hasText(book.getKeywords())) {
            for (String kw : book.getKeywords().split("[,，]+")) {
                String trimmed = kw.trim();
                if (!trimmed.isEmpty() && keywords.stream().noneMatch(e -> e.getName().equals(trimmed))) {
                    keywords.add(new Entity(trimmed, 0.9));
                }
            }
        }
        result.setKeywords(keywords);

        // 学科：按 category 名称
        List<Entity> subjects = new ArrayList<>();
        if (book.getCategoryId() != null) {
            Category cat = categoryMapper.selectById(book.getCategoryId());
            if (cat != null && cat.getDeleted() == 0) {
                subjects.add(new Entity(cat.getName(), 0.9));
            }
        }
        result.setSubjects(subjects);

        return result;
    }

    private String buildNerPrompt(Book book) {
        return """
                你是一位图书馆学领域的命名实体识别专家。请从以下图书信息中提取实体。

                ## 图书信息
                - 书名：%s
                - 作者（原始字段）：%s
                - 内容简介：%s
                - 关键词（原始字段）：%s

                ## 要求
                请以 JSON 格式返回识别结果（不要输出其他内容），包含：
                - authors：作者姓名列表（每项含 name、confidence 0-1）
                - keywords：关键词列表（每项含 name、confidence 0-1，提取 5-20 个）
                - subjects：学科方向列表（每项含 name、confidence 0-1，1-3 个）
                """.formatted(
                        nullToEmpty(book.getTitle()),
                        nullToEmpty(book.getAuthor()),
                        nullToEmpty(book.getDescription()),
                        nullToEmpty(book.getKeywords()));
    }

    private boolean hasAnyEntity(BookEntityList entities) {
        return (entities.getAuthors() != null && !entities.getAuthors().isEmpty())
                || (entities.getKeywords() != null && !entities.getKeywords().isEmpty())
                || (entities.getSubjects() != null && !entities.getSubjects().isEmpty());
    }

    // ---- Neo4j 写入 ----

    private void writeToNeo4j(Book book, BookEntityList entities) {
        // 写入 Book 节点
        Map<String, Object> bookMatch = Map.of("id", book.getId());
        Map<String, Object> bookSet = Map.of(
                "id", book.getId(),
                "title", nullToEmpty(book.getTitle()),
                "isbn", nullToEmpty(book.getIsbn()),
                "borrowCount", book.getBorrowCount() != null ? book.getBorrowCount() : 0,
                "categoryId", book.getCategoryId() != null ? book.getCategoryId() : 0);
        neo4jRepository.saveNode("Book", bookMatch, bookSet);

        // 批量写入 Author / Keyword / Subject 节点及关系（UNWIND 消除 N+1，结构相同故提取统一方法）
        mergeEntities("Author", "AUTHORED_BY", entities.getAuthors(), book.getId());
        mergeEntities("Keyword", "HAS_KEYWORD", entities.getKeywords(), book.getId());
        mergeEntities("Subject", "BELONGS_TO", entities.getSubjects(), book.getId());

        // 基于共享关键词构建 CITES 引用边（课设场景下的合理代理，详见 buildCitationsBySharedKeywords javadoc）
        buildCitationsBySharedKeywords(book, entities);
    }

    /**
     * 基于"共享关键词"启发式构建 CITES 引用边.
     * <p>
     * <b>业务正当性</b>：课设场景下没有真实的参考文献元数据（如 CrossRef DOI 引用列表），
     * 因此采用领域内常见的代理方式——同主题图书之间存在隐含的知识传承关系（类似 co-citation analysis）。
     * 策略为：找出与当前图书共享至少 2 个关键词的其他图书，按"新书 → 旧书"方向（createTime 较晚的指向较早的）
     * 建立 CITES 边，权重为共享关键词数 / 当前图书关键词总数。
     * <p>
     * 限制每本书最多创建 5 条 CITES 边，防止图谱过密；MERGE 语义保证幂等。
     * <p>
     * 异常仅 log.warn 不抛出，保持主流程降级。
     *
     * @param book     当前正在构建图谱的图书（含 createTime 用于方向判定）
     * @param entities NER 识别出的实体（用于计算 totalKeywords）
     */
    private void buildCitationsBySharedKeywords(Book book, BookEntityList entities) {
        int totalKeywords = (entities.getKeywords() == null) ? 0 : entities.getKeywords().size();
        if (totalKeywords == 0) {
            return;
        }
        try {
            // 共享关键词 ≥ 2 视为存在引用关系；weight = shared / totalKeywords 保证 > 0；每本最多 5 条。
            // 设计权衡：原计划用 createTime 限定"新书 → 旧书"方向，但 Book 节点未持久化 createTime 属性
            // （writeToNeo4j 仅写入 id/title/isbn/borrowCount/categoryId）；
            // 课设场景下采用 MIN(id) 比较作为方向代理——较小 id 的图书通常更早入库，让较大 id 的指向它。
            // 此外 Cypher 5.x 要求 WHERE-ORDER BY-LIMIT 必须依附同一 WITH/RETURN，
            // 不能在 WHERE 之后裸接 ORDER BY，故用第二个 WITH 子句封装。
            String cypher = """
                    MATCH (newBook:Book {id: $bookId})-[:HAS_KEYWORD]->(k:Keyword)<-[:HAS_KEYWORD]-(oldBook:Book)
                    WHERE oldBook.id < $bookId
                    WITH oldBook, count(DISTINCT k) AS shared
                    WHERE shared >= 2
                    WITH oldBook, shared
                    ORDER BY shared DESC
                    LIMIT 5
                    MATCH (src:Book {id: $bookId})
                    MERGE (src)-[r:CITES]->(oldBook)
                    SET r.weight = toFloat(shared) / $totalKeywords
                    RETURN count(r) AS created
                    """;
            Map<String, Object> params = new HashMap<>();
            params.put("bookId", book.getId());
            params.put("totalKeywords", totalKeywords);
            List<Long> result = neo4jRepository.query(cypher, params,
                    rec -> rec.get("created").asLong());
            long created = result.isEmpty() ? 0L : result.get(0);
            log.info("CITES 引用边构建完成: bookId={}, 创建 {} 条 (共享关键词≥2, 共 {} 关键词)",
                    book.getId(), created, totalKeywords);
        } catch (Exception e) {
            log.warn("CITES 引用边构建失败 bookId={}: {}", book.getId(), e.getMessage());
        }
    }

    /**
     * 批量 MERGE 同类实体节点 + Book→实体 关系（UNWIND 消除 N+1）.
     * <p>
     * Author / Keyword / Subject 三类实体的写入结构完全相同（仅 label / relType / 实体列表不同），
     * 提取此方法消除重复。
     *
     * @param label    目标节点标签（Author/Keyword/Subject）
     * @param relType  关系类型（AUTHORED_BY/HAS_KEYWORD/BELONGS_TO）
     * @param entities 实体列表（可为 null/空，方法内跳过）
     * @param bookId   源 Book 业务 ID
     */
    private void mergeEntities(String label, String relType, List<Entity> entities, Long bookId) {
        if (entities == null || entities.isEmpty()) return;
        List<String> names = entities.stream()
                .map(e -> normalizeName(e.getName()))
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        List<Map<String, Object>> rows = entities.stream()
                .filter(e -> StringUtils.hasText(e.getName()))
                .map(e -> Map.of("name", (Object) normalizeName(e.getName()),
                        "confidence", e.getConfidence() != null ? e.getConfidence() : 0.5))
                .collect(Collectors.toList());
        neo4jRepository.batchMergeNodes(label, "name", names);
        neo4jRepository.batchMergeRelationships("Book", "id", bookId, label, "name", relType, rows);
    }

    // ---- 工具方法 ----

    private String buildExtractionText(Book book) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(book.getTitle())) sb.append(book.getTitle()).append(" ");
        if (StringUtils.hasText(book.getDescription())) sb.append(book.getDescription()).append(" ");
        if (StringUtils.hasText(book.getKeywords())) sb.append(book.getKeywords());
        return sb.toString();
    }

    /**
     * 规范化实体名称：去除首尾空白，统一全角空格等.
     */
    private String normalizeName(String name) {
        return name.trim().replace('　', ' ');
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
