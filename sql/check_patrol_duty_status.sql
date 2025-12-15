-- ==========================================
-- 值班状态功能 - 快速检查SQL脚本
-- ==========================================

-- 1. 检查patrol表结构
SHOW COLUMNS FROM patrol;

-- 2. 检查是否有openid字段
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'patrol'
  AND COLUMN_NAME IN ('openid', 'notification_enabled', 'last_status_change_time');

-- 3. 查看所有巡检员的openid和状态
SELECT 
    id,
    username AS '姓名',
    usercode AS '代码',
    phone AS '电话',
    community AS '小区',
    openid AS 'OpenID',
    notification_enabled AS '通知状态',
    CASE 
        WHEN notification_enabled = 1 THEN '✅ 值班中'
        WHEN notification_enabled = 0 THEN '❌ 离岗'
        ELSE '⚠️ 未设置'
    END AS '状态说明',
    last_status_change_time AS '最后变更时间'
FROM patrol
ORDER BY last_status_change_time DESC;

-- 4. 统计有效和无效的openid
SELECT 
    '总记录数' AS '类型',
    COUNT(*) AS '数量'
FROM patrol
UNION ALL
SELECT 
    '有openid的记录',
    COUNT(*)
FROM patrol 
WHERE openid IS NOT NULL AND openid != ''
UNION ALL
SELECT 
    'openid为空的记录',
    COUNT(*)
FROM patrol 
WHERE openid IS NULL OR openid = '';

-- 5. 查找没有openid的巡检员
SELECT 
    id,
    username,
    usercode,
    phone,
    community,
    '❌ 缺少openid' AS '问题'
FROM patrol
WHERE openid IS NULL OR openid = ''
LIMIT 20;

-- 6. 检查notification_enabled字段的分布
SELECT 
    notification_enabled AS '状态值',
    CASE 
        WHEN notification_enabled = 1 THEN '✅ 值班中'
        WHEN notification_enabled = 0 THEN '❌ 离岗'
        ELSE '⚠️ 未知'
    END AS '状态说明',
    COUNT(*) AS '数量'
FROM patrol
GROUP BY notification_enabled;

-- 7. 查看最近的状态变更记录
SELECT 
    username AS '姓名',
    community AS '小区',
    notification_enabled AS '当前状态',
    CASE 
        WHEN notification_enabled = 1 THEN '✅ 值班中'
        ELSE '❌ 离岗'
    END AS '状态文字',
    last_status_change_time AS '变更时间',
    TIMESTAMPDIFF(MINUTE, last_status_change_time, NOW()) AS '距今分钟数'
FROM patrol
WHERE last_status_change_time IS NOT NULL
ORDER BY last_status_change_time DESC
LIMIT 10;

-- ==========================================
-- 如果发现问题，使用以下SQL修复
-- ==========================================

-- 修复1：如果缺少openid字段
-- ALTER TABLE patrol ADD COLUMN openid VARCHAR(64) COMMENT '微信openid';

-- 修复2：如果缺少notification_enabled字段
-- ALTER TABLE patrol ADD COLUMN notification_enabled INT DEFAULT 1 COMMENT '是否接收通知：1=是，0=否';

-- 修复3：如果缺少last_status_change_time字段
-- ALTER TABLE patrol ADD COLUMN last_status_change_time DATETIME COMMENT '最后状态变更时间';

-- 修复4：为所有记录设置默认值
-- UPDATE patrol SET notification_enabled = 1 WHERE notification_enabled IS NULL;

-- 修复5：为测试环境生成假的openid（仅用于开发测试！）
-- UPDATE patrol 
-- SET openid = CONCAT('o', LPAD(id, 28, '0'))
-- WHERE openid IS NULL OR openid = '';

-- 修复6：根据手机号更新特定巡检员的openid（替换为实际值）
-- UPDATE patrol 
-- SET openid = 'oXXXXXXXXXXXXXXXXXXX'  -- 从小程序获取的实际openid
-- WHERE phone = '13800138000';  -- 巡检员的手机号

-- ==========================================
-- 手动测试更新功能
-- ==========================================

-- 测试1：手动设置为离岗
-- UPDATE patrol 
-- SET notification_enabled = 0,
--     last_status_change_time = NOW()
-- WHERE openid = 'oXXXXXXXXXXXXXXXXXXX';  -- 替换为实际openid

-- 测试2：手动设置为值班中
-- UPDATE patrol 
-- SET notification_enabled = 1,
--     last_status_change_time = NOW()
-- WHERE openid = 'oXXXXXXXXXXXXXXXXXXX';  -- 替换为实际openid

-- 测试3：验证更新结果
-- SELECT 
--     username,
--     notification_enabled,
--     CASE WHEN notification_enabled = 1 THEN '值班中' ELSE '离岗' END AS status,
--     last_status_change_time
-- FROM patrol
-- WHERE openid = 'oXXXXXXXXXXXXXXXXXX';  -- 替换为实际openid
