-- 车场与短信模板关联表（多对多关系）

-- 1. 创建车场短信模板关联表
CREATE TABLE IF NOT EXISTS `yard_sms_template_relation` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `yard_id` int(11) NOT NULL COMMENT '车场ID',
  `sms_template_id` int(11) NOT NULL COMMENT '短信模板ID',
  `template_usage` int(11) DEFAULT NULL COMMENT '模板用途：1-违规提醒，2-停车超时，3-其他',
  `is_default` tinyint(1) DEFAULT 0 COMMENT '是否为默认模板：0-否，1-是',
  `deleted` int(11) DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_yard_id` (`yard_id`),
  KEY `idx_sms_template_id` (`sms_template_id`),
  UNIQUE KEY `uk_yard_template` (`yard_id`, `sms_template_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车场短信模板关联表';

-- 2. 如果之前已经添加了 sms_template_id 字段到 yard_info 表，可以选择保留或删除
-- 保留的话可以作为快速访问字段（存储默认模板ID）
-- 或者完全删除，只使用关联表
-- ALTER TABLE `yard_info` DROP COLUMN `sms_template_id`;

-- 3. 插入示例关联数据（假设车场ID为1，模板ID为1和2）
-- INSERT INTO `yard_sms_template_relation` (`yard_id`, `sms_template_id`, `template_usage`, `is_default`)
-- VALUES 
-- (1, 1, 1, 1),  -- 车场1关联违规提醒模板，设为默认
-- (1, 2, 2, 0);  -- 车场1关联停车超时模板

-- 4. 添加外键约束（可选，建议在生产环境中使用）
-- ALTER TABLE `yard_sms_template_relation` 
-- ADD CONSTRAINT `fk_yard_relation` 
-- FOREIGN KEY (`yard_id`) REFERENCES `yard_info`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- ALTER TABLE `yard_sms_template_relation` 
-- ADD CONSTRAINT `fk_template_relation` 
-- FOREIGN KEY (`sms_template_id`) REFERENCES `sms_template`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- 5. 查询某个车场的所有短信模板
-- SELECT 
--     y.id as yard_id,
--     y.yard_name,
--     st.id as template_id,
--     st.template_name,
--     st.sign_name,
--     st.template_code,
--     st.template_type,
--     r.is_default
-- FROM yard_info y
-- INNER JOIN yard_sms_template_relation r ON y.id = r.yard_id
-- INNER JOIN sms_template st ON r.sms_template_id = st.id
-- WHERE y.id = ? AND r.deleted = 0 AND st.deleted = 0;

