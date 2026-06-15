package com.library.security.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 限流配置属性.
 * <p>
 * 绑定 {@code application.yml} 中 {@code ratelimit.*} 配置键。未配置时使用默认阈值：
 * <ul>
 *   <li>login / register（按 IP 防爆破）：20 次/分钟</li>
 *   <li>authenticated（按 userId）：100 次/分钟</li>
 *   <li>anonymous（按 IP）：100 次/分钟</li>
 * </ul>
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {

    /** 登录防爆破桶（按 IP） */
    private Bucket login = new Bucket(20, 20);
    /** 注册防爆破桶（按 IP） */
    private Bucket register = new Bucket(20, 20);
    /** 认证用户桶（按 userId） */
    private Bucket authenticated = new Bucket(100, 100);
    /** 匿名用户桶（按 IP） */
    private Bucket anonymous = new Bucket(100, 100);

    /**
     * 令牌桶配置.
     */
    @Data
    public static class Bucket {

        /** 桶容量（最大并发令牌数） */
        private int capacity;
        /** 每分钟补充令牌数 */
        private int refillPerMin;

        public Bucket() {
        }

        public Bucket(int capacity, int refillPerMin) {
            this.capacity = capacity;
            this.refillPerMin = refillPerMin;
        }

        /** 每秒补充令牌数 */
        public double refillPerSec() {
            return refillPerMin / 60.0;
        }
    }
}
