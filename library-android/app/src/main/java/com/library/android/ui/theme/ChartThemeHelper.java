package com.library.android.ui.theme;

import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.library.android.R;

/**
 * MPAndroidChart 主题适配助手。
 *
 * <p>统一处理借阅统计、管理端 Dashboard 等页面中图表的深浅色适配：
 * 坐标轴、标签、网格线、图例文字均跟随当前主题 Token，避免暗色模式下白字/灰底不可见。
 */
public final class ChartThemeHelper {

    private ChartThemeHelper() {
    }

    /**
     * 判断当前是否应使用暗色主题渲染图表。
     *
     * <p>优先使用 {@link ThemeManager#isDarkMode()}（强制深色页面）；
     * 若未设置，则回退到系统 DayNight 配置。
     */
    public static boolean isDarkMode(@NonNull Context context) {
        if (ThemeManager.getInstance().isDarkMode()) {
            return true;
        }
        int nightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    @ColorInt
    public static int getTextPrimaryColor(@NonNull Context context) {
        return ContextCompat.getColor(context, isDarkMode(context) ? R.color.dark_text_primary : R.color.text_primary);
    }

    @ColorInt
    public static int getTextSecondaryColor(@NonNull Context context) {
        return ContextCompat.getColor(context, isDarkMode(context) ? R.color.dark_text_secondary : R.color.text_secondary);
    }

    @ColorInt
    public static int getTextTertiaryColor(@NonNull Context context) {
        return ContextCompat.getColor(context, isDarkMode(context) ? R.color.dark_text_tertiary : R.color.text_tertiary);
    }

    @ColorInt
    public static int getBorderColor(@NonNull Context context) {
        return ContextCompat.getColor(context, isDarkMode(context) ? R.color.dark_border_light : R.color.border_light);
    }

    @ColorInt
    public static int getBackgroundColor(@NonNull Context context) {
        return ContextCompat.getColor(context, isDarkMode(context) ? R.color.dark_bg_primary : R.color.bg_primary);
    }

    /**
     * 应用折线图主题：背景透明、文字/坐标轴/网格线使用主题色。
     *
     * @param lineChart 目标折线图
     */
    public static void applyLineChartTheme(@NonNull LineChart lineChart) {
        Context context = lineChart.getContext();
        int textPrimary = getTextPrimaryColor(context);
        int textSecondary = getTextSecondaryColor(context);
        int borderColor = getBorderColor(context);

        lineChart.setBackgroundColor(getBackgroundColor(context));
        lineChart.getDescription().setTextColor(textSecondary);

        Legend legend = lineChart.getLegend();
        legend.setTextColor(textPrimary);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setTextColor(textSecondary);
        xAxis.setAxisLineColor(borderColor);
        xAxis.setGridColor(borderColor);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(textSecondary);
        leftAxis.setAxisLineColor(borderColor);
        leftAxis.setGridColor(borderColor);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    /**
     * 应用饼图主题：背景透明、标签/图例使用主题色。
     *
     * @param pieChart 目标饼图
     */
    public static void applyPieChartTheme(@NonNull PieChart pieChart) {
        Context context = pieChart.getContext();
        int textPrimary = getTextPrimaryColor(context);
        int textSecondary = getTextSecondaryColor(context);

        pieChart.setBackgroundColor(getBackgroundColor(context));
        pieChart.getDescription().setTextColor(textSecondary);

        Legend legend = pieChart.getLegend();
        legend.setTextColor(textPrimary);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        // 饼图扇区标签颜色使用主题主文字色，保证深浅模式可读
        pieChart.setEntryLabelColor(textPrimary);
    }
}
