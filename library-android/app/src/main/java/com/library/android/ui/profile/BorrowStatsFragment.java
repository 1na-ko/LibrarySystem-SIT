package com.library.android.ui.profile;

import android.os.Bundle;

import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.library.android.R;
import com.library.android.databinding.FragmentBorrowStatsBinding;
import com.library.android.model.BorrowStatsVO;
import com.library.android.ui.main.MainActivity;
import com.library.android.ui.theme.ChartThemeHelper;
import com.library.android.viewmodel.ProfileViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 借阅统计 Fragment — 数字卡片 + MPAndroidChart 图表.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class BorrowStatsFragment extends Fragment {

    private FragmentBorrowStatsBinding binding;
    private ProfileViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBorrowStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        ((MainActivity) requireActivity()).setGlobalTitle("借阅统计");

        viewModel.getBorrowStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                bindStats(stats);
            }
        });

        if (viewModel.getBorrowStats().getValue() == null) {
            viewModel.loadBorrowStats();
        }
    }

    private void bindStats(BorrowStatsVO stats) {
        // 数字卡片
        binding.tvTotalBorrows.setText(String.valueOf(stats.getTotalBorrows()));
        binding.tvCurrentBorrows.setText(String.valueOf(stats.getCurrentBorrows()));
        binding.tvOverdueCount.setText(String.valueOf(stats.getTotalOverdue()));
        binding.tvTotalFines.setText(String.format("¥%.1f", stats.getTotalFines()));

        // A.5 关键修复：每次回调先清空容器再添加新图表，避免重复 addView 导致视图泄漏与重叠
        binding.layoutPieChart.removeAllViews();
        binding.layoutLineChart.removeAllViews();

        // 分类分布饼图
        if (stats.getCategoryDistribution() != null && !stats.getCategoryDistribution().isEmpty()) {
            PieChart pieChart = new PieChart(requireContext());
            ChartThemeHelper.applyPieChartTheme(pieChart);
            binding.layoutPieChart.addView(pieChart);

            List<PieEntry> pieEntries = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            int[] colorSet = {
                    ContextCompat.getColor(requireContext(), R.color.chart_color_1),
                    ContextCompat.getColor(requireContext(), R.color.chart_color_2),
                    ContextCompat.getColor(requireContext(), R.color.chart_color_3),
                    ContextCompat.getColor(requireContext(), R.color.chart_color_4),
                    ContextCompat.getColor(requireContext(), R.color.chart_color_5),
                    ContextCompat.getColor(requireContext(), R.color.chart_color_6)
            };

            for (int i = 0; i < stats.getCategoryDistribution().size(); i++) {
                BorrowStatsVO.CategoryCount cc = stats.getCategoryDistribution().get(i);
                pieEntries.add(new PieEntry(cc.getCount(), cc.getCategoryName()));
                colors.add(colorSet[i % colorSet.length]);
            }

            PieDataSet dataSet = new PieDataSet(pieEntries, "分类分布");
            dataSet.setColors(colors);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(ChartThemeHelper.getTextPrimaryColor(requireContext()));

            PieData data = new PieData(dataSet);
            pieChart.setData(data);
            pieChart.setUsePercentValues(true);
            pieChart.getDescription().setEnabled(false);
            pieChart.setDrawEntryLabels(true);
            pieChart.setEntryLabelTextSize(10f);
            pieChart.invalidate();
        }

        // 月度趋势折线图
        if (stats.getMonthlyTrend() != null && !stats.getMonthlyTrend().isEmpty()) {
            LineChart lineChart = new LineChart(requireContext());
            ChartThemeHelper.applyLineChartTheme(lineChart);
            binding.layoutLineChart.addView(lineChart);

            List<Entry> lineEntries = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            for (int i = 0; i < stats.getMonthlyTrend().size(); i++) {
                BorrowStatsVO.MonthlyCount mc = stats.getMonthlyTrend().get(i);
                lineEntries.add(new Entry(i, mc.getCount()));
                labels.add(mc.getMonth());
            }

            LineDataSet lineDataSet = new LineDataSet(lineEntries, "月度借阅趋势");
            lineDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.chart_color_1));
            lineDataSet.setLineWidth(2f);
            lineDataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.chart_color_1));
            lineDataSet.setCircleRadius(3f);
            lineDataSet.setValueTextSize(10f);
            lineDataSet.setValueTextColor(ChartThemeHelper.getTextPrimaryColor(requireContext()));

            LineData lineData = new LineData(lineDataSet);
            lineChart.setData(lineData);
            lineChart.getDescription().setEnabled(false);

            XAxis xAxis = lineChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setLabelRotationAngle(-45f);

            lineChart.invalidate();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}