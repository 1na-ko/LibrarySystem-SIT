-- ============================================================
-- V1: 初始化数据库骨架
-- 各业务模块的完整 DDL 将在对应 feature 分支中补充
-- 本脚本仅建立数据库迁移基线，确保 Flyway 正常启动
-- ============================================================

-- Flyway 基线占位表（后续将由各模块的迁移脚本替代为真实表结构）
CREATE TABLE IF NOT EXISTS flyway_baseline (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Flyway 迁移基线表（框架占位，后续替换）';
