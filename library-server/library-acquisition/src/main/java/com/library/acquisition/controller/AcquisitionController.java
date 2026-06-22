package com.library.acquisition.controller;

import com.library.acquisition.dto.PurchaseRequestDTO;
import com.library.acquisition.dto.PriceRangeDTO;
import com.library.acquisition.entity.ElectronicResource;
import com.library.acquisition.entity.NegotiationRecord;
import com.library.acquisition.entity.Supplier;
import com.library.acquisition.mapper.ElectronicResourceMapper;
import com.library.acquisition.mapper.NegotiationMapper;
import com.library.acquisition.mapper.SupplierMapper;
import com.library.acquisition.service.DuplicateCheckService;
import com.library.acquisition.service.GapAnalysisService;
import com.library.acquisition.service.NegotiationAdvisor;
import com.library.acquisition.service.NegotiationService;
import com.library.acquisition.service.PredictionService;
import com.library.acquisition.vo.DuplicateCheckResultVO;
import com.library.acquisition.vo.GapAnalysisResultVO;
import com.library.acquisition.vo.NegotiationSuggestionVO;
import com.library.acquisition.vo.NegotiationVO;
import com.library.acquisition.vo.PurchasePredictionVO;
import com.library.common.result.Result;
import com.library.security.aspect.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "智能采编", description = "采购需求预测、查重查缺、电子资源智能谈判")
public class AcquisitionController {

    private final PredictionService predictionService;
    private final DuplicateCheckService duplicateCheckService;
    private final GapAnalysisService gapAnalysisService;
    private final NegotiationAdvisor negotiationAdvisor;
    private final NegotiationService negotiationService;
    private final SupplierMapper supplierMapper;
    private final ElectronicResourceMapper electronicResourceMapper;
    private final NegotiationMapper negotiationMapper;

    @GetMapping("/acquisition/predict")
    @RequirePermission("acquisition:predict")
    @Operation(summary = "采购需求预测")
    public Result<List<PurchasePredictionVO>> predict(
            @Parameter(description = "学科ID") @RequestParam Long subjectId,
            @Parameter(description = "预测月数（1-12）") @RequestParam(defaultValue = "3")
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(12) int months) {
        return Result.success(predictionService.predict(subjectId, months));
    }

    @PostMapping("/acquisition/duplicate-check")
    @RequirePermission("acquisition:duplicate-check")
    @Operation(summary = "采购查重")
    public Result<DuplicateCheckResultVO> duplicateCheck(
            @Valid @RequestBody PurchaseRequestDTO dto) {
        return Result.success(duplicateCheckService.checkDuplicate(
                dto.getIsbn(), dto.getTitle(), dto.getAuthor()));
    }

    @GetMapping("/acquisition/gap-analysis")
    @RequirePermission("acquisition:gap")
    @Operation(summary = "馆藏缺口分析")
    public Result<GapAnalysisResultVO> gapAnalysis(
            @Parameter(description = "学科ID") @RequestParam Long subjectId) {
        return Result.success(gapAnalysisService.analyze(subjectId));
    }

    @GetMapping("/acquisition/suppliers")
    @RequirePermission("acquisition:negotiation")
    @Operation(summary = "供应商列表（下拉选择用）")
    public Result<List<Supplier>> listSuppliers() {
        return Result.success(supplierMapper.selectList(null));
    }

    @GetMapping("/acquisition/resources")
    @RequirePermission("acquisition:negotiation")
    @Operation(summary = "电子资源列表（下拉选择用）")
    public Result<List<ElectronicResource>> listResources() {
        return Result.success(electronicResourceMapper.selectList(null));
    }

    @PostMapping("/acquisition/negotiation")
    @RequirePermission("acquisition:negotiation")
    @Operation(summary = "创建谈判记录")
    public Result<NegotiationVO> createNegotiation(
            @Parameter(description = "电子资源ID") @RequestParam Long resourceId,
            @Parameter(description = "供应商ID") @RequestParam Long supplierId) {
        // 谈判人 ID 从当前认证用户派生，防止冒充
        Long negotiatorId = com.library.security.context.SecurityUtils.getCurrentUserId();
        NegotiationRecord record = negotiationService.createNegotiation(resourceId, supplierId, negotiatorId);
        return Result.success(NegotiationVO.builder()
                .id(record.getId())
                .resourceId(record.getResourceId())
                .supplierId(record.getSupplierId())
                .negotiatorId(record.getNegotiatorId())
                .status(record.getStatus())
                .floorPrice(record.getFloorPrice())
                .ceilingPrice(record.getCeilingPrice())
                .suggestedOffer(record.getSuggestedOffer())
                .createTime(record.getCreateTime())
                .build());
    }

    @GetMapping("/acquisition/negotiation/{id}/suggestion")
    @RequirePermission("acquisition:negotiation")
    @Operation(summary = "获取谈判建议")
    public Result<NegotiationSuggestionVO> getSuggestion(@PathVariable Long id) {
        return Result.success(negotiationService.getSuggestion(id));
    }

    /**
     * SSE 流式谈判建议（WP6）：价格区间秒回 + LLM 文本逐 token 流式.
     * <p>事件序列：priceRange（秒推 JSON）→ text（多次，逐 token）→ done
     */
    @GetMapping(value = "/acquisition/negotiation/{id}/suggestion/stream",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequirePermission("acquisition:negotiation")
    @Operation(summary = "流式谈判建议（SSE）")
    public SseEmitter streamSuggestion(@PathVariable Long id) {
        SseEmitter emitter = new SseEmitter(120_000L); // 2min 超时容 LLM 长输出
        emitter.onTimeout(() -> {
            log.warn("谈判 SSE 流超时: negotiationId={}", id);
            emitter.complete();
        });
        emitter.onError(e -> log.warn("谈判 SSE 流异常: id={}, err={}", id, e.getMessage()));

        try {
            // 1. 加载谈判记录拿 resourceId / supplierId
            NegotiationRecord record = negotiationMapper.selectById(id);
            if (record == null) {
                emitter.send(SseEmitter.event().name("error").data("谈判记录不存在"));
                emitter.send(SseEmitter.event().name("done").data(""));
                emitter.complete();
                return emitter;
            }

            // 2. 价格区间秒推（本地计算）
            PriceRangeDTO priceRange = negotiationAdvisor.calculatePriceRange(
                    record.getResourceId(), record.getSupplierId());
            emitter.send(SseEmitter.event().name("priceRange").data(priceRange, MediaType.APPLICATION_JSON));

            // 3. LLM 文本流式推送
            negotiationAdvisor.streamSuggestionText(record.getResourceId(), record.getSupplierId())
                    .doOnNext(token -> {
                        try {
                            emitter.send(SseEmitter.event().name("text").data(token));
                        } catch (IOException ignore) {
                            // 客户端断开，忽略
                        }
                    })
                    .doOnError(e -> {
                        log.warn("谈判 LLM 流失败，降级提示: {}", e.getMessage());
                        try {
                            emitter.send(SseEmitter.event().name("text")
                                    .data("AI 生成遇到问题，请稍后重试或查看本地降级建议。"));
                            emitter.send(SseEmitter.event().name("done").data(""));
                            emitter.complete();
                        } catch (IOException ignore) {
                        }
                    })
                    .doOnComplete(() -> {
                        try {
                            emitter.send(SseEmitter.event().name("done").data(""));
                            emitter.complete();
                        } catch (IOException ignore) {
                        }
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("谈判 SSE 流初始化失败: id={}", id, e);
            emitter.completeWithError(e);
        }
        return emitter;
    }
}
