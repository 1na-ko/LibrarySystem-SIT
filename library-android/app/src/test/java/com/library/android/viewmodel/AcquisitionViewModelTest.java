package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.library.android.model.CategoryVO;
import com.library.android.model.DuplicateCheckResult;
import com.library.android.model.ElectronicResourceVO;
import com.library.android.model.GapAnalysisResult;
import com.library.android.model.NegotiationVO;
import com.library.android.model.PurchasePredictionVO;
import com.library.android.model.SupplierVO;
import com.library.android.repository.AcquisitionRepository;
import com.library.android.repository.BookRepository;
import com.library.android.testutil.ResultFactory;
import com.library.android.testutil.RxJava3SchedulerRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import io.reactivex.rxjava3.core.Single;

public class AcquisitionViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();
    @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();

    private AcquisitionRepository repository;
    private BookRepository bookRepository;
    private AcquisitionViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(AcquisitionRepository.class);
        bookRepository = mock(BookRepository.class);
        viewModel = new AcquisitionViewModel(repository, bookRepository);
    }

    @Test
    public void predictDemand_success_shouldExposePredictions() {
        PurchasePredictionVO p = mock(PurchasePredictionVO.class);
        when(repository.predictDemand(anyLong(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(Collections.singletonList(p))));

        viewModel.predictDemand(1L, 3);

        assertNotNull(viewModel.getPredictions().getValue());
        assertEquals(1, viewModel.getPredictions().getValue().size());
    }

    @Test
    public void checkDuplicate_success_shouldExposeResult() {
        DuplicateCheckResult result = mock(DuplicateCheckResult.class);
        when(repository.checkDuplicate(anyString(), anyString(), anyString()))
                .thenReturn(Single.just(ResultFactory.success(result)));

        viewModel.checkDuplicate("9787-x", "title", "author");

        assertNotNull(viewModel.getDuplicateResult().getValue());
    }

    @Test
    public void checkDuplicate_failure_shouldPostError() {
        when(repository.checkDuplicate(anyString(), anyString(), anyString()))
                .thenReturn(Single.just(ResultFactory.failure(500, "boom")));

        viewModel.checkDuplicate("a", "b", "c");

        assertNotNull(viewModel.getErrorEvent().getValue());
    }

    @Test
    public void analyzeGap_success_shouldExposeResult() {
        GapAnalysisResult result = mock(GapAnalysisResult.class);
        when(repository.analyzeGap(anyLong()))
                .thenReturn(Single.just(ResultFactory.success(result)));

        viewModel.analyzeGap(1L);

        assertNotNull(viewModel.getGapResult().getValue());
    }

    @Test
    public void createNegotiation_success_shouldExposeNegotiation() {
        NegotiationVO n = mock(NegotiationVO.class);
        when(repository.createNegotiation(anyLong(), anyLong()))
                .thenReturn(Single.just(ResultFactory.success(n)));

        viewModel.createNegotiation(1L, 2L);

        assertNotNull(viewModel.getNegotiationCreated().getValue());
    }

    @Test
    public void loadSuppliers_success_shouldExposeSuppliers() {
        SupplierVO s = mock(SupplierVO.class);
        when(repository.listSuppliers())
                .thenReturn(Single.just(ResultFactory.success(Collections.singletonList(s))));

        viewModel.loadSuppliers();

        assertEquals(1, viewModel.getSuppliers().getValue().size());
    }

    @Test
    public void loadResources_success_shouldExposeResources() {
        ElectronicResourceVO r = mock(ElectronicResourceVO.class);
        when(repository.listResources())
                .thenReturn(Single.just(ResultFactory.success(Collections.singletonList(r))));

        viewModel.loadResources();

        assertEquals(1, viewModel.getResources().getValue().size());
    }

    @Test
    public void loadCategories_success_shouldExposeCategories() {
        CategoryVO c = mock(CategoryVO.class);
        when(bookRepository.getCategoryTree())
                .thenReturn(Single.just(ResultFactory.success(Collections.singletonList(c))));

        viewModel.loadCategories();

        assertEquals(1, viewModel.getCategories().getValue().size());
    }
}
