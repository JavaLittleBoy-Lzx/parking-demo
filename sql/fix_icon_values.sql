-- ============================================
-- 修复 violation_types 表的 icon 字段值
-- 将中文描述改为正确的图标组件名称
-- 创建时间: 2025-10-08
-- ============================================

-- 1. 先查看当前的 icon 值
SELECT id, type_name, type_code, icon 
FROM violation_types 
ORDER BY id;

-- 2. 根据违规类型更新为合适的图标名称
-- 注意：这些图标名称必须在前端 iconComponents 中存在

-- 违规停车类
UPDATE violation_types SET icon = 'WarningFilled' 
WHERE type_name LIKE '%违规停车%' OR type_name LIKE '%违章停车%';

-- 超时停车类
UPDATE violation_types SET icon = 'Clock' 
WHERE type_name LIKE '%超时%' OR type_name LIKE '%超期%';

-- 占用消防通道
UPDATE violation_types SET icon = 'WarningFilled' 
WHERE type_name LIKE '%消防%' OR type_name LIKE '%火灾%';

-- 占用盲道
UPDATE violation_types SET icon = 'Location' 
WHERE type_name LIKE '%盲道%';

-- 占用残疾人车位
UPDATE violation_types SET icon = 'Lock' 
WHERE type_name LIKE '%残疾%' OR type_name LIKE '%无障碍%';

-- 逆向停车
UPDATE violation_types SET icon = 'CircleClose' 
WHERE type_name LIKE '%逆向%' OR type_name LIKE '%反向%';

-- 跨位停车
UPDATE violation_types SET icon = 'Van' 
WHERE type_name LIKE '%跨位%' OR type_name LIKE '%占用多个%';

-- 未缴费离场
UPDATE violation_types SET icon = 'CircleClose' 
WHERE type_name LIKE '%未缴费%' OR type_name LIKE '%逃费%';

-- 监控相关
UPDATE violation_types SET icon = 'Camera' 
WHERE type_name LIKE '%监控%' OR type_name LIKE '%抓拍%';

-- 其他通用违规（如果上面都没匹配到）
UPDATE violation_types SET icon = 'Warning' 
WHERE icon NOT IN ('WarningFilled', 'Clock', 'Location', 'Lock', 'CircleClose', 'Van', 'Camera', 'Warning')
   OR icon IS NULL;

-- 3. 验证更新结果
SELECT id, type_name, type_code, severity_level, icon 
FROM violation_types 
ORDER BY sort_order;

-- 4. 可用的图标列表（供参考）
/*
前端支持的图标：
- Warning, WarningFilled (警告)
- CircleClose (禁止)
- Lock (锁定/禁止)
- Clock, Timer (时间)
- Location, Position, MapLocation, Coordinate (位置)
- Van, Bicycle (车辆)
- Camera, VideoCamera (监控)
- Flag, Stamp, Tickets (标记)
- Bell, Message, Document, Files (其他)
*/
