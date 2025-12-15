-- 短信模板管理菜单配置SQL脚本

-- 1. 在 permission 表中添加短信模板管理权限
-- 假设"外来车辆管理"或类似父级菜单的ID为 4（请根据实际情况调整）
-- 首先查询现有的菜单结构，找到合适的父级菜单ID

-- 添加短信模板管理权限项
INSERT INTO `permission` (`NAME`, `PATH`, `PID`, `DELETED`) 
VALUES ('短信模板管理', '/admin/smsTemplate', 4, 0);

-- 获取刚插入的权限ID（如果是自增ID，通常是最新的ID）
-- 假设新插入的ID为 93（与前端路由权限配置一致）

-- 2. 如果需要为现有角色添加此权限，更新 role 表
-- 注意：PERMISSION 字段存储的是权限ID列表，通常是逗号分隔的字符串
-- 示例：为管理员角色（ID=1）添加短信模板管理权限

-- 方法1：如果知道具体的角色ID和现有权限列表
-- UPDATE `role` SET `PERMISSION` = CONCAT(`PERMISSION`, ',93') WHERE `id` = 1 AND `PERMISSION` NOT LIKE '%,93%' AND `PERMISSION` NOT LIKE '93,%';

-- 方法2：手动查询并更新
-- SELECT id, name, permission FROM role WHERE deleted = 0;
-- 然后根据查询结果，手动添加权限ID 93 到相应角色的 permission 字段中

-- ========================================
-- 使用说明
-- ========================================
-- 1. 执行此SQL前，请先查询 permission 表，确认父级菜单ID（PID）
--    SELECT * FROM permission WHERE deleted = 0 ORDER BY pid, id;
--    找到"外来车辆管理"或其他适合的父级菜单ID，替换上面SQL中的 PID 值

-- 2. 执行插入语句后，记录新插入权限的ID

-- 3. 查询需要添加此权限的角色
--    SELECT * FROM role WHERE deleted = 0;

-- 4. 更新角色的权限列表，在 PERMISSION 字段中添加新权限ID
--    例如：如果角色的现有权限是 "11,12,13,43,44"
--    更新为："11,12,13,43,93,44" （在车场信息管理43后面添加93）

-- 示例更新语句（请根据实际情况修改）：
-- UPDATE role SET permission = '11,12,13,21,22,23,31,34,42,43,93,44,45,46,47,48,49,71,72,76,77,78,79,80,81' 
-- WHERE id = 1 AND deleted = 0;

-- ========================================
-- 完整示例（请根据实际数据库结构调整）
-- ========================================

-- 步骤1：查看现有权限结构
SELECT id, NAME, PATH, PID FROM permission WHERE deleted = 0 ORDER BY PID, id;

-- 步骤2：查看"外来车辆管理"或相关父级菜单的ID
-- 假设查询结果显示"外来车辆管理"的ID为 4

-- 步骤3：插入短信模板管理权限（请确认PID值）
-- INSERT INTO `permission` (`NAME`, `PATH`, `PID`, `DELETED`) 
-- VALUES ('短信模板管理', '/admin/smsTemplate', 4, 0);

-- 步骤4：查看插入结果
-- SELECT * FROM permission WHERE NAME = '短信模板管理';

-- 步骤5：查看需要更新的角色
-- SELECT id, name, permission FROM role WHERE deleted = 0;

-- 步骤6：更新管理员角色权限（假设管理员角色ID为1，新权限ID为93）
-- UPDATE role 
-- SET permission = CONCAT(
--     SUBSTRING_INDEX(permission, ',44', 1),  -- 取到44之前的部分
--     ',93,44',                                -- 在43和44之间插入93
--     SUBSTRING(permission, LENGTH(SUBSTRING_INDEX(permission, ',44', 1)) + 4)  -- 44之后的部分
-- )
-- WHERE id = 1 AND permission LIKE '%,43,44%';

-- ========================================
-- 验证
-- ========================================
-- 执行完成后，验证权限是否正确添加
SELECT r.id, r.name, r.permission 
FROM role r 
WHERE r.deleted = 0 
  AND r.permission LIKE '%93%';

-- 查看完整的权限结构
SELECT p.id, p.NAME, p.PATH, p.PID, 
       CASE WHEN p.PID = 0 THEN '顶级菜单' ELSE (SELECT NAME FROM permission WHERE id = p.PID) END as parent_name
FROM permission p 
WHERE p.deleted = 0 
ORDER BY p.PID, p.id;

