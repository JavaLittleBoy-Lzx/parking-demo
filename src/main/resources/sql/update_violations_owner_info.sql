-- ==========================================
-- 更新violations表的业主信息
-- 功能：将有month_ticket_id但业主信息为空的记录，从month_tick表中填充业主信息
-- 创建时间：2025-11-21
-- ==========================================

-- 步骤1：先查看需要更新的记录（预览）
-- 执行此查询可以查看哪些记录会被更新
SELECT 
    v.id,
    v.plate_number,
    v.month_ticket_id,
    v.vip_type_name as old_vip_type,
    mt.ticket_name as new_vip_type,
    v.owner_name as old_owner_name,
    mt.user_name as new_owner_name,
    v.owner_phone as old_owner_phone,
    mt.user_phone as new_owner_phone,
    v.owner_address as old_owner_address,
    mt.remark1 as new_owner_address
FROM violations v
INNER JOIN month_tick mt ON v.month_ticket_id = mt.id
WHERE 
    v.month_ticket_id IS NOT NULL
    AND (
        v.vip_type_name IS NULL OR v.vip_type_name = ''
        OR v.owner_name IS NULL OR v.owner_name = ''
        OR v.owner_phone IS NULL OR v.owner_phone = ''
        OR v.owner_address IS NULL OR v.owner_address = ''
    );

-- ==========================================
-- 步骤2：执行更新操作
-- ==========================================

-- 方案1：基础更新（不更新owner_address）
-- 更新vip_type_name、owner_name、owner_phone三个字段
UPDATE violations v
INNER JOIN month_tick mt ON v.month_ticket_id = mt.id
SET 
    v.vip_type_name = mt.ticket_name,
    v.owner_name = mt.user_name,
    v.owner_phone = mt.user_phone
WHERE 
    v.month_ticket_id IS NOT NULL
    AND (
        v.vip_type_name IS NULL OR v.vip_type_name = ''
        OR v.owner_name IS NULL OR v.owner_name = ''
        OR v.owner_phone IS NULL OR v.owner_phone = ''
    );

-- ==========================================

-- 方案2：完整更新（包含owner_address）
-- 如果month_tick表的remark1字段存储了地址信息，使用此方案
-- 此方案使用COALESCE函数，只更新空值，保留已有值
/*
UPDATE violations v
INNER JOIN month_tick mt ON v.month_ticket_id = mt.id
SET 
    v.vip_type_name = COALESCE(NULLIF(v.vip_type_name, ''), mt.ticket_name),
    v.owner_name = COALESCE(NULLIF(v.owner_name, ''), mt.user_name),
    v.owner_phone = COALESCE(NULLIF(v.owner_phone, ''), mt.user_phone),
    v.owner_address = COALESCE(NULLIF(v.owner_address, ''), mt.remark1)
WHERE 
    v.month_ticket_id IS NOT NULL;
*/

-- ==========================================
-- 步骤3：验证更新结果
-- ==========================================

-- 查看更新后仍然为空的记录（如果有的话）
SELECT 
    v.id,
    v.plate_number,
    v.month_ticket_id,
    v.vip_type_name,
    v.owner_name,
    v.owner_phone,
    v.owner_address
FROM violations v
WHERE 
    v.month_ticket_id IS NOT NULL
    AND (
        v.vip_type_name IS NULL OR v.vip_type_name = ''
        OR v.owner_name IS NULL OR v.owner_name = ''
        OR v.owner_phone IS NULL OR v.owner_phone = ''
    );

-- 统计更新情况
SELECT 
    COUNT(*) as total_with_month_ticket,
    SUM(CASE WHEN v.vip_type_name IS NOT NULL AND v.vip_type_name != '' THEN 1 ELSE 0 END) as has_vip_type,
    SUM(CASE WHEN v.owner_name IS NOT NULL AND v.owner_name != '' THEN 1 ELSE 0 END) as has_owner_name,
    SUM(CASE WHEN v.owner_phone IS NOT NULL AND v.owner_phone != '' THEN 1 ELSE 0 END) as has_owner_phone
FROM violations v
WHERE v.month_ticket_id IS NOT NULL;

-- ==========================================
-- 使用说明
-- ==========================================
-- 1. 执行前请先备份violations表数据
-- 2. 先执行"步骤1"的查询，确认需要更新的记录
-- 3. 根据实际情况选择"方案1"或"方案2"执行更新
-- 4. 执行"步骤3"验证更新结果
-- 5. 如果owner_address字段需要从其他字段获取，请修改方案2中的mt.remark1
-- ==========================================
