-- ============================================
-- 为 violation_types 表添加 icon 字段
-- 创建时间: 2025-10-08
-- 说明: 支持前端显示图标
-- ============================================

-- 检查并添加 icon 字段
ALTER TABLE `violation_types` 
ADD COLUMN IF NOT EXISTS `icon` VARCHAR(50) DEFAULT NULL COMMENT '图标名称' 
AFTER `severity_level`;

-- 验证字段是否添加成功
-- SHOW COLUMNS FROM `violation_types` LIKE 'icon';

-- 更新现有数据的图标（可选）
UPDATE `violation_types` SET `icon` = 'WarningFilled' WHERE `type_code` = 'illegal_parking';
UPDATE `violation_types` SET `icon` = 'FireFilled' WHERE `type_code` = 'fire_lane_blocking';
UPDATE `violation_types` SET `icon` = 'Clock' WHERE `type_code` = 'overtime_parking';
UPDATE `violation_types` SET `icon` = 'TurnOff' WHERE `type_code` = 'reverse_parking';
UPDATE `violation_types` SET `icon` = 'Grid' WHERE `type_code` = 'cross_space_parking';
UPDATE `violation_types` SET `icon` = 'CircleCloseFilled' WHERE `type_code` = 'unpaid_exit';

-- 查看更新结果
-- SELECT id, type_name, type_code, icon FROM `violation_types`;
