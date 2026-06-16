package com.library.core.event;

/**
 * 图书归还事件.
 * <p>
 * 由 {@code BorrowService} 在归还成功后发布：
 * <ul>
 *   <li>{@code ESSyncListener} 异步监听 → 更新 ES 中 {@code avail_copies} 和 {@code borrow_count}</li>
 *   <li>{@code ReservationNotifier} 异步监听 → 检查预约队列并通知排队首位读者</li>
 * </ul>
 *
 * @param bookId 归还的图书 ID
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public record BookReturnedEvent(Long bookId) {
}
