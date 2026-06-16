package com.library.core.event;

/**
 * 图书更新事件.
 * <p>
 * 由 AdminBookController 发布，{@code ESSyncListener} 异步监听 → 更新 Elasticsearch 文档。
 *
 * @param bookId 更新的图书 ID
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public record BookUpdatedEvent(Long bookId) {
}
