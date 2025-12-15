-- 修复唯一约束问题
-- 问题：当前唯一索引包含 deleted 字段，导致可以重复插入

-- 方案1：修改唯一索引，不包含 deleted 字段（推荐）
-- 这样同一个 (yard_id, sms_template_id) 组合在表中只能存在一条记录

-- 1. 删除旧的唯一索引
ALTER TABLE yard_sms_template_relation DROP INDEX uk_yard_template;

-- 2. 添加新的唯一索引（不包含 deleted）
ALTER TABLE yard_sms_template_relation 
ADD UNIQUE KEY uk_yard_template (yard_id, sms_template_id);

-- 3. 执行前先清理重复数据
-- 先查看是否有重复
SELECT 
    yard_id,
    sms_template_id,
    COUNT(*) as count
FROM yard_sms_template_relation
GROUP BY yard_id, sms_template_id
HAVING COUNT(*) > 1;

-- 如果有重复，执行清理（保留最新的有效记录）
-- 注意：执行前请先备份数据！

-- 删除重复记录，只保留每组中 id 最大的记录
DELETE t1 FROM yard_sms_template_relation t1
INNER JOIN (
    SELECT yard_id, sms_template_id, MAX(id) as max_id
    FROM yard_sms_template_relation
    GROUP BY yard_id, sms_template_id
) t2 ON t1.yard_id = t2.yard_id 
    AND t1.sms_template_id = t2.sms_template_id
    AND t1.id < t2.max_id;

-- 验证清理结果
SELECT 
    yard_id,
    sms_template_id,
    COUNT(*) as count
FROM yard_sms_template_relation
GROUP BY yard_id, sms_template_id
HAVING COUNT(*) > 1;

-- 应该返回空结果

-- 4. 现在可以安全地添加唯一索引了
-- ALTER TABLE yard_sms_template_relation 
-- ADD UNIQUE KEY uk_yard_template (yard_id, sms_template_id);

