package com.library.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

/**
 * Redis Lua 脚本 Bean 注册.
 * <p>
 * 脚本位于 {@code classpath:scripts/}，由 {@code DefaultRedisScript} 加载并缓存，
 * 经 EVALSHA 执行以减少网络开销。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Configuration
public class LuaScriptConfig {

    /**
     * Refresh Token 轮换脚本（返回 0=成功 / 1=重放）.
     */
    @Bean
    public DefaultRedisScript<Long> rotateScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("scripts/refresh_rotate.lua")));
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 令牌桶限流脚本（返回 {allowed, remaining, resetEpochSec}）.
     */
    @Bean
    public DefaultRedisScript<List> tokenBucketScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("scripts/token_bucket.lua")));
        script.setResultType(List.class);
        return script;
    }
}
