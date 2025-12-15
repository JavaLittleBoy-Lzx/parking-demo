-- 创建短信模板表
CREATE TABLE IF NOT EXISTS `sms_template` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID号',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `sign_name` varchar(100) NOT NULL COMMENT '模板签名',
  `template_code` varchar(50) NOT NULL COMMENT '模板CODE',
  `template_type` int(11) DEFAULT NULL COMMENT '模板类型：1-违规提醒，2-停车超时，3-其他',
  `description` varchar(500) DEFAULT NULL COMMENT '模板描述',
  `deleted` int(11) DEFAULT '0' COMMENT '逻辑删除标识：0：未删除，1：已删除',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信模板管理表';

-- 插入示例数据
INSERT INTO `sms_template` (`template_name`, `sign_name`, `template_code`, `template_type`, `description`)
VALUES 
('违规提醒模板', '东北林业大学', 'SMS_496020098', 1, '用于车辆违规停车提醒通知'),
('停车超时模板', '东北林业大学', 'SMS_498220005', 2, '用于车辆停车超时提醒通知');

-- 修改车场信息表，添加短信模板关联字段
ALTER TABLE `yard_info` ADD COLUMN `sms_template_id` int(11) DEFAULT NULL COMMENT '关联的短信模板ID' AFTER `yard_no`;

-- 添加外键约束（可选）
-- ALTER TABLE `yard_info` ADD CONSTRAINT `fk_yard_sms_template` 
-- FOREIGN KEY (`sms_template_id`) REFERENCES `sms_template`(`id`) ON DELETE SET NULL ON UPDATE CASCADE;

