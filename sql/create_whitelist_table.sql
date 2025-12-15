-- 创建白名单管理表
-- 用于存储车辆白名单信息，实现免检功能

CREATE TABLE IF NOT EXISTS `whitelist` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plate_number` varchar(20) NOT NULL COMMENT '车牌号',
  `park_name` varchar(100) NOT NULL COMMENT '停车场名称',
  `owner_name` varchar(50) DEFAULT NULL COMMENT '车主姓名',
  `owner_phone` varchar(20) DEFAULT NULL COMMENT '车主电话',
  `owner_address` varchar(500) DEFAULT NULL COMMENT '车主地址',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plate_park` (`plate_number`, `park_name`) COMMENT '车牌+车场唯一索引',
  KEY `idx_plate_number` (`plate_number`) COMMENT '车牌号索引',
  KEY `idx_park_name` (`park_name`) COMMENT '车场名称索引',
  KEY `idx_owner_name` (`owner_name`) COMMENT '车主姓名索引',
  KEY `idx_owner_phone` (`owner_phone`) COMMENT '车主电话索引',
  KEY `idx_created_at` (`created_at`) COMMENT '创建时间索引'
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='车辆白名单管理表';

-- 添加表备注
ALTER TABLE `whitelist` COMMENT = '车辆白名单管理表，用于存储免检车辆信息';

