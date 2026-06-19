package com.library.android.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.library.android.R;
import com.library.android.databinding.FragmentAdminDashboardBinding;
import com.library.android.model.DashboardVO;
import com.library.android.repository.AdminRepository;
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.common.Debounce;
import com.library.android.ui.main.MainActivity;
import com.library.android.viewmodel.AdminDashboardViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 管理端流通统计 Dashboard Fragment（C.2 新增）.
 *
 * <p>展示 4 张数字卡片 + 月趋势折线图 + 热门分类饼图 + 图谱重建按钮.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class AdminDashboardFragment extends BaseFragment {

    private FragmentAdminDashboardBinding binding;
    private AdminDashboardViewModel viewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    AdminRepository adminRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // WP-12：暗色模式 wrapContext（管理端属 DARK_MODE_DESTINATIONS）
        android.content.Context themedContext = com.library.android.ui.theme.ThemeManager.getInstance().wrapContext(requireContext());
        android.view.LayoutInflater themedInflater = inflater.cloneInContext(themedContext);
        binding = FragmentAdminDashboardBinding.inflate(themedInflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminDashboardViewModel.class);

        ((MainActivity) requireActivity()).setGlobalTitle(getString(R.string.dashboard_title));

        observeError(viewModel.getErrorEvent());
        viewModel.getDashboard().observe(getViewLifecycleOwner(), this::renderDashboard);
        viewModel.getLoadingState().observe(getViewLifecycleOwner(), state ->
                binding.progress.setVisibility(
                        state == com.library.android.ui.common.LoadingState.LOADING
                                ? View.VISIBLE : View.GONE));

        binding.btnRebuildKg.setOnClickListener(v -> {
            if (!Debounce.allow(v)) return;
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dashboard_rebuild_kg)
                    .setMessage(R.string.dashboard_rebuild_kg_confirm)
                    .setPositiveButton(R.string.confirm, (d, w) -> rebuildKg(v))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        viewModel.load();
    }

    private void renderDashboard(@Nullable DashboardVO data) {
        if (data == null || binding == null) return;

        binding.tvTodayBorrows.setText(String.valueOf(data.getTodayBorrows()));
        binding.tvTodayReturns.setText(String.valueOf(data.getTodayReturns()));
        binding.tvTodayOverdue.setText(String.valueOf(data.getTodayOverdue()));
        binding.tvActiveBorrowers.setText(String.valueOf(data.getActiveBorrowers()));

        renderTrendChart(data.getMonthTrend());
        renderCategoryPie(data.getHotCategories());
    }

    private void renderTrendChart(List<DashboardVO.DailyTrend> trend) {
        if (trend == null || trend.isEmpty()) return;

        List<Entry> borrowEntries = new ArrayList<>();
        List<Entry> returnEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < trend.size(); i++) {
            DashboardVO.DailyTrend point = trend.get(i);
            borrowEntries.add(new Entry(i, point.getBorrows()));
            returnEntries.add(new Entry(i, point.getReturns()));
            // 仅展示日期"DD"以避免拥挤
            String date = point.getDate() != null ? point.getDate() : "";
            labels.add(date.length() >= 10 ? date.substring(8, 10) : date);
        }

        int colorBorrow = ContextCompat.getColor(requireContext(), R.color.chart_color_1);
        int colorReturn = ContextCompat.getColor(requireContext(), R.color.chart_color_2);

        LineDataSet borrowSet = new LineDataSet(borrowEntries, getString(R.string.dashboard_today_borrows));
        borrowSet.setColor(colorBorrow);
        borrowSet.setCircleColor(colorBorrow);
        borrowSet.setLineWidth(2f);
        borrowSet.setCircleRadius(3f);

        LineDataSet returnSet = new LineDataSet(returnEntries, getString(R.string.dashboard_today_returns));
        returnSet.setColor(colorReturn);
        returnSet.setCircleColor(colorReturn);
        returnSet.setLineWidth(2f);
        returnSet.setCircleRadius(3f);

        binding.lineChartTrend.setData(new LineData(borrowSet, returnSet));
        binding.lineChartTrend.getDescription().setEnabled(false);
        XAxis xAxis = binding.lineChartTrend.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        binding.lineChartTrend.invalidate();
    }

    private void renderCategoryPie(List<DashboardVO.CategoryHotStat> categories) {
        if (categories == null || categories.isEmpty()) return;

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int[] palette = {
                ContextCompat.getColor(requireContext(), R.color.chart_color_1),
                ContextCompat.getColor(requireContext(), R.color.chart_color_2),
                ContextCompat.getColor(requireContext(), R.color.chart_color_3),
                ContextCompat.getColor(requireContext(), R.color.chart_color_4),
                ContextCompat.getColor(requireContext(), R.color.chart_color_5),
                ContextCompat.getColor(requireContext(), R.color.chart_color_6)
        };
        for (int i = 0; i < categories.size(); i++) {
            DashboardVO.CategoryHotStat cat = categories.get(i);
            // WP-4 契约对齐：后端字段为 borrowCount（long），强转 float 用于饼图
            entries.add(new PieEntry((float) cat.getBorrowCount(),
                    cat.getCategoryName() != null ? cat.getCategoryName() : ""));
            colors.add(palette[i % palette.length]);
        }
        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(colors);
        ds.setValueTextSize(11f);
        binding.pieChartCategories.setData(new PieData(ds));
        binding.pieChartCategories.setUsePercentValues(true);
        binding.pieChartCategories.getDescription().setEnabled(false);
        binding.pieChartCategories.setEntryLabelTextSize(10f);
        binding.pieChartCategories.invalidate();
    }

    private void rebuildKg(View v) {
        v.setEnabled(false);
        // WP-10：执行中显示"重建中..."文案 + 进度条，给用户明确反馈（原版仅按钮变灰无提示）
        if (v instanceof com.google.android.material.button.MaterialButton) {
            ((com.google.android.material.button.MaterialButton) v).setText(R.string.dashboard_rebuild_kg_running);
        }
        binding.progress.setVisibility(View.VISIBLE);
        disposables.add(adminRepository.rebuildKgAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (binding == null) return;
                            v.setEnabled(true);
                            if (v instanceof com.google.android.material.button.MaterialButton) {
                                ((com.google.android.material.button.MaterialButton) v).setText(R.string.dashboard_rebuild_kg);
                            }
                            binding.progress.setVisibility(View.GONE);
                            int processed = result.getData() != null ? result.getData() : 0;
                            Snackbar.make(binding.getRoot(),
                                    getString(R.string.dashboard_rebuild_kg_done_format, processed),
                                    Snackbar.LENGTH_LONG).show();
                        },
                        throwable -> {
                            if (binding == null) return;
                            v.setEnabled(true);
                            if (v instanceof com.google.android.material.button.MaterialButton) {
                                ((com.google.android.material.button.MaterialButton) v).setText(R.string.dashboard_rebuild_kg);
                            }
                            binding.progress.setVisibility(View.GONE);
                            showErrorSnackbar(throwable);
                        }));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }
}
