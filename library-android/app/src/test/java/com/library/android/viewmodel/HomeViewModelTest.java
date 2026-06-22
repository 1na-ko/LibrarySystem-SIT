package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.library.android.model.BookSimpleVO;
import com.library.android.model.CategoryVO;
import com.library.android.repository.BookRepository;
import com.library.android.repository.UserRepository;
import com.library.android.testutil.ResultFactory;
import com.library.android.testutil.RxJava3SchedulerRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import io.reactivex.rxjava3.core.Single;

public class HomeViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();
    @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();

    private UserRepository userRepository;
    private BookRepository bookRepository;
    private HomeViewModel viewModel;

    @Before
    public void setUp() {
        userRepository = mock(UserRepository.class);
        bookRepository = mock(BookRepository.class);
        viewModel = new HomeViewModel(userRepository, bookRepository);
    }

    @Test
    public void loadHotBooks_success_shouldUpdateLiveData() {
        BookSimpleVO b = mock(BookSimpleVO.class);
        when(bookRepository.getHotBooks(any(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(Collections.singletonList(b))));

        viewModel.loadHotBooks(5);

        assertNotNull(viewModel.getHotBooks().getValue());
        assertEquals(1, viewModel.getHotBooks().getValue().size());
    }

    @Test
    public void loadHotBooks_apiFailure_shouldPostError() {
        when(bookRepository.getHotBooks(any(), anyInt()))
                .thenReturn(Single.just(ResultFactory.failure(500, "服务异常")));

        viewModel.loadHotBooks(5);

        assertNull(viewModel.getHotBooks().getValue());
        assertNotNull(viewModel.getErrorEvent().getValue());
    }

    @Test
    public void loadCategoryTree_success_shouldExposeCategories() {
        CategoryVO c = mock(CategoryVO.class);
        when(bookRepository.getCategoryTree())
                .thenReturn(Single.just(ResultFactory.success(Arrays.asList(c, c))));

        viewModel.loadCategoryTree();

        assertNotNull(viewModel.getCategories().getValue());
        assertEquals(2, viewModel.getCategories().getValue().size());
    }

    @Test
    public void loadCategoryTree_networkError_shouldPostError() {
        when(bookRepository.getCategoryTree())
                .thenReturn(Single.error(new java.io.IOException("offline")));

        viewModel.loadCategoryTree();

        assertNotNull(viewModel.getErrorEvent().getValue());
    }
}
