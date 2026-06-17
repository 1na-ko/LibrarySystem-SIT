-- ============================================================
-- V7: 操作日志表
-- 目的：记录管理员的关键操作（状态变更、图书编目等），
--      支撑运维审计与事后追溯。
-- 约定：操作日志不可编辑、不可删除——仅追加写入。
-- ============================================================

CREATE TABLE operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(50) COMMENT '操作人用户名（冗余，方便审计查看）',
    module VARCHAR(50) COMMENT '操作模块，如"用户管理"',
    action VARCHAR(50) COMMENT '操作动作，如"状态变更"',
    target VARCHAR(200) COMMENT '操作目标描述',
    request_params VARCHAR(2000) COMMENT '请求参数JSON（截断）',
    result VARCHAR(10) COMMENT '操作结果：SUCCESS / FAIL',
    error_message VARCHAR(500) COMMENT '失败原因（截断）',
    client_ip VARCHAR(45) COMMENT '客户端IP（IPv4/IPv6）',
    duration_ms BIGINT COMMENT '执行耗时（毫秒）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_operator (operator_id),
    INDEX idx_create_time (create_time),
    INDEX idx_module_action (module, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
