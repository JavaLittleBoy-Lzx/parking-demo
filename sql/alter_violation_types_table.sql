-- ============================================
-- 修改 violation_types 表结构
-- 说明: 添加缺失的字段，与实体类保持一致
-- 创建时间: 2025-10-08
-- ============================================

-- 1. 重命名字段 is_active 为 is_enabled
ALTER TABLE `violation_types` CHANGE COLUMN `is_active` `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用（0-禁用，1-启用）';

-- 2. 添加 severity_level 字段（严重程度）
ALTER TABLE `violation_types` ADD COLUMN `severity_level` VARCHAR(20) DEFAULT NULL COMMENT '严重程度（mild-轻微，moderate-中等，severe-严重）' AFTER `park_name`;

-- 3. 添加 description 字段（类型描述）
ALTER TABLE `violation_types` ADD COLUMN `description` VARCHAR(500) DEFAULT NULL COMMENT '类型描述' AFTER `severity_level`;

-- 4. 添加 created_by 字段（创建人）
ALTER TABLE `violation_types` ADD COLUMN `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人' AFTER `sort_order`;

-- 6. 更新索引（如果需要）
ALTER TABLE `violation_types` DROP INDEX IF EXISTS `idx_is_active`;
ALTER TABLE `violation_types` ADD INDEX `idx_is_enabled` (`is_enabled`);

-- 执行完成后，表结构应该包含以下字段：
-- id, type_name, type_code, park_name, severity_level, description, 
-- sort_order, created_by, is_enabled, created_at, updated_at
-- (以及可能保留的 icon, category, usage_count)

