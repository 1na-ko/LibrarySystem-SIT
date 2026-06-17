package com.library.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体（对应 operation_log 表）.
 * <p>
 * 由 {@code OperationLogAspect} 在管理员操作时异步写入。
 * 日志不可编辑、不可逻辑删除——运维审计用途。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@TableName("operation_log")
public class OperationLogEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 ID */
    private Long operatorId;

    /** 操作人用户名（冗余，方便审计直接查看） */
    private String operatorName;

    /** 操作模块 */
    private String module;

    /** 操作动作 */
    private String action;

    /** 操作目标描述，如 "用户ID:123" */
    private String target;

    /** 请求参数 JSON（截断至 2000 字符） */
    private String requestParams;

    /** 操作结果：SUCCESS / FAIL */
    private String result;

    /** 失败原因（截断至 500 字符） */
    private String errorMessage;

    /** 客户端 IP */
    private String clientIp;

    /** 执行耗时（毫秒） */
    private Long durationMs;

    /** 创建时间 */
    private LocalDateTime createTime;
}
