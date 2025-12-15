-- ============================================
-- 巡检员值班状态管理功能 - 数据库脚本
-- 创建日期：2025-12-04
-- 功能说明：为巡检员添加值班/离岗状态管理
-- ============================================

-- 1. patrol表添加字段
-- ============================================

-- 添加消息通知开关字段
ALTER TABLE patrol 
ADD COLUMN notification_enabled TINYINT(1) DEFAULT 1 
COMMENT '消息通知开关：1=接收（值班中），0=不接收（离岗）';

-- 添加最后状态变更时间
ALTER TABLE patrol 
ADD COLUMN last_status_change_time DATETIME DEFAULT CURRENT_TIMESTAMP
COMMENT '最后一次值班状态变更时间';

-- 添加索引（用于快速查询值班中的巡检员）
CREATE INDEX idx_notification_enabled ON patrol(notification_enabled);

-- 添加组合索引（用于按小区查询值班巡检员）
CREATE INDEX idx_community_notification ON patrol(community, notification_enabled);

-- ============================================
-- 2. 值班状态变更日志表（可选，用于审计）
-- ============================================

CREATE TABLE IF NOT EXISTS patrol_duty_log (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  patrol_id INT NOT NULL COMMENT '巡检员ID',
  patrol_name VARCHAR(50) COMMENT '巡检员姓名',
  openid VARCHAR(100) COMMENT '微信openid',
  community VARCHAR(100) COMMENT '所属小区',
  old_status TINYINT(1) COMMENT '变更前状态：0=离岗，1=值班',
  new_status TINYINT(1) COMMENT '变更后状态：0=离岗，1=值班',
  change_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
  change_source VARCHAR(50) DEFAULT 'APP' COMMENT '变更来源：APP（小程序）/ADMIN（管理后台）/SYSTEM（系统）',
  remark VARCHAR(200) COMMENT '备注',
  
  INDEX idx_patrol_id (patrol_id),
  INDEX idx_change_time (change_time),
  INDEX idx_community (community)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检员值班状态变更日志';

-- ============================================
-- 3. 初始化数据（可选）
-- ============================================

-- 为所有现有巡检员设置默认值班状态（值班中）
UPDATE patrol 
SET notification_enabled = 1,
    last_status_change_time = CURRENT_TIMESTAMP
WHERE notification_enabled IS NULL;

-- ============================================
-- 4. 验证查询（测试用）
-- ============================================

-- 查询所有巡检员的值班状态
-- SELECT 
--   id,
--   username,
--   community,
--   phone,
--   openid,
--   notification_enabled,
--   CASE 
--     WHEN notification_enabled = 1 THEN '值班中'
--     WHEN notification_enabled = 0 THEN '离岗'
--     ELSE '未知'
--   END as status_text,
--   last_status_change_time
-- FROM patrol
-- WHERE status = '已确定'
-- ORDER BY community, username;

-- 查询各小区值班中的巡检员数量
-- SELECT 
--   community,
--   COUNT(*) as total_patrol,
--   SUM(CASE WHEN notification_enabled = 1 THEN 1 ELSE 0 END) as on_duty_count,
--   SUM(CASE WHEN notification_enabled = 0 THEN 1 ELSE 0 END) as off_duty_count
-- FROM patrol
-- WHERE status = '已确定'
-- GROUP BY community;

-- ============================================
-- 脚本执行完成
-- ============================================
