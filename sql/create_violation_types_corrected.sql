-- ============================================
-- 违规类型配置表 - 修正版建表语句
-- 创建时间: 2025-10-08
-- 说明: 与Java实体类 ViolationType 保持一致
-- ============================================

CREATE TABLE IF NOT EXISTS `violation_types` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type_name` VARCHAR(50) NOT NULL COMMENT '违规类型名称',
  `type_code` VARCHAR(50) NOT NULL COMMENT '违规类型代码',
  `park_name` VARCHAR(255) DEFAULT NULL COMMENT '所属车场名称（为空表示通用）',
  `severity_level` VARCHAR(20) DEFAULT NULL COMMENT '严重程度（mild-轻微，moderate-中等，severe-严重）',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '类型描述',
  `sort_order` INT(11) DEFAULT 0 COMMENT '排序顺序',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用（0-禁用，1-启用）',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code_park` (`type_code`, `park_name`),
  KEY `idx_park_name` (`park_name`),
  KEY `idx_is_enabled` (`is_enabled`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规类型配置表';

-- 插入一些示例数据
INSERT INTO `violation_types` (`type_name`, `type_code`, `park_name`, `severity_level`, `description`, `sort_order`, `is_enabled`) VALUES
('违规停车', 'illegal_parking', NULL, 'moderate', '在禁止停车区域停放车辆', 1, 1),
('占用消防通道', 'fire_lane_blocking', NULL, 'severe', '占用消防通道停放车辆', 2, 1),
('超时停车', 'overtime_parking', NULL, 'mild', '超过规定时间停放车辆', 3, 1),
('逆向停车', 'reverse_parking', NULL, 'moderate', '车辆停放方向与规定相反', 4, 1),
('跨位停车', 'cross_space_parking', NULL, 'moderate', '车辆占用多个停车位', 5, 1),
('未缴费离场', 'unpaid_exit', NULL, 'severe', '未支付停车费用强行离场', 6, 1);

