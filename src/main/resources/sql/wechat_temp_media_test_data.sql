-- 微信临时素材测试数据
-- 注意：这些是模拟数据，media_id是假的，仅用于测试定时任务逻辑

-- 清空现有数据（可选）
-- TRUNCATE TABLE wechat_temp_media;

-- 插入测试数据 - 小程序二维码（即将过期，需要刷新）
INSERT INTO `wechat_temp_media` (
    `id`, 
    `media_type`, 
    `media_id`, 
    `description`, 
    `file_name`, 
    `file_path`, 
    `file_size`, 
    `created_at`, 
    `expired_at`, 
    `updated_at`, 
    `status`, 
    `remark`
) VALUES (
    1,
    'image',
    'TEST_MEDIA_ID_001_MINIAPP_QRCODE',
    '小程序二维码',
    'miniapp_qrcode.jpg',
    'd:/PakingDemo/temp/wechat/media/小程序二维码_1732423200000_miniapp_qrcode.jpg',
    102400,
    DATE_SUB(NOW(), INTERVAL 2 DAY),  -- 2天前创建
    DATE_ADD(NOW(), INTERVAL 1 DAY),  -- 1天后过期
    NOW(),
    1,
    '用于用户关注时发送的小程序二维码（测试数据）'
);

-- 插入测试数据 - 欢迎图片（刚上传，不需要刷新）
INSERT INTO `wechat_temp_media` (
    `id`, 
    `media_type`, 
    `media_id`, 
    `description`, 
    `file_name`, 
    `file_path`, 
    `file_size`, 
    `created_at`, 
    `expired_at`, 
    `updated_at`, 
    `status`, 
    `remark`
) VALUES (
    2,
    'image',
    'TEST_MEDIA_ID_002_WELCOME_IMAGE',
    '欢迎图片',
    'welcome.png',
    'd:/PakingDemo/temp/wechat/media/欢迎图片_1732509600000_welcome.png',
    256000,
    NOW(),  -- 刚创建
    DATE_ADD(NOW(), INTERVAL 3 DAY),  -- 3天后过期
    NOW(),
    1,
    '欢迎消息配图（测试数据）'
);

-- 插入测试数据 - 活动海报（已过期，需要刷新）
INSERT INTO `wechat_temp_media` (
    `id`, 
    `media_type`, 
    `media_id`, 
    `description`, 
    `file_name`, 
    `file_path`, 
    `file_size`, 
    `created_at`, 
    `expired_at`, 
    `updated_at`, 
    `status`, 
    `remark`
) VALUES (
    3,
    'image',
    'TEST_MEDIA_ID_003_ACTIVITY_POSTER',
    '活动海报',
    'activity_poster.jpg',
    'd:/PakingDemo/temp/wechat/media/活动海报_1731990400000_activity_poster.jpg',
    512000,
    DATE_SUB(NOW(), INTERVAL 4 DAY),  -- 4天前创建
    DATE_SUB(NOW(), INTERVAL 1 DAY),  -- 1天前已过期
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    0,  -- 已过期状态
    '停车场活动海报（测试数据，已过期）'
);

-- 查询测试数据
SELECT 
    id,
    media_type AS '类型',
    description AS '用途',
    file_name AS '文件名',
    CONCAT(ROUND(file_size/1024, 2), ' KB') AS '大小',
    created_at AS '创建时间',
    expired_at AS '过期时间',
    CASE 
        WHEN expired_at < NOW() THEN '已过期'
        WHEN TIMESTAMPDIFF(HOUR, NOW(), expired_at) < 24 THEN '即将过期'
        ELSE '有效'
    END AS '状态说明',
    status AS '状态码'
FROM wechat_temp_media
ORDER BY id;

-- 查看距离过期的时间
SELECT 
    description AS '用途',
    TIMESTAMPDIFF(HOUR, NOW(), expired_at) AS '距离过期(小时)',
    CASE 
        WHEN expired_at < NOW() THEN '❌ 已过期'
        WHEN TIMESTAMPDIFF(HOUR, NOW(), expired_at) < 6 THEN '⚠️ 6小时内过期'
        WHEN TIMESTAMPDIFF(HOUR, NOW(), expired_at) < 24 THEN '⚠️ 24小时内过期'
        WHEN TIMESTAMPDIFF(HOUR, NOW(), expired_at) < 48 THEN '⚠️ 48小时内过期'
        ELSE '✅ 有效期充足'
    END AS '状态'
FROM wechat_temp_media
ORDER BY expired_at;
