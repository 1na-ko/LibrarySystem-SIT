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
import com.library.kg.dto.RelationList;
import com.library.kg.dto.RelationList.Relation;
import com.library.kg.repository.Neo4jRepository;
import com.library.kg.service.GraphBuildService;
import lombok.extern.slf4j.Slf4j;
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
 * NER（实体识别）→ RE（关系抽取）→ MERGE（Neo4j 写入）→ 实体对齐。
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

    public GraphBuildServiceImpl(
            Neo4jRepository neo4jRepository,
            NlpService nlpService,
            BookMapper bookMapper,
            CategoryMapper categoryMapper,
            KnowledgeGraphProperties kgProperties,
            @Autowired(required = false) LlmService llmService) {
        this.neo4jRepository = neo4jRepository;
        this.nlpService = nlpService;
        this.bookMapper = bookMapper;
        this.categoryMapper = categoryMapper;
        this.kgProperties = kgProperties;
        this.llmService = llmService;
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

            // 2. 关系抽取
            List<Relation> relations = extractRelations(book, entities);

            // 3. 写入 Neo4j
            writeToNeo4j(book, entities, relations);

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

    // ---- RE ----

    private List<Relation> extractRelations(Book book, BookEntityList entities) {
        if (llmService != null && hasAnyEntity(entities)) {
            try {
                String prompt = buildRePrompt(book, entities);
                RelationList result = llmService.chat(prompt, RelationList.class);
                if (result != null && result.getRelations() != null && !result.getRelations().isEmpty()) {
                    return result.getRelations();
                }
            } catch (LlmUnavailableException e) {
                log.warn("LLM RE 失败，降级共现统计: {}", e.getMessage());
            } catch (Exception e) {
                log.warn("LLM RE 异常，降级共现统计: {}", e.getMessage());
            }
        }
        return fallbackRe(book, entities);
    }

    private List<Relation> fallbackRe(Book book, BookEntityList entities) {
        List<Relation> relations = new ArrayList<>();
        // 作者 → 书：AUTHORED_BY
        if (entities.getAuthors() != null) {
            for (Entity author : entities.getAuthors()) {
                relations.add(new Relation(author.getName(), book.getTitle(), "AUTHORED_BY", 0.9));
            }
        }
        // 关键词 → 书：HAS_KEYWORD
        if (entities.getKeywords() != null) {
            for (Entity kw : entities.getKeywords()) {
                relations.add(new Relation(kw.getName(), book.getTitle(), "HAS_KEYWORD", kw.getConfidence() != null ? kw.getConfidence() : 0.7));
            }
        }
        // 书 → 学科：BELONGS_TO
        if (entities.getSubjects() != null) {
            for (Entity subj : entities.getSubjects()) {
                relations.add(new Relation(book.getTitle(), subj.getName(), "BELONGS_TO", subj.getConfidence() != null ? subj.getConfidence() : 0.9));
            }
        }
        return relations;
    }

    private String buildRePrompt(Book book, BookEntityList entities) {
        return """
                你是一位知识图谱关系抽取专家。给定一本图书和已识别的实体列表，
                请判断哪些实体与该图书之间存在关系。

                ## 图书
                - 书名：%s

                ## 已识别实体
                - 作者：%s
                - 关键词：%s
                - 学科：%s

                ## 关系类型
                - AUTHORED_BY：某作者撰写了本书
                - BELONGS_TO：本书属于某学科
                - HAS_KEYWORD：本书包含某关键词

                ## 要求
                请以 JSON 格式返回关系列表（relations，不要输出其他内容）：
                每项含 source（实体名）、target（图书名或实体名）、type、weight（0-1）
                例如：{"source":"周志明","target":"深入理解Java虚拟机","type":"AUTHORED_BY","weight":1.0}
                """.formatted(
                        nullToEmpty(book.getTitle()),
                        entitiesListStr(entities.getAuthors()),
                        entitiesListStr(entities.getKeywords()),
                        entitiesListStr(entities.getSubjects()));
    }

    // ---- Neo4j 写入 ----

    private void writeToNeo4j(Book book, BookEntityList entities, List<Relation> relations) {
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

    private String entitiesListStr(List<Entity> entities) {
        if (entities == null || entities.isEmpty()) return "无";
        return entities.stream()
                .map(e -> e.getName() + (e.getConfidence() != null ? "(" + e.getConfidence() + ")" : ""))
                .collect(Collectors.joining("、"));
    }
}
