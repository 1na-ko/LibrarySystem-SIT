package com.library.android.ui.kg;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
public class SubjectNetworkFragment extends Fragment {

    private FragmentSubjectNetworkBinding binding;
    private KnowledgeGraphViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSubjectNetworkBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(KnowledgeGraphViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

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

    private void setupWebView() {
        WebSettings settings = binding.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        binding.webView.setWebViewClient(new WebViewClient());
    }

    private void observeViewModel() {
        viewModel.getSubjectNetwork().observe(getViewLifecycleOwner(), this::renderNetwork);
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));
    }

    private void renderNetwork(KnowledgeGraphVO graph) {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            binding.webView.loadDataWithBaseURL(null,
                    "<html><body style='display:flex;align-items:center;justify-content:center;font-family:sans-serif;color:#999'><p>暂无学科网络数据</p></body></html>",
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
                    "<html><body style='color:red'>渲染失败: " + e.getMessage() + "</body></html>",
                    "text/html", "UTF-8", null);
        }
    }

    private String buildSubjectHtml(String nodesJson, String edgesJson) {
        return "<!DOCTYPE html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'>"
                + "<script src='https://cdn.jsdelivr.net/npm/echarts@5.5.0/dist/echarts.min.js'></script>"
                + "<style>body,html,#chart{margin:0;padding:0;width:100%;height:100%;overflow:hidden}</style>"
                + "</head><body><div id='chart'></div><script>"
                + "var chart=echarts.init(document.getElementById('chart'));"
                + "var option={tooltip:{},legend:{data:['关键词','图书','作者','学科'],orient:'vertical',left:4,top:4,textStyle:{fontSize:10}},"
                + "series:[{type:'graph',layout:'force',force:{repulsion:500,gravity:0.1,edgeLength:[50,200]},"
                + "roam:true,draggable:true,"
                + "categories:[{name:'关键词',itemStyle:{color:'#fac858'}},{name:'图书',itemStyle:{color:'#5470c6'}},{name:'作者',itemStyle:{color:'#91cc75'}},{name:'学科',itemStyle:{color:'#ee6666'}}],"
                + "data:" + nodesJson + ",edges:" + edgesJson + ","
                + "edgeSymbol:['none','none'],"
                + "label:{show:true,fontSize:9,formatter:function(p){return p.name.length>6?p.name.substring(0,5)+'…':p.name}}}]};"
                + "chart.setOption(option);"
                + "</script></body></html>";
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
    public void onDestroyView() {
        super.onDestroyView();
        binding.webView.destroy();
        binding = null;
    }
}