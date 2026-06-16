package com.library.acquisition.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.acquisition.entity.ElectronicResource;
import com.library.acquisition.entity.NegotiationRecord;
import com.library.acquisition.entity.Supplier;
import com.library.acquisition.mapper.ElectronicResourceMapper;
import com.library.acquisition.mapper.NegotiationMapper;
import com.library.acquisition.mapper.SupplierMapper;
import com.library.acquisition.service.NegotiationAdvisor;
import com.library.acquisition.service.NegotiationService;
import com.library.acquisition.vo.NegotiationSuggestionVO;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class NegotiationServiceImpl implements NegotiationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final NegotiationMapper negotiationMapper;
    private final NegotiationAdvisor negotiationAdvisor;
    private final ElectronicResourceMapper resourceMapper;
    private final SupplierMapper supplierMapper;

    @Override
    @Transactional
    public NegotiationRecord createNegotiation(Long resourceId, Long supplierId, Long negotiatorId) {
        // 业务层存在性校验：避免依赖 DB 外键抛 DataIntegrityViolationException（返回 500 而非 4xx 友好错误）
        ElectronicResource resource = resourceMapper.selectById(resourceId);
        if (resource == null || (resource.getDeleted() != null && resource.getDeleted() == 1)) {
            throw new BizException(ErrorCode.ELECTRONIC_RESOURCE_NOT_FOUND);
        }
        Supplier supplier = supplierMapper.selectById(supplierId);
        if (supplier == null || (supplier.getDeleted() != null && supplier.getDeleted() == 1)) {
            throw new BizException(ErrorCode.SUPPLIER_NOT_FOUND);
        }
        NegotiationRecord record = new NegotiationRecord();
        record.setResourceId(resourceId);
        record.setSupplierId(supplierId);
        record.setNegotiatorId(negotiatorId);
        record.setStatus(com.library.acquisition.enums.NegotiationStatusEnum.DRAFT);
        record.setFloorPrice(BigDecimal.ZERO);
        record.setCeilingPrice(BigDecimal.ZERO);
        record.setSuggestedPrice(BigDecimal.ZERO);
        record.setDeleted(0);
        negotiationMapper.insert(record);
        log.info("谈判记录创建: id={}, resourceId={}, supplierId={}", record.getId(), resourceId, supplierId);
        return record;
    }

    @Override
    public NegotiationSuggestionVO getSuggestion(Long negotiationId) {
        // 1. 加载谈判记录（只读，单次查询无需显式事务）
        NegotiationRecord record = loadRecord(negotiationId);

        // 2. LLM 调用（无事务包裹，避免长耗时调用持有数据库连接）
        NegotiationSuggestionVO suggestion = negotiationAdvisor.generateSuggestion(
                record.getResourceId(), record.getSupplierId());

        // 3. 写回建议（单表更新）
        updateRecord(negotiationId, suggestion);
        return suggestion;
    }

    /**
     * 加载谈判记录并校验存在性.
     */
    private NegotiationRecord loadRecord(Long negotiationId) {
        NegotiationRecord record = negotiationMapper.selectById(negotiationId);
        if (record == null || (record.getDeleted() != null && record.getDeleted() == 1)) {
            throw new BizException(ErrorCode.NEGOTIATION_NOT_FOUND);
        }
        return record;
    }

    /**
     * 写回谈判建议至记录.
     * <p>
     * 价格与策略/条款/风险作为一个整体写回：若 JSON 序列化失败则直接抛异常，
     * 不执行 updateById，避免"价格已更新但策略丢失"的数据不一致。
     */
    private void updateRecord(Long negotiationId, NegotiationSuggestionVO suggestion) {
        NegotiationRecord record = negotiationMapper.selectById(negotiationId);
        if (suggestion.getPriceRange() != null) {
            record.setFloorPrice(suggestion.getPriceRange().getFloorPrice());
            record.setCeilingPrice(suggestion.getPriceRange().getCeilingPrice());
            record.setSuggestedPrice(suggestion.getPriceRange().getSuggestedOffer());
        }
        try {
            record.setStrategies(OBJECT_MAPPER.writeValueAsString(suggestion.getStrategies()));
            record.setKeyTerms(OBJECT_MAPPER.writeValueAsString(suggestion.getKeyTerms()));
            record.setRiskWarnings(OBJECT_MAPPER.writeValueAsString(suggestion.getRiskWarnings()));
        } catch (JsonProcessingException e) {
            log.error("策略 JSON 序列化失败，谈判记录不予更新: negotiationId={}", negotiationId, e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "谈判策略序列化失败");
        }
        negotiationMapper.updateById(record);
    }
}
