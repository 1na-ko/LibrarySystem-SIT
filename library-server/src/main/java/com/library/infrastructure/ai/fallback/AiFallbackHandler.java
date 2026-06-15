package com.library.infrastructure.ai.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AiFallbackHandler {

    private final StringRedisTemplate redisTemplate;

    public AiFallbackHandler(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String getCached(String promptHash) {
        return redisTemplate.opsForValue().get("ai:cache:" + promptHash);
    }

    public void cache(String promptHash, String response, int ttlSeconds) {
        redisTemplate.opsForValue().set("ai:cache:" + promptHash, response, ttlSeconds, TimeUnit.SECONDS);
    }

    public String fallback(String context) {
        log.warn("AI服务降级, 使用默认策略: {}", context);
        return "{\"message\": \"AI服务暂时不可用，请稍后重试\"}";
    }
}
