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
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.library.android.R;
import com.library.android.databinding.FragmentLiteratureTraceBinding;
import com.library.android.model.GraphEdge;
import com.library.android.model.GraphNode;
import com.library.android.model.TraceGraph;
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.common.LoadingState;
import com.library.android.ui.common.NavArgKeys;
import com.library.android.ui.main.MainActivity;
import com.library.android.ui.theme.ThemeManager;
import com.library.android.ui.theme.WebViewThemeHelper;
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
public class LiteratureTraceFragment extends BaseFragment {

    private FragmentLiteratureTraceBinding binding;
    private KnowledgeGraphViewModel viewModel;
    private long bookId;
    private String currentDirection = "BOTH";
    private int currentMaxDepth = 3;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ThemeManager.getInstance().setDarkMode(true);
        android.content.Context themedContext = ThemeManager.getInstance().wrapContext(requireContext());
        android.view.LayoutInflater themedInflater = inflater.cloneInContext(themedContext);
        binding = FragmentLiteratureTraceBinding.inflate(themedInflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(KnowledgeGraphViewModel.class);

        ((MainActivity) requireActivity()).setGlobalTitle("文献溯源");

        if (getArguments() != null) {
            bookId = getArguments().getLong(NavArgKeys.BOOK_ID, 0);
        }

        setupWebView();
        setupControls();
        observeViewModel();
        loadTrace();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = binding.webView.getSettings();
        // JS 为 ECharts 有向图必需，不可禁用
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        // 安全加固：禁用文件/内容访问、混合内容、地理定位
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setGeolocationEnabled(false);

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
        // WP-6 P0：原代码把 LiveData<LoadingState> 当 Boolean 用，Boolean.TRUE.equals(LoadingState.LOADING) 永远 false
        viewModel.getLoadingState().observe(getViewLifecycleOwner(), state ->
                binding.textLoading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE));
        // WP-6：错误事件订阅（原版无 observeError，加载失败页面空白用户无感知）
        observeError(viewModel.getErrorEvent());
    }

    private void renderTrace(TraceGraph trace) {
        if (trace == null || trace.getPaths() == null || trace.getPaths().isEmpty()) {
            binding.webView.loadDataWithBaseURL(null,
                    WebViewThemeHelper.buildEmptyHtml(requireContext(), getString(R.string.no_trace_data)),
                    "text/html", "UTF-8", null);
            return;
        }

        try {
            // 合并所有路径的节点和边
            JSONArray nodes = new JSONArray();
            JSONArray edges = new JSONArray();
            java.util.Set<Long> addedNodeIds = new java.util.HashSet<>();

            for (TraceGraph.Path path : trace.getPaths()) {
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
                    WebViewThemeHelper.buildEmptyHtml(requireContext(), getString(R.string.render_failed_format, e.getMessage())),
                    "text/html", "UTF-8", null);
        }
    }

    private String buildTraceHtml(String nodesJson, String edgesJson, String sourceTitle) {
        String colors = WebViewThemeHelper.colorsToJson(WebViewThemeHelper.getChartColors(requireContext()));
        String start = WebViewThemeHelper.buildChartPageStart(requireContext(), colors);
        String option = "var option={"
                + "backgroundColor:'transparent',"
                + "textStyle:{color:__themeTextColor},"
                + "title:{text:'文献溯源: " + sourceTitle + "',textStyle:{fontSize:12,color:__themeTextColor},left:'center',top:4},"
                + "tooltip:{},"
                + "color:__themeColors,"
                + "series:[{type:'graph',layout:'force',force:{repulsion:200,edgeLength:[80,250]},"
                + "roam:true,draggable:true,"
                + "data:" + nodesJson + ",edges:" + edgesJson + ","
                + "edgeSymbol:['circle','arrow'],edgeSymbolSize:[6,10],"
                + "label:{show:true,fontSize:9,color:__themeTextColor}}]};"
                + "chart.setOption(option);";
        return start + option + WebViewThemeHelper.buildChartPageEnd();
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