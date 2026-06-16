package com.library.core.event;

/**
 * 图书删除事件.
 * <p>
 * 由 AdminBookController 发布，{@code ESSyncListener} 异步监听 → 从 Elasticsearch 删除文档。
 *
 * @param bookId 被删除的图书 ID
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public record BookDeletedEvent(Long bookId) {
}
