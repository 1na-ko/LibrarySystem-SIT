package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.library.android.model.BookDetailVO;
import com.library.android.model.BookRecommendVO;
import com.library.android.model.ReservationVO;
import com.library.android.repository.BookRepository;
import com.library.android.repository.ReservationRepository;
import com.library.android.testutil.ResultFactory;
import com.library.android.testutil.RxJava3SchedulerRule;
import com.library.android.ui.common.LoadingState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import io.reactivex.rxjava3.core.Single;

public class BookDetailViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();
    @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();

    private BookRepository bookRepo;
    private ReservationRepository reservationRepo;
    private BookDetailViewModel viewModel;

    @Before
    public void setUp() {
        bookRepo = mock(BookRepository.class);
        reservationRepo = mock(ReservationRepository.class);
        viewModel = new BookDetailViewModel(bookRepo, reservationRepo);
    }

    @Test
    public void loadBookDetail_success_shouldExposeDetailAndRelatedBooks() {
        BookDetailVO detail = mock(BookDetailVO.class);
        BookRecommendVO related = mock(BookRecommendVO.class);
        when(bookRepo.getBookDetail(anyLong()))
                .thenReturn(Single.just(ResultFactory.success(detail)));
        when(bookRepo.getRelatedBooks(anyLong(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(Collections.singletonList(related))));

        viewModel.loadBookDetail(1L);

        assertNotNull(viewModel.getBookDetail().getValue());
        assertEquals(LoadingState.CONTENT, viewModel.getLoadingState().getValue());
        assertEquals(1, viewModel.getRelatedBooks().getValue().size());
    }

    @Test
    public void loadBookDetail_apiFailure_shouldPostErrorAndEnterErrorState() {
        when(bookRepo.getBookDetail(anyLong()))
                .thenReturn(Single.just(ResultFactory.failure(404, "未找到")));
        when(bookRepo.getRelatedBooks(anyLong(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(Collections.emptyList())));

        viewModel.loadBookDetail(99L);

        assertEquals(LoadingState.ERROR, viewModel.getLoadingState().getValue());
        assertNotNull(viewModel.getErrorEvent().getValue());
    }

    @Test
    public void reserveBook_success_shouldEmitReserveSuccessAndRefreshDetail() {
        ReservationVO rsvn = mock(ReservationVO.class);
        when(rsvn.getQueuePosition()).thenReturn(2);
        when(reservationRepo.reserveBook(anyLong()))
                .thenReturn(Single.just(ResultFactory.success(rsvn)));
        // 后续 loadBookDetail 触发的 mock
        BookDetailVO detail = mock(BookDetailVO.class);
        when(bookRepo.getBookDetail(anyLong()))
                .thenReturn(Single.just(ResultFactory.success(detail)));
        when(bookRepo.getRelatedBooks(anyLong(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(Collections.emptyList())));

        viewModel.reserveBook(5L);

        assertNotNull(viewModel.getReserveSuccess().getValue());
        assertEquals(2, viewModel.getReserveSuccess().getValue().getQueuePosition());
        // reserve 成功后自动 reload 详情
        verify(bookRepo, times(1)).getBookDetail(5L);
        // reserving 流程结束 — 不能假定 LiveData 最终值（因为 reload 会再次触发其他状态），
        // 这里只做"曾经 true"的验证：验证 reserveSuccess 已 emit 即足够.
    }

    @Test
    public void reserveBook_apiFailure_shouldPostErrorAndNotReload() {
        when(reservationRepo.reserveBook(anyLong()))
                .thenReturn(Single.just(ResultFactory.failure(409, "已预约")));

        viewModel.reserveBook(5L);

        assertNull(viewModel.getReserveSuccess().getValue());
        assertNotNull(viewModel.getErrorEvent().getValue());
        verify(bookRepo, times(0)).getBookDetail(anyLong());
    }

    @Test
    public void reserveBook_concurrentCalls_shouldDebounceSecondCall() {
        // 第一次调用进入 reserving=true 后，第二次调用应被拒绝
        ReservationVO rsvn = mock(ReservationVO.class);
        when(reservationRepo.reserveBook(anyLong()))
                .thenReturn(Single.never());  // 永不完成，确保 reserving 维持 true

        viewModel.reserveBook(1L);
        // 第二次调用 — 由于 reserving=true 防抖，repository 不会被调用第二次
        viewModel.reserveBook(1L);

        verify(reservationRepo, times(1)).reserveBook(1L);
    }

    @Test
    public void loadBookDetail_relatedBooksFailure_shouldStillExposeDetail() {
        BookDetailVO detail = mock(BookDetailVO.class);
        when(bookRepo.getBookDetail(anyLong()))
                .thenReturn(Single.just(ResultFactory.success(detail)));
        // 相关推荐加载失败
        when(bookRepo.getRelatedBooks(anyLong(), anyInt()))
                .thenReturn(Single.error(new java.io.IOException("offline")));

        viewModel.loadBookDetail(1L);

        // 即使相关推荐失败，详情仍应正确返回
        assertNotNull(viewModel.getBookDetail().getValue());
        assertEquals(LoadingState.CONTENT, viewModel.getLoadingState().getValue());
    }
}
