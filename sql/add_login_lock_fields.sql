-- 添加登录失败锁定相关字段到 user 表
-- 执行时间：2025-11-13

-- 添加登录失败次数字段
ALTER TABLE `user` 
ADD COLUMN `failed_login_count` INT DEFAULT 0 COMMENT '登录失败次数' AFTER `login_count`;

-- 添加账户锁定时间字段
ALTER TABLE `user` 
ADD COLUMN `lock_time` DATETIME NULL COMMENT '账户锁定时间' AFTER `failed_login_count`;

-- 为现有用户初始化字段值
UPDATE `user` SET `failed_login_count` = 0 WHERE `failed_login_count` IS NULL;

