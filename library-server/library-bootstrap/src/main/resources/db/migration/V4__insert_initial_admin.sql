-- =============================================================================
-- V4: 初始管理员账号种子
-- 用户名: admin  密码: Admin@123456  (BCrypt cost=12)
-- ⚠️ 安全提示：生产环境首次登录后务必修改默认密码，或通过环境变量 ADMIN_INIT_PASSWORD
--    覆盖（当前为开发环境固定凭据，便于联调）。
-- 哈希由 BCryptPasswordEncoder(12) 离线生成，盐内嵌于哈希字符串。
-- =============================================================================

INSERT INTO sys_user (username, password_hash, real_name, role, email, status, max_books, deleted)
VALUES ('admin',
        '$2a$12$jUYkKnNwsNRkA0RKsMl5f.E1Vk5VWhWphbwqjroE7aTvrvGj.IuBe',
        '系统管理员',
        'ADMIN',
        'admin@library.edu.cn',
        'ACTIVE',
        15,
        0);
