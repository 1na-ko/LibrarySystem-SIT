package com.library.core.event;

/**
 * 图书新增事件.
 * <p>
 * 由 AdminBookController 发布，{@code ESSyncListener} 异步监听 → 同步至 Elasticsearch。
 *
 * @param bookId 新增的图书 ID
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public record BookCreatedEvent(Long bookId) {
}
