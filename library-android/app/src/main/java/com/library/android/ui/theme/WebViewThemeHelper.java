package com.library.android.ui.theme;

import android.content.Context;
import android.content.res.Configuration;
import android.webkit.WebView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.library.android.R;

/**
 * WebView 主题适配助手。
 *
 * <p>统一处理知识图谱等模块中 WebView 内嵌 ECharts 页面的深浅色适配：
 * 根据当前 {@link ThemeManager} 状态或系统 uiMode 返回对应颜色，
 * 并生成主题一致的 CSS / ECharts 配置片段，避免白底/白字不可见问题。
 */
public final class WebViewThemeHelper {

    private WebViewThemeHelper() {
    }

    /**
     * 判断当前是否应使用暗色主题渲染 WebView 内容。
     *
     * <p>WP-FE-FIX：直接读取系统/应用 DayNight 配置（uiMode），
     * 让 KG WebView 与应用主题一致，不再依赖 {@link ThemeManager} 的强制深色状态
     * （后者可能因采编/管理页设置而残留 true）。
     */
    public static boolean isDarkMode(@NonNull Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /** 页面背景色。 */
    @ColorInt
    public static int getBackgroundColor(@NonNull Context context) {
        return ContextCompat.getColor(context,
                isDarkMode(context) ? R.color.dark_bg_primary : R.color.bg_primary);
    }

    /** 主文字色。 */
    @ColorInt
    public static int getTextColor(@NonNull Context context) {
        return ContextCompat.getColor(context,
                isDarkMode(context) ? R.color.dark_text_primary : R.color.text_primary);
    }

    /** 次要文字色（用于空态/错误提示）。 */
    @ColorInt
    public static int getSecondaryTextColor(@NonNull Context context) {
        return ContextCompat.getColor(context,
                isDarkMode(context) ? R.color.dark_text_secondary : R.color.text_secondary);
    }

    /** ECharts 坐标轴/图例/提示文字颜色。 */
    @ColorInt
    public static int getChartTextColor(@NonNull Context context) {
        return ContextCompat.getColor(context,
                isDarkMode(context) ? R.color.dark_text_secondary : R.color.text_secondary);
    }

    /** ECharts 数据项颜色盘（暗色下提高饱和度以保证可读性）。 */
    @NonNull
    public static String[] getChartColors(@NonNull Context context) {
        if (isDarkMode(context)) {
            return new String[]{
                    "#C93756", // 朱砂红
                    "#73c0de", // 浅蓝
                    "#fac858", // 金黄
                    "#91cc75", // 浅绿
                    "#ee6666", // 珊瑚红
                    "#3ba272"  // 青绿
            };
        }
        return new String[]{
                "#5470c6", "#91cc75", "#fac858", "#ee6666", "#73c0de", "#3ba272"
        };
    }

    /** 将 @ColorInt 转为 #RRGGBB 字符串。 */
    @NonNull
    public static String toHex(@ColorInt int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    /**
     * 生成空态/错误页 HTML。
     */
    @NonNull
    public static String buildEmptyHtml(@NonNull Context context, @NonNull String message) {
        String bg = toHex(getBackgroundColor(context));
        String text = toHex(getSecondaryTextColor(context));
        return "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>"
                + "body,html{margin:0;padding:0;width:100%;height:100%;"
                + "display:flex;align-items:center;justify-content:center;"
                + "background-color:" + bg + ";color:" + text + ";font-family:sans-serif;}"
                + "p{margin:16px;text-align:center;}"
                + "</style></head><body><p>" + escapeHtml(message) + "</p></body></html>";
    }

    /**
     * 生成 ECharts 页面公共头部（含主题化 CSS 与颜色变量）。
     *
     * @param colors ECharts 数据项颜色盘 JSON 数组字符串，如 "['#5470c6','#91cc75']"
     */
    @NonNull
    public static String buildChartPageStart(@NonNull Context context, @NonNull String colors) {
        String bg = toHex(getBackgroundColor(context));
        String text = toHex(getChartTextColor(context));
        return "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>"
                + "<script src='https://cdn.jsdelivr.net/npm/echarts@5.5.0/dist/echarts.min.js'></script>"
                + "<style>"
                + "body,html,#chart{margin:0;padding:0;width:100%;height:100%;overflow:hidden;background-color:" + bg + ";}"
                + "</style>"
                + "</head><body><div id='chart'></div><script>"
                + "var chart=echarts.init(document.getElementById('chart'),null,{renderer:'canvas'});"
                + "var __themeColors=" + colors + ";"
                + "var __themeTextColor='" + text + "';"
                + "var __themeBgColor='" + bg + "';";
    }

    @NonNull
    public static String buildChartPageEnd() {
        return "window.addEventListener('resize',function(){chart.resize();});"
                + "</script></body></html>";
    }

    /**
     * 将颜色数组转为 ECharts 可用的 JSON 数组字符串。
     */
    @NonNull
    public static String colorsToJson(@NonNull String[] colors) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < colors.length; i++) {
            sb.append("'").append(colors[i]).append("'");
            if (i < colors.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 设置 WebView 背景色与主题一致，减少加载前白闪。
     */
    public static void applyBackgroundColor(@NonNull WebView webView, @NonNull Context context) {
        webView.setBackgroundColor(getBackgroundColor(context));
    }

    @NonNull
    private static String escapeHtml(@NonNull String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
