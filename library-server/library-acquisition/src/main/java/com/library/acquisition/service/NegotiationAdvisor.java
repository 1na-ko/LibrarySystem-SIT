package com.library.acquisition.service;

import com.library.acquisition.vo.NegotiationSuggestionVO;

public interface NegotiationAdvisor {
    NegotiationSuggestionVO generateSuggestion(Long resourceId, Long supplierId);
}
