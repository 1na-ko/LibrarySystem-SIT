package com.library.infrastructure.ai;

import com.library.infrastructure.ai.dto.ChatRequest;
import com.library.infrastructure.ai.dto.ChatResponse;
import reactor.core.publisher.Flux;

public interface AIClient {
    ChatResponse chatCompletion(ChatRequest request);
    Flux<String> chatCompletionStream(ChatRequest request);
}
