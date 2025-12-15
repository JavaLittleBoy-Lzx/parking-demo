-- 修复重复的短信模板关联数据
-- 这个脚本用于清理重复的车场-模板关联记录

-- 1. 查看当前重复的数据
SELECT 
    yard_id,
    sms_template_id,
    COUNT(*) as count,
    GROUP_CONCAT(id ORDER BY id) as ids,
    GROUP_CONCAT(deleted ORDER BY id) as deleted_status
FROM yard_sms_template_relation
GROUP BY yard_id, sms_template_id
HAVING COUNT(*) > 1;

-- 2. 删除重复数据，只保留最新的一条记录
-- 方法1：对于每个 (yard_id, sms_template_id) 组合，只保留 id 最大的记录
DELETE t1 FROM yard_sms_template_relation t1
INNER JOIN yard_sms_template_relation t2 
WHERE t1.yard_id = t2.yard_id 
  AND t1.sms_template_id = t2.sms_template_id
  AND t1.id < t2.id;

-- 3. 或者更安全的方法：先备份，再清理
-- 创建备份表
CREATE TABLE IF NOT EXISTS yard_sms_template_relation_backup AS 
SELECT * FROM yard_sms_template_relation;

-- 4. 清理重复数据（保留最新的有效记录）
-- 删除所有旧的重复记录
DELETE FROM yard_sms_template_relation
WHERE id NOT IN (
    SELECT max_id FROM (
        SELECT MAX(id) as max_id
        FROM yard_sms_template_relation
        WHERE deleted = 0
        GROUP BY yard_id, sms_template_id
    ) AS temp
);

-- 5. 验证清理结果
SELECT 
    yard_id,
    sms_template_id,
    COUNT(*) as count
FROM yard_sms_template_relation
WHERE deleted = 0
GROUP BY yard_id, sms_template_id
HAVING COUNT(*) > 1;

-- 如果上面的查询返回空结果，说明清理成功

-- 6. 查看特定车场的模板关联（例如车场ID为你说的60编码的车场）
SELECT 
    r.id,
    r.yard_id,
    y.yard_code,
    y.yard_name,
    r.sms_template_id,
    st.template_name,
    st.template_code,
    r.is_default,
    r.deleted,
    r.gmt_create,
    r.gmt_modified
FROM yard_sms_template_relation r
LEFT JOIN yard_info y ON r.yard_id = y.id
LEFT JOIN sms_template st ON r.sms_template_id = st.id
WHERE y.yard_code = '60'  -- 替换为实际的车场编码
ORDER BY r.deleted ASC, r.gmt_create DESC;

-- 7. 只显示有效的关联（deleted=0）
SELECT 
    y.yard_code,
    y.yard_name,
    GROUP_CONCAT(st.template_name ORDER BY r.is_default DESC, r.id) as templates,
    GROUP_CONCAT(st.template_code ORDER BY r.is_default DESC, r.id) as template_codes,
    COUNT(*) as template_count
FROM yard_sms_template_relation r
INNER JOIN yard_info y ON r.yard_id = y.id
INNER JOIN sms_template st ON r.sms_template_id = st.id
WHERE r.deleted = 0
GROUP BY y.id, y.yard_code, y.yard_name
ORDER BY y.yard_code;

