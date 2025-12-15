-- 插入基础的违规类型数据（适用于当前数据库结构）
-- 不包含 severity_level, description, created_by 字段

-- 清空现有数据（可选，如果需要重置数据）
-- TRUNCATE TABLE violation_types;

-- 插入通用违规类型（park_name 为 NULL 表示所有车场通用）
INSERT INTO `violation_types` (`type_name`, `type_code`, `park_name`, `sort_order`, `is_enabled`, `created_at`, `updated_at`) 
VALUES
('违规停车', 'illegal_parking', NULL, 1, 1, NOW(), NOW()),
('占用消防通道', 'fire_lane_blocking', NULL, 2, 1, NOW(), NOW()),
('超时停车', 'overtime_parking', NULL, 3, 1, NOW(), NOW()),
('占用绿化带', 'green_belt_occupation', NULL, 4, 1, NOW(), NOW()),
('逆向停车', 'reverse_parking', NULL, 5, 1, NOW(), NOW()),
('占用充电车位', 'charging_spot_occupation', NULL, 6, 1, NOW(), NOW()),
('跨位停车', 'cross_parking', NULL, 7, 1, NOW(), NOW()),
('车辆异常', 'vehicle_abnormal', NULL, 8, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    type_name = VALUES(type_name),
    sort_order = VALUES(sort_order),
    is_enabled = VALUES(is_enabled),
    updated_at = NOW();

-- 插入东北林业大学专用违规类型
INSERT INTO `violation_types` (`type_name`, `type_code`, `park_name`, `sort_order`, `is_enabled`, `created_at`, `updated_at`) 
VALUES
('校园道路违停', 'campus_road_violation', '东北林业大学', 1, 1, NOW(), NOW()),
('教学楼前违停', 'building_violation', '东北林业大学', 2, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    type_name = VALUES(type_name),
    sort_order = VALUES(sort_order),
    is_enabled = VALUES(is_enabled),
    updated_at = NOW();

-- 查看插入结果
SELECT * FROM violation_types ORDER BY park_name, sort_order;

