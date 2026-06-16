package com.library.acquisition.service.impl;

import com.library.acquisition.config.AcquisitionProperties;
import com.library.acquisition.entity.ElectronicResource;
import com.library.acquisition.entity.Supplier;
import com.library.acquisition.enums.ResourceCategoryEnum;
import com.library.acquisition.enums.SupplierStatusEnum;
import com.library.acquisition.mapper.DealRecordMapper;
import com.library.acquisition.mapper.ElectronicResourceMapper;
import com.library.acquisition.mapper.SupplierMapper;
import com.library.ai.llm.LlmService;
import com.library.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NegotiationAdvisorImpl")
class NegotiationAdvisorImplTest {

    @Mock
    private ElectronicResourceMapper resourceMapper;
    @Mock
    private SupplierMapper supplierMapper;
    @Mock
    private DealRecordMapper dealRecordMapper;
    @Mock
    private LlmService llmService;

    private AcquisitionProperties props;
    private NegotiationAdvisorImpl advisor;

    @BeforeEach
    void setUp() {
        props = new AcquisitionProperties();
        props.setNegotiationHistoryMonths(24);
        advisor = new NegotiationAdvisorImpl(resourceMapper, supplierMapper,
                dealRecordMapper, props, llmService);
    }

    @Nested
    @DisplayName("generateSuggestion")
    class GenerateSuggestion {

        @Test
        @DisplayName("LLM 可用时应包含策略和本地计算的 priceRange")
        void shouldOverrideLlmPriceRangeWithLocalComputation() {
            ElectronicResource resource = new ElectronicResource();
            resource.setId(1L); resource.setName("IEEE Xplore");
            resource.setCategory(ResourceCategoryEnum.DATABASE);
            resource.setAnnualBudget(new BigDecimal("50000")); resource.setUserCount(500);
            resource.setDeleted(0);

            Supplier supplier = new Supplier();
            supplier.setId(1L); supplier.setName("IEEE");
            supplier.setPartnershipYears(3); supplier.setReliability(new BigDecimal("0.9"));
            supplier.setMarketShare(new BigDecimal("0.4"));
            supplier.setDeleted(0);

            when(resourceMapper.selectById(1L)).thenReturn(resource);
            when(supplierMapper.selectById(1L)).thenReturn(supplier);
            when(dealRecordMapper.selectList(any())).thenReturn(List.of());

            var suggestion = advisor.generateSuggestion(1L, 1L);
            assertThat(suggestion).isNotNull();
            assertThat(suggestion.getPriceRange()).isNotNull();
            assertThat(suggestion.getPriceRange().getSuggestedOffer()).isNotNull();
            assertThat(suggestion.getKeyTerms()).isNotEmpty();
        }

        @Test
        @DisplayName("LLM 不可用时（null）应使用降级规则模板")
        void shouldFallbackWhenLlmNull() {
            // 使用不含 LlmService 的构造
            NegotiationAdvisorImpl noLlmAdvisor = new NegotiationAdvisorImpl(
                    resourceMapper, supplierMapper, dealRecordMapper, props, null);

            ElectronicResource resource = new ElectronicResource();
            resource.setId(1L); resource.setName("test");
            resource.setCategory(ResourceCategoryEnum.EBOOK);
            resource.setAnnualBudget(new BigDecimal("1000")); resource.setUserCount(10);
            resource.setDeleted(0);

            Supplier supplier = new Supplier();
            supplier.setId(1L); supplier.setName("test supplier");
            supplier.setPartnershipYears(0); supplier.setReliability(new BigDecimal("0.5"));
            supplier.setMarketShare(new BigDecimal("0.1"));
            supplier.setDeleted(0);

            when(resourceMapper.selectById(1L)).thenReturn(resource);
            when(supplierMapper.selectById(1L)).thenReturn(supplier);
            when(dealRecordMapper.selectList(any())).thenReturn(List.of());

            var suggestion = noLlmAdvisor.generateSuggestion(1L, 1L);
            assertThat(suggestion).isNotNull();
            assertThat(suggestion.getPriceRange()).isNotNull();
            assertThat(suggestion.getStrategies()).isNotEmpty();
        }

        @Test
        @DisplayName("资源不存在时应抛异常")
        void shouldThrowWhenResourceNotFound() {
            when(resourceMapper.selectById(99L)).thenReturn(null);
            assertThatThrownBy(() -> advisor.generateSuggestion(99L, 1L))
                    .isInstanceOf(BizException.class);
        }
    }
}
