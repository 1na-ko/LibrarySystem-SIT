package com.library.common.aspect;

import com.library.common.annotation.RateLimit;
import com.library.common.exception.BusinessException;
import com.library.common.exception.ErrorCode;
import com.library.common.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = buildKey(joinPoint, rateLimit);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, rateLimit.window(), rateLimit.timeUnit());
        }
        if (count != null && count > rateLimit.limit()) {
            log.warn("接口限流触发: key={}, count={}, limit={}", key, count, rateLimit.limit());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE.getCode(), rateLimit.message());
        }
        return joinPoint.proceed();
    }

    private String buildKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String prefix = rateLimit.key().isEmpty()
                ? joinPoint.getSignature().toShortString()
                : rateLimit.key();
        Long userId = UserContext.getUserId();
        String ip = getClientIp();
        return String.format("rate:limit:%s:%s:%s", prefix, userId != null ? userId : "anon", ip);
    }

    private String getClientIp() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes()).getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
            if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
            return ip != null ? ip : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
