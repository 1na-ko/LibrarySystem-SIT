package com.library.core.controller;

import com.library.common.result.PageResult;
import com.library.common.result.Result;
import com.library.core.dto.BookAdvancedSearchDTO;
import com.library.core.dto.BookSearchDTO;
import com.library.core.service.BookSearchService;
import com.library.core.service.BookService;
import com.library.core.service.RelatedBookService;
import com.library.core.vo.BookDetailVO;
import com.library.core.vo.BookRecommendVO;
import com.library.core.vo.BookSimpleVO;
import com.library.core.vo.SuggestVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 图书检索控制器.
 * <p>
 * 提供关键词搜索、高级搜索、自动补全、热门图书、图书详情和相关图书 6 个端点。
 * 所有认证用户均可访问。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookSearchService bookSearchService;
    private final BookService bookService;
    private final RelatedBookService relatedBookService;

    /**
     * 关键词全文搜索.
     * <p>
     * 支持按书名、作者、关键词的全文搜索，默认按相关度排序（BM25 + 借阅热度加权）。
     * 高并发搜索使用 Redis 缓存热点词（TTL 30min）。
     *
     * @param dto 搜索请求参数（keyword 必填）
     */
    @GetMapping("/search")
    public Result<PageResult<BookSimpleVO>> search(@Valid BookSearchDTO dto) {
        return Result.success(bookSearchService.search(dto));
    }

    /**
     * 高级组合搜索.
     * <p>
     * 多字段组合精确/模糊搜索，支持出版年份范围、仅显示有库存等筛选。
     */
    @GetMapping("/search/advanced")
    public Result<PageResult<BookSimpleVO>> advancedSearch(@Valid BookAdvancedSearchDTO dto) {
        return Result.success(bookSearchService.advancedSearch(dto));
    }

    /**
     * 搜索自动补全.
     * <p>
     * 根据用户输入前缀，基于 ES Completion Suggester 返回补全建议。
     *
     * @param prefix 输入前缀（必填）
     * @param limit  返回条数（默认 10，最大 20）
     */
    @GetMapping("/suggest")
    public Result<List<SuggestVO>> suggest(@RequestParam String prefix,
                                           @RequestParam(defaultValue = "10") int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), 20);
        return Result.success(bookSearchService.suggest(prefix, boundedLimit));
    }

    /**
     * 热门图书榜.
     * <p>
     * 按借阅次数降序排列，可选按分类筛选。
     *
     * @param categoryId 分类筛选（可选）
     * @param limit      返回条数（默认 10，最大 50）
     */
    @GetMapping("/hot")
    public Result<List<BookSimpleVO>> hotBooks(@RequestParam(required = false) Long categoryId,
                                               @RequestParam(defaultValue = "10") int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), 50);
        return Result.success(bookSearchService.hotBooks(categoryId, boundedLimit));
    }

    /**
     * 图书详情.
     * <p>
     * 获取图书完整信息，含关键词列表、相关图书推荐、当前预约人数。
     *
     * @param id 图书 ID
     */
    @GetMapping("/{id}")
    public Result<BookDetailVO> getDetail(@PathVariable Long id) {
        // 图书详情（含 reservationCount，由 BookService 填充）
        BookDetailVO vo = bookService.getDetail(id);

        // 补充相关图书（RelatedBookService 是合理的 Service 层调用）
        List<BookRecommendVO> related = relatedBookService.getRelated(id, 10);
        List<BookSimpleVO> relatedBooks = related.stream()
                .map(BookRecommendVO::getBook)
                .toList();
        vo.setRelatedBooks(relatedBooks);

        return Result.success(vo);
    }

    /**
     * 相关图书推荐.
     * <p>
     * 基于同分类/同作者的图书推荐（KG 模块未就绪时的降级策略）。
     *
     * @param id    目标图书 ID
     * @param limit 返回条数（默认 10，最大 20）
     */
    @GetMapping("/{id}/related")
    public Result<List<BookRecommendVO>> getRelated(@PathVariable Long id,
                                                     @RequestParam(defaultValue = "10") int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), 20);
        return Result.success(relatedBookService.getRelated(id, boundedLimit));
    }
}
