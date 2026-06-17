package com.library.android.ui.profile;

import android.graphics.Color;
import android.os.Bundle;
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
import com.library.android.databinding.FragmentBorrowStatsBinding;
import com.library.android.model.BorrowStatsVO;
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

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

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

        // 分类分布饼图
        if (stats.getCategoryDistribution() != null && !stats.getCategoryDistribution().isEmpty()) {
            PieChart pieChart = new PieChart(requireContext());
            binding.layoutPieChart.addView(pieChart);

            List<PieEntry> pieEntries = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            int[] colorSet = {
                    Color.parseColor("#1565C0"), Color.parseColor("#42A5F5"),
                    Color.parseColor("#7CB342"), Color.parseColor("#FFA726"),
                    Color.parseColor("#EF5350"), Color.parseColor("#AB47BC")
            };

            for (int i = 0; i < stats.getCategoryDistribution().size(); i++) {
                BorrowStatsVO.CategoryCount cc = stats.getCategoryDistribution().get(i);
                pieEntries.add(new PieEntry(cc.getCount(), cc.getCategoryName()));
                colors.add(colorSet[i % colorSet.length]);
            }

            PieDataSet dataSet = new PieDataSet(pieEntries, "分类分布");
            dataSet.setColors(colors);
            dataSet.setValueTextSize(12f);

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
            binding.layoutLineChart.addView(lineChart);

            List<Entry> lineEntries = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            for (int i = 0; i < stats.getMonthlyTrend().size(); i++) {
                BorrowStatsVO.MonthlyCount mc = stats.getMonthlyTrend().get(i);
                lineEntries.add(new Entry(i, mc.getCount()));
                labels.add(mc.getMonth());
            }

            LineDataSet lineDataSet = new LineDataSet(lineEntries, "月度借阅趋势");
            lineDataSet.setColor(Color.parseColor("#1565C0"));
            lineDataSet.setLineWidth(2f);
            lineDataSet.setCircleColor(Color.parseColor("#1565C0"));
            lineDataSet.setCircleRadius(3f);
            lineDataSet.setValueTextSize(10f);

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