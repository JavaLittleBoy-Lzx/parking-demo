-- 检查和更新 visitorname 字段

-- 1. 查看有多少条记录的 visitorname 为空
SELECT 
    COUNT(*) as 空值数量,
    (SELECT COUNT(*) FROM appointment) as 总记录数
FROM appointment 
WHERE visitorname IS NULL OR visitorname = '';

-- 2. 查看最近10条预约记录的 visitorname 状态
SELECT 
    id,
    platenumber AS 车牌号,
    visitorname AS 访客微信昵称,
    visitorphone AS 访客手机号,
    ownername AS 业主姓名,
    ownerphone AS 业主手机号,
    recorddate AS 预约时间,
    CASE 
        WHEN visitorname IS NULL OR visitorname = '' THEN '❌ 空'
        ELSE '✅ 有值'
    END AS 状态
FROM appointment 
ORDER BY recorddate DESC 
LIMIT 10;

-- 3. 查找有 visitorname 值的记录
SELECT 
    COUNT(*) as 有值记录数,
    MIN(recorddate) as 最早记录时间,
    MAX(recorddate) as 最新记录时间
FROM appointment 
WHERE visitorname IS NOT NULL AND visitorname != '';

-- 4. 【可选】为测试目的，更新几条记录添加测试数据
-- 注意：仅用于测试，请根据实际情况修改
-- UPDATE appointment 
-- SET visitorname = CONCAT('测试访客_', id)
-- WHERE (visitorname IS NULL OR visitorname = '')
--   AND id IN (
--     SELECT id FROM (
--       SELECT id FROM appointment 
--       ORDER BY recorddate DESC 
--       LIMIT 5
--     ) as temp
--   );

-- 5. 【推荐】为最近的预约记录设置真实的访客昵称
-- 如果访客电话和业主电话不同，可能是真正的访客
UPDATE appointment 
SET visitorname = '张三（测试访客）'
WHERE platenumber = '黑A11111'
  AND (visitorname IS NULL OR visitorname = '')
LIMIT 1;

-- 6. 验证更新结果
SELECT 
    id,
    platenumber AS 车牌号,
    visitorname AS 访客微信昵称,
    ownername AS 业主姓名,
    recorddate AS 预约时间
FROM appointment 
WHERE platenumber = '黑A11111'
ORDER BY recorddate DESC 
LIMIT 3;
