package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.library.android.model.ReservationVO;
import com.library.android.repository.ReservationRepository;
import com.library.android.testutil.PageResults;
import com.library.android.testutil.ResultFactory;
import com.library.android.testutil.RxJava3SchedulerRule;
import com.library.android.ui.common.LoadingState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import io.reactivex.rxjava3.core.Single;

public class ReservationViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();
    @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();

    private ReservationRepository repository;
    private ReservationViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(ReservationRepository.class);
        viewModel = new ReservationViewModel(repository);
    }

    @Test
    public void loadReservations_success_shouldEnterContentState() {
        ReservationVO r = mock(ReservationVO.class);
        when(repository.getMyReservations(any(), anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.single(Collections.singletonList(r)))));

        viewModel.loadReservations("WAITING");

        assertEquals(LoadingState.CONTENT, viewModel.getLoadingState().getValue());
        assertEquals(1, viewModel.getReservationList().getValue().size());
    }

    @Test
    public void loadReservations_emptyData_shouldEnterEmptyState() {
        when(repository.getMyReservations(any(), anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.single(Collections.emptyList()))));

        viewModel.loadReservations(null);

        assertEquals(LoadingState.EMPTY, viewModel.getLoadingState().getValue());
    }

    @Test
    public void cancelReservation_success_shouldEmitTrue() {
        when(repository.cancelReservation(anyLong()))
                .thenReturn(Single.just(ResultFactory.success(null)));

        viewModel.cancelReservation(1L);

        assertEquals(Boolean.TRUE, viewModel.getCancelResult().getValue());
    }

    @Test
    public void cancelReservation_apiFailure_shouldEmitFalseAndPostError() {
        when(repository.cancelReservation(anyLong()))
                .thenReturn(Single.just(ResultFactory.failure(409, "不可取消")));

        viewModel.cancelReservation(1L);

        assertEquals(Boolean.FALSE, viewModel.getCancelResult().getValue());
        assertNotNull(viewModel.getErrorEvent().getValue());
    }

    @Test
    public void cancelReservation_throwable_shouldEmitFalseAndPostError() {
        when(repository.cancelReservation(anyLong()))
                .thenReturn(Single.error(new RuntimeException("net")));

        viewModel.cancelReservation(1L);

        assertEquals(Boolean.FALSE, viewModel.getCancelResult().getValue());
        assertNotNull(viewModel.getErrorEvent().getValue());
    }

    @Test
    public void cancelReservation_concurrentSameId_shouldDebounce() {
        // 防抖：同一 ID 第二次调用应被忽略
        when(repository.cancelReservation(anyLong())).thenReturn(Single.never());

        viewModel.cancelReservation(1L);
        viewModel.cancelReservation(1L);

        verify(repository, times(1)).cancelReservation(1L);
    }

    @Test
    public void queryQueuePosition_success_shouldExposePosition() {
        when(repository.getQueuePosition(anyLong()))
                .thenReturn(Single.just(ResultFactory.success(3)));

        viewModel.queryQueuePosition(1L);

        assertEquals(Integer.valueOf(3), viewModel.getQueuePosition().getValue());
    }
}
