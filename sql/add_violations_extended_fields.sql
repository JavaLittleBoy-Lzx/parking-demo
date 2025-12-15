-- 为violations表添加东北林业大学扩展字段
-- 用于存储从ACMS融合接口获取的详细车主信息
-- 执行时间: 2025-10-02

-- 1. 添加人员类别字段
ALTER TABLE violations 
ADD COLUMN owner_category VARCHAR(50) COMMENT '人员类别(仅东北林业大学，如：教职工、学生等)';

-- 2. 添加单位/公司字段
ALTER TABLE violations 
ADD COLUMN customer_company VARCHAR(200) COMMENT '单位/公司(仅东北林业大学)';

-- 3. 添加车位号字段
ALTER TABLE violations 
ADD COLUMN customer_room_number VARCHAR(50) COMMENT '车位号(仅东北林业大学)';

-- 添加索引以提高查询性能
CREATE INDEX idx_violations_owner_category ON violations(owner_category);
CREATE INDEX idx_violations_customer_company ON violations(customer_company);
CREATE INDEX idx_violations_room_number ON violations(customer_room_number);

-- 查看表结构确认
-- SHOW CREATE TABLE violations;

-- 查询示例：查询某单位的违规记录
-- SELECT * FROM violations WHERE customer_company = '某学院' ORDER BY created_at DESC;

-- 查询示例：查询某人员类别的违规记录
-- SELECT * FROM violations WHERE owner_category = '教职工' ORDER BY created_at DESC; 