package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.library.android.model.BookCreateRequest;
import com.library.android.model.BookDetailVO;
import com.library.android.model.BookUpdateRequest;
import com.library.android.model.CategoryVO;
import com.library.android.model.UserManageVO;
import com.library.android.repository.AdminRepository;
import com.library.android.repository.BookRepository;
import com.library.android.testutil.PageResults;
import com.library.android.testutil.ResultFactory;
import com.library.android.testutil.RxJava3SchedulerRule;
import com.library.android.ui.common.LoadingState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import io.reactivex.rxjava3.core.Single;

public class AdminViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();
    @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();

    private AdminRepository adminRepo;
    private BookRepository bookRepo;
    private AdminViewModel viewModel;

    @Before
    public void setUp() {
        adminRepo = mock(AdminRepository.class);
        bookRepo = mock(BookRepository.class);
        viewModel = new AdminViewModel(adminRepo, bookRepo);
    }

    @Test
    public void loadUsers_success_shouldEnterContentState() {
        UserManageVO u = mock(UserManageVO.class);
        when(adminRepo.listUsers(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.single(Collections.singletonList(u)))));

        viewModel.loadUsers(null, null, null);

        assertEquals(LoadingState.CONTENT, viewModel.getUserLoadingState().getValue());
        assertEquals(1, viewModel.getUserList().getValue().size());
    }

    @Test
    public void loadUsers_emptyData_shouldEnterEmptyState() {
        when(adminRepo.listUsers(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.single(Collections.emptyList()))));

        viewModel.loadUsers(null, null, null);

        assertEquals(LoadingState.EMPTY, viewModel.getUserLoadingState().getValue());
    }

    @Test
    public void updateUserStatus_success_shouldFlagTrue() {
        when(adminRepo.updateUserStatus(anyLong(), anyString()))
                .thenReturn(Single.just(ResultFactory.success(null)));

        viewModel.updateUserStatus(1L, "FROZEN");

        assertEquals(Boolean.TRUE, viewModel.getStatusUpdateResult().getValue());
    }

    @Test
    public void updateUserStatus_failure_shouldFlagFalse() {
        when(adminRepo.updateUserStatus(anyLong(), anyString()))
                .thenReturn(Single.error(new RuntimeException("403")));

        viewModel.updateUserStatus(1L, "DISABLED");

        assertEquals(Boolean.FALSE, viewModel.getStatusUpdateResult().getValue());
    }

    @Test
    public void createBook_success_shouldExposeCreatedBook() {
        BookDetailVO created = mock(BookDetailVO.class);
        when(adminRepo.createBook(any(BookCreateRequest.class)))
                .thenReturn(Single.just(ResultFactory.success(created)));

        viewModel.createBook(new BookCreateRequest());

        assertNotNull(viewModel.getCreatedBook().getValue());
    }

    @Test
    public void updateBook_success_shouldExposeUpdatedBook() {
        BookDetailVO updated = mock(BookDetailVO.class);
        when(adminRepo.updateBook(anyLong(), any(BookUpdateRequest.class)))
                .thenReturn(Single.just(ResultFactory.success(updated)));

        viewModel.updateBook(1L, new BookUpdateRequest());

        assertNotNull(viewModel.getUpdatedBook().getValue());
    }

    @Test
    public void deleteBook_success_shouldFlagTrue() {
        when(adminRepo.deleteBook(anyLong()))
                .thenReturn(Single.just(ResultFactory.success(null)));

        viewModel.deleteBook(1L);

        assertEquals(Boolean.TRUE, viewModel.getDeleteResult().getValue());
    }

    @Test
    public void loadCategoryTree_success_shouldExposeCategories() {
        CategoryVO c = mock(CategoryVO.class);
        when(bookRepo.getCategoryTree())
                .thenReturn(Single.just(ResultFactory.success(Collections.singletonList(c))));

        viewModel.loadCategoryTree();

        assertNotNull(viewModel.getCategoryTree().getValue());
        assertEquals(1, viewModel.getCategoryTree().getValue().size());
    }

    @Test
    public void loadCategoryTree_apiFailure_shouldPostError() {
        when(bookRepo.getCategoryTree())
                .thenReturn(Single.just(ResultFactory.failure(500, "boom")));

        viewModel.loadCategoryTree();

        assertNotNull(viewModel.getErrorEvent().getValue());
    }
}
