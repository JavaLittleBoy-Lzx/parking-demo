-- ============================================
-- 违规配置管理表 - 数据库表创建
-- 创建时间: 2025-01-31
-- 说明: 违规位置、违规类型、违规描述、拉黑原因配置表
-- ============================================

-- 1. 违规位置配置表
CREATE TABLE IF NOT EXISTS `violation_locations` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `location_name` VARCHAR(200) NOT NULL COMMENT '位置名称',
  `park_name` VARCHAR(100) DEFAULT NULL COMMENT '所属车场名称（为空表示通用）',
  `longitude` DECIMAL(10, 7) DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10, 7) DEFAULT NULL COMMENT '纬度',
  `address_detail` VARCHAR(500) DEFAULT NULL COMMENT '详细地址描述',
  `sort_order` INT(11) DEFAULT 0 COMMENT '排序顺序',
  `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用（0-禁用，1-启用）',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_park_name` (`park_name`),
  KEY `idx_is_enabled` (`is_enabled`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规位置配置表';

-- 2. 违规类型配置表
CREATE TABLE IF NOT EXISTS `violation_types` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type_name` VARCHAR(50) NOT NULL COMMENT '违规类型名称',
  `type_code` VARCHAR(50) NOT NULL COMMENT '违规类型值',
  `park_name` VARCHAR(255) DEFAULT NULL COMMENT '车场名称',
  `icon` VARCHAR(10) DEFAULT NULL COMMENT '图标',
  `category` ENUM('common', 'others') DEFAULT 'common' COMMENT '分类',
  `usage_count` INT(11) DEFAULT 0 COMMENT '使用次数',
  `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `sort_order` INT(11) DEFAULT 0 COMMENT '排序',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code_park` (`type_code`, `park_name`),
  KEY `idx_park_name` (`park_name`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规类型配置表';

-- 3. 违规描述模板表
CREATE TABLE IF NOT EXISTS `violation_descriptions` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `description_text` TEXT NOT NULL COMMENT '违规描述内容',
  `violation_type_code` VARCHAR(50) DEFAULT NULL COMMENT '关联的违规类型代码（为空表示通用）',
  `park_name` VARCHAR(100) DEFAULT NULL COMMENT '所属车场名称（为空表示通用）',
  `usage_count` INT(11) DEFAULT 0 COMMENT '使用次数',
  `sort_order` INT(11) DEFAULT 0 COMMENT '排序顺序',
  `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用（0-禁用，1-启用）',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_violation_type` (`violation_type_code`),
  KEY `idx_park_name` (`park_name`),
  KEY `idx_is_enabled` (`is_enabled`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_usage_count` (`usage_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规描述模板表';

-- 4. 拉黑原因模板表
CREATE TABLE IF NOT EXISTS `blacklist_reasons` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reason_text` VARCHAR(500) NOT NULL COMMENT '拉黑原因内容',
  `reason_category` VARCHAR(50) DEFAULT 'violation' COMMENT '原因分类（violation-违规，security-安全，other-其他）',
  `park_name` VARCHAR(100) DEFAULT NULL COMMENT '所属车场名称（为空表示通用）',
  `usage_count` INT(11) DEFAULT 0 COMMENT '使用次数',
  `sort_order` INT(11) DEFAULT 0 COMMENT '排序顺序',
  `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用（0-禁用，1-启用）',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_reason_category` (`reason_category`),
  KEY `idx_park_name` (`park_name`),
  KEY `idx_is_enabled` (`is_enabled`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_usage_count` (`usage_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拉黑原因模板表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入默认违规位置（通用）
INSERT INTO `violation_locations` (`location_name`, `park_name`, `sort_order`, `is_enabled`, `created_by`) VALUES
('消防通道', NULL, 1, 1, 'system'),
('绿化带', NULL, 2, 1, 'system'),
('盲道', NULL, 3, 1, 'system'),
('非机动车道', NULL, 4, 1, 'system'),
('停车场入口', NULL, 5, 1, 'system'),
('车位通道', NULL, 6, 1, 'system');

-- 插入东北林业大学专用违规位置
INSERT INTO `violation_locations` (`location_name`, `park_name`, `sort_order`, `is_enabled`, `created_by`) VALUES
('东北林业大学-北门', '东北林业大学', 1, 1, 'system'),
('东北林业大学-南门', '东北林业大学', 2, 1, 'system'),
('东北林业大学-教学楼区域', '东北林业大学', 3, 1, 'system'),
('东北林业大学-宿舍区', '东北林业大学', 4, 1, 'system');

-- 清空并插入默认违规类型（通用）
TRUNCATE TABLE `violation_types`;

INSERT INTO `violation_types` (`type_name`, `type_code`, `park_name`, `icon`, `category`, `usage_count`, `sort_order`, `is_active`) VALUES
('违规停车', 'illegal_parking', NULL, '🚫', 'common', 0, 1, 1),
('占用消防通道', 'fire_lane', NULL, '🔥', 'common', 0, 2, 1),
('占用绿化带', 'green_belt', NULL, '🌳', 'common', 0, 3, 1),
('占用盲道', 'blind_road', NULL, '♿', 'others', 0, 4, 1),
('超时停车', 'overtime_parking', NULL, '⏰', 'common', 0, 5, 1),
('未按位停车', 'out_of_space', NULL, '📍', 'common', 0, 6, 1),
('占用他人车位', 'occupy_others_space', NULL, '⛔', 'others', 0, 7, 1);

-- 插入默认违规描述模板
INSERT INTO `violation_descriptions` (`description_text`, `violation_type_code`, `park_name`, `sort_order`, `is_enabled`, `created_by`) VALUES
('车辆违规停放在消防通道，严重影响消防安全', 'fire_lane', NULL, 1, 1, 'system'),
('车辆停放在绿化带区域，影响环境美观', 'green_belt', NULL, 2, 1, 'system'),
('车辆占用盲道，影响行人通行', 'blind_road', NULL, 3, 1, 'system'),
('车辆未按规定停放在指定车位', 'out_of_space', NULL, 4, 1, 'system'),
('车辆占用他人固定车位', 'occupy_others_space', NULL, 5, 1, 'system'),
('车辆停放超过规定时间', 'overtime_parking', NULL, 6, 1, 'system');

-- 插入默认拉黑原因模板
INSERT INTO `blacklist_reasons` (`reason_text`, `reason_category`, `park_name`, `sort_order`, `is_enabled`, `created_by`) VALUES
('多次违规停车', 'violation', NULL, 1, 1, 'system'),
('占用消防通道，存在安全隐患', 'security', NULL, 2, 1, 'system'),
('恶意占用公共资源', 'violation', NULL, 3, 1, 'system'),
('占用他人车位', 'violation', NULL, 4, 1, 'system'),
('过夜停车违规', 'violation', NULL, 5, 1, 'system'),
('超时停车累计违规', 'violation', NULL, 6, 1, 'system');

-- 验证表结构
-- SELECT * FROM violation_locations;
-- SELECT * FROM violation_types;
-- SELECT * FROM violation_descriptions;
-- SELECT * FROM blacklist_reasons;

