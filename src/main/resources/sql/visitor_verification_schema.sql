-- ========================================
-- 外来访客二维码验证方案 - 数据库表创建脚本
-- 版本: v2.0 (纯数据库版本)
-- 日期: 2025-11-23
-- 说明: 无需Redis，纯MySQL实现
-- ========================================

-- 1. 创建访客使用记录表
CREATE TABLE IF NOT EXISTS qr_visitor_usage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    qr_id VARCHAR(50) NOT NULL COMMENT '车场二维码ID',
    phone VARCHAR(20) NOT NULL COMMENT '访客手机号',
    
    -- 时间相关
    first_scan_time DATETIME NOT NULL COMMENT '首次扫码时间',
    last_scan_time DATETIME NOT NULL COMMENT '最后扫码时间',
    expires_at DATETIME NOT NULL COMMENT '过期时间（首次扫码+24小时）',
    
    -- 使用统计
    scan_count INT DEFAULT 1 COMMENT '今日扫码次数',
    total_count INT DEFAULT 1 COMMENT '累计使用次数',
    
    -- 位置信息
    last_latitude DECIMAL(10, 6) COMMENT '最后扫码纬度',
    last_longitude DECIMAL(10, 6) COMMENT '最后扫码经度',
    last_distance DECIMAL(10, 2) COMMENT '最后扫码距离（米）',
    
    -- 状态管理
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态: active-活跃, expired-已过期, blocked-已封禁',
    block_reason VARCHAR(200) COMMENT '封禁原因',
    
    -- 审计字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    UNIQUE KEY uk_qr_phone (qr_id, phone) COMMENT '唯一索引：一个手机号在一个车场只有一条有效记录',
    INDEX idx_phone (phone) COMMENT '手机号索引',
    INDEX idx_qr_id (qr_id) COMMENT '二维码ID索引',
    INDEX idx_first_scan (first_scan_time) COMMENT '首次扫码时间索引',
    INDEX idx_expires (expires_at) COMMENT '过期时间索引',
    INDEX idx_status (status) COMMENT '状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车场二维码访客使用记录表';

-- 2. 创建访客Token临时表
CREATE TABLE IF NOT EXISTS visitor_token (
    token VARCHAR(100) PRIMARY KEY COMMENT 'UUID token',
    qr_id VARCHAR(50) NOT NULL COMMENT '车场二维码ID',
    phone VARCHAR(20) NOT NULL COMMENT '访客手机号',
    
    -- 位置信息
    latitude DECIMAL(10, 6) COMMENT 'GPS纬度',
    longitude DECIMAL(10, 6) COMMENT 'GPS经度',
    distance DECIMAL(10, 2) COMMENT '距离车场的距离（米）',
    
    -- 时间管理
    create_time DATETIME NOT NULL COMMENT '创建时间',
    expire_time DATETIME NOT NULL COMMENT '过期时间（创建时间+5分钟）',
    
    -- 使用状态
    is_used TINYINT DEFAULT 0 COMMENT '是否已使用（0-未使用，1-已使用，一次性token）',
    used_time DATETIME COMMENT '使用时间',
    
    -- 索引
    INDEX idx_expire (expire_time) COMMENT '过期时间索引（定时清理）',
    INDEX idx_phone (phone) COMMENT '手机号索引',
    INDEX idx_qr_id (qr_id) COMMENT '二维码ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客临时Token表（5分钟有效期）';

-- 3. 修改预约表（新增访客token相关字段）
ALTER TABLE appointment 
ADD COLUMN IF NOT EXISTS visitor_token VARCHAR(100) COMMENT '访客token（用于验证）',
ADD COLUMN IF NOT EXISTS scan_time DATETIME COMMENT '扫码时间',
ADD INDEX IF NOT EXISTS idx_visitor_token (visitor_token),
ADD INDEX IF NOT EXISTS idx_scan_time (scan_time);

-- 4. 修改车场表（新增GPS坐标字段）
ALTER TABLE parking_lot
ADD COLUMN IF NOT EXISTS latitude DECIMAL(10, 6) COMMENT '纬度（WGS84坐标系）',
ADD COLUMN IF NOT EXISTS longitude DECIMAL(10, 6) COMMENT '经度（WGS84坐标系）',
ADD COLUMN IF NOT EXISTS location_radius INT DEFAULT 500 COMMENT '有效范围（米）',
ADD COLUMN IF NOT EXISTS qr_id VARCHAR(50) COMMENT '车场二维码ID',
ADD UNIQUE INDEX IF NOT EXISTS uk_qr_id (qr_id) COMMENT '二维码ID唯一索引';

-- 5. 插入示例数据（根据实际情况修改）
-- 欧洲新城停车场GPS坐标
UPDATE parking_lot 
SET latitude = 45.7568,          -- 哈尔滨欧洲新城纬度
    longitude = 126.6425,        -- 哈尔滨欧洲新城经度
    location_radius = 500,       -- 有效范围500米
    qr_id = '11802'              -- 车场二维码ID
WHERE name = '欧洲新城停车场';

-- ========================================
-- 完成！
-- 接下来请执行以下操作：
-- 1. 创建Java实体类（VisitorToken、QrVisitorUsage）
-- 2. 创建Mapper接口
-- 3. 创建Service层
-- 4. 创建Controller
-- 5. 创建定时任务
-- ========================================
