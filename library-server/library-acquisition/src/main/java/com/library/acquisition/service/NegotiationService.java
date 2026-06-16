package com.library.acquisition.service;

import com.library.acquisition.entity.NegotiationRecord;
import com.library.acquisition.vo.NegotiationSuggestionVO;

public interface NegotiationService {
    NegotiationRecord createNegotiation(Long resourceId, Long supplierId, Long negotiatorId);
    NegotiationSuggestionVO getSuggestion(Long negotiationId);
}
