package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.CategoryVO;
import com.library.android.model.DuplicateCheckResult;
import com.library.android.model.ElectronicResourceVO;
import com.library.android.model.GapAnalysisResult;
import com.library.android.model.NegotiationSuggestion;
import com.library.android.model.NegotiationVO;
import com.library.android.model.PurchasePredictionVO;
import com.library.android.model.SupplierVO;
import com.library.android.repository.AcquisitionRepository;
import com.library.android.repository.BookRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 智能采编共享 ViewModel（C.1 新增）— 5 个端点的统一入口.
 *
 * <p>注：合并到单 ViewModel 是出于简化考虑（5 个子页都仅做"查询→展示"）。
 * 若后续业务复杂化，再拆分为独立 ViewModel.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class AcquisitionViewModel extends BaseViewModel {

    private static final String TAG = "AcquisitionVM";

    private final AcquisitionRepository repository;
    private final BookRepository bookRepository;

    private final MutableLiveData<List<PurchasePredictionVO>> predictions = new MutableLiveData<>();
    private final MutableLiveData<DuplicateCheckResult> duplicateResult = new MutableLiveData<>();
    private final MutableLiveData<GapAnalysisResult> gapResult = new MutableLiveData<>();
    private final MutableLiveData<NegotiationVO> negotiationCreated = new MutableLiveData<>();
    private final MutableLiveData<NegotiationSuggestion> negotiationSuggestion = new MutableLiveData<>();

    // WP2.3-2.4：下拉选择器数据（Fragment 不再直接注入 Repository）
    private final MutableLiveData<List<SupplierVO>> suppliers = new MutableLiveData<>();
    private final MutableLiveData<List<ElectronicResourceVO>> resources = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryVO>> categories = new MutableLiveData<>();

    // WP6 流式 LiveData（WP2.6：PriceRange 统一为 NegotiationSuggestion.PriceRange）
    private final MutableLiveData<NegotiationSuggestion.PriceRange> priceRange = new MutableLiveData<>();
    private final MutableLiveData<String> suggestionText = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> suggestionStreaming = new MutableLiveData<>(false);
    private final StringBuilder textBuf = new StringBuilder();
    /** WP-11：记录已流式加载完成的 negotiationId，避免返回再进入重复触发 AI. */
    private long loadedSuggestionNegotiationId = 0L;

    @Inject
    public AcquisitionViewModel(AcquisitionRepository repository, BookRepository bookRepository) {
        this.repository = repository;
        this.bookRepository = bookRepository;
    }

    public LiveData<List<PurchasePredictionVO>> getPredictions() { return predictions; }
    public LiveData<DuplicateCheckResult> getDuplicateResult() { return duplicateResult; }
    public LiveData<GapAnalysisResult> getGapResult() { return gapResult; }
    public LiveData<NegotiationVO> getNegotiationCreated() { return negotiationCreated; }
    public LiveData<NegotiationSuggestion> getNegotiationSuggestion() { return negotiationSuggestion; }
    public LiveData<NegotiationSuggestion.PriceRange> getPriceRange() { return priceRange; }
    public LiveData<String> getSuggestionText() { return suggestionText; }
    public LiveData<Boolean> isSuggestionStreaming() { return suggestionStreaming; }

    /** WP-11：是否已为该 negotiationId 流式加载过建议（已完成）。 */
    public boolean isSuggestionLoadedFor(long negotiationId) {
        if (loadedSuggestionNegotiationId != negotiationId) return false;
        String text = suggestionText.getValue();
        return text != null && !text.isEmpty();
    }

    // WP2.3-2.4：下拉选择器数据加载
    public LiveData<List<SupplierVO>> getSuppliers() { return suppliers; }
    public LiveData<List<ElectronicResourceVO>> getResources() { return resources; }
    public LiveData<List<CategoryVO>> getCategories() { return categories; }

    public void loadSuppliers() {
        disposables.add(repository.listSuppliers()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result.isSuccess()) suppliers.setValue(result.getData());
                            else postError(new RuntimeException(result.getMessage()));
                        },
                        this::postError));
    }

    public void loadResources() {
        disposables.add(repository.listResources()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result.isSuccess()) resources.setValue(result.getData());
                            else postError(new RuntimeException(result.getMessage()));
                        },
                        this::postError));
    }

    public void loadCategories() {
        disposables.add(bookRepository.getCategoryTree()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result.isSuccess()) categories.setValue(result.getData());
                            else postError(new RuntimeException(result.getMessage()));
                        },
                        this::postError));
    }

    public void predictDemand(long subjectId, int months) {
        disposables.add(repository.predictDemand(subjectId, months)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result.isSuccess()) predictions.setValue(result.getData());
                            else postError(new RuntimeException(result.getMessage()));
                        },
                        this::postError));
    }

    public void checkDuplicate(String isbn, String title, String author) {
        disposables.add(repository.checkDuplicate(isbn, title, author)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result.isSuccess()) duplicateResult.setValue(result.getData());
                            else postError(new RuntimeException(result.getMessage()));
                        },
                        this::postError));
    }

    public void analyzeGap(long subjectId) {
        disposables.add(repository.analyzeGap(subjectId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result.isSuccess()) gapResult.setValue(result.getData());
                            else postError(new RuntimeException(result.getMessage()));
                        },
                        this::postError));
    }

    public void createNegotiation(long resourceId, long supplierId) {
        disposables.add(repository.createNegotiation(resourceId, supplierId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result.isSuccess()) negotiationCreated.setValue(result.getData());
                            else postError(new RuntimeException(result.getMessage()));
                        },
                        this::postError));
    }

    public void loadNegotiationSuggestion(long negotiationId) {
        disposables.add(repository.getNegotiationSuggestion(negotiationId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result.isSuccess()) negotiationSuggestion.setValue(result.getData());
                            else postError(new RuntimeException(result.getMessage()));
                        },
                        this::postError));
    }

    /**
     * WP6：流式加载谈判建议（priceRange 秒回 + text 逐 token 流式）.
     * <p>取代同步 loadNegotiationSuggestion，改善长文本等待体验.
     */
    public void streamNegotiationSuggestion(long negotiationId) {
        suggestionStreaming.setValue(true);
        textBuf.setLength(0);
        suggestionText.setValue("");
        priceRange.setValue(null);
        loadedSuggestionNegotiationId = negotiationId;
        repository.streamNegotiationSuggestion(negotiationId,
                new AcquisitionRepository.NegotiationStreamCallback() {
                    @Override
                    public void onPriceRange(NegotiationSuggestion.PriceRange range) {
                        try { priceRange.postValue(range); } catch (Exception e) {
                            Log.w(TAG, "postValue priceRange failed", e);
                        }
                    }
                    @Override
                    public void onTextToken(String token) {
                        try {
                            textBuf.append(token);
                            suggestionText.postValue(textBuf.toString());
                        } catch (Exception e) {
                            Log.w(TAG, "postValue suggestionText failed", e);
                        }
                    }
                    @Override
                    public void onDone() {
                        try { suggestionStreaming.postValue(false); } catch (Exception e) {
                            Log.w(TAG, "postValue suggestionStreaming done failed", e);
                        }
                    }
                    @Override
                    public void onError(Throwable e) {
                        try {
                            suggestionStreaming.postValue(false);
                            loadedSuggestionNegotiationId = 0L;
                            postError(e);
                        } catch (Exception ex) {
                            Log.w(TAG, "postError/suggestionStreaming failed in onError", ex);
                        }
                    }
                });
    }

    /** 主动释放 SSE 流资源（Fragment 销毁时调用，防止回调泄漏）. */
    public void disposeStreams() {
        repository.disposeStreams();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 注意：不在 onCleared 中调 repository.disposeStreams()
        // —— repository 是全局单例，创建页的 VM 清理会错误地取消详情页的活跃流。
        // 流式取消由 NegotiationDetailFragment.onDestroyView 主动调用。
    }
}
