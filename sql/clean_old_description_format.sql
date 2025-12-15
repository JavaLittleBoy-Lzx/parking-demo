-- ========================================
-- 清理旧格式的 description 字段
-- ========================================
-- 用途：将旧的文本格式description清空，以便重新保存为JSON格式
-- 执行时机：如果前端提示"检测到旧版本配置格式"时执行
-- ========================================

-- 方法1：仅清空万象上东的旧格式description
UPDATE monthly_ticket_timeout_config 
SET description = NULL
WHERE park_code = '2KST9MNP'
  AND description IS NOT NULL
  AND description NOT LIKE '{%';  -- 不是JSON格式（不以{开头）

-- 方法2：清空所有旧格式的description（谨慎使用）
-- UPDATE monthly_ticket_timeout_config 
-- SET description = NULL
-- WHERE description IS NOT NULL
--   AND description NOT LIKE '{%';

-- ========================================
-- 验证清理结果
-- ========================================

-- 查看万象上东的配置
SELECT 
    id,
    park_code,
    park_name,
    night_start_time,
    night_end_time,
    night_time_hours,
    enable_overnight_check,
    description,
    CASE 
        WHEN description IS NULL THEN '空'
        WHEN description LIKE '{%' THEN 'JSON格式'
        ELSE '旧文本格式'
    END AS description_format
FROM monthly_ticket_timeout_config
WHERE park_code = '2KST9MNP';

-- ========================================
-- 说明
-- ========================================

/*
旧格式示例：
月票车配置: 夜间(23:00-06:00)超过2小时拉黑 | [待检:万象上东,万象上东业主] | [拉黑天数:永久] | [黑名单类型:违规黑名单]

新格式示例：
{
  "nightStartTime": "23:00",
  "nightEndTime": "06:00",
  "nightTimeHours": 2,
  "vipCheckMode": "include",
  "vipTicketTypes": ["万象上东", "万象上东业主"],
  "blacklistName": "local_violation|违规黑名单",
  "isPermanent": true,
  "blacklistDays": 9999,
  "notificationStartTime": "23:00",
  "notificationEndTime": "06:00"
}

执行清理后：
1. 打开前端"万象上东拉黑规则配置"
2. 基础配置项（夜间时间等）会从数据库基础字段读取
3. VIP类型等配置项使用默认值
4. 重新选择配置并保存
5. 新配置会以JSON格式存储到description字段
*/
