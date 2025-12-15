-- 为用户表添加个人中心相关字段
ALTER TABLE `user` ADD COLUMN `email` VARCHAR(100) COMMENT '邮箱' AFTER `telephone`;
ALTER TABLE `user` ADD COLUMN `avatar` VARCHAR(255) COMMENT '头像URL' AFTER `email`;
ALTER TABLE `user` ADD COLUMN `gender` TINYINT(1) DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女' AFTER `avatar`;
ALTER TABLE `user` ADD COLUMN `birthday` DATE COMMENT '生日' AFTER `gender`;
ALTER TABLE `user` ADD COLUMN `address` VARCHAR(255) COMMENT '地址' AFTER `birthday`;
ALTER TABLE `user` ADD COLUMN `last_login_time` DATETIME COMMENT '最后登录时间' AFTER `address`;
ALTER TABLE `user` ADD COLUMN `last_login_ip` VARCHAR(50) COMMENT '最后登录IP' AFTER `last_login_time`;
ALTER TABLE `user` ADD COLUMN `login_count` INT DEFAULT 0 COMMENT '登录次数' AFTER `last_login_ip`;
ALTER TABLE `user` ADD COLUMN `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0-禁用，1-启用' AFTER `login_count`;
ALTER TABLE `user` ADD COLUMN `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `status`;
ALTER TABLE `user` ADD COLUMN `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_time`;
