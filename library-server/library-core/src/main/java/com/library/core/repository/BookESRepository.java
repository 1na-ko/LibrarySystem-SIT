package com.library.core.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.library.common.result.PageResult;
import com.library.core.config.EsIndexInitializer;
import com.library.core.dto.BookAdvancedSearchDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;

/**
 * Elasticsearch 图书数据访问层.
 * <p>
 * 封装 ES 客户端操作：索引/删除文档、全文搜索、高级搜索、自动补全、热门图书。
 * 所有搜索方法在 ES 不可用时降级返回空结果，不抛异常。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BookESRepository {

    private final ElasticsearchClient esClient;

    /**
     * 索引（创建或全量替换）图书文档.
     * <p>
     * ES IO 异常包装为 RuntimeException 向上传播，由调用方（{@code ESSyncListener}）负责重试。
     */
    public void save(BookDocument doc) {
        try {
            esClient.index(i -> i
                    .index(EsIndexInitializer.BOOKS_INDEX)
                    .id(String.valueOf(doc.getId()))
                    .document(doc)
            );
            log.debug("ES 文档已索引: bookId={}", doc.getId());
        } catch (IOException e) {
            throw new UncheckedIOException("ES 索引失败: bookId=" + doc.getId(), e);
        }
    }

    /**
     * 从 ES 删除图书文档.
     * <p>
     * ES IO 异常包装为 RuntimeException 向上传播，由调用方（{@code ESSyncListener}）负责重试。
     */
    public void delete(Long bookId) {
        try {
            esClient.delete(d -> d
                    .index(EsIndexInitializer.BOOKS_INDEX)
                    .id(String.valueOf(bookId))
            );
            log.debug("ES 文档已删除: bookId={}", bookId);
        } catch (IOException e) {
            throw new UncheckedIOException("ES 删除失败: bookId=" + bookId, e);
        }
    }

    /**
     * 全文搜索.
     * <p>
     * 使用 {@code multi_match} 跨 title(^3)/author(^2)/keywords(^2)/description 匹配，
     * 叠加 {@code function_score} 对 borrowCount 做 field_value_factor 加权，
     * 既奖励热门书又避免完全被热度主导排序。
     *
     * @param keyword    搜索关键词
     * @param author     作者筛选（可选）
     * @param categoryId 分类筛选（可选）
     * @param sortBy     排序方式: relevance / borrowCount / pubDate
     * @param pageNum    页码（1-based）
     * @param pageSize   每页条数
     * @return 分页结果（records 为 ES 返回的 bookId 列表，需调用方转为 VO）
     */
    public PageResult<Long> fullTextSearch(String keyword, String author, Long categoryId,
                                           String sortBy, int pageNum, int pageSize) {
        if (!StringUtils.hasText(keyword)) {
            return PageResult.empty(pageNum, pageSize);
        }

        try {
            int from = (pageNum - 1) * pageSize;

            SearchResponse<BookDocument> response = esClient.search(s -> s
                    .index(EsIndexInitializer.BOOKS_INDEX)
                    .from(from)
                    .size(pageSize)
                    .query(q -> q
                            .functionScore(fs -> fs
                                    .query(inner -> inner
                                            .bool(b -> {
                                                b.must(m -> m.multiMatch(mm -> mm
                                                        .query(keyword)
                                                        .fields("title^3", "author^2", "keywords^2", "description")
                                                ));
                                                if (StringUtils.hasText(author)) {
                                                    b.filter(f -> f.term(t -> t.field("author.keyword").value(author)));
                                                }
                                                if (categoryId != null) {
                                                    b.filter(f -> f.term(t -> t.field("categoryId").value(categoryId)));
                                                }
                                                return b;
                                            })
                                    )
                                    .functions(fn -> fn
                                            .fieldValueFactor(fvf -> fvf
                                                    .field("borrowCount")
                                                    .factor(1.0)
                                                    .modifier(co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier.Log1p)
                                                    .missing(0.0)
                                            )
                                    )
                                    .boostMode(FunctionBoostMode.Multiply)
                                    .scoreMode(FunctionScoreMode.Multiply)
                            )
                    )
                    .sort(sortBuilder -> {
                        if ("borrowCount".equals(sortBy)) {
                            sortBuilder.field(f -> f.field("borrowCount").order(SortOrder.Desc));
                        } else if ("pubDate".equals(sortBy)) {
                            sortBuilder.field(f -> f.field("pubDate").order(SortOrder.Desc));
                        }
                        return sortBuilder;
                    })
                    .trackTotalHits(th -> th.enabled(true)),
                    BookDocument.class
            );

            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            List<Long> bookIds = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(doc -> doc != null)
                    .map(BookDocument::getId)
                    .toList();

            return PageResult.of(bookIds, total, pageNum, pageSize);
        } catch (Exception e) {
            log.warn("ES 全文搜索失败: keyword={}, error={}", keyword, e.getMessage());
            return PageResult.empty(pageNum, pageSize);
        }
    }

    /**
     * 高级组合搜索.
     * <p>
     * 使用 {@code bool} query 组合多个 {@code must}/{@code filter} 子句，
     * 各字段按数值精确或文本模糊匹配。
     */
    public PageResult<Long> advancedSearch(BookAdvancedSearchDTO dto) {
        try {
            int pageNum = dto.getPageNum() != null ? dto.getPageNum() : 1;
            int pageSize = dto.getPageSize() != null ? dto.getPageSize() : 20;
            int from = (pageNum - 1) * pageSize;

            SearchResponse<BookDocument> response = esClient.search(s -> s
                    .index(EsIndexInitializer.BOOKS_INDEX)
                    .from(from)
                    .size(pageSize)
                    .query(q -> q
                            .bool(b -> {
                                if (StringUtils.hasText(dto.getTitle())) {
                                    b.must(m -> m.match(ma -> ma.field("title").query(dto.getTitle())));
                                }
                                if (StringUtils.hasText(dto.getAuthor())) {
                                    b.must(m -> m.term(t -> t.field("author.keyword").value(dto.getAuthor())));
                                }
                                if (StringUtils.hasText(dto.getIsbn())) {
                                    b.must(m -> m.term(t -> t.field("isbn").value(dto.getIsbn())));
                                }
                                if (StringUtils.hasText(dto.getPublisher())) {
                                    b.must(m -> m.match(ma -> ma.field("publisher").query(dto.getPublisher())));
                                }
                                if (dto.getPubYearFrom() != null || dto.getPubYearTo() != null) {
                                    b.filter(f -> f.range(r -> {
                                        r.field("pubDate");
                                        if (dto.getPubYearFrom() != null) {
                                            r.gte(JsonData.of(dto.getPubYearFrom() + "-01-01"));
                                        }
                                        if (dto.getPubYearTo() != null) {
                                            r.lte(JsonData.of(dto.getPubYearTo() + "-12-31"));
                                        }
                                        return r;
                                    }));
                                }
                                if (dto.getCategoryId() != null) {
                                    b.filter(f -> f.term(t -> t.field("categoryId").value(dto.getCategoryId())));
                                }
                                if (Boolean.TRUE.equals(dto.getOnlyAvailable())) {
                                    b.filter(f -> f.range(r -> r.field("availCopies").gt(JsonData.of(0))));
                                }
                                return b;
                            })
                    )
                    .sort(sortBuilder -> sortBuilder.field(f -> f.field("borrowCount").order(SortOrder.Desc)))
                    .trackTotalHits(th -> th.enabled(true)),
                    BookDocument.class
            );

            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            List<Long> bookIds = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(doc -> doc != null)
                    .map(BookDocument::getId)
                    .toList();

            return PageResult.of(bookIds, total, pageNum, pageSize);
        } catch (Exception e) {
            log.warn("ES 高级搜索失败: error={}", e.getMessage());
            return PageResult.empty(
                    dto.getPageNum() != null ? dto.getPageNum() : 1,
                    dto.getPageSize() != null ? dto.getPageSize() : 20);
        }
    }

    /**
     * 搜索自动补全.
     * <p>
     * 使用 ES Completion Suggester，基于 {@code suggest} 字段。
     *
     * @param prefix 输入前缀
     * @param limit  返回条数上限
     * @return 补全建议文本列表
     */
    public List<String> suggest(String prefix, int limit) {
        if (!StringUtils.hasText(prefix)) {
            return Collections.emptyList();
        }

        try {
            var response = esClient.search(s -> s
                    .index(EsIndexInitializer.BOOKS_INDEX)
                    .suggest(sug -> sug
                            .suggesters("book-suggest", ss -> ss
                                    .prefix(prefix)
                                    .completion(c -> c
                                            .field("suggest")
                                            .size(limit)
                                            .skipDuplicates(true)
                                    )
                            )
                    ),
                    BookDocument.class
            );

            if (response.suggest() == null || !response.suggest().containsKey("book-suggest")) {
                return Collections.emptyList();
            }

            return response.suggest().get("book-suggest").stream()
                    .flatMap(s -> s.completion().options().stream())
                    .map(CompletionSuggestOption::text)
                    .filter(text -> text != null)
                    .distinct()
                    .limit(limit)
                    .toList();

        } catch (Exception e) {
            log.warn("ES 自动补全失败: prefix={}, error={}", prefix, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 热门图书榜.
     * <p>
     * 按 {@code borrowCount} 降序排列，可选按分类筛选。
     *
     * @param categoryId 分类筛选（可选）
     * @param limit      返回条数
     * @return 图书 ID 列表
     */
    public List<Long> hotBooks(Long categoryId, int limit) {
        try {
            SearchResponse<BookDocument> response = esClient.search(s -> {
                s.index(EsIndexInitializer.BOOKS_INDEX)
                        .size(limit)
                        .sort(sort -> sort.field(f -> f.field("borrowCount").order(SortOrder.Desc)))
                        .trackTotalHits(th -> th.enabled(true));
                if (categoryId != null) {
                    s.query(q -> q.term(t -> t.field("categoryId").value(categoryId)));
                }
                return s;
            }, BookDocument.class);

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(doc -> doc != null)
                    .map(BookDocument::getId)
                    .toList();
        } catch (Exception e) {
            log.warn("ES 热门图书查询失败: error={}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
