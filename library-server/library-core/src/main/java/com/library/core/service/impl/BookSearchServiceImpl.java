package com.library.core.service.impl;

import com.library.common.result.PageResult;
import com.library.core.dto.BookAdvancedSearchDTO;
import com.library.core.dto.BookSearchDTO;
import com.library.core.repository.BookESRepository;
import com.library.core.service.BookSearchService;
import com.library.core.service.BookService;
import com.library.core.vo.BookSimpleVO;
import com.library.core.vo.SuggestVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 图书搜索服务实现.
 * <p>
 * 搜索流程：Redis 缓存（TTL 30min）→ ES 搜索 → 回写缓存。
 * ES 不可用时降级返回空结果，不抛异常。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookSearchServiceImpl implements BookSearchService {

    private final BookESRepository bookESRepository;
    private final BookService bookService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final long CACHE_TTL_MINUTES = 30;
    private static final String CACHE_KEY_PREFIX = "search:";

    @Override
    public PageResult<BookSimpleVO> search(BookSearchDTO dto) {
        // 仅对"relevance"排序尝试缓存热点词
        if ("relevance".equals(dto.getSortBy()) || dto.getSortBy() == null) {
            String cacheKey = buildCacheKey(dto);
            PageResult<BookSimpleVO> cached = getCachedResult(cacheKey);
            if (cached != null) {
                log.debug("搜索缓存命中: {}", cacheKey);
                return cached;
            }

            PageResult<BookSimpleVO> result = doSearch(dto);
            if (result.getTotal() > 0) {
                cacheResult(cacheKey, result);
            }
            return result;
        }

        // 自定义排序（borrowCount/pubDate）不缓存
        return doSearch(dto);
    }

    @Override
    public PageResult<BookSimpleVO> advancedSearch(BookAdvancedSearchDTO dto) {
        PageResult<Long> idResult = bookESRepository.advancedSearch(dto);
        return convertToBookVO(idResult);
    }

    @Override
    public List<SuggestVO> suggest(String prefix, int limit) {
        if (!StringUtils.hasText(prefix)) {
            return Collections.emptyList();
        }
        List<String> texts = bookESRepository.suggest(prefix, limit);
        return texts.stream()
                .map(text -> SuggestVO.builder().text(text).type("book").build())
                .toList();
    }

    @Override
    public List<BookSimpleVO> hotBooks(Long categoryId, int limit) {
        List<Long> bookIds = bookESRepository.hotBooks(categoryId, limit);
        if (bookIds.isEmpty()) {
            return Collections.emptyList();
        }
        return bookService.listByIds(bookIds);
    }

    /**
     * 执行 ES 搜索并转换为 VO.
     */
    private PageResult<BookSimpleVO> doSearch(BookSearchDTO dto) {
        PageResult<Long> idResult = bookESRepository.fullTextSearch(
                dto.getKeyword(), dto.getAuthor(), dto.getCategoryId(),
                dto.getSortBy(), dto.getPageNum(), dto.getPageSize());
        return convertToBookVO(idResult);
    }

    /**
     * 将 bookId 分页结果转换为 BookSimpleVO 分页结果.
     */
    private PageResult<BookSimpleVO> convertToBookVO(PageResult<Long> idResult) {
        if (idResult.getRecords().isEmpty()) {
            return PageResult.of(Collections.emptyList(), idResult.getTotal(),
                    idResult.getPageNum(), idResult.getPageSize());
        }
        List<BookSimpleVO> vos = bookService.listByIds(idResult.getRecords());
        return PageResult.of(vos, idResult.getTotal(), idResult.getPageNum(), idResult.getPageSize());
    }

    /**
     * 构建 Redis 缓存 Key.
     */
    private String buildCacheKey(BookSearchDTO dto) {
        return CACHE_KEY_PREFIX + dto.getKeyword() + ":" +
                dto.getPageNum() + ":" + dto.getPageSize() + ":" +
                (dto.getAuthor() != null ? dto.getAuthor() : "") + ":" +
                (dto.getCategoryId() != null ? dto.getCategoryId() : "");
    }

    /**
     * 从 Redis 读取缓存.
     */
    @SuppressWarnings("unchecked")
    private PageResult<BookSimpleVO> getCachedResult(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof PageResult) {
                return (PageResult<BookSimpleVO>) cached;
            }
        } catch (Exception e) {
            log.warn("读取搜索缓存失败（Redis 不可用）: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 将结果写入 Redis 缓存.
     */
    private void cacheResult(String cacheKey, PageResult<BookSimpleVO> result) {
        try {
            redisTemplate.opsForValue().set(cacheKey, result, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("搜索结果已缓存: {}", cacheKey);
        } catch (Exception e) {
            log.warn("写入搜索缓存失败（Redis 不可用）: {}", e.getMessage());
        }
    }

    /**
     * 清除全部搜索缓存.
     * <p>
     * 图书变更（新增/修改/删除）时调用，通过 Redis SCAN 匹配 {@code search:*} 键并批量删除。
     * Redis 不可用时静默降级，缓存将在 TTL（30min）后自然过期。
     */
    public void evictAllSearchCache() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("搜索缓存已全局清除: {} 个键", keys.size());
            }
        } catch (Exception e) {
            log.warn("清除搜索缓存失败（Redis 不可用，缓存在 TTL 后自动过期）: {}", e.getMessage());
        }
    }
}
