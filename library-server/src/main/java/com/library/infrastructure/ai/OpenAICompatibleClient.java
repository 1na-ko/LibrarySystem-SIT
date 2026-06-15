package com.library.infrastructure.ai;

import com.library.infrastructure.ai.dto.ChatRequest;
import com.library.infrastructure.ai.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Slf4j
@Component
public class OpenAICompatibleClient implements AIClient {

    private final WebClient webClient;
    private final AiProperties properties;

    public OpenAICompatibleClient(WebClient.Builder builder, AiProperties properties) {
        this.properties = properties;
        this.webClient = builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
        log.info("AI客户端初始化完成: provider={}, baseUrl={}", properties.getProvider(), properties.getBaseUrl());
    }

    @Override
    public ChatResponse chatCompletion(ChatRequest request) {
        if (request.getModel() == null) {
            request.setModel(properties.getModels().getChat());
        }
        request.setStream(false);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .timeout(properties.getParameters().getTimeout())
                .block();
    }

    @Override
    public Flux<String> chatCompletionStream(ChatRequest request) {
        if (request.getModel() == null) {
            request.setModel(properties.getModels().getChat());
        }
        request.setStream(true);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(120));
    }
}
