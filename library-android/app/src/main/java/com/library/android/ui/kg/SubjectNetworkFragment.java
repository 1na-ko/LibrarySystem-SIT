package com.library.android.ui.kg;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.library.android.R;
import com.library.android.databinding.FragmentSubjectNetworkBinding;
import com.library.android.model.GraphEdge;
import com.library.android.model.GraphNode;
import com.library.android.model.KnowledgeGraphVO;
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.common.LoadingState;
import com.library.android.ui.main.MainActivity;
import com.library.android.ui.theme.ThemeManager;
import com.library.android.ui.theme.WebViewThemeHelper;
import com.library.android.viewmodel.KnowledgeGraphViewModel;

import org.json.JSONArray;
import org.json.JSONObject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 学科主题网络 Fragment（人员 B 主导）.
 *
 * <p>展示指定学科的关键词关联网络，Top-K 可调（50-200）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class SubjectNetworkFragment extends BaseFragment {

    private FragmentSubjectNetworkBinding binding;
    private KnowledgeGraphViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ThemeManager.getInstance().setDarkMode(true);
        android.content.Context themedContext = ThemeManager.getInstance().wrapContext(requireContext());
        android.view.LayoutInflater themedInflater = inflater.cloneInContext(themedContext);
        binding = FragmentSubjectNetworkBinding.inflate(themedInflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(KnowledgeGraphViewModel.class);

        ((MainActivity) requireActivity()).setGlobalTitle("学科主题网络");

        setupWebView();
        observeViewModel();

        binding.btnSearch.setOnClickListener(v -> {
            String subject = binding.editSubject.getText() != null
                    ? binding.editSubject.getText().toString().trim() : "";
            if (!subject.isEmpty()) {
                viewModel.loadSubjectNetwork(subject, 100);
            }
        });

        // 首屏加载默认学科
        viewModel.loadSubjectNetwork(getString(R.string.default_subject), 100);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = binding.webView.getSettings();
        // JS 为 ECharts 力导向图必需，不可禁用
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        // 安全加固：禁用文件/内容访问、混合内容、地理定位
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setGeolocationEnabled(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // P1-09：WebView 背景与当前主题一致，避免加载前/空态时白底闪烁
        WebViewThemeHelper.applyBackgroundColor(binding.webView, requireContext());

        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String scheme = request.getUrl().getScheme();
                if ("about".equals(scheme) || "https".equals(scheme)) {
                    return false;
                }
                return true;
            }
        });
    }

    private void observeViewModel() {
        viewModel.getSubjectNetwork().observe(getViewLifecycleOwner(), this::renderNetwork);
        // WP-6 P0：Loading 类型修复（原 Boolean.TRUE.equals 永远 false 致指示器永不显示）
        viewModel.getLoadingState().observe(getViewLifecycleOwner(), state ->
                binding.textLoading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE));
        observeError(viewModel.getErrorEvent());
    }

    private void renderNetwork(KnowledgeGraphVO graph) {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            binding.webView.loadDataWithBaseURL(null,
                    WebViewThemeHelper.buildEmptyHtml(requireContext(), getString(R.string.no_subject_network)),
                    "text/html", "UTF-8", null);
            return;
        }

        try {
            JSONArray nodes = new JSONArray();
            for (GraphNode node : graph.getNodes()) {
                JSONObject n = new JSONObject();
                n.put("id", node.getId());
                n.put("name", node.getLabel());
                // 按节点类型着色
                n.put("category", getCategoryIndex(node.getType()));
                n.put("symbolSize", node.getLabel().length() > 6 ? 15 : 22);
                nodes.put(n);
            }

            JSONArray edges = new JSONArray();
            for (GraphEdge edge : graph.getEdges()) {
                JSONObject e = new JSONObject();
                e.put("source", edge.getSourceId());
                e.put("target", edge.getTargetId());
                e.put("value", edge.getWeight());
                edges.put(e);
            }

            String html = buildSubjectHtml(nodes.toString(), edges.toString());
            binding.webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        } catch (Exception e) {
            binding.webView.loadDataWithBaseURL(null,
                    WebViewThemeHelper.buildEmptyHtml(requireContext(), getString(R.string.render_failed_format, e.getMessage())),
                    "text/html", "UTF-8", null);
        }
    }

    private String buildSubjectHtml(String nodesJson, String edgesJson) {
        String colors = WebViewThemeHelper.colorsToJson(WebViewThemeHelper.getChartColors(requireContext()));
        String start = WebViewThemeHelper.buildChartPageStart(requireContext(), colors);
        String option = "var option={"
                + "backgroundColor:'transparent',"
                + "textStyle:{color:__themeTextColor},"
                + "tooltip:{},"
                + "legend:{data:['关键词','图书','作者','学科'],orient:'vertical',left:4,top:4,textStyle:{fontSize:10,color:__themeTextColor}},"
                + "color:__themeColors,"
                + "series:[{type:'graph',layout:'force',force:{repulsion:500,gravity:0.1,edgeLength:[50,200]},"
                + "roam:true,draggable:true,"
                + "categories:[{name:'关键词'},{name:'图书'},{name:'作者'},{name:'学科'}],"
                + "data:" + nodesJson + ",edges:" + edgesJson + ","
                + "edgeSymbol:['none','none'],"
                + "label:{show:true,fontSize:9,color:__themeTextColor,formatter:function(p){return p.name.length>6?p.name.substring(0,5)+'…':p.name}}}]};"
                + "chart.setOption(option);";
        return start + option + WebViewThemeHelper.buildChartPageEnd();
    }

    private int getCategoryIndex(String type) {
        if (type == null) return 0;
        switch (type) {
            case "BOOK": return 1;
            case "AUTHOR": return 2;
            case "SUBJECT": return 3;
            default: return 0;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && binding.webView != null) {
            binding.webView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (binding != null && binding.webView != null) {
            binding.webView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (binding != null && binding.webView != null) {
            binding.webView.clearCache(true);
            binding.webView.destroy();
        }
        super.onDestroyView();
        binding = null;
    }
}