-- 清空 month_ticket 表中"欧洲新城"的数据
-- 保留在 violations 表中被引用的记录

-- 步骤1: 查看将要删除的数据（建议先执行此查询确认）
SELECT mt.* 
FROM month_ticket mt
WHERE mt.site_name = '欧洲新城'  -- 或使用其他字段来识别"欧洲新城"
  AND mt.id NOT IN (
    SELECT DISTINCT month_ticket_id 
    FROM violations 
    WHERE month_ticket_id IS NOT NULL
  );

-- 步骤2: 执行删除操作
-- 删除所有"欧洲新城"的月票记录，但排除被 violations 表引用的记录
DELETE FROM month_ticket
WHERE site_name = '欧洲新城'  -- 根据实际字段调整
  AND id NOT IN (
    SELECT DISTINCT month_ticket_id 
    FROM violations 
    WHERE month_ticket_id IS NOT NULL
  );

-- 注意事项：
-- 1. 如果 month_ticket 表中识别"欧洲新城"的字段不是 site_name，请修改相应的字段名
-- 2. 建议先执行步骤1的查询，确认要删除的数据无误后再执行步骤2
-- 3. 执行前建议备份数据库

-- 可选：查看删除后剩余的"欧洲新城"数据（应该都是被violations引用的）
-- SELECT mt.* 
-- FROM month_ticket mt
-- WHERE mt.site_name = '欧洲新城';
