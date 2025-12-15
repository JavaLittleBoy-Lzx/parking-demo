-- 为 appointment 表添加 updatetime 字段
-- 用于记录预约记录的更新时间

-- 添加 updatetime 字段
ALTER TABLE appointment 
ADD COLUMN updatetime DATETIME COMMENT '更新时间';

-- 为现有记录设置初始值（使用 recorddate 作为初始更新时间）
UPDATE appointment 
SET updatetime = recorddate 
WHERE updatetime IS NULL;

-- 创建索引以优化查询性能
CREATE INDEX idx_updatetime ON appointment(updatetime);

-- 说明：
-- 1. updatetime 字段用于记录预约的最后更新时间
-- 2. 定时任务标记过期时会更新此字段
-- 3. MyBatis-Plus 配置了自动填充（INSERT_UPDATE）
-- 4. 字段类型为 DATETIME，支持精确到秒
