package com.library.core.event;

/**
 * 图书借出事件.
 * <p>
 * 由 {@code BorrowService} 在借书成功后发布，
 * {@code ESSyncListener} 异步监听 → 更新 ES 中 {@code avail_copies} 和 {@code borrow_count}。
 *
 * @param bookId 借出的图书 ID
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public record BookBorrowedEvent(Long bookId) {
}
