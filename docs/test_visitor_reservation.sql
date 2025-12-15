-- ========================================
-- 测试用访客预约数据表
-- 用于模拟外部接口的数据源
-- ========================================

CREATE TABLE `test_visitor_reservation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  
  -- 预约基本信息
  `reservation_id` VARCHAR(100) NOT NULL COMMENT '预约记录ID',
  
  -- 访客信息
  `visitor_name` VARCHAR(100) DEFAULT NULL COMMENT '访客姓名',
  `visitor_phone` VARCHAR(50) DEFAULT NULL COMMENT '访客手机号码',
  `visitor_id_card` VARCHAR(50) DEFAULT NULL COMMENT '访客身份证号码',
  
  -- 被访信息
  `department_name` VARCHAR(200) DEFAULT NULL COMMENT '被访部门名称',
  
  -- 车辆信息
  `car_number` VARCHAR(200) DEFAULT NULL COMMENT '随行车辆（多个用逗号分隔）',
  `park_name` VARCHAR(200) DEFAULT NULL COMMENT '车场名称',
  
  -- 预约时间
  `start_time` VARCHAR(50) DEFAULT NULL COMMENT '预约开始时间',
  `end_time` VARCHAR(50) DEFAULT NULL COMMENT '预约结束时间',
  
  -- 表单信息
  `vip_type_name` VARCHAR(100) DEFAULT NULL COMMENT '表单名称（VIP类型）',
  
  -- 备注信息
  `remark1` VARCHAR(500) DEFAULT NULL COMMENT '备注信息1',
  `remark2` VARCHAR(500) DEFAULT NULL COMMENT '备注信息2',
  `remark3` VARCHAR(500) DEFAULT NULL COMMENT '备注信息3',
  
  -- 系统字段
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reservation_id` (`reservation_id`) COMMENT '预约ID唯一索引',
  KEY `idx_create_time` (`create_time`) COMMENT '创建时间索引，用于时间范围查询',
  KEY `idx_visitor_phone` (`visitor_phone`) COMMENT '访客手机号索引',
  KEY `idx_car_number` (`car_number`(50)) COMMENT '车牌号索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用访客预约数据表';

-- ========================================
-- 插入测试数据
-- ========================================

INSERT INTO `test_visitor_reservation` (
  reservation_id, visitor_name, visitor_phone, visitor_id_card, department_name,
  car_number, park_name, start_time, end_time, vip_type_name,
  remark1, remark2, remark3
) VALUES
-- 今天的预约
('R2024010101', '张三', '13800138001', '110101199001011234', '信息技术部',
 '京A12345', '东门停车场', '2024-01-01 09:00:00', '2024-01-01 18:00:00', '临时访客',
 '来访洽谈业务', '需要会议室', '已确认'),

('R2024010102', '李四', '13800138002', '110101199002021234', '人力资源部',
 '京B67890', '西门停车场', '2024-01-01 10:00:00', '2024-01-01 17:00:00', '临时访客',
 '面试候选人', '', '已确认'),

('R2024010103', '王五', '13800138003', '110101199003031234', '财务部',
 '京C11111', '南门停车场', '2024-01-01 14:00:00', '2024-01-01 16:00:00', '临时访客',
 '财务审计', '需要停车位', ''),

('R2024010104', '赵六', '13800138004', '', '市场部',
 '京D22222,京D33333', '北门停车场', '2024-01-01 08:00:00', '2024-01-01 12:00:00', '临时访客',
 '产品展示', '两辆车', '已确认'),

('R2024010105', '孙七', '13800138005', '110101199005051234', '研发部',
 '京E44444', '东门停车场', '2024-01-01 13:00:00', '2024-01-01 19:00:00', '长期访客',
 '技术交流', '需要开通长期VIP', '已确认'),

-- 昨天的预约
('R2023123101', '周八', '13800138006', '110101199006061234', '行政部',
 '京F55555', '西门停车场', '2023-12-31 09:00:00', '2023-12-31 17:00:00', '临时访客',
 '年终总结', '', '已确认'),

('R2023123102', '吴九', '13800138007', '', '技术部',
 '京G66666', '南门停车场', '2023-12-31 10:00:00', '2023-12-31 18:00:00', '临时访客',
 '技术支持', '', '已确认'),

-- 明天的预约
('R2024010201', '郑十', '13800138008', '110101199008081234', '客服部',
 '京H77777', '北门停车场', '2024-01-02 09:00:00', '2024-01-02 17:00:00', '临时访客',
 '客户拜访', '', '待确认'),

('R2024010202', '陈十一', '13800138009', '110101199009091234', '采购部',
 '京J88888', '东门停车场', '2024-01-02 14:00:00', '2024-01-02 16:00:00', '临时访客',
 '供应商考察', '', '待确认'),

-- 体育馆访客（会被排除）
('R2024010301', '体育馆访客1', '13800138010', '', '体育馆',
 '京K99999', '体育馆停车场', '2024-01-03 08:00:00', '2024-01-03 20:00:00', '体育馆自助访客',
 '参加活动', '', ''),

('R2024010302', '体育馆访客2', '13800138011', '', '体育馆',
 '京L00000', '体育馆停车场', '2024-01-03 09:00:00', '2024-01-03 21:00:00', '体育馆访客车辆',
 '观看比赛', '', '');

-- ========================================
-- 查询示例
-- ========================================

-- 查询今天的预约
-- SELECT * FROM test_visitor_reservation 
-- WHERE create_time >= '2024-01-01 00:00:00' 
--   AND create_time < '2024-01-02 00:00:00';

-- 查询指定时间范围的预约（带分页）
-- SELECT * FROM test_visitor_reservation 
-- WHERE create_time BETWEEN '2023-12-31 00:00:00' AND '2024-01-02 00:00:00'
-- ORDER BY create_time DESC
-- LIMIT 0, 10;

-- 统计预约数量
-- SELECT COUNT(*) as total FROM test_visitor_reservation 
-- WHERE create_time BETWEEN '2024-01-01 00:00:00' AND '2024-01-01 23:59:59';

