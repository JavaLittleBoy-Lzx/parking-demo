-- 添加登录失败锁定和禁用相关字段到 park_staff 表
-- 执行时间：2025-11-13

-- 添加登录失败次数字段
ALTER TABLE `park_staff` 
ADD COLUMN `failed_login_count` INT DEFAULT 0 COMMENT '登录失败次数' AFTER `last_login_ip`;

-- 添加账户锁定时间字段
ALTER TABLE `park_staff` 
ADD COLUMN `lock_time` DATETIME NULL COMMENT '账户锁定时间' AFTER `failed_login_count`;

-- 添加锁定次数字段（记录账户被锁定的总次数）
ALTER TABLE `park_staff` 
ADD COLUMN `lock_count` INT DEFAULT 0 COMMENT '账户锁定次数（累计）' AFTER `lock_time`;

-- 添加禁用原因字段
ALTER TABLE `park_staff` 
ADD COLUMN `disable_reason` VARCHAR(500) NULL COMMENT '禁用原因' AFTER `status`;

-- 添加禁用时间字段
ALTER TABLE `park_staff` 
ADD COLUMN `disable_time` DATETIME NULL COMMENT '禁用时间' AFTER `disable_reason`;

-- 为现有人员初始化字段值
UPDATE `park_staff` SET `failed_login_count` = 0 WHERE `failed_login_count` IS NULL;
UPDATE `park_staff` SET `lock_count` = 0 WHERE `lock_count` IS NULL;

