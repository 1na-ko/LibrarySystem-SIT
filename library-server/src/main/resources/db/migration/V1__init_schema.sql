-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `username` VARCHAR(50) NOT NULL, `password` VARCHAR(255) NOT NULL,
    `real_name` VARCHAR(50), `role` VARCHAR(20) NOT NULL DEFAULT 'READER', `email` VARCHAR(100),
    `phone` VARCHAR(20), `status` TINYINT NOT NULL DEFAULT 1, `max_borrow` INT NOT NULL DEFAULT 5,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`), UNIQUE KEY `uk_username`(`username`), UNIQUE KEY `uk_email`(`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 图书表
CREATE TABLE IF NOT EXISTS `book` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `title` VARCHAR(255) NOT NULL, `author` VARCHAR(255) NOT NULL,
    `isbn` VARCHAR(20) NOT NULL, `category` VARCHAR(100), `publisher` VARCHAR(255), `publish_year` INT,
    `description` TEXT, `cover_url` VARCHAR(500), `total_copies` INT NOT NULL DEFAULT 1, `available_copies` INT NOT NULL DEFAULT 1,
    `location` VARCHAR(100), `keywords` VARCHAR(500), `status` TINYINT NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`), UNIQUE KEY `uk_isbn`(`isbn`), KEY `idx_title`(`title`), KEY `idx_author`(`author`), KEY `idx_category`(`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 借阅记录表
CREATE TABLE IF NOT EXISTS `borrow_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `user_id` BIGINT NOT NULL, `book_id` BIGINT NOT NULL,
    `borrow_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `due_date` DATETIME NOT NULL,
    `return_date` DATETIME, `status` VARCHAR(20) NOT NULL DEFAULT 'BORROWED', `renew_count` INT NOT NULL DEFAULT 0,
    `fine` DECIMAL(10,2) DEFAULT 0.00, `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`), KEY `idx_user_id`(`user_id`), KEY `idx_book_id`(`book_id`), KEY `idx_status`(`status`),
    CONSTRAINT `fk_br_user` FOREIGN KEY(`user_id`) REFERENCES `user`(`id`), CONSTRAINT `fk_br_book` FOREIGN KEY(`book_id`) REFERENCES `book`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预约记录表
CREATE TABLE IF NOT EXISTS `reserve_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `user_id` BIGINT NOT NULL, `book_id` BIGINT NOT NULL,
    `reserve_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `queue_position` INT NOT NULL DEFAULT 1,
    `status` VARCHAR(20) NOT NULL DEFAULT 'WAITING', `notify_date` DATETIME, `expire_date` DATETIME,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`), UNIQUE KEY `uk_user_book`(`user_id`,`book_id`,`status`), KEY `idx_book_id`(`book_id`),
    CONSTRAINT `fk_rr_user` FOREIGN KEY(`user_id`) REFERENCES `user`(`id`), CONSTRAINT `fk_rr_book` FOREIGN KEY(`book_id`) REFERENCES `book`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通知表
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `user_id` BIGINT NOT NULL, `type` VARCHAR(50) NOT NULL,
    `title` VARCHAR(200) NOT NULL, `content` TEXT NOT NULL, `is_read` TINYINT NOT NULL DEFAULT 0,
    `related_id` BIGINT, `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`), KEY `idx_user_read`(`user_id`,`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `user_id` BIGINT, `operation` VARCHAR(50) NOT NULL,
    `target_id` BIGINT, `target_type` VARCHAR(50), `detail` JSON, `ip_address` VARCHAR(50),
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`), KEY `idx_user_id`(`user_id`), KEY `idx_created`(`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
