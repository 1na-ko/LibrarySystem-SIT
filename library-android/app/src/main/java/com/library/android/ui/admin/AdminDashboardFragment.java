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
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.common.Debounce;
import com.library.android.ui.main.MainActivity;
import com.library.android.ui.theme.ChartThemeHelper;
import com.library.android.viewmodel.AdminDashboardViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 管理端流通统计 Dashboard Fragment（C.2 新增）.
 *
 * <p>展示 4 张数字卡片 + 月趋势折线图 + 热门分类饼图 + 图谱重建按钮.
 *
 * <p>P1-01：移除 {@code @Inject AdminRepository}，图谱重建动作下沉至
 * {@link AdminDashboardViewModel#rebuildKnowledgeGraph()}，UI 三路观察
 * rebuildResult / rebuildInProgress / errorEvent.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class AdminDashboardFragment extends BaseFragment {

    private FragmentAdminDashboardBinding binding;
    private AdminDashboardViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // P1-07：管理端强制深色，先设置状态再 wrapContext，避免从返回栈恢复时使用浅色主题
        com.library.android.ui.theme.ThemeManager.getInstance().setDarkMode(true);
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

        // P1-01：图谱重建状态观察（取代原 Fragment 内 disposables.add 内联订阅）
        viewModel.isRebuildInProgress().observe(getViewLifecycleOwner(), this::renderRebuildState);
        viewModel.getRebuildResult().observe(getViewLifecycleOwner(), processed -> {
            if (processed == null || binding == null) return;
            Snackbar.make(binding.getRoot(),
                    getString(R.string.dashboard_rebuild_kg_done_format, processed),
                    Snackbar.LENGTH_LONG).show();
        });

        binding.btnRebuildKg.setOnClickListener(v -> {
            if (!Debounce.allow(v)) return;
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dashboard_rebuild_kg)
                    .setMessage(R.string.dashboard_rebuild_kg_confirm)
                    .setPositiveButton(R.string.confirm, (d, w) -> viewModel.rebuildKnowledgeGraph())
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

    /** P1-01：图谱重建按钮状态/文案/进度条统一切换. */
    private void renderRebuildState(@Nullable Boolean inProgress) {
        if (binding == null || inProgress == null) return;
        binding.btnRebuildKg.setEnabled(!inProgress);
        if (binding.btnRebuildKg instanceof com.google.android.material.button.MaterialButton) {
            com.google.android.material.button.MaterialButton btn =
                    (com.google.android.material.button.MaterialButton) binding.btnRebuildKg;
            btn.setText(inProgress
                    ? R.string.dashboard_rebuild_kg_running
                    : R.string.dashboard_rebuild_kg);
        }
        binding.progress.setVisibility(inProgress ? View.VISIBLE : View.GONE);
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
        borrowSet.setValueTextColor(ChartThemeHelper.getTextPrimaryColor(requireContext()));

        LineDataSet returnSet = new LineDataSet(returnEntries, getString(R.string.dashboard_today_returns));
        returnSet.setColor(colorReturn);
        returnSet.setCircleColor(colorReturn);
        returnSet.setLineWidth(2f);
        returnSet.setCircleRadius(3f);
        returnSet.setValueTextColor(ChartThemeHelper.getTextPrimaryColor(requireContext()));

        ChartThemeHelper.applyLineChartTheme(binding.lineChartTrend);
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
            entries.add(new PieEntry((float) cat.getBorrowCount(),
                    cat.getCategoryName() != null ? cat.getCategoryName() : ""));
            colors.add(palette[i % palette.length]);
        }
        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(colors);
        ds.setValueTextSize(11f);
        ds.setValueTextColor(ChartThemeHelper.getTextPrimaryColor(requireContext()));
        ChartThemeHelper.applyPieChartTheme(binding.pieChartCategories);
        binding.pieChartCategories.setData(new PieData(ds));
        binding.pieChartCategories.setUsePercentValues(true);
        binding.pieChartCategories.getDescription().setEnabled(false);
        binding.pieChartCategories.setEntryLabelTextSize(10f);
        binding.pieChartCategories.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
