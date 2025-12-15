-- 创建系统配置表
CREATE TABLE `system_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '配置值',
  `config_name` varchar(100) NOT NULL COMMENT '配置名称',
  `config_desc` varchar(255) DEFAULT NULL COMMENT '配置描述',
  `config_type` varchar(20) NOT NULL DEFAULT 'system' COMMENT '配置类型：system-系统配置，business-业务配置，security-安全配置',
  `editable` tinyint(1) DEFAULT 1 COMMENT '是否可编辑：0-不可编辑，1-可编辑',
  `sort_order` int(11) DEFAULT 0 COMMENT '排序',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_config_type` (`config_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 插入默认系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `config_name`, `config_desc`, `config_type`, `editable`, `sort_order`, `status`, `created_by`) VALUES
('system.name', '智慧停车管理系统', '系统名称', '系统显示名称', 'system', 1, 1, 1, 'system'),
('system.version', '1.0.0', '系统版本', '当前系统版本号', 'system', 0, 2, 1, 'system'),
('system.copyright', '© 2024 智慧停车管理系统', '版权信息', '系统版权信息', 'system', 1, 3, 1, 'system'),
('system.logo', '/static/images/logo.png', '系统Logo', '系统Logo图片路径', 'system', 1, 4, 1, 'system'),
('system.favicon', '/static/images/favicon.ico', '系统图标', '系统图标路径', 'system', 1, 5, 1, 'system'),
('business.parking.timeout', '30', '停车超时时间(分钟)', '车辆停车超时判定时间', 'business', 1, 1, 1, 'system'),
('business.monthly.ticket.timeout', '60', '月票超时时间(分钟)', '月票车辆超时判定时间', 'business', 1, 2, 1, 'system'),
('business.violation.auto.blacklist', 'true', '违规自动拉黑', '是否启用违规自动拉黑功能', 'business', 1, 3, 1, 'system'),
('business.violation.blacklist.days', '7', '违规拉黑天数', '违规自动拉黑的天数', 'business', 1, 4, 1, 'system'),
('security.password.min.length', '6', '密码最小长度', '用户密码最小长度要求', 'security', 1, 1, 1, 'system'),
('security.login.max.attempts', '5', '最大登录尝试次数', '用户最大登录失败次数', 'security', 1, 2, 1, 'system'),
('security.session.timeout', '30', '会话超时时间(分钟)', '用户会话超时时间', 'security', 1, 3, 1, 'system'),
('notification.sms.enabled', 'true', '短信通知开关', '是否启用短信通知功能', 'business', 1, 1, 1, 'system'),
('notification.wechat.enabled', 'true', '微信通知开关', '是否启用微信通知功能', 'business', 1, 2, 1, 'system'),
('notification.email.enabled', 'false', '邮件通知开关', '是否启用邮件通知功能', 'business', 1, 3, 1, 'system');
