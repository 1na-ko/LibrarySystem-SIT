package com.library.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(40000, "请求参数错误"),
    UNAUTHORIZED(40100, "未登录或Token已过期"),
    FORBIDDEN(40300, "无权限访问"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "数据冲突"),
    INTERNAL_ERROR(50000, "服务器内部错误"),
    SERVICE_UNAVAILABLE(50300, "服务暂时不可用"),

    // 业务 - 用户
    USERNAME_EXISTS(40001, "用户名已存在"),
    EMAIL_EXISTS(40002, "邮箱已注册"),
    USER_NOT_FOUND(40401, "用户不存在"),
    PASSWORD_ERROR(40101, "密码错误"),
    ACCOUNT_DISABLED(40301, "账号已被禁用"),

    // 业务 - 图书
    BOOK_NOT_FOUND(40402, "图书不存在"),
    BOOK_NOT_AVAILABLE(40201, "图书暂无可借库存"),
    ISBN_EXISTS(40003, "该ISBN已存在"),

    // 业务 - 借阅
    BORROW_LIMIT_EXCEEDED(40202, "借阅数量已达上限"),
    HAS_OVERDUE_BOOKS(40203, "存在超期未还图书，无法继续借阅"),
    BORROW_RECORD_NOT_FOUND(40403, "借阅记录不存在"),
    ALREADY_RETURNED(40204, "该图书已归还"),
    CANNOT_RENEW_OVERDUE(40205, "超期图书不可续借"),
    RENEW_LIMIT_EXCEEDED(40206, "续借次数已达上限"),
    CANNOT_RENEW_RESERVED(40207, "该书已被他人预约，不可续借"),

    // 业务 - 预约
    RESERVE_LIMIT_EXCEEDED(40208, "预约队列已满"),
    ALREADY_RESERVED(40901, "您已预约过该书"),
    CANNOT_RESERVE_BORROWED(40209, "您正在借阅该书，不可预约"),

    // AI
    AI_SERVICE_ERROR(50301, "AI服务异常"),
    AI_TIMEOUT(50302, "AI服务响应超时"),
    AI_RATE_LIMIT(50303, "AI请求频率过高，请稍后重试"),
    AI_CONTENT_FILTERED(50304, "内容不合规，已被过滤");

    private final int code;
    private final String message;
}
