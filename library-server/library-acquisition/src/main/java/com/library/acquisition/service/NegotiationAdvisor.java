package com.library.acquisition.service;

import com.library.acquisition.dto.PriceRangeDTO;
import com.library.acquisition.vo.NegotiationSuggestionVO;
import reactor.core.publisher.Flux;

public interface NegotiationAdvisor {
    NegotiationSuggestionVO generateSuggestion(Long resourceId, Long supplierId);

    /**
     * 计算价格区间（本地计算，秒回）— SSE 流式端点的第一个事件.
     */
    PriceRangeDTO calculatePriceRange(Long resourceId, Long supplierId);

    /**
     * 流式生成谈判建议正文（LLM 逐 token 返回，纯文本）.
     * <p>调用方应订阅 Flux 逐 token 推送给前端 SSE，LLM 不可用时返回降级文案的 Flux.
     */
    Flux<String> streamSuggestionText(Long resourceId, Long supplierId);
}
