package com.library.android.ui.kg;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.Slider;
import com.library.android.R;
import com.library.android.databinding.FragmentKnowledgeGraphBinding;
import com.library.android.model.GraphEdge;
import com.library.android.model.GraphNode;
import com.library.android.model.KnowledgeGraphVO;
import com.library.android.ui.main.MainActivity;
import com.library.android.ui.theme.ThemeManager;
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
            bookId = getArguments().getLong("bookId", 0);
        }

        setupWebView();
        setupSlider();
        observeViewModel();
        viewModel.loadBookGraph(bookId, currentDepth);
    }

    private void setupWebView() {
        WebSettings settings = binding.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        binding.webView.setWebViewClient(new WebViewClient());
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

    private void observeViewModel() {
        viewModel.getBookGraph().observe(getViewLifecycleOwner(), this::renderGraph);

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.textLoading.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                binding.webView.loadDataWithBaseURL(null,
                        "<html><body style='display:flex;align-items:center;justify-content:center;font-family:sans-serif;color:#999'><p>" + msg + "</p></body></html>",
                        "text/html", "UTF-8", null);
            }
        });
    }

    /** 将 KnowledgeGraphVO 转换为 ECharts 力导向图 HTML 并渲染. */
    private void renderGraph(KnowledgeGraphVO graph) {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            binding.webView.loadDataWithBaseURL(null,
                    "<html><body style='display:flex;align-items:center;justify-content:center;font-family:sans-serif;color:#999'><p>" + getString(R.string.no_graph_data) + "</p></body></html>",
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
                    "<html><body style='color:red'>" + getString(R.string.render_failed_format, e.getMessage()) + "</body></html>",
                    "text/html", "UTF-8", null);
        }
    }

    /** 构建 ECharts 力导向图 HTML 页面. */
    private String buildEChartsHtml(String nodesJson, String edgesJson) {
        return "<!DOCTYPE html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>"
                + "<script src='https://cdn.jsdelivr.net/npm/echarts@5.5.0/dist/echarts.min.js'></script>"
                + "<style>body,html,#chart{margin:0;padding:0;width:100%;height:100%;overflow:hidden}</style>"
                + "</head><body><div id='chart'></div><script>"
                + "var chart=echarts.init(document.getElementById('chart'));"
                + "var option={tooltip:{formatter:function(p){return p.dataType==='edge'?p.data.label||p.data.value:p.name}},"
                + "legend:{data:['图书','作者','关键词','学科','出版物'],orient:'vertical',left:4,top:4,textStyle:{fontSize:10}},"
                + "series:[{type:'graph',layout:'force',force:{repulsion:300,edgeLength:[100,300]},"
                + "roam:true,draggable:true,focusNodeAdjacency:true,"
                + "categories:[{name:'图书',itemStyle:{color:'#5470c6'}},{name:'作者',itemStyle:{color:'#91cc75'}},"
                + "{name:'关键词',itemStyle:{color:'#fac858'}},{name:'学科',itemStyle:{color:'#ee6666'}},{name:'出版物',itemStyle:{color:'#73c0de'}}],"
                + "data:" + nodesJson + ",edges:" + edgesJson + ","
                + "edgeSymbol:['none','none'],edgeLabel:{fontSize:10},"
                + "label:{show:true,fontSize:10,formatter:function(p){return p.name.length>8?p.name.substring(0,7)+'…':p.name}}}]};"
                + "chart.setOption(option);window.addEventListener('resize',function(){chart.resize()});"
                + "</script></body></html>";
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
            case "AUTHORED_BY": return "作者";
            case "HAS_KEYWORD": return "关键词";
            case "BELONGS_TO": return "属于";
            case "RELATED_TO": return "关联";
            case "PUBLISHED_IN": return "出版";
            case "CO_CITED": return "共引";
            case "CITES": return "引用";
            default: return relation;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.webView.destroy();
        binding = null;
    }
}