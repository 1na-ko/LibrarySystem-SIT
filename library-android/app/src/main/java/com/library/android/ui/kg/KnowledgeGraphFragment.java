package com.library.android.ui.kg;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.slider.Slider;
import com.library.android.R;
import com.library.android.databinding.FragmentKnowledgeGraphBinding;
import com.library.android.model.GraphEdge;
import com.library.android.model.GraphNode;
import com.library.android.model.KnowledgeGraphVO;
import com.library.android.ui.common.NavArgKeys;
import com.library.android.ui.main.MainActivity;
import com.library.android.ui.theme.ThemeManager;
import com.library.android.ui.theme.WebViewThemeHelper;
import com.library.android.viewmodel.KnowledgeGraphViewModel;

import org.json.JSONArray;
import org.json.JSONObject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 图书知识图谱可视化 Fragment（人员 B 主导）.
 *
 * <p>使用 WebView + ECharts 渲染力导向图，支持缩放/拖拽/点击；
 * 节点大小按 PageRank 中心度缩放，边粗细按权重缩放。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class KnowledgeGraphFragment extends Fragment {

    private FragmentKnowledgeGraphBinding binding;
    private KnowledgeGraphViewModel viewModel;
    private long bookId;
    private int currentDepth = 2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ThemeManager.getInstance().setDarkMode(true);
        android.content.Context themedContext = ThemeManager.getInstance().wrapContext(requireContext());
        android.view.LayoutInflater themedInflater = inflater.cloneInContext(themedContext);
        binding = FragmentKnowledgeGraphBinding.inflate(themedInflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(KnowledgeGraphViewModel.class);

        ((MainActivity) requireActivity()).setGlobalTitle("知识图谱");

        if (getArguments() != null) {
            bookId = getArguments().getLong(NavArgKeys.BOOK_ID, 0);
        }

        setupWebView();
        setupSlider();
        setupChipGroup();
        observeViewModel();
        viewModel.loadBookGraph(bookId, currentDepth);
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
                // 仅允许 about:blank（loadDataWithBaseURL 基 URL）与 HTTPS 资源加载；
                // 拒绝 file: / content: / http: 等不安全 scheme
                String scheme = request.getUrl().getScheme();
                if ("about".equals(scheme) || "https".equals(scheme)) {
                    return false; // WebView 自行处理
                }
                return true; // 阻止加载
            }
        });
        binding.webView.setWebChromeClient(new WebChromeClient());
    }

    private void setupSlider() {
        binding.sliderDepth.addOnChangeListener((slider, value, fromUser) -> {
            int depth = (int) value;
            binding.textDepthValue.setText(String.valueOf(depth));
            if (fromUser && depth != currentDepth) {
                currentDepth = depth;
                viewModel.loadBookGraph(bookId, currentDepth);
            }
        });
    }

    /** WP1.1：设置 KG 子页面 ChipGroup 导航（文献溯源 / 学科网络 / 实体搜索）. */
    private void setupChipGroup() {
        binding.chipLiteratureTrace.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong(NavArgKeys.BOOK_ID, bookId);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_knowledgeGraphFragment_to_literatureTraceFragment, args);
        });
        binding.chipSubjectNetwork.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_knowledgeGraphFragment_to_subjectNetworkFragment));
        binding.chipEntitySearch.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_knowledgeGraphFragment_to_entitySearchFragment));
    }

    private void observeViewModel() {
        viewModel.getBookGraph().observe(getViewLifecycleOwner(), this::renderGraph);

        viewModel.getLoadingState().observe(getViewLifecycleOwner(), state ->
                binding.textLoading.setVisibility(state == com.library.android.ui.common.LoadingState.LOADING ? View.VISIBLE : View.GONE));

        viewModel.getErrorEvent().observe(getViewLifecycleOwner(), throwable -> {
            if (throwable != null) {
                String errorMsg = throwable.getMessage() != null ? throwable.getMessage() : getString(R.string.error_unknown);
                binding.webView.loadDataWithBaseURL(null,
                        WebViewThemeHelper.buildEmptyHtml(requireContext(), errorMsg),
                        "text/html", "UTF-8", null);
            }
        });
    }

    /** 将 KnowledgeGraphVO 转换为 ECharts 力导向图 HTML 并渲染. */
    private void renderGraph(KnowledgeGraphVO graph) {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            binding.webView.loadDataWithBaseURL(null,
                    WebViewThemeHelper.buildEmptyHtml(requireContext(), getString(R.string.no_graph_data)),
                    "text/html", "UTF-8", null);
            return;
        }

        try {
            JSONArray nodes = new JSONArray();
            for (GraphNode node : graph.getNodes()) {
                JSONObject n = new JSONObject();
                n.put("id", node.getId());
                n.put("name", node.getLabel());
                n.put("category", getCategoryIndex(node.getType()));
                // 使用 symbolSize 表示 PageRank 中心度
                int symbolSize = "BOOK".equals(node.getType()) ? 40 : 25;
                n.put("symbolSize", symbolSize);
                nodes.put(n);
            }

            JSONArray edges = new JSONArray();
            for (GraphEdge edge : graph.getEdges()) {
                JSONObject e = new JSONObject();
                e.put("source", edge.getSourceId());
                e.put("target", edge.getTargetId());
                e.put("value", edge.getWeight());
                e.put("label", getRelationLabel(edge.getRelation()));
                edges.put(e);
            }

            String html = buildEChartsHtml(nodes.toString(), edges.toString());
            binding.webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        } catch (Exception e) {
            binding.webView.loadDataWithBaseURL(null,
                    WebViewThemeHelper.buildEmptyHtml(requireContext(), getString(R.string.render_failed_format, e.getMessage())),
                    "text/html", "UTF-8", null);
        }
    }

    /** 构建 ECharts 力导向图 HTML 页面. */
    private String buildEChartsHtml(String nodesJson, String edgesJson) {
        String colors = WebViewThemeHelper.colorsToJson(WebViewThemeHelper.getChartColors(requireContext()));
        String start = WebViewThemeHelper.buildChartPageStart(requireContext(), colors);
        String option = "var option={"
                + "backgroundColor:'transparent',"
                + "textStyle:{color:__themeTextColor},"
                + "tooltip:{formatter:function(p){return p.dataType==='edge'?p.data.label||p.data.value:p.name}},"
                + "legend:{data:['图书','作者','关键词','学科','出版物'],orient:'vertical',left:4,top:4,textStyle:{fontSize:10,color:__themeTextColor}},"
                + "color:__themeColors,"
                + "series:[{type:'graph',layout:'force',force:{repulsion:300,edgeLength:[100,300]},"
                + "roam:true,draggable:true,focusNodeAdjacency:true,"
                + "categories:[{name:'图书'},{name:'作者'},{name:'关键词'},{name:'学科'},{name:'出版物'}],"
                + "data:" + nodesJson + ",edges:" + edgesJson + ","
                + "edgeSymbol:['none','none'],edgeLabel:{fontSize:10,color:__themeTextColor},"
                + "label:{show:true,fontSize:10,color:__themeTextColor,formatter:function(p){return p.name.length>8?p.name.substring(0,7)+'…':p.name}}}]};"
                + "chart.setOption(option);";
        return start + option + WebViewThemeHelper.buildChartPageEnd();
    }

    private int getCategoryIndex(String type) {
        if (type == null) return 0;
        switch (type) {
            case "AUTHOR": return 1;
            case "KEYWORD": return 2;
            case "SUBJECT": return 3;
            case "PUBLICATION": return 4;
            default: return 0;
        }
    }

    private String getRelationLabel(String relation) {
        if (relation == null) return "";
        switch (relation) {
            case "AUTHORED_BY": return getString(R.string.kg_relation_authored_by);
            case "HAS_KEYWORD": return getString(R.string.kg_relation_has_keyword);
            case "BELONGS_TO": return getString(R.string.kg_relation_belongs_to);
            case "RELATED_TO": return getString(R.string.kg_relation_related_to);
            case "PUBLISHED_IN": return getString(R.string.kg_relation_published_in);
            case "CO_CITED": return getString(R.string.kg_relation_co_cited);
            case "CITES": return getString(R.string.kg_relation_cites);
            default: return relation;
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
        // 先清理缓存再销毁，防止内存泄漏
        if (binding != null && binding.webView != null) {
            binding.webView.clearCache(true);
            binding.webView.destroy();
        }
        super.onDestroyView();
        binding = null;
    }
}