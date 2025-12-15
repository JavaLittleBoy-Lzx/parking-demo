-- 检查和修复车场数据问题
-- 问题：违规记录查询页面车场名称下拉框显示"无数据"

-- =============================================
-- 第1步：检查 yard_info 表是否有数据
-- =============================================
SELECT '=== 第1步：检查 yard_info 表数据 ===' AS step;
SELECT 
    COUNT(*) AS total_count,
    SUM(CASE WHEN deleted = 0 THEN 1 ELSE 0 END) AS active_count,
    SUM(CASE WHEN deleted = 1 THEN 1 ELSE 0 END) AS deleted_count
FROM yard_info;

-- 查看具体的车场数据
SELECT 
    id,
    yard_name,
    yard_code,
    deleted,
    create_time
FROM yard_info
ORDER BY deleted, create_time DESC;

-- =============================================
-- 第2步：如果没有数据或所有数据都被删除，插入测试数据
-- =============================================
-- 注意：只有在yard_info表为空或所有数据都被删除时才执行以下INSERT语句

-- 检查是否需要插入数据
SELECT '=== 第2步：检查是否需要插入数据 ===' AS step;
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '需要插入数据'
        WHEN SUM(CASE WHEN deleted = 0 THEN 1 ELSE 0 END) = 0 THEN '所有数据都被删除，需要恢复'
        ELSE '已有有效数据，无需操作'
    END AS status
FROM yard_info;

-- 如果需要，手动执行以下插入语句
-- INSERT INTO yard_info (yard_name, yard_code, deleted, create_time, update_time) 
-- VALUES 
-- ('东北林业大学', 'NEFU001', 0, NOW(), NOW()),
-- ('欧洲新城', 'OUZH001', 0, NOW(), NOW()),
-- ('学院新城', 'XUEY001', 0, NOW(), NOW());

-- =============================================
-- 第3步：检查用户的管理员权限
-- =============================================
SELECT '=== 第3步：检查用户权限 ===' AS step;
SELECT 
    user_id,
    login_name,
    user_name,
    role_id,
    role_name,
    managed_parks,
    CASE 
        WHEN role_id = 1 THEN '管理员'
        WHEN role_name LIKE '%管理员%' THEN '管理员(按角色名)'
        WHEN managed_parks IS NOT NULL AND managed_parks != '' THEN '普通用户(有授权车场)'
        ELSE '普通用户(无授权车场)'
    END AS user_type
FROM sys_user
ORDER BY role_id, user_id;

-- =============================================
-- 第4步：如果某个用户应该是管理员但权限不对，执行以下修复
-- =============================================
SELECT '=== 第4步：修复管理员权限（请根据实际情况修改user_id） ===' AS step;

-- 示例：将user_id=1的用户设置为管理员
-- UPDATE sys_user SET role_id = 1, role_name = '管理员' WHERE user_id = 1;

-- =============================================
-- 第5步：验证修复结果
-- =============================================
SELECT '=== 第5步：验证修复结果 ===' AS step;

-- 检查车场数据
SELECT '车场数据:' AS check_type;
SELECT yard_name, yard_code, deleted FROM yard_info WHERE deleted = 0;

-- 检查管理员账号
SELECT '管理员账号:' AS check_type;
SELECT 
    user_id,
    login_name,
    user_name,
    role_id,
    role_name
FROM sys_user 
WHERE role_id = 1 OR role_name LIKE '%管理员%';

-- =============================================
-- 常见问题解决方案
-- =============================================

-- 问题A：yard_info表为空
-- 解决方案：插入车场数据
/*
INSERT INTO yard_info (yard_name, yard_code, deleted, create_time, update_time) 
VALUES 
('东北林业大学', 'NEFU001', 0, NOW(), NOW()),
('欧洲新城', 'OUZH001', 0, NOW(), NOW()),
('学院新城', 'XUEY001', 0, NOW(), NOW());
*/

-- 问题B：所有车场都被标记为已删除
-- 解决方案：恢复车场数据
/*
UPDATE yard_info SET deleted = 0 WHERE yard_name IN ('东北林业大学', '欧洲新城', '学院新城');
*/

-- 问题C：用户不是管理员且没有授权车场
-- 解决方案1：设置为管理员
/*
UPDATE sys_user SET role_id = 1, role_name = '管理员' WHERE user_id = ?;
*/

-- 解决方案2：分配授权车场
/*
UPDATE sys_user SET managed_parks = '东北林业大学,欧洲新城,学院新城' WHERE user_id = ?;
*/

-- 问题D：前端缓存问题
-- 解决方案：清除浏览器localStorage并重新登录
/*
在浏览器控制台执行:
localStorage.clear();
location.reload();
然后重新登录
*/
