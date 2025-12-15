-- 月票车超时配置表
CREATE TABLE `monthly_ticket_timeout_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `park_code` varchar(50) NOT NULL COMMENT '车场编码',
  `park_name` varchar(100) DEFAULT NULL COMMENT '车场名称',
  `timeout_minutes` int(11) NOT NULL DEFAULT 60 COMMENT '超时时间（分钟）',
  `max_violation_count` int(11) NOT NULL DEFAULT 5 COMMENT '最大违规次数',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用，0-禁用',
  `description` varchar(255) DEFAULT NULL COMMENT '配置说明',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_park_code` (`park_code`) COMMENT '车场编码唯一索引',
  KEY `idx_park_code_active` (`park_code`, `is_active`) COMMENT '车场编码和启用状态联合索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月票车超时配置表';

-- 插入一些示例数据
INSERT INTO `monthly_ticket_timeout_config` (`park_code`, `park_name`, `timeout_minutes`, `max_violation_count`, `is_active`, `description`, `created_by`) VALUES
('PARK001', '示例车场1', 30, 5, 1, '月票车超时配置: timeout=30分钟,maxCount=5次', 'SYSTEM'),
('PARK002', '示例车场2', 60, 3, 1, '月票车超时配置: timeout=60分钟,maxCount=3次', 'SYSTEM'); 