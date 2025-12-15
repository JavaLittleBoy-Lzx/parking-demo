-- 违规提醒记录表
CREATE TABLE `violation_reminders` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plate_number` varchar(20) NOT NULL COMMENT '车牌号',
  `owner_name` varchar(50) DEFAULT NULL COMMENT '车主姓名',
  `owner_phone` varchar(20) NOT NULL COMMENT '车主电话',
  `violation_type` varchar(50) NOT NULL COMMENT '违规类型',
  `violation_location` varchar(200) NOT NULL COMMENT '违规地点',
  `violation_time` datetime NOT NULL COMMENT '违规时间',
  `reminder_time` datetime NOT NULL COMMENT '提醒发送时间',
  `reminder_template_code` varchar(50) DEFAULT NULL COMMENT '提醒模板代码',
  `reminder_content` text COMMENT '提醒内容',
  `is_processed` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已处理(0:未处理,1:已处理)',
  `processed_time` datetime DEFAULT NULL COMMENT '处理时间',
  `processed_by` varchar(50) DEFAULT NULL COMMENT '处理人',
  `park_code` varchar(50) DEFAULT NULL COMMENT '车场编码',
  `park_name` varchar(100) DEFAULT NULL COMMENT '车场名称',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_plate_number` (`plate_number`),
  KEY `idx_owner_phone` (`owner_phone`),
  KEY `idx_violation_time` (`violation_time`),
  KEY `idx_reminder_time` (`reminder_time`),
  KEY `idx_is_processed` (`is_processed`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规提醒记录表';

-- 插入示例数据
INSERT INTO `violation_reminders` (
  `plate_number`, 
  `owner_name`, 
  `owner_phone`, 
  `violation_type`, 
  `violation_location`, 
  `violation_time`, 
  `reminder_time`, 
  `reminder_template_code`, 
  `reminder_content`, 
  `is_processed`, 
  `park_code`, 
  `park_name`
) VALUES 
(
  '京A12345', 
  '张三', 
  '13800138001', 
  '违停', 
  'A区停车场入口', 
  '2024-01-15 14:30:00', 
  '2024-01-15 14:35:00', 
  'SMS_496055951', 
  '【停车提醒】您的车辆京A12345在A区停车场入口发生违停违规，请及时处理。', 
  0, 
  'PARK001', 
  '测试停车场'
),
(
  '京B67890', 
  '李四', 
  '13800138002', 
  '超时', 
  'B区停车场', 
  '2024-01-15 15:20:00', 
  '2024-01-15 15:25:00', 
  'SMS_496055951', 
  '【违规通知】您的车辆京B67890在B区停车场再次发生超时违规，请立即处理，否则将影响您的停车权益。', 
  1, 
  'PARK001', 
  '测试停车场'
);
