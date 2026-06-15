-- =============================================================================
-- V3: 补充高频查询字段的复合索引
-- V1 基线已为各表建立单列索引，此处针对多条件组合查询场景补充复合索引
-- =============================================================================

-- 借阅记录：按用户+状态查询（我的借阅列表接口最高频查询）
-- V1 已有 idx_user_id 和 idx_status 单列索引，复合索引可覆盖 WHERE user_id=? AND status=? 场景
ALTER TABLE borrow_record ADD INDEX idx_user_status (user_id, status);

-- 借阅记录：按用户+还书日期查询（借阅历史按年份筛选）
ALTER TABLE borrow_record ADD INDEX idx_user_return (user_id, return_date);

-- 借阅记录：超期检查定时任务（每日凌晨扫描 status IN ('BORROWED','RENEWED') AND due_date < NOW()）
ALTER TABLE borrow_record ADD INDEX idx_status_due_date (status, due_date);

-- 预约记录：按图书+状态查询（还书后查找排队读者）
ALTER TABLE reservation ADD INDEX idx_book_status (book_id, status);

-- 预约记录：按用户+状态查询（我的预约列表）
ALTER TABLE reservation ADD INDEX idx_user_status (user_id, status);

-- 罚款记录：按借阅 ID 查询（归还时查询关联罚款）
-- V1 已有 idx_borrow_id 单列索引，此处补充复合索引覆盖 paid 筛选
ALTER TABLE fine_record ADD INDEX idx_borrow_paid (borrow_id, paid);

-- 成交记录：按供应商+类别查询（谈判时查询同类资源历史成交价）
ALTER TABLE deal_record ADD INDEX idx_supplier_category (supplier_id, category);

-- 电子资源：按类别+状态查询（采编列表筛选）
ALTER TABLE electronic_resource ADD INDEX idx_category_status (category, status);
