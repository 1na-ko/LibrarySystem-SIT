package com.library.kg.controller;

import com.library.common.result.Result;
import com.library.kg.enums.TraceDirection;
import com.library.kg.service.GraphBuildService;
import com.library.kg.service.GraphQueryService;
import com.library.kg.service.LiteratureTracingService;
import com.library.kg.service.TopicNetworkBuilder;
import com.library.kg.vo.KnowledgeGraphVO;
import com.library.kg.vo.TraceGraphVO;
import com.library.security.aspect.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识图谱 Controller.
 * <p>
 * 提供图谱可视化查询、文献溯源、主题网络、实体搜索端点。
 * 管理端端点受 {@code kg:admin} 权限保护。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "知识图谱", description = "知识图谱可视化、文献溯源与主题网络")
public class KnowledgeGraphController {

    private final GraphQueryService graphQueryService;
    private final LiteratureTracingService literatureTracingService;
    private final TopicNetworkBuilder topicNetworkBuilder;
    private final GraphBuildService graphBuildService;

    // ---- 查询端点 ----

    @GetMapping("/kg/book/{bookId}")
    @RequirePermission("kg:read")
    @Operation(summary = "查询图书知识图谱", description = "以图书为中心取 1-3 跳邻居节点与关系")
    public Result<KnowledgeGraphVO> getBookGraph(
            @PathVariable Long bookId,
            @Parameter(description = "查询深度（1-3）") @RequestParam(defaultValue = "2") int depth) {
        return Result.success(graphQueryService.getBookGraph(bookId, depth));
    }

    @GetMapping("/kg/book/{bookId}/trace")
    @RequirePermission("kg:read")
    @Operation(summary = "文献溯源", description = "以图书为起点通过引用链构建文献演变关系图")
    public Result<TraceGraphVO> trace(
            @PathVariable Long bookId,
            @Parameter(description = "溯源方向：FORWARD/BACKWARD/BOTH") @RequestParam(defaultValue = "BOTH") String direction,
            @Parameter(description = "最大跳数（1-5）") @RequestParam(defaultValue = "3") int maxDepth) {
        TraceDirection dir = parseDirection(direction);
        return Result.success(literatureTracingService.trace(bookId, dir, maxDepth));
    }

    @GetMapping("/kg/book/{bookId}/keypath")
    @RequirePermission("kg:read")
    @Operation(summary = "关键路径发现", description = "寻找从源文献到目标文献的最优引用路径")
    public Result<TraceGraphVO> findKeyPath(
            @PathVariable Long bookId,
            @Parameter(description = "目标图书 ID") @RequestParam Long targetBookId) {
        TraceGraphVO result = literatureTracingService.findKeyPath(bookId, targetBookId);
        if (result == null) {
            return Result.success(null);
        }
        return Result.success(result);
    }

    @GetMapping("/kg/subject/{name}")
    @RequirePermission("kg:read")
    @Operation(summary = "学科主题网络", description = "按学科名称获取 Top-K 高 PageRank 关键词及其关联网络")
    public Result<KnowledgeGraphVO> getSubjectNetwork(
            @PathVariable String name,
            @Parameter(description = "返回关键词数量") @RequestParam(defaultValue = "50") int topK) {
        return Result.success(topicNetworkBuilder.buildSubjectNetwork(name, topK));
    }

    @GetMapping("/kg/search")
    @RequirePermission("kg:read")
    @Operation(summary = "知识实体搜索", description = "模糊搜索图谱中的实体节点，按 PageRank 排序")
    public Result<KnowledgeGraphVO> searchEntities(
            @Parameter(description = "模糊搜索词") @RequestParam String entity,
            @Parameter(description = "实体类型筛选（可选）：BOOK/AUTHOR/KEYWORD/SUBJECT")
            @RequestParam(required = false) String type) {
        return Result.success(graphQueryService.searchEntities(entity, type));
    }

    // ---- 管理端端点 ----

    @PostMapping("/admin/kg/rebuild/{bookId}")
    @RequirePermission("kg:admin")
    @Operation(summary = "重建指定图书图谱", description = "为指定图书重新执行 NER→RE→MERGE 流水线")
    public Result<Void> rebuildBook(@PathVariable Long bookId) {
        graphBuildService.buildGraph(bookId);
        return Result.success();
    }

    @PostMapping("/admin/kg/rebuild-all")
    @RequirePermission("kg:admin")
    @Operation(summary = "全量重建图谱", description = "分页扫描所有馆藏图书，逐本重建知识图谱")
    public Result<Integer> rebuildAll() {
        int count = graphBuildService.rebuildAll();
        return Result.success(count);
    }

    // ---- 内部 ----

    private TraceDirection parseDirection(String direction) {
        try {
            return TraceDirection.valueOf(direction.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TraceDirection.BOTH;
        }
    }
}
