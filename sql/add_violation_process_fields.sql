-- ============================================
-- 违规记录处理功能 - 数据库字段新增
-- 创建时间: 2025-01-31
-- 说明: 为violations表添加处理状态管理字段
-- ============================================

-- 添加处理状态字段
ALTER TABLE violations 
ADD COLUMN process_status VARCHAR(20) DEFAULT 'pending' 
COMMENT '处理状态: pending-未处理, processed-已处理';

-- 添加处理方式字段  
ALTER TABLE violations 
ADD COLUMN process_type VARCHAR(50) DEFAULT NULL 
COMMENT '处理方式: auto_blacklist-系统自动拉黑, manual-手动处理';

-- 添加处理时间字段
ALTER TABLE violations 
ADD COLUMN processed_at TIMESTAMP NULL DEFAULT NULL 
COMMENT '处理时间';

-- 添加处理人字段
ALTER TABLE violations 
ADD COLUMN processed_by VARCHAR(255) DEFAULT NULL 
COMMENT '处理人（用户名或SYSTEM）';

-- 添加处理备注字段
ALTER TABLE violations 
ADD COLUMN process_remark TEXT DEFAULT NULL 
COMMENT '处理备注说明';

-- 添加索引优化查询性能
CREATE INDEX idx_process_status ON violations(process_status);
CREATE INDEX idx_process_type ON violations(process_type);
CREATE INDEX idx_plate_process ON violations(plate_number, process_status);

-- 验证表结构
-- DESC violations;
-- SHOW INDEX FROM violations;

