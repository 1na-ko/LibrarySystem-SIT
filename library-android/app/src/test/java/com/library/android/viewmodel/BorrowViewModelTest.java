package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.library.android.model.BorrowRecordVO;
import com.library.android.model.BorrowResultVO;
import com.library.android.model.RenewResultVO;
import com.library.android.network.exception.BizConflictException;
import com.library.android.repository.BorrowRepository;
import com.library.android.testutil.PageResults;
import com.library.android.testutil.ResultFactory;
import com.library.android.testutil.RxJava3SchedulerRule;
import com.library.android.ui.common.LoadingState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import io.reactivex.rxjava3.core.Single;

public class BorrowViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();
    @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();

    private BorrowRepository repository;
    private BorrowViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(BorrowRepository.class);
        viewModel = new BorrowViewModel(repository);
    }

    @Test
    public void loadBorrows_success_shouldEnterContentStateWithRecords() {
        BorrowRecordVO r = mock(BorrowRecordVO.class);
        when(repository.getMyBorrows(any(), anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.single(Collections.singletonList(r)))));

        viewModel.loadBorrows("BORROWED");

        assertEquals(LoadingState.CONTENT, viewModel.getLoadingState().getValue());
        assertEquals(1, viewModel.getBorrowList().getValue().size());
    }

    @Test
    public void loadBorrows_emptyData_shouldEnterEmptyState() {
        when(repository.getMyBorrows(any(), anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.single(Collections.emptyList()))));

        viewModel.loadBorrows(null);

        assertEquals(LoadingState.EMPTY, viewModel.getLoadingState().getValue());
    }

    @Test
    public void loadBorrows_apiError_shouldEnterErrorStateAndPostError() {
        when(repository.getMyBorrows(any(), anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.failure(500, "boom")));

        viewModel.loadBorrows(null);

        assertEquals(LoadingState.ERROR, viewModel.getLoadingState().getValue());
        assertNotNull(viewModel.getErrorEvent().getValue());
    }

    @Test
    public void borrowBook_success_shouldClearErrorMessageAndReturnTrue() {
        BorrowResultVO vo = mock(BorrowResultVO.class);
        when(repository.borrowBook(anyLong()))
                .thenReturn(Single.just(ResultFactory.success(vo)));

        Boolean result = viewModel.borrowBook(1L).getValue();

        assertEquals(Boolean.TRUE, result);
        assertNull(viewModel.getBorrowErrorMessage().getValue());
    }

    @Test
    public void borrowBook_apiFailure_shouldExposeBackendMessage() {
        when(repository.borrowBook(anyLong()))
                .thenReturn(Single.just(ResultFactory.failure(409, "您已借阅该书")));

        Boolean result = viewModel.borrowBook(1L).getValue();

        assertEquals(Boolean.FALSE, result);
        assertEquals("您已借阅该书", viewModel.getBorrowErrorMessage().getValue());
    }

    @Test
    public void borrowBook_bizConflictException_shouldExposeServerMessage() {
        BizConflictException ex = new BizConflictException("库存不足");
        when(repository.borrowBook(anyLong())).thenReturn(Single.error(ex));

        Boolean result = viewModel.borrowBook(1L).getValue();

        assertEquals(Boolean.FALSE, result);
        assertEquals("库存不足", viewModel.getBorrowErrorMessage().getValue());
    }

    @Test
    public void renewBook_success_shouldExposeNewDueDate() {
        RenewResultVO vo = mock(RenewResultVO.class);
        when(vo.getNewDueDate()).thenReturn("2026-07-30");
        when(repository.renewBook(anyLong()))
                .thenReturn(Single.just(ResultFactory.success(vo)));

        String date = viewModel.renewBook(1L).getValue();
        assertEquals("2026-07-30", date);
    }

    @Test
    public void renewBook_apiFailure_shouldReturnNull() {
        when(repository.renewBook(anyLong()))
                .thenReturn(Single.just(ResultFactory.failure(409, "已超期")));

        String date = viewModel.renewBook(1L).getValue();
        assertNull(date);
    }

    @Test
    public void returnBook_success_shouldReturnTrue() {
        BorrowRecordVO rec = mock(BorrowRecordVO.class);
        when(repository.returnBook(anyLong()))
                .thenReturn(Single.just(ResultFactory.success(rec)));

        Boolean result = viewModel.returnBook(1L).getValue();
        assertEquals(Boolean.TRUE, result);
    }
}
