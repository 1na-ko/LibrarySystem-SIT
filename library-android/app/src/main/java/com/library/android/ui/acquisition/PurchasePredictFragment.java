package com.library.android.ui.acquisition;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.library.android.R;
import com.library.android.databinding.FragmentPurchasePredictBinding;
import com.library.android.databinding.ItemPredictionBinding;
import com.library.android.model.CategoryVO;
import com.library.android.model.PurchasePredictionVO;
import com.library.android.repository.BookRepository;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.main.MainActivity;
import com.library.android.viewmodel.AcquisitionViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 采购预测 Fragment — WP5 改造：subjectId 改分类选择器（不再手填 ID）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class PurchasePredictFragment extends BaseFragment {

    private FragmentPurchasePredictBinding binding;
    private AcquisitionViewModel viewModel;
    private PredictionAdapter adapter;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    BookRepository bookRepository;

    private final List<CategoryVO> flatCategories = new ArrayList<>();
    private long selectedSubjectId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.content.Context themedContext = com.library.android.ui.theme.ThemeManager.getInstance().wrapContext(requireContext());
        android.view.LayoutInflater themedInflater = inflater.cloneInContext(themedContext);
        binding = FragmentPurchasePredictBinding.inflate(themedInflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AcquisitionViewModel.class);
        ((MainActivity) requireActivity()).setGlobalTitle(getString(R.string.acquisition_predict));

        adapter = new PredictionAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        // WP5：subjectId 输入框改分类选择器
        binding.etSubjectId.setFocusable(false);
        binding.etSubjectId.setClickable(true);
        binding.etSubjectId.setOnClickListener(v -> openCategoryPicker());

        binding.btnRun.setOnClickListener(v -> {
            if (selectedSubjectId <= 0) {
                Toast.makeText(requireContext(), getString(R.string.acquisition_predict_select_subject), Toast.LENGTH_SHORT).show();
                return;
            }
            String monthsStr = binding.etMonths.getText() != null
                    ? binding.etMonths.getText().toString().trim() : "";
            try {
                int months = TextUtils.isEmpty(monthsStr) ? 3
                        : Math.max(1, Math.min(12, Integer.parseInt(monthsStr)));
                binding.progress.setVisibility(View.VISIBLE);
                viewModel.predictDemand(selectedSubjectId, months);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), getString(R.string.acquisition_predict_months_error), Toast.LENGTH_SHORT).show();
            }
        });

        observeError(viewModel.getErrorEvent());
        viewModel.getPredictions().observe(getViewLifecycleOwner(), this::renderResult);
        loadCategories();
    }

    private void loadCategories() {
        disposables.add(bookRepository.getCategoryTree()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        flatCategories.clear();
                        flatten(result.getData());
                    }
                }, Throwable::printStackTrace));
    }

    private void flatten(List<CategoryVO> nodes) {
        if (nodes == null) return;
        for (CategoryVO node : nodes) {
            flatCategories.add(node);
            if (node.getChildren() != null) flatten(node.getChildren());
        }
    }

    private void openCategoryPicker() {
        if (flatCategories.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.acquisition_predict_category_loading), Toast.LENGTH_SHORT).show();
            loadCategories();
            return;
        }
        String[] names = new String[flatCategories.size()];
        for (int i = 0; i < flatCategories.size(); i++) {
            names[i] = flatCategories.get(i).getName() != null ? flatCategories.get(i).getName() : "";
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.acquisition_predict_select_category))
                .setItems(names, (d, which) -> {
                    CategoryVO c = flatCategories.get(which);
                    selectedSubjectId = c.getId();
                    binding.etSubjectId.setText(c.getName());
                })
                .show();
    }

    private void renderResult(List<PurchasePredictionVO> data) {
        if (binding == null) return;
        binding.progress.setVisibility(View.GONE);
        adapter.submitList(data);
        binding.tvEmpty.setVisibility(data == null || data.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }

    private static class PredictionAdapter extends BaseAdapter<PurchasePredictionVO, ItemPredictionBinding> {

        PredictionAdapter() {
            super(R.layout.item_prediction, new DiffUtil.ItemCallback<PurchasePredictionVO>() {
                @Override
                public boolean areItemsTheSame(@NonNull PurchasePredictionVO o, @NonNull PurchasePredictionVO n) {
                    String om = o.getMonth() != null ? o.getMonth() : "";
                    String nm = n.getMonth() != null ? n.getMonth() : "";
                    return o.getSubjectId() == n.getSubjectId() && om.equals(nm);
                }

                @Override
                public boolean areContentsTheSame(@NonNull PurchasePredictionVO o, @NonNull PurchasePredictionVO n) {
                    return o.getPredictedDemand() == n.getPredictedDemand()
                            && Double.compare(o.getConfidence(), n.getConfidence()) == 0;
                }
            });
        }

        @Override
        protected ItemPredictionBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return ItemPredictionBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(ItemPredictionBinding b, PurchasePredictionVO item, int position) {
            b.tvSubject.setText(item.getSubjectName() != null ? item.getSubjectName() : ("学科 " + item.getSubjectId()));
            b.tvMonth.setText(item.getMonth() != null ? item.getMonth() : "");
            b.tvDemand.setText(String.valueOf(item.getPredictedDemand()));
            b.tvConfidence.setText(String.format("置信度 %.0f%%", item.getConfidence() * 100));
        }
    }
}
