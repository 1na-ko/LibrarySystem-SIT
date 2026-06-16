package com.library.common.exception;

import lombok.Getter;

/**
 * 全局错误码枚举.
 * <p>
 * 业务错误码采用 4 位数字：1xxx 借阅域 / 2xxx 知识图谱域 / 3xxx 采编域 / 4xxx 认证域。
 * HTTP 状态码由 {@link GlobalExceptionHandler} 依据错误码映射，此处仅定义业务语义。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
public enum ErrorCode {

    // ==================== 通用 ====================
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数不合法"),
    UNAUTHORIZED(401, "请先登录"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突，请检查后重试"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    RATE_LIMITED(429, "请求过于频繁，请稍后再试"),

    // ==================== 借阅域 (1xxx) ====================
    BOOK_NOT_FOUND(1000, "图书不存在"),
    BOOK_STOCK_EMPTY(1001, "图书库存不足"),
    BORROW_LIMIT_EXCEEDED(1002, "借阅数量超限，最多可借 %d 本"),
    ACCOUNT_FROZEN(1003, "账户已被冻结，若有疑问请联系图书馆管理员"),
    ALREADY_BORROWED(1004, "您已借阅该书，不可重复借阅"),
    OVERDUE_UNRETURNED(1005, "存在超期未还图书，请先归还后再借阅"),
    BOOK_ALREADY_RETURNED(1006, "该书已归还，不可重复操作"),
    RENEW_LIMIT_EXCEEDED(1007, "续借次数已达上限（最多 1 次）"),
    RENEW_OVERDUE(1008, "超期图书不可续借，请先归还"),
    RENEW_RESERVED(1009, "该书已被其他读者预约，不可续借"),
    BOOK_AVAILABLE(1010, "图书有库存，请直接借阅"),
    ALREADY_RESERVED(1011, "您已预约该书，不可重复预约"),
    RESERVATION_NOT_FOUND(1012, "预约记录不存在"),
    RESERVATION_EXPIRED(1013, "预约已过期"),
    BORROW_RECORD_NOT_FOUND(1014, "借阅记录不存在"),
    CATEGORY_NOT_FOUND(1015, "分类不存在"),

    // ==================== 知识图谱域 (2xxx) ====================
    KG_BUILD_FAILED(2001, "知识图谱构建失败"),
    KG_ENTITY_NOT_FOUND(2002, "知识实体不存在"),
    KG_GRAPH_EMPTY(2003, "该图书暂无知识图谱数据"),
    KG_NEO4J_UNAVAILABLE(2004, "图数据库服务不可用"),

    // ==================== 智能采编域 (3xxx) ====================
    PREDICTION_DATA_INSUFFICIENT(3001, "采购预测数据不足，需要至少 6 个月的历史数据"),
    SUPPLIER_NOT_FOUND(3002, "供应商不存在"),
    NEGOTIATION_NOT_FOUND(3003, "谈判记录不存在"),
    DUPLICATE_ISBN(3004, "ISBN 已存在，无法重复编目"),
    ELECTRONIC_RESOURCE_NOT_FOUND(3005, "电子资源不存在"),
    LLM_UNAVAILABLE(3006, "AI 服务暂时不可用，已启用降级策略"),

    // ==================== 认证域 (4xxx) ====================
    USERNAME_EXISTS(4001, "用户名已存在"),
    USER_NOT_FOUND(4002, "用户不存在"),
    BAD_CREDENTIALS(4003, "用户名或密码错误"),
    TOKEN_EXPIRED(4004, "登录已过期，请重新登录"),
    TOKEN_INVALID(4005, "令牌无效"),
    USER_DISABLED(4006, "账户已被禁用"),

    // ==================== 推荐域 (5xxx) ====================
    RECOMMEND_PARALLEL_TIMEOUT(5001, "推荐计算超时，已返回部分结果");

    /** 业务错误码 */
    private final int code;
    /** 错误描述（支持 String.format 占位符） */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据 code 查找枚举值.
     *
     * @param code 业务错误码
     * @return 对应的枚举值，未匹配时返回 INTERNAL_ERROR
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode ec : values()) {
            if (ec.code == code) {
                return ec;
            }
        }
        return INTERNAL_ERROR;
    }
}
