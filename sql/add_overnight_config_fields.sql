-- 为月票车超时配置表添加过夜停车配置字段
-- 执行日期：2025-09-19

USE project_lzx;

-- 添加过夜停车配置字段
ALTER TABLE `monthly_ticket_timeout_config` 
ADD COLUMN `night_start_time` varchar(5) DEFAULT '22:00' COMMENT '夜间开始时间（如：22:00）' AFTER `description`,
ADD COLUMN `night_end_time` varchar(5) DEFAULT '06:00' COMMENT '夜间结束时间（如：06:00）' AFTER `night_start_time`,
ADD COLUMN `night_time_hours` int(11) DEFAULT 4 COMMENT '夜间时段停车超过X小时算违规' AFTER `night_end_time`,
ADD COLUMN `enable_overnight_check` tinyint(1) DEFAULT 1 COMMENT '是否启用过夜停车检查：1-启用，0-禁用' AFTER `night_time_hours`;

-- 添加索引
ALTER TABLE `monthly_ticket_timeout_config` 
ADD KEY `idx_overnight_config` (`park_code`, `enable_overnight_check`) COMMENT '过夜检查配置索引';

-- 更新现有配置记录（如果有的话）
UPDATE `monthly_ticket_timeout_config` 
SET 
    `night_start_time` = '22:00',
    `night_end_time` = '06:00', 
    `night_time_hours` = 4,
    `enable_overnight_check` = 1,
    `updated_at` = NOW()
WHERE `night_start_time` IS NULL;

-- 验证表结构
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'project_lzx' 
  AND TABLE_NAME = 'monthly_ticket_timeout_config' 
ORDER BY ORDINAL_POSITION; 