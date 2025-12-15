-- ============================================
-- 消息发送记录表
-- 功能：记录所有微信模板消息的发送情况
-- 用途：1. 防止重复发送  2. 追踪发送历史  3. 问题排查  4. 统计分析
-- ============================================

DROP TABLE IF EXISTS `notification_log`;

CREATE TABLE `notification_log` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  
  -- 关联信息
  `appointment_id` INT(11) DEFAULT NULL COMMENT '预约ID（关联appointment表）',
  `plate_number` VARCHAR(20) DEFAULT NULL COMMENT '车牌号',
  `openid` VARCHAR(100) NOT NULL COMMENT '接收者openid',
  
  -- 通知类型
  `notification_type` VARCHAR(50) NOT NULL COMMENT '通知类型：timeout_15min(超时15分钟), timeout_5min(超时5分钟), timeout_1min(超时1分钟), violation(违规停车告警), entry(进场通知), exit(出场通知), blacklist_add(加入黑名单), appointment_success(预约成功), appointment_pending(待审核提醒), appointment_audit(审核结果通知)',
  `template_id` VARCHAR(100) DEFAULT NULL COMMENT '微信模板ID',
  
  -- 通知内容
  `notify_time_point` INT(11) DEFAULT NULL COMMENT '通知时间点（分钟）：15/5/1，用于超时通知',
  `remaining_minutes` INT(11) DEFAULT NULL COMMENT '剩余时间（分钟），用于超时通知',
  `parking_minutes` INT(11) DEFAULT NULL COMMENT '停车时长（分钟），用于超时/违规通知',
  `park_name` VARCHAR(100) DEFAULT NULL COMMENT '停车场名称',
  `violation_location` VARCHAR(200) DEFAULT NULL COMMENT '违规地点',
  `entry_time` DATETIME DEFAULT NULL COMMENT '进场时间',
  `exit_time` DATETIME DEFAULT NULL COMMENT '出场时间',
  `parking_duration` VARCHAR(50) DEFAULT NULL COMMENT '停车时长文本（格式化后），如"2小时30分"',
  `blacklist_reason` VARCHAR(200) DEFAULT NULL COMMENT '加入黑名单原因',
  `blacklist_days` INT(11) DEFAULT NULL COMMENT '黑名单有效天数',
  `audit_status` VARCHAR(20) DEFAULT NULL COMMENT '审核状态：通过/拒绝',
  `audit_reason` VARCHAR(200) DEFAULT NULL COMMENT '审核原因/拒绝原因',
  `visit_date` VARCHAR(50) DEFAULT NULL COMMENT '预约访问日期',
  
  -- 接收者信息
  `receiver_type` VARCHAR(20) DEFAULT NULL COMMENT '接收者类型：visitor(访客), owner(业主), housekeeper(管家)',
  `appoint_type` VARCHAR(20) DEFAULT NULL COMMENT '预约类型：邀请/代人/自助/业主',
  
  -- 发送结果
  `send_status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '发送状态：0=失败, 1=成功, 2=跳过（重复）',
  `send_time` DATETIME NOT NULL COMMENT '发送时间',
  `success` TINYINT(1) DEFAULT 0 COMMENT '是否成功：0=否, 1=是',
  `error_code` VARCHAR(50) DEFAULT NULL COMMENT '错误代码',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  
  -- 微信返回
  `wechat_msg_id` VARCHAR(100) DEFAULT NULL COMMENT '微信消息ID',
  `wechat_response` TEXT DEFAULT NULL COMMENT '微信返回的完整响应（JSON格式）',
  
  -- 元数据
  `send_by` VARCHAR(50) DEFAULT 'scheduled_task' COMMENT '发送来源：scheduled_task(定时任务), api(API调用), manual(手动)',
  `retry_count` INT(11) DEFAULT 0 COMMENT '重试次数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  
  -- 索引优化
  KEY `idx_appointment_id` (`appointment_id`),
  KEY `idx_plate_number` (`plate_number`),
  KEY `idx_openid` (`openid`),
  KEY `idx_notification_type` (`notification_type`),
  KEY `idx_send_status` (`send_status`),
  KEY `idx_send_time` (`send_time`),
  KEY `idx_appoint_notify` (`appointment_id`, `notification_type`, `send_time`),
  KEY `idx_duplicate_check` (`appointment_id`, `notify_time_point`, `send_status`)
  
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息发送记录表';

-- ============================================
-- 创建视图：发送统计
-- ============================================

CREATE OR REPLACE VIEW `v_notification_statistics` AS
SELECT 
    DATE(send_time) AS send_date,
    notification_type,
    COUNT(*) AS total_count,
    SUM(CASE WHEN send_status = 1 THEN 1 ELSE 0 END) AS success_count,
    SUM(CASE WHEN send_status = 0 THEN 1 ELSE 0 END) AS fail_count,
    SUM(CASE WHEN send_status = 2 THEN 1 ELSE 0 END) AS skip_count,
    ROUND(SUM(CASE WHEN send_status = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS success_rate
FROM notification_log
GROUP BY DATE(send_time), notification_type
ORDER BY send_date DESC, notification_type;

-- ============================================
-- 示例查询
-- ============================================

-- 1. 查询某个预约的所有通知记录
-- SELECT * FROM notification_log WHERE appointment_id = 123 ORDER BY send_time;

-- 2. 查询今天发送失败的通知
-- SELECT * FROM notification_log WHERE DATE(send_time) = CURDATE() AND send_status = 0;

-- 3. 查询某辆车的通知历史
-- SELECT * FROM notification_log WHERE plate_number = '京A12345' ORDER BY send_time DESC;

-- 4. 统计今天的发送情况
-- SELECT * FROM v_notification_statistics WHERE send_date = CURDATE();

-- 5. 检查重复发送（5分钟内）
-- SELECT appointment_id, notify_time_point, COUNT(*) as count 
-- FROM notification_log 
-- WHERE send_time > DATE_SUB(NOW(), INTERVAL 5 MINUTE) 
-- GROUP BY appointment_id, notify_time_point 
-- HAVING count > 1;

-- 6. 查询发送成功率（按类型）
-- SELECT notification_type, 
--        COUNT(*) as total,
--        SUM(success) as success,
--        ROUND(SUM(success) * 100.0 / COUNT(*), 2) as success_rate
-- FROM notification_log
-- WHERE send_time > DATE_SUB(NOW(), INTERVAL 7 DAY)
-- GROUP BY notification_type;
