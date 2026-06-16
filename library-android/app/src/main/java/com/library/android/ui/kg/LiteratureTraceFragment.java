package com.library.android.ui.kg;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.library.android.databinding.FragmentLiteratureTraceBinding;
import com.library.android.model.GraphEdge;
import com.library.android.model.GraphNode;
import com.library.android.model.TraceGraph;
import com.library.android.viewmodel.KnowledgeGraphViewModel;

import org.json.JSONArray;
import org.json.JSONObject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 文献溯源 Fragment（人员 B 主导）.
 *
 * <p>有向图渲染引用链（前向/后向/双向溯源），最大跳数 1-5。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class LiteratureTraceFragment extends Fragment {

    private FragmentLiteratureTraceBinding binding;
    private KnowledgeGraphViewModel viewModel;
    private long bookId;
    private String currentDirection = "BOTH";
    private int currentMaxDepth = 3;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLiteratureTraceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(KnowledgeGraphViewModel.class);

        if (getArguments() != null) {
            bookId = getArguments().getLong("bookId", 0);
        }

        setupWebView();
        setupControls();
        observeViewModel();
        loadTrace();
    }

    private void setupWebView() {
        WebSettings settings = binding.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        binding.webView.setWebViewClient(new WebViewClient());
    }

    private void setupControls() {
        binding.spinnerDirection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                String[] dirs = {"BOTH", "FORWARD", "BACKWARD"};
                String newDir = dirs[pos];
                if (!newDir.equals(currentDirection)) {
                    currentDirection = newDir;
                    loadTrace();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.spinnerDepth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                int newDepth = pos + 1;
                if (newDepth != currentMaxDepth) {
                    currentMaxDepth = newDepth;
                    loadTrace();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadTrace() {
        viewModel.traceLiterature(bookId, currentDirection, currentMaxDepth);
    }

    private void observeViewModel() {
        viewModel.getTraceGraph().observe(getViewLifecycleOwner(), this::renderTrace);
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));
    }

    private void renderTrace(TraceGraph trace) {
        if (trace == null || trace.getPaths() == null || trace.getPaths().isEmpty()) {
            binding.webView.loadDataWithBaseURL(null,
                    "<html><body style='display:flex;align-items:center;justify-content:center;font-family:sans-serif;color:#999'><p>" + getString(R.string.no_trace_data) + "</p></body></html>",
                    "text/html", "UTF-8", null);
            return;
        }

        try {
            // 合并所有路径的节点和边
            JSONArray nodes = new JSONArray();
            JSONArray edges = new JSONArray();
            java.util.Set<Long> addedNodeIds = new java.util.HashSet<>();

            for (TraceGraph.TracePath path : trace.getPaths()) {
                if (path.getNodes() != null) {
                    for (GraphNode node : path.getNodes()) {
                        if (addedNodeIds.add(node.getId())) {
                            JSONObject n = new JSONObject();
                            n.put("id", node.getId());
                            n.put("name", node.getLabel());
                            n.put("category", getCategoryIndex(node.getType()));
                            n.put("symbolSize", node.getLabel().length() > 10 ? 20 : 30);
                            nodes.put(n);
                        }
                    }
                }
                if (path.getEdges() != null) {
                    for (GraphEdge edge : path.getEdges()) {
                        JSONObject e = new JSONObject();
                        e.put("source", edge.getSourceId());
                        e.put("target", edge.getTargetId());
                        e.put("value", edge.getWeight());
                        edges.put(e);
                    }
                }
            }

            String html = buildTraceHtml(nodes.toString(), edges.toString(),
                    trace.getSourceBook() != null ? trace.getSourceBook().getTitle() : getString(R.string.source_book_label));
            binding.webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        } catch (Exception e) {
            binding.webView.loadDataWithBaseURL(null,
                    "<html><body style='color:red'>" + getString(R.string.render_failed_format, e.getMessage()) + "</body></html>",
                    "text/html", "UTF-8", null);
        }
    }

    private String buildTraceHtml(String nodesJson, String edgesJson, String sourceTitle) {
        return "<!DOCTYPE html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'>"
                + "<script src='https://cdn.jsdelivr.net/npm/echarts@5.5.0/dist/echarts.min.js'></script>"
                + "<style>body,html,#chart{margin:0;padding:0;width:100%;height:100%;overflow:hidden}</style>"
                + "</head><body><div id='chart'></div><script>"
                + "var chart=echarts.init(document.getElementById('chart'));"
                + "var option={title:{text:'文献溯源: " + sourceTitle + "',textStyle:{fontSize:12},left:'center',top:4},"
                + "tooltip:{},"
                + "series:[{type:'graph',layout:'force',force:{repulsion:200,edgeLength:[80,250]},"
                + "roam:true,draggable:true,"
                + "data:" + nodesJson + ",edges:" + edgesJson + ","
                + "edgeSymbol:['circle','arrow'],edgeSymbolSize:[6,10],"
                + "label:{show:true,fontSize:9}}]};"
                + "chart.setOption(option);"
                + "</script></body></html>";
    }

    private int getCategoryIndex(String type) {
        if (type == null) return 0;
        switch (type) {
            case "AUTHOR": return 1;
            case "KEYWORD": return 2;
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
