package com.library.acquisition.controller;

import com.library.acquisition.dto.PurchaseRequestDTO;
import com.library.acquisition.entity.NegotiationRecord;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
