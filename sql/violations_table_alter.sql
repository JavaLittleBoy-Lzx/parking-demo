-- 为violations表添加东北林业大学专用字段
-- 用于存储从ACMS接口获取的VIP车主信息

ALTER TABLE violations 
ADD COLUMN vip_type_name VARCHAR(100) COMMENT 'VIP类型名称(仅东北林业大学)';

ALTER TABLE violations 
ADD COLUMN owner_name VARCHAR(50) COMMENT '车主姓名(仅东北林业大学)';

ALTER TABLE violations 
ADD COLUMN owner_phone VARCHAR(20) COMMENT '车主手机号(仅东北林业大学)';

ALTER TABLE violations 
ADD COLUMN owner_address VARCHAR(500) COMMENT '车主单位地址(仅东北林业大学，由公司+部门+地址+房间号组成)';

-- 添加索引以提高查询性能
CREATE INDEX idx_violations_vip_type ON violations(vip_type_name);
CREATE INDEX idx_violations_owner_name ON violations(owner_name);
CREATE INDEX idx_violations_owner_phone ON violations(owner_phone);

-- 添加备注说明
ALTER TABLE violations COMMENT = '违规记录表，包含东北林业大学专用VIP车主信息字段'; 