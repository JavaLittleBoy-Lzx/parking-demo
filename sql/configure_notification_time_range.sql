-- ========================================
-- 停车超时推送时间段配置脚本
-- ========================================
-- 功能：配置超时车辆推送消息的时间段
-- 默认：每天晚上23:00到早上06:00之间推送
-- ========================================

-- 方法1：为万象上东车场添加推送时间段配置
-- 如果该车场还没有配置记录，使用此脚本
INSERT INTO monthly_ticket_timeout_config (
    park_code,
    park_name,
    timeout_minutes,
    max_violation_count,
    is_active,
    description,
    created_by,
    created_at,
    updated_at
) VALUES (
    '2KST9MNP',  -- 车场编码（万象上东）
    '万象上东',   -- 车场名称
    120,          -- 超时时间（分钟）：2小时
    3,            -- 最大违规次数
    1,            -- 是否启用：1-启用，0-禁用
    '{"notificationStartTime": "23:00", "notificationEndTime": "06:00"}',  -- 推送时间段：晚上11点到早上6点
    'system',     -- 创建人
    NOW(),        -- 创建时间
    NOW()         -- 更新时间
);

-- ========================================
-- 方法2：更新现有配置记录
-- 如果该车场已有配置，使用此脚本更新
-- ========================================

-- 更新万象上东的推送时间段配置
UPDATE monthly_ticket_timeout_config 
SET description = '{"notificationStartTime": "23:00", "notificationEndTime": "06:00"}',
    updated_at = NOW()
WHERE park_code = '2KST9MNP'
  AND is_active = 1;

-- ========================================
-- 方法3：批量更新所有启用的配置
-- ========================================

-- 为所有启用的配置添加推送时间段（晚上11点到早上6点）
UPDATE monthly_ticket_timeout_config 
SET description = CASE 
    WHEN description IS NULL OR description = '' THEN 
        '{"notificationStartTime": "23:00", "notificationEndTime": "06:00"}'
    WHEN description LIKE '%notificationStartTime%' THEN 
        description  -- 已有配置，不覆盖
    ELSE 
        -- 尝试合并现有JSON（如果description是JSON格式）
        CONCAT(
            SUBSTRING(description, 1, LENGTH(description) - 1),
            ', "notificationStartTime": "23:00", "notificationEndTime": "06:00"}'
        )
    END,
    updated_at = NOW()
WHERE is_active = 1;

-- ========================================
-- 验证配置
-- ========================================

-- 查询所有启用的配置，检查推送时间段设置
SELECT 
    id,
    park_code,
    park_name,
    timeout_minutes,
    is_active,
    description,
    updated_at
FROM monthly_ticket_timeout_config
WHERE is_active = 1
ORDER BY park_code;

-- ========================================
-- 常用配置示例
-- ========================================

-- 示例1：晚上11点到早上6点（默认）
-- {"notificationStartTime": "23:00", "notificationEndTime": "06:00"}

-- 示例2：晚上10点到早上7点
-- {"notificationStartTime": "22:00", "notificationEndTime": "07:00"}

-- 示例3：全天推送
-- {"notificationStartTime": "00:00", "notificationEndTime": "23:59"}

-- 示例4：仅白天推送（早上8点到晚上6点）
-- {"notificationStartTime": "08:00", "notificationEndTime": "18:00"}

-- 示例5：带备注的配置
-- {"notificationStartTime": "23:00", "notificationEndTime": "06:00", "remark": "夜间推送"}

-- ========================================
-- 特定车场配置模板
-- ========================================

-- 为新车场添加配置（模板）
-- 注意：请根据实际情况修改 park_code 和 park_name
/*
INSERT INTO monthly_ticket_timeout_config (
    park_code,
    park_name,
    timeout_minutes,
    max_violation_count,
    is_active,
    description,
    created_by,
    created_at,
    updated_at
) VALUES (
    'YOUR_PARK_CODE',    -- 替换为实际车场编码
    'YOUR_PARK_NAME',    -- 替换为实际车场名称
    120,                  -- 超时时间（分钟）
    3,                    -- 最大违规次数
    1,                    -- 是否启用
    '{"notificationStartTime": "23:00", "notificationEndTime": "06:00"}',
    'admin',
    NOW(),
    NOW()
);
*/

-- ========================================
-- 测试查询
-- ========================================

-- 检查配置是否正确
SELECT 
    park_code,
    park_name,
    JSON_EXTRACT(description, '$.notificationStartTime') AS start_time,
    JSON_EXTRACT(description, '$.notificationEndTime') AS end_time,
    is_active
FROM monthly_ticket_timeout_config
WHERE is_active = 1
  AND description IS NOT NULL;

-- ========================================
-- 禁用推送（如需临时关闭）
-- ========================================

-- 临时禁用某个车场的配置
-- UPDATE monthly_ticket_timeout_config 
-- SET is_active = 0,
--     updated_at = NOW()
-- WHERE park_code = '2KST9MNP';

-- ========================================
-- 删除配置（谨慎操作）
-- ========================================

-- 删除某个车场的配置记录
-- DELETE FROM monthly_ticket_timeout_config 
-- WHERE park_code = '2KST9MNP';
