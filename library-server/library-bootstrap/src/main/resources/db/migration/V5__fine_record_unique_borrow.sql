-- ============================================================
-- V5: fine_record.borrow_id 唯一约束
-- 目的：防止 OverdueCheckJob（每日定时生成超期罚款）与 returnBook（还书时生成/更新罚款）
--      并发执行时为同一借阅记录产生重复罚款。
-- 依据：一条借阅记录（borrow_record.id）业务上至多对应一条罚款记录，故 borrow_id 业务唯一。
-- 前提：若存量数据已存在重复 borrow_id，需先去重再执行本迁移（开发环境可清空 fine_record）。
-- ============================================================

ALTER TABLE fine_record
    ADD UNIQUE INDEX uk_borrow_id (borrow_id);
