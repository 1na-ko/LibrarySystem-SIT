package com.library.security.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.annotation.OperationLog;
import com.library.common.exception.BizException;
import com.library.core.entity.OperationLogEntity;
import com.library.core.service.OperationLogService;
import com.library.security.context.LoginUser;
import com.library.security.context.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 操作日志 AOP 切面.
 * <p>
 * 拦截标注 {@link OperationLog} 的 Controller 方法，异步记录操作日志到
 * {@code operation_log} 表。日志写入失败仅 log.error，不抛异常——
 * 审计日志为辅助功能，不应阻塞主流程。
 * <p>
 * 日志写入使用 {@link CompletableFuture#runAsync} 异步执行，
 * 避免阻塞业务主线程。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    private static final int MAX_PARAMS_LENGTH = 2000;
    private static final int MAX_ERROR_LENGTH = 500;

    /**
     * 敏感字段名匹配（不区分大小写）：命中后其值脱敏为 ***，避免密码/令牌/密钥泄露到 operation_log 表.
     * <p>
     * 兑现 {@link com.library.common.annotation.OperationLog#logParams()} 的 Javadoc 承诺——
     * "敏感字段（password/token/secret 等）将由切面自动脱敏为 ***"。
     */
    private static final java.util.regex.Pattern SENSITIVE_FIELD_PATTERN = java.util.regex.Pattern.compile(
            "(\"(?:password|passwd|passwordHash|password_hash|secret|token|accessToken|access_token|refreshToken|refresh_token|credential|apiKey|api_key)\"\\s*:\\s*)\"[^\"]*\"",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * 拦截 @OperationLog 方法，环绕记录操作日志.
     */
    @Around("@annotation(opLog)")
    public Object logOperation(ProceedingJoinPoint pjp, OperationLog opLog) throws Throwable {
        long start = System.currentTimeMillis();

        LoginUser operator = SecurityUtils.getCurrentUser();
        String clientIp = extractClientIp();
        String target = buildTarget(pjp, opLog);
        String paramsJson = opLog.logParams()
                ? truncate(maskSensitive(toJson(pjp.getArgs())), MAX_PARAMS_LENGTH)
                : null;

        OperationLogEntity record = new OperationLogEntity();
        record.setOperatorId(operator != null ? operator.getUserId() : null);
        record.setOperatorName(operator != null ? operator.getUsername() : "SYSTEM");
        record.setModule(opLog.module());
        record.setAction(opLog.action());
        record.setTarget(target);
        record.setRequestParams(paramsJson);
        record.setClientIp(clientIp);
        record.setCreateTime(LocalDateTime.now());

        try {
            Object result = pjp.proceed();
            record.setResult("SUCCESS");
            record.setDurationMs(System.currentTimeMillis() - start);
            // logResult=true 时记录返回摘要到 requestParams 后缀（errorMessage 仅用于 FAIL）
            if (opLog.logResult()) {
                String summary = truncate(toJson(result), MAX_ERROR_LENGTH);
                record.setRequestParams(
                        (record.getRequestParams() != null ? record.getRequestParams() + " | " : "")
                                + "result:" + summary);
            }
            asyncInsert(record);
            return result;
        } catch (BizException e) {
            record.setResult("FAIL");
            record.setErrorMessage(truncate(e.getMessage(), MAX_ERROR_LENGTH));
            record.setDurationMs(System.currentTimeMillis() - start);
            asyncInsert(record);
            throw e;
        } catch (Exception e) {
            record.setResult("FAIL");
            record.setErrorMessage(truncate(e.getMessage(), MAX_ERROR_LENGTH));
            record.setDurationMs(System.currentTimeMillis() - start);
            asyncInsert(record);
            throw e;
        }
    }

    /**
     * 异步写入日志（fire-and-forget，失败不影响主流程）.
     */
    void asyncInsert(OperationLogEntity record) {
        CompletableFuture.runAsync(() -> {
            try {
                operationLogService.insert(record);
            } catch (Exception e) {
                log.error("操作日志写入失败: module={}, action={}, operator={}",
                        record.getModule(), record.getAction(), record.getOperatorName(), e);
            }
        });
    }

    /**
     * 从 RequestContextHolder 提取客户端 IP.
     */
    String extractClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("无法获取客户端 IP: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * 构建操作目标描述.
     * <p>
     * 从方法签名中提取第一个参数作为目标描述，
     * 例如 {@code updateStatus(@PathVariable Long id, ...)} → "用户ID:{id}".
     */
    String buildTarget(ProceedingJoinPoint pjp, OperationLog opLog) {
        if (!(pjp.getSignature() instanceof MethodSignature signature)) {
            return null;
        }
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = pjp.getArgs();

        if (args.length == 0) {
            return null;
        }

        // 尝试找到第一个 PathVariable 参数构建 target
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            // 优先使用 @PathVariable 参数
            if (param.isAnnotationPresent(org.springframework.web.bind.annotation.PathVariable.class)) {
                String name = param.getAnnotation(org.springframework.web.bind.annotation.PathVariable.class).value();
                if (name.isEmpty()) {
                    name = param.getName();
                }
                return name + ":" + args[i];
            }
        }

        // 降级：使用第一个参数的类型简称
        if (args[0] != null) {
            return args[0].getClass().getSimpleName() + ":" + args[0];
        }
        return null;
    }

    /**
     * 将对象序列化为 JSON 字符串.
     */
    String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    /**
     * 截断字符串至指定长度.
     */
    String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * 脱敏 JSON 字符串中的敏感字段值.
     * <p>
     * 将 password / token / secret / credential / apiKey 等字段的值替换为 {@code "***"}，
     * 防止敏感信息随操作参数写入 {@code operation_log} 表。
     * 在 {@link #toJson(Object)} 之后、{@link #truncate(String, int)} 之前调用，
     * 兑现 {@link com.library.common.annotation.OperationLog#logParams()} 的脱敏承诺。
     *
     * @param json 原始 JSON 字符串
     * @return 脱敏后的 JSON 字符串
     */
    String maskSensitive(String json) {
        if (json == null) {
            return null;
        }
        return SENSITIVE_FIELD_PATTERN.matcher(json).replaceAll("$1\"***\"");
    }
}
