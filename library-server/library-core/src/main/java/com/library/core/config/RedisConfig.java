package com.library.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置.
 * <p>
 * 定义 {@code RedisTemplate<String, Object>} Bean：key 用 {@link StringRedisSerializer}，
 * value 用 {@link GenericJackson2JsonRedisSerializer}（含类型信息，兼容 Long/String/对象）。
 * <p>
 * Spring Boot Data Redis 默认仅装配 {@code StringRedisTemplate} 与 {@code RedisTemplate<Object, Object>}，
 * 泛型不匹配 {@code <String, Object>}，故需显式定义。供预约 ZSET（ReservationServiceImpl/
 * ReservationNotifier）、借阅锁（BorrowServiceImpl）、搜索缓存（BookSearchServiceImpl）、
 * 指标（MetricsConfig）等组件注入。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
