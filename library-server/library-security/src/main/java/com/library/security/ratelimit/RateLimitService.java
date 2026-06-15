package com.library.security.ratelimit;

/**
 * 限流服务.
 * <p>
 * 封装 Redis 令牌桶判定逻辑，Filter 层通过此接口获取结果，避免 Filter 直接耦合 Redis。
 * 便于单元测试 mock、未来替换为真实集成测试。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface RateLimitService {

    /**
     * 尝试获取一个令牌.
     *
     * @param key          限流 key（如 "login:127.0.0.1" 或 "auth:123"）
     * @param capacity     桶容量
     * @param refillPerSec 每秒补充令牌数
     * @return 限流判定结果
     */
    RateLimitResult tryAcquire(String key, int capacity, double refillPerSec);
}
