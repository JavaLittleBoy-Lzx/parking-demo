-- ============================================
-- 数据库迁移脚本：添加临时拉黑有效天数字段
-- 表名：violation_config
-- 执行环境：现有数据库（适用于已有表的情况）
-- 执行时间：2025-10-07
-- ============================================

-- 检查字段是否已存在，如果不存在则添加
SET @dbname = DATABASE();
SET @tablename = 'violation_config';
SET @columnname = 'blacklist_valid_days';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE 
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  "SELECT 'Column already exists, skipping...' AS message;",
  "ALTER TABLE `violation_config` 
   ADD COLUMN `blacklist_valid_days` INT(11) DEFAULT 30 
   COMMENT '临时拉黑有效天数（从最后一次违规时间开始计算，单位：天）' 
   AFTER `is_permanent`;"
));

PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- ============================================
-- 字段说明
-- ============================================
-- 字段名：blacklist_valid_days
-- 类型：INT(11)
-- 默认值：30（天）
-- 位置：在 is_permanent 字段之后
-- 
-- 使用说明：
-- 1. 仅在 is_permanent=0（临时拉黑）时生效
-- 2. 范围：1-365 天
-- 3. 触发自动拉黑时：
--    - blacklist_start_time = 最后一次违规时间
--    - blacklist_end_time = blacklist_start_time + blacklist_valid_days
-- 4. 配置示例：
--    - 违规3次临时拉黑30天：max_violation_count=3, is_permanent=0, blacklist_valid_days=30
--    - 违规5次永久拉黑：max_violation_count=5, is_permanent=1, blacklist_valid_days=NULL
-- ============================================

-- 查看修改结果
SELECT 
    COLUMN_NAME AS '字段名',
    COLUMN_TYPE AS '类型',
    IS_NULLABLE AS '可空',
    COLUMN_DEFAULT AS '默认值',
    COLUMN_COMMENT AS '注释'
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'violation_config'
  AND COLUMN_NAME = 'blacklist_valid_days';

