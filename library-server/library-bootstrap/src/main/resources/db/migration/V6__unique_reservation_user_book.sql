-- ============================================================
-- V6: reservation (user_id, book_id) 唯一约束（仅 WAITING 状态）
-- 目的：防止并发请求下同一用户对同一本书创建多条 WAITING 预约记录，
--      消除 ReservationServiceImpl.reserve() 中 selectCount → insert
--      的 TOCTOU 竞态窗口（阶段 0-8 综合审计 P1-2）。
-- 依据：同一本书的预约队列中，每位用户最多可有一条 WAITING 状态的记录。
-- 注意：唯一键仅约束 status='WAITING'，用户取消后重新预约时旧记录
--       status 变为 CANCELLED 不再命中该约束，故不会阻止重新排队。
-- 前提：若存量数据已存在重复 (user_id, book_id, WAITING) 组合，
--       需先去重再执行本迁移（开发环境可执行：
--         DELETE r1 FROM reservation r1
--         INNER JOIN reservation r2
--           ON r1.user_id = r2.user_id AND r1.book_id = r2.book_id
--           AND r1.status = 'WAITING' AND r2.status = 'WAITING'
--           AND r1.id < r2.id;
--       ）。
-- ============================================================

ALTER TABLE reservation
    ADD UNIQUE INDEX uk_user_book_waiting (user_id, book_id, status);
