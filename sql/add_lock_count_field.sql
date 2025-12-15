-- 添加锁定次数字段到 park_staff 表
-- 执行时间：2025-11-14
-- 说明：如果之前已经执行过 add_park_staff_lock_fields.sql，此脚本会报错，可以忽略

-- 检查字段是否存在，如果不存在则添加
SET @dbname = DATABASE();
SET @tablename = 'park_staff';
SET @columnname = 'lock_count';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1', -- 字段已存在，不执行任何操作
  CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @columnname, '` INT DEFAULT 0 COMMENT ''账户锁定次数（累计）'' AFTER `lock_time`;')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 为现有人员初始化字段值
UPDATE `park_staff` SET `lock_count` = 0 WHERE `lock_count` IS NULL;

