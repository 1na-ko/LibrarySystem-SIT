package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.library.android.model.BorrowRecordVO;
import com.library.android.model.RenewResultVO;
import com.library.android.repository.BorrowRepository;
import com.library.android.testutil.ResultFactory;
import com.library.android.testutil.RxJava3SchedulerRule;
import com.library.android.ui.common.LoadingState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.reactivex.rxjava3.core.Single;

public class BorrowDetailViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();
    @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();

    private BorrowRepository repository;
    private BorrowDetailViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(BorrowRepository.class);
        viewModel = new BorrowDetailViewModel(repository);
    }

    @Test
    public void loadDetail_success_shouldExposeBorrowRecord() {
        BorrowRecordVO rec = mock(BorrowRecordVO.class);
        when(repository.getBorrowDetail(anyLong()))
                .thenReturn(Single.just(ResultFactory.success(rec)));

        viewModel.loadDetail(1L);

        assertNotNull(viewModel.getBorrowDetail().getValue());
    }

    @Test
    public void loadDetail_failure_shouldEnterErrorAndPostError() {
        when(repository.getBorrowDetail(anyLong()))
                .thenReturn(Single.error(new RuntimeException("offline")));

        viewModel.loadDetail(1L);

        assertEquals(LoadingState.ERROR, viewModel.getLoadingState().getValue());
        assertNotNull(viewModel.getErrorEvent().getValue());
    }

    @Test
    public void returnBook_success_shouldFlagReturnSuccessTrue() {
        when(repository.returnBook(anyLong()))
                .thenReturn(Single.just(ResultFactory.success(mock(BorrowRecordVO.class))));

        viewModel.returnBook(1L);

        assertEquals(Boolean.TRUE, viewModel.getReturnSuccess().getValue());
    }

    @Test
    public void returnBook_throwable_shouldFlagReturnSuccessFalse() {
        when(repository.returnBook(anyLong()))
                .thenReturn(Single.error(new RuntimeException("net")));

        viewModel.returnBook(1L);

        assertEquals(Boolean.FALSE, viewModel.getReturnSuccess().getValue());
    }

    @Test
    public void renewBook_success_shouldExposeNewDueDate() {
        RenewResultVO vo = mock(RenewResultVO.class);
        when(vo.getNewDueDate()).thenReturn("2026-07-30");
        when(repository.renewBook(anyLong()))
                .thenReturn(Single.just(ResultFactory.success(vo)));

        viewModel.renewBook(1L);

        assertEquals("2026-07-30", viewModel.getRenewResult().getValue());
    }
}
