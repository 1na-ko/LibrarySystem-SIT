-- ============================================================
-- V1: 初始化数据库 schema（核心业务表）
-- 覆盖架构设计文档 §5.2 定义的全部 10 张表 + 统一的逻辑删除字段
-- 设计要点：
--   1. 所有业务表统一 utf8mb4，便于中文与 emoji 存储
--   2. 统一审计列 create_time / update_time，由数据库自动维护
--   3. 统一逻辑删除列 deleted（0=未删除 / 1=已删除），与 MyBatis-Plus
--      全局配置 logic-delete-field 保持一致，实现软删除
--   4. 主键 BIGINT 自增，符合雪花/自增兼容约定
-- ============================================================

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
CREATE TABLE sys_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名/学号/工号',
    password_hash   VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码哈希',
    real_name       VARCHAR(100) COMMENT '真实姓名',
    role            ENUM('STUDENT','TEACHER','LIBRARIAN','ACQUISITOR','ADMIN') NOT NULL DEFAULT 'STUDENT' COMMENT '角色',
    email           VARCHAR(200) COMMENT '邮箱',
    phone           VARCHAR(20)  COMMENT '手机号',
    max_books       TINYINT      NOT NULL DEFAULT 5 COMMENT '最大可借数量',
    status          ENUM('ACTIVE','FROZEN','DISABLED') NOT NULL DEFAULT 'ACTIVE' COMMENT '账户状态',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删除, 1=已删除）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_role    (role),
    INDEX idx_status  (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. 图书分类表（自引用，多级树形结构，须先于 book 创建）
-- ------------------------------------------------------------
CREATE TABLE category (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL COMMENT '分类名称',
    parent_id       BIGINT COMMENT '父分类ID（支持多级分类树）',
    sort_order      INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent  (parent_id),
    INDEX idx_deleted (deleted),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书分类表';

-- ------------------------------------------------------------
-- 3. 图书表
-- ------------------------------------------------------------
CREATE TABLE book (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    isbn            VARCHAR(20)  NOT NULL UNIQUE COMMENT 'ISBN 号',
    title           VARCHAR(500) NOT NULL COMMENT '书名',
    author          VARCHAR(200) NOT NULL COMMENT '作者',
    publisher       VARCHAR(200) COMMENT '出版社',
    pub_date        DATE COMMENT '出版日期',
    category_id     BIGINT COMMENT '分类ID',
    total_copies    INT NOT NULL DEFAULT 1 COMMENT '总册数',
    avail_copies    INT NOT NULL DEFAULT 1 COMMENT '可借册数',
    description     TEXT COMMENT '内容简介',
    cover_url       VARCHAR(500) COMMENT '封面URL',
    location        VARCHAR(100) COMMENT '馆藏位置（如：A区-3架-12层）',
    keywords        VARCHAR(500) COMMENT '关键词（逗号分隔，用于搜索）',
    borrow_count    INT NOT NULL DEFAULT 0 COMMENT '累计借阅次数',
    version         INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_title    (title),
    INDEX idx_author   (author),
    INDEX idx_category (category_id),
    INDEX idx_keywords (keywords),
    INDEX idx_deleted  (deleted),
    FULLTEXT INDEX ft_title_desc (title, description),
    CONSTRAINT fk_book_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书表';

-- ------------------------------------------------------------
-- 4. 借阅记录表
-- ------------------------------------------------------------
CREATE TABLE borrow_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    book_id         BIGINT NOT NULL COMMENT '图书ID',
    borrow_date     DATE NOT NULL COMMENT '借阅日期',
    due_date        DATE NOT NULL COMMENT '应还日期',
    return_date     DATE COMMENT '实际归还日期',
    renew_count     TINYINT NOT NULL DEFAULT 0 COMMENT '续借次数（最多1次）',
    status          ENUM('BORROWED','RENEWED','RETURNED','OVERDUE') NOT NULL DEFAULT 'BORROWED' COMMENT '状态',
    fine_amount     DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '罚款金额',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id  (user_id),
    INDEX idx_book_id  (book_id),
    INDEX idx_status   (status),
    INDEX idx_due_date (due_date),
    INDEX idx_deleted  (deleted),
    CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_borrow_book FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='借阅记录表';

-- ------------------------------------------------------------
-- 5. 预约表
-- ------------------------------------------------------------
CREATE TABLE reservation (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    book_id         BIGINT NOT NULL COMMENT '图书ID',
    reserve_time    DATETIME NOT NULL COMMENT '预约时间',
    notify_time     DATETIME COMMENT '通知时间',
    expire_time     DATETIME COMMENT '过期时间（通知后48h）',
    status          ENUM('WAITING','NOTIFIED','RESERVED','EXPIRED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'WAITING' COMMENT '状态',
    queue_position  INT COMMENT '排队序号',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id  (user_id),
    INDEX idx_book_id  (book_id),
    INDEX idx_status   (status),
    INDEX idx_deleted  (deleted),
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_book FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约表';

-- ------------------------------------------------------------
-- 6. 罚款记录表
-- ------------------------------------------------------------
CREATE TABLE fine_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    borrow_id       BIGINT NOT NULL COMMENT '关联借阅记录ID',
    amount          DECIMAL(8,2) NOT NULL COMMENT '罚款金额',
    reason          VARCHAR(500) COMMENT '罚款原因',
    paid            TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已缴（0=未缴, 1=已缴）',
    paid_date       DATETIME COMMENT '缴费日期',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_borrow_id (borrow_id),
    INDEX idx_paid      (paid),
    INDEX idx_deleted   (deleted),
    CONSTRAINT fk_fine_borrow FOREIGN KEY (borrow_id) REFERENCES borrow_record(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='罚款记录表';

-- ------------------------------------------------------------
-- 7. 供应商表
-- ------------------------------------------------------------
CREATE TABLE supplier (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(200) NOT NULL COMMENT '供应商名称',
    contact_person    VARCHAR(100) COMMENT '联系人',
    contact_email     VARCHAR(200) COMMENT '联系邮箱',
    contact_phone     VARCHAR(20)  COMMENT '联系电话',
    partnership_years INT NOT NULL DEFAULT 0 COMMENT '合作年限',
    reliability       DECIMAL(3,2) NOT NULL DEFAULT 0.50 COMMENT '可靠度评分（0.00-1.00）',
    market_share      DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '市场份额（0.00-1.00）',
    status            ENUM('ACTIVE','INACTIVE','BLACKLISTED') NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    deleted           TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_name    (name),
    INDEX idx_status  (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商表';

-- ------------------------------------------------------------
-- 8. 成交记录表
-- ------------------------------------------------------------
CREATE TABLE deal_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id     BIGINT NOT NULL COMMENT '供应商ID',
    resource_name   VARCHAR(300) NOT NULL COMMENT '资源名称',
    category        ENUM('JOURNAL','DATABASE','EBOOK','CONFERENCE') NOT NULL COMMENT '资源类别',
    deal_price      DECIMAL(10,2) NOT NULL COMMENT '成交价格',
    deal_date       DATE NOT NULL COMMENT '成交日期',
    contract_period VARCHAR(50) COMMENT '合同期限（如 12个月）',
    notes           TEXT COMMENT '备注',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_supplier_id (supplier_id),
    INDEX idx_category    (category),
    INDEX idx_deal_date   (deal_date),
    INDEX idx_deleted     (deleted),
    CONSTRAINT fk_deal_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成交记录表';

-- ------------------------------------------------------------
-- 9. 电子资源表
-- ------------------------------------------------------------
CREATE TABLE electronic_resource (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(300) NOT NULL COMMENT '资源名称',
    category        ENUM('JOURNAL','DATABASE','EBOOK','CONFERENCE') NOT NULL COMMENT '资源类别',
    publisher       VARCHAR(200) COMMENT '出版商/平台',
    annual_budget   DECIMAL(10,2) COMMENT '年度预算',
    user_count      INT NOT NULL DEFAULT 0 COMMENT '预计使用人数',
    access_url      VARCHAR(500) COMMENT '访问地址',
    status          ENUM('ACTIVE','TRIAL','EXPIRED','PENDING') NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category),
    INDEX idx_status   (status),
    INDEX idx_deleted  (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子资源表';

-- ------------------------------------------------------------
-- 10. 谈判记录表
-- ------------------------------------------------------------
CREATE TABLE negotiation_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id     BIGINT NOT NULL COMMENT '电子资源ID',
    supplier_id     BIGINT NOT NULL COMMENT '供应商ID',
    negotiator_id   BIGINT NOT NULL COMMENT '谈判人（用户ID）',
    floor_price     DECIMAL(10,2) COMMENT '参考最低价',
    ceiling_price   DECIMAL(10,2) COMMENT '参考最高价',
    suggested_offer DECIMAL(10,2) COMMENT '建议报价',
    strategies      JSON COMMENT '谈判策略（JSON数组）',
    key_terms       JSON COMMENT '关键条款关注点（JSON数组）',
    risk_warnings   JSON COMMENT '风险提示（JSON数组）',
    status          ENUM('DRAFT','IN_PROGRESS','COMPLETED','CANCELLED') NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_resource_id   (resource_id),
    INDEX idx_supplier_id   (supplier_id),
    INDEX idx_negotiator_id (negotiator_id),
    INDEX idx_status        (status),
    INDEX idx_deleted       (deleted),
    CONSTRAINT fk_negotiation_resource   FOREIGN KEY (resource_id)   REFERENCES electronic_resource(id) ON DELETE CASCADE,
    CONSTRAINT fk_negotiation_supplier   FOREIGN KEY (supplier_id)   REFERENCES supplier(id) ON DELETE RESTRICT,
    CONSTRAINT fk_negotiation_negotiator FOREIGN KEY (negotiator_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='谈判记录表';
