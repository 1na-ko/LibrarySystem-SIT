package com.library.infrastructure.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "library.ai")
public class AiProperties {
    private String provider;
    private String baseUrl;
    private String apiKey;
    private Models models = new Models();
    private Parameters parameters = new Parameters();
    private Retry retry = new Retry();
    private Fallback fallback = new Fallback();
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class Models {
        private String chat = "deepseek-chat";
        private String reasoner = "deepseek-reasoner";
    }

    @Data
    public static class Parameters {
        private double defaultTemperature = 0.7;
        private int defaultMaxTokens = 2048;
        private Duration timeout = Duration.ofSeconds(60);
    }

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private double backoffMultiplier = 2.0;
    }

    @Data
    public static class Fallback {
        private int cacheTtl = 3600;
    }

    @Data
    public static class RateLimit {
        private int maxRequestsPerMinute = 60;
    }
}
