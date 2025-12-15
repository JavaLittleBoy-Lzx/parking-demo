-- ========================================
-- 黑名单功能数据库迁移脚本
-- 创建时间: 2025-10-04
-- 功能说明: 为 violations 和 black_list 表添加黑名单相关字段
-- ========================================

USE project_lzx;

-- ========================================
-- 1. 为 violations 表添加黑名单相关字段
-- ========================================

-- 添加黑名单类型编码字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'project_lzx' 
AND TABLE_NAME = 'violations' 
AND COLUMN_NAME = 'blacklist_type_code';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE violations ADD COLUMN blacklist_type_code VARCHAR(100) COMMENT ''黑名单类型编码'' AFTER blacklist_reason', 
    'SELECT ''字段 blacklist_type_code 已存在'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加黑名单类型名称字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'project_lzx' 
AND TABLE_NAME = 'violations' 
AND COLUMN_NAME = 'blacklist_type_name';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE violations ADD COLUMN blacklist_type_name VARCHAR(255) COMMENT ''黑名单类型名称'' AFTER blacklist_type_code', 
    'SELECT ''字段 blacklist_type_name 已存在'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加拉黑时长类型字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'project_lzx' 
AND TABLE_NAME = 'violations' 
AND COLUMN_NAME = 'blacklist_duration_type';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE violations ADD COLUMN blacklist_duration_type VARCHAR(50) COMMENT ''拉黑时长类型：permanent(永久)/temporary(临时)'' AFTER blacklist_type_name', 
    'SELECT ''字段 blacklist_duration_type 已存在'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加拉黑开始时间字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'project_lzx' 
AND TABLE_NAME = 'violations' 
AND COLUMN_NAME = 'blacklist_start_time';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE violations ADD COLUMN blacklist_start_time DATETIME COMMENT ''拉黑开始时间'' AFTER blacklist_duration_type', 
    'SELECT ''字段 blacklist_start_time 已存在'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加拉黑结束时间字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'project_lzx' 
AND TABLE_NAME = 'violations' 
AND COLUMN_NAME = 'blacklist_end_time';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE violations ADD COLUMN blacklist_end_time DATETIME COMMENT ''拉黑结束时间'' AFTER blacklist_start_time', 
    'SELECT ''字段 blacklist_end_time 已存在'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========================================
-- 2. 为 black_list 表添加黑名单相关字段
-- ========================================

-- 添加黑名单类型编码字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'project_lzx' 
AND TABLE_NAME = 'black_list' 
AND COLUMN_NAME = 'blacklist_type_code';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE black_list ADD COLUMN blacklist_type_code VARCHAR(100) COMMENT ''黑名单类型编码'' AFTER special_car_type_config_name', 
    'SELECT ''字段 blacklist_type_code 已存在'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加拉黑开始时间字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'project_lzx' 
AND TABLE_NAME = 'black_list' 
AND COLUMN_NAME = 'blacklist_start_time';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE black_list ADD COLUMN blacklist_start_time DATETIME COMMENT ''拉黑开始时间'' AFTER blacklist_type_code', 
    'SELECT ''字段 blacklist_start_time 已存在'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加拉黑结束时间字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'project_lzx' 
AND TABLE_NAME = 'black_list' 
AND COLUMN_NAME = 'blacklist_end_time';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE black_list ADD COLUMN blacklist_end_time DATETIME COMMENT ''拉黑结束时间'' AFTER blacklist_start_time', 
    'SELECT ''字段 blacklist_end_time 已存在'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========================================
-- 3. 为现有数据更新默认值（可选）
-- ========================================

-- 为已有的永久拉黑记录设置类型
UPDATE violations 
SET blacklist_duration_type = 'permanent'
WHERE should_blacklist = 1 
AND blacklist_duration_type IS NULL;

-- 为已有的黑名单记录设置类型编码（如果有默认类型）
UPDATE black_list 
SET blacklist_type_code = 'default_violation'
WHERE blacklist_type_code IS NULL 
AND special_car_type_config_name LIKE '%违规%';

-- ========================================
-- 4. 创建索引以提高查询性能
-- ========================================

-- 为 violations 表创建黑名单类型索引
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'project_lzx' 
AND TABLE_NAME = 'violations' 
AND INDEX_NAME = 'idx_violations_blacklist_type';

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_violations_blacklist_type ON violations(blacklist_type_code)', 
    'SELECT ''索引 idx_violations_blacklist_type 已存在'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为 violations 表创建拉黑时长类型索引
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'project_lzx' 
AND TABLE_NAME = 'violations' 
AND INDEX_NAME = 'idx_violations_blacklist_duration';

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_violations_blacklist_duration ON violations(blacklist_duration_type)', 
    'SELECT ''索引 idx_violations_blacklist_duration 已存在'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为 violations 表创建时间范围索引
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'project_lzx' 
AND TABLE_NAME = 'violations' 
AND INDEX_NAME = 'idx_violations_blacklist_time_range';

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_violations_blacklist_time_range ON violations(blacklist_start_time, blacklist_end_time)', 
    'SELECT ''索引 idx_violations_blacklist_time_range 已存在'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为 black_list 表创建黑名单类型索引
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'project_lzx' 
AND TABLE_NAME = 'black_list' 
AND INDEX_NAME = 'idx_blacklist_type_code';

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_blacklist_type_code ON black_list(blacklist_type_code)', 
    'SELECT ''索引 idx_blacklist_type_code 已存在'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为 black_list 表创建时间范围索引
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'project_lzx' 
AND TABLE_NAME = 'black_list' 
AND INDEX_NAME = 'idx_blacklist_time_range';

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_blacklist_time_range ON black_list(blacklist_start_time, blacklist_end_time)', 
    'SELECT ''索引 idx_blacklist_time_range 已存在'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========================================
-- 5. 验证迁移结果
-- ========================================

-- 查看 violations 表结构
SELECT 
    COLUMN_NAME AS '字段名',
    COLUMN_TYPE AS '数据类型',
    IS_NULLABLE AS '允许空值',
    COLUMN_DEFAULT AS '默认值',
    COLUMN_COMMENT AS '注释'
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'project_lzx'
AND TABLE_NAME = 'violations'
AND COLUMN_NAME LIKE 'blacklist%'
ORDER BY ORDINAL_POSITION;

-- 查看 black_list 表结构
SELECT 
    COLUMN_NAME AS '字段名',
    COLUMN_TYPE AS '数据类型',
    IS_NULLABLE AS '允许空值',
    COLUMN_DEFAULT AS '默认值',
    COLUMN_COMMENT AS '注释'
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'project_lzx'
AND TABLE_NAME = 'black_list'
AND COLUMN_NAME LIKE 'blacklist%'
ORDER BY ORDINAL_POSITION;

-- ========================================
-- 迁移完成说明
-- ========================================

-- violations 表新增字段：
--   1. blacklist_type_code - 黑名单类型编码
--   2. blacklist_type_name - 黑名单类型名称
--   3. blacklist_duration_type - 拉黑时长类型（permanent/temporary）
--   4. blacklist_start_time - 拉黑开始时间
--   5. blacklist_end_time - 拉黑结束时间

-- black_list 表新增字段：
--   1. blacklist_type_code - 黑名单类型编码
--   2. blacklist_start_time - 拉黑开始时间
--   3. blacklist_end_time - 拉黑结束时间

-- ========================================

