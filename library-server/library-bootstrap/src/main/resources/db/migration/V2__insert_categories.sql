-- =============================================================================
-- V2: 插入图书分类种子数据（中图法大类 + 常用子类）
-- 配合 library-common 的 CategoryService.getTree() 提供初始分类树
-- =============================================================================

-- 顶级分类
INSERT INTO category (id, name, parent_id, sort_order, deleted, create_time, update_time) VALUES
(1,  '计算机科学',     NULL, 1, 0, NOW(), NOW()),
(2,  '文学',           NULL, 2, 0, NOW(), NOW()),
(3,  '历史',           NULL, 3, 0, NOW(), NOW()),
(4,  '数学',           NULL, 4, 0, NOW(), NOW()),
(5,  '物理学',         NULL, 5, 0, NOW(), NOW()),
(6,  '化学',           NULL, 6, 0, NOW(), NOW()),
(7,  '生物学',         NULL, 7, 0, NOW(), NOW()),
(8,  '经济学',         NULL, 8, 0, NOW(), NOW()),
(9,  '管理学',         NULL, 9, 0, NOW(), NOW()),
(10, '哲学',           NULL, 10, 0, NOW(), NOW()),
(11, '法学',           NULL, 11, 0, NOW(), NOW()),
(12, '艺术',           NULL, 12, 0, NOW(), NOW()),
(13, '医学',           NULL, 13, 0, NOW(), NOW()),
(14, '工程技术',       NULL, 14, 0, NOW(), NOW()),
(15, '教育学',         NULL, 15, 0, NOW(), NOW());

-- 计算机科学子分类
INSERT INTO category (id, name, parent_id, sort_order, deleted, create_time, update_time) VALUES
(101, '编程语言',      1, 1, 0, NOW(), NOW()),
(102, '数据结构与算法', 1, 2, 0, NOW(), NOW()),
(103, '操作系统',      1, 3, 0, NOW(), NOW()),
(104, '计算机网络',    1, 4, 0, NOW(), NOW()),
(105, '数据库',        1, 5, 0, NOW(), NOW()),
(106, '人工智能',      1, 6, 0, NOW(), NOW()),
(107, '软件工程',      1, 7, 0, NOW(), NOW()),
(108, '信息安全',      1, 8, 0, NOW(), NOW());

-- 文学子分类
INSERT INTO category (id, name, parent_id, sort_order, deleted, create_time, update_time) VALUES
(201, '中国文学',      2, 1, 0, NOW(), NOW()),
(202, '外国文学',      2, 2, 0, NOW(), NOW()),
(203, '文学理论',      2, 3, 0, NOW(), NOW());

-- 历史子分类
INSERT INTO category (id, name, parent_id, sort_order, deleted, create_time, update_time) VALUES
(301, '中国历史',      3, 1, 0, NOW(), NOW()),
(302, '世界历史',      3, 2, 0, NOW(), NOW());

-- 数学子分类
INSERT INTO category (id, name, parent_id, sort_order, deleted, create_time, update_time) VALUES
(401, '高等数学',      4, 1, 0, NOW(), NOW()),
(402, '线性代数',      4, 2, 0, NOW(), NOW()),
(403, '概率论与数理统计', 4, 3, 0, NOW(), NOW()),
(404, '离散数学',      4, 4, 0, NOW(), NOW());

-- 经济学子分类
INSERT INTO category (id, name, parent_id, sort_order, deleted, create_time, update_time) VALUES
(801, '宏观经济学',    8, 1, 0, NOW(), NOW()),
(802, '微观经济学',    8, 2, 0, NOW(), NOW()),
(803, '金融学',        8, 3, 0, NOW(), NOW());

-- 人工智能子分类（深度方向）
INSERT INTO category (id, name, parent_id, sort_order, deleted, create_time, update_time) VALUES
(1061, '机器学习',     106, 1, 0, NOW(), NOW()),
(1062, '深度学习',     106, 2, 0, NOW(), NOW()),
(1063, '自然语言处理', 106, 3, 0, NOW(), NOW()),
(1064, '计算机视觉',   106, 4, 0, NOW(), NOW());
