package com.library.acquisition.service.impl;

import com.library.acquisition.config.AcquisitionProperties;
import com.library.acquisition.dto.PriceRangeDTO;
import com.library.acquisition.dto.llm.LlmNegotiationSuggestion;
import com.library.acquisition.entity.DealRecord;
import com.library.acquisition.entity.ElectronicResource;
import com.library.acquisition.entity.Supplier;
import com.library.acquisition.mapper.DealRecordMapper;
import com.library.acquisition.mapper.ElectronicResourceMapper;
import com.library.acquisition.mapper.SupplierMapper;
import com.library.acquisition.service.NegotiationAdvisor;
import com.library.acquisition.vo.NegotiationSuggestionVO;
import com.library.ai.llm.LlmService;
import com.library.ai.llm.LlmUnavailableException;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class NegotiationAdvisorImpl implements NegotiationAdvisor {

    private final ElectronicResourceMapper resourceMapper;
    private final SupplierMapper supplierMapper;
    private final DealRecordMapper dealRecordMapper;
    private final AcquisitionProperties props;
    private final LlmService llmService;

    public NegotiationAdvisorImpl(ElectronicResourceMapper resourceMapper,
                                  SupplierMapper supplierMapper,
                                  DealRecordMapper dealRecordMapper,
                                  AcquisitionProperties props,
                                  @Autowired(required = false) LlmService llmService) {
        this.resourceMapper = resourceMapper;
        this.supplierMapper = supplierMapper;
        this.dealRecordMapper = dealRecordMapper;
        this.props = props;
        this.llmService = llmService;
    }

    @Override
    public NegotiationSuggestionVO generateSuggestion(Long resourceId, Long supplierId) {
        ElectronicResource resource = resourceMapper.selectById(resourceId);
        if (resource == null || (resource.getDeleted() != null && resource.getDeleted() == 1)) {
            throw new BizException(ErrorCode.ELECTRONIC_RESOURCE_NOT_FOUND);
        }
        Supplier supplier = supplierMapper.selectById(supplierId);
        if (supplier == null || (supplier.getDeleted() != null && supplier.getDeleted() == 1)) {
            throw new BizException(ErrorCode.SUPPLIER_NOT_FOUND);
        }

        // 1. 历史成交价 → 本地计算 PriceRange
        List<DealRecord> historyDeals = dealRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DealRecord>()
                        .eq(DealRecord::getSupplierId, supplierId)
                        .eq(DealRecord::getCategory, resource.getCategory())
                        .ge(DealRecord::getDealDate,
                                LocalDate.now().minusMonths(props.getNegotiationHistoryMonths()))
                        .eq(DealRecord::getDeleted, 0));

        PriceRangeDTO priceRange = calculatePriceRange(historyDeals, resource.getAnnualBudget());

        // 2. LLM 生成策略（语言智能层）
        List<NegotiationSuggestionVO.StrategyItem> strategies;
        List<String> keyTerms;
        List<String> riskWarnings;

        if (llmService != null) {
            try {
                String prompt = buildPrompt(resource, supplier, priceRange, historyDeals.size());
                LlmNegotiationSuggestion llmResult = llmService.chat(prompt, LlmNegotiationSuggestion.class);
                if (llmResult != null && llmResult.getStrategies() != null) {
                    strategies = llmResult.getStrategies().stream()
                            .map(s -> NegotiationSuggestionVO.StrategyItem.builder()
                                    .code(s.getCode()).description(s.getDescription())
                                    .priority(s.getPriority()).build())
                            .toList();
                    keyTerms = llmResult.getKeyTerms() != null ? llmResult.getKeyTerms() : List.of();
                    riskWarnings = llmResult.getRiskWarnings() != null ? llmResult.getRiskWarnings() : List.of();
                    return buildVO(resource, supplier, priceRange, strategies, keyTerms, riskWarnings);
                }
            } catch (LlmUnavailableException e) {
                log.warn("LLM 不可用，降级本地规则模板: {}", e.getMessage());
            } catch (Exception e) {
                // LLM 调用任何异常（JSON 解析/网络/超时等）均降级本地规则，保证谈判建议可用
                log.warn("LLM 生成建议异常，降级本地规则模板: {}", e.getMessage(), e);
            }
        }

        // 3. 降级：本地规则模板
        strategies = fallbackStrategies(supplier);
        keyTerms = List.of("订阅范围与权限", "续约条款", "违约责任", "数据永久使用权");
        riskWarnings = List.of("注意隐性费用（如平台费/手续费）", "确认数据永久使用权条款",
                "注意年度涨幅上限");
        return buildVO(resource, supplier, priceRange, strategies, keyTerms, riskWarnings);
    }

    @Override
    public PriceRangeDTO calculatePriceRange(Long resourceId, Long supplierId) {
        ElectronicResource resource = resourceMapper.selectById(resourceId);
        if (resource == null || (resource.getDeleted() != null && resource.getDeleted() == 1)) {
            throw new BizException(ErrorCode.ELECTRONIC_RESOURCE_NOT_FOUND);
        }
        Supplier supplier = supplierMapper.selectById(supplierId);
        if (supplier == null || (supplier.getDeleted() != null && supplier.getDeleted() == 1)) {
            throw new BizException(ErrorCode.SUPPLIER_NOT_FOUND);
        }
        List<DealRecord> historyDeals = dealRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DealRecord>()
                        .eq(DealRecord::getSupplierId, supplierId)
                        .eq(DealRecord::getCategory, resource.getCategory())
                        .ge(DealRecord::getDealDate,
                                LocalDate.now().minusMonths(props.getNegotiationHistoryMonths()))
                        .eq(DealRecord::getDeleted, 0));
        return calculatePriceRange(historyDeals, resource.getAnnualBudget());
    }

    @Override
    public Flux<String> streamSuggestionText(Long resourceId, Long supplierId) {
        ElectronicResource resource = resourceMapper.selectById(resourceId);
        Supplier supplier = supplierMapper.selectById(supplierId);
        if (resource == null || supplier == null) {
            return Flux.just("资源或供应商不存在，请检查参数。");
        }
        List<DealRecord> historyDeals = dealRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DealRecord>()
                        .eq(DealRecord::getSupplierId, supplierId)
                        .eq(DealRecord::getCategory, resource.getCategory())
                        .ge(DealRecord::getDealDate,
                                LocalDate.now().minusMonths(props.getNegotiationHistoryMonths()))
                        .eq(DealRecord::getDeleted, 0));
        PriceRangeDTO pr = calculatePriceRange(historyDeals, resource.getAnnualBudget());

        if (llmService == null) {
            // LLM 不可用：返回降级文案
            return Flux.just(buildFallbackText(resource, supplier, pr));
        }
        String prompt = buildStreamPrompt(resource, supplier, pr, historyDeals.size());
        return llmService.chatStream(prompt);
    }

    /**
     * 构建流式 Prompt — 输出纯文本（非 JSON），便于逐 token 流式渲染.
     */
    private String buildStreamPrompt(ElectronicResource r, Supplier s, PriceRangeDTO pr, int dealCount) {
        return String.format("""
                你是高校图书馆电子资源采购谈判顾问。请根据以下信息生成一份结构化的谈判建议报告，
                以纯文本（Markdown 风格小标题）输出，不要使用 JSON 或代码块标记。

                ## 资源信息
                - 名称：%s
                - 类别：%s
                - 年度预算：%s 元
                - 预计使用人数：%d

                ## 供应商画像
                - 名称：%s
                - 合作年限：%d 年
                - 可靠度评分：%s
                - 市场份额：%s

                ## 参考价位
                - 最低价：%s 元
                - 最高价：%s 元
                - 中位价：%s 元
                - 建议报价：%s 元

                ## 历史成交记录数：%d 条

                请按以下结构输出（每节用换行分隔，每节内 3-5 条要点，每条 30-60 字）：

                【谈判策略】（按优先级降序，标注 [P数字] 前缀，如 [P90] 锚定低价：…）
                【关键条款】（合同/SLA/数据使用权等需重点协商的条款）
                【风险预警】（隐性费用、价格陷阱、续约风险等）

                直接输出报告内容，不要寒暄不要总结，语气专业简洁。""",
                r.getName(), r.getCategory(), r.getAnnualBudget(), r.getUserCount(),
                s.getName(), s.getPartnershipYears(), s.getReliability(), s.getMarketShare(),
                pr.getFloorPrice(), pr.getCeilingPrice(), pr.getMedianPrice(), pr.getSuggestedOffer(),
                dealCount);
    }

    private String buildFallbackText(ElectronicResource r, Supplier s, PriceRangeDTO pr) {
        return String.format("""
                【谈判策略】
                [P90] 标准谈判：建议按中位价 %s 元以下报价，预留 15%%-20%% 谈判空间。
                [P80] 长期合作：合作 %d 年的供应商，可争取额外 10%%-15%% 折扣。
                [P70] 多源竞价：市场份额 %s 的供应商，可引入备选议价。

                【关键条款】
                · 订阅范围与权限
                · 续约条款与年度涨幅上限（建议 ≤3%%）
                · 数据永久使用权（合同终止后存档权）
                · SLA 服务级别协议

                【风险预警】
                · 注意隐性费用（平台费/手续费/技术支持费）
                · 确认数据永久使用权条款
                · 防范年度涨幅超出预期
                """, pr.getMedianPrice(),
                s.getPartnershipYears() != null ? s.getPartnershipYears() : 0,
                s.getMarketShare() != null ? s.getMarketShare() : "未知");
    }

    private PriceRangeDTO calculatePriceRange(List<DealRecord> deals, BigDecimal budget) {
        if (deals.isEmpty()) {
            BigDecimal b = budget != null ? budget : BigDecimal.ZERO;
            return PriceRangeDTO.builder()
                    .floorPrice(b.multiply(BigDecimal.valueOf(0.8)))
                    .ceilingPrice(b.multiply(BigDecimal.valueOf(1.2)))
                    .medianPrice(b)
                    .suggestedOffer(b.multiply(BigDecimal.valueOf(0.9)))
                    .build();
        }
        List<BigDecimal> prices = deals.stream()
                .map(DealRecord::getDealPrice)
                .sorted()
                .toList();
        BigDecimal floor = prices.get(0).multiply(BigDecimal.valueOf(0.95));
        // ceiling 基于历史最高价上浮 5%（还原卖方报价空间，不向下压缩）
        BigDecimal ceiling = prices.get(prices.size() - 1).multiply(BigDecimal.valueOf(1.05));
        BigDecimal median = prices.get(prices.size() / 2);
        BigDecimal suggested = median.multiply(BigDecimal.valueOf(0.92))
                .setScale(2, RoundingMode.HALF_UP);
        return PriceRangeDTO.builder()
                .floorPrice(floor).ceilingPrice(ceiling)
                .medianPrice(median).suggestedOffer(suggested).build();
    }

    private List<NegotiationSuggestionVO.StrategyItem> fallbackStrategies(Supplier supplier) {
        List<NegotiationSuggestionVO.StrategyItem> strategies = new ArrayList<>();
        if (supplier.getPartnershipYears() != null && supplier.getPartnershipYears() > 5
                && supplier.getReliability() != null
                && supplier.getReliability().compareTo(BigDecimal.valueOf(0.8)) > 0) {
            strategies.add(NegotiationSuggestionVO.StrategyItem.builder()
                    .code("LONG_TERM_DISCOUNT")
                    .description("建议以长期合作身份要求额外10%-15%折扣").priority(80).build());
        }
        if (supplier.getMarketShare() != null
                && supplier.getMarketShare().compareTo(BigDecimal.valueOf(0.3)) < 0) {
            strategies.add(NegotiationSuggestionVO.StrategyItem.builder()
                    .code("MULTI_SUPPLIER_BIDDING")
                    .description("市场存在多家供应商，建议采用招标竞价方式").priority(90).build());
        }
        if (strategies.isEmpty()) {
            strategies.add(NegotiationSuggestionVO.StrategyItem.builder()
                    .code("STANDARD_NEGOTIATION")
                    .description("建议按中位价以下报价，预留15%-20%谈判空间").priority(70).build());
        }
        return strategies;
    }

    private String buildPrompt(ElectronicResource r, Supplier s, PriceRangeDTO pr, int dealCount) {
        return String.format("""
                你是高校图书馆电子资源采购谈判顾问。根据以下信息生成谈判策略建议。

                ## 资源信息
                - 名称：%s
                - 类别：%s
                - 年度预算：%s 元
                - 预计使用人数：%d

                ## 供应商画像
                - 名称：%s
                - 合作年限：%d 年
                - 可靠度评分：%s
                - 市场份额：%s

                ## 参考价位（基于历史成交数据）
                - 最低价：%s 元
                - 最高价：%s 元
                - 中位价：%s 元
                - 建议报价：%s 元

                ## 历史成交记录数：%d 条

                请以 JSON 格式输出谈判建议，包含 strategies（策略数组，每项含 code/description/priority 0-100）、
                keyTerms（关键条款关注点数组）、riskWarnings（风险提示数组）。
                """,
                r.getName(), r.getCategory(), r.getAnnualBudget(), r.getUserCount(),
                s.getName(), s.getPartnershipYears(), s.getReliability(), s.getMarketShare(),
                pr.getFloorPrice(), pr.getCeilingPrice(), pr.getMedianPrice(), pr.getSuggestedOffer(),
                dealCount);
    }

    private NegotiationSuggestionVO buildVO(ElectronicResource r, Supplier s, PriceRangeDTO pr,
                                             List<NegotiationSuggestionVO.StrategyItem> strategies,
                                             List<String> keyTerms, List<String> riskWarnings) {
        return NegotiationSuggestionVO.builder()
                .resourceName(r.getName()).supplierName(s.getName())
                .priceRange(pr).strategies(strategies)
                .keyTerms(keyTerms).riskWarnings(riskWarnings).build();
    }
}
