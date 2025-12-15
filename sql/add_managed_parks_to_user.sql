-- ============================================================================
-- 车场权限管理功能 - 数据库迁移脚本
-- 功能：在用户表中添加managed_parks字段，用于存储用户管理的车场列表
-- 创建时间：2025-01-01
-- ============================================================================

-- 步骤1：添加managed_parks字段到user表
ALTER TABLE `user` ADD COLUMN `managed_parks` VARCHAR(500) NULL DEFAULT NULL COMMENT '管理的车场列表（逗号分隔，NULL或空表示可访问所有车场）' AFTER `status`;

-- 步骤2：为字段添加索引（可选，如果经常根据车场权限查询用户）
-- ALTER TABLE `user` ADD INDEX `idx_managed_parks` (`managed_parks`(255));

-- 步骤3：验证字段是否添加成功
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE,
    COLUMN_COMMENT,
    IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'user'
  AND COLUMN_NAME = 'managed_parks';

-- ============================================================================
-- 使用说明
-- ============================================================================
-- 1. managed_parks字段存储格式：
--    - NULL 或空字符串：用户可以访问所有车场（默认行为，向后兼容）
--    - "A区车场,B区车场,C区车场"：用户只能访问列出的车场（逗号分隔）
--
-- 2. 示例数据：
--    -- 用户只能访问A区和B区车场
--    UPDATE `user` SET managed_parks = 'A区车场,B区车场' WHERE user_id = 10;
--    
--    -- 用户可以访问所有车场（设置为NULL）
--    UPDATE `user` SET managed_parks = NULL WHERE user_id = 11;
--    
--    -- 用户只能访问东北林业大学车场
--    UPDATE `user` SET managed_parks = '东北林业大学' WHERE user_id = 12;
--
-- 3. 特殊用户处理：
--    -- 管理员用户（roleId = 1）无论managed_parks设置如何，都可以访问所有车场
--    -- 这是在应用层（Java代码）中处理的
--
-- 4. 回滚脚本（如需回滚，请谨慎使用）：
--    -- ALTER TABLE `user` DROP COLUMN `managed_parks`;
-- ============================================================================

-- 步骤4：查看当前所有用户的车场权限设置
SELECT 
    user_id,
    user_name,
    login_name,
    role_id,
    managed_parks,
    CASE 
        WHEN managed_parks IS NULL OR managed_parks = '' THEN '可访问所有车场'
        ELSE CONCAT('限制访问: ', managed_parks)
    END AS access_description
FROM `user`
WHERE deleted = 0
ORDER BY user_id;

-- ============================================================================
-- 完成提示
-- ============================================================================
-- ✅ 数据库迁移脚本执行完成！
-- 
-- 下一步操作：
-- 1. 确认字段已成功添加
-- 2. 为需要限制权限的用户设置managed_parks字段
-- 3. 重启后端服务，使实体类变更生效
-- 4. 测试功能是否正常工作
-- ============================================================================

