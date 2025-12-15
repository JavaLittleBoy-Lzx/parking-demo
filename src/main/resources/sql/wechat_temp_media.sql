-- 微信临时素材表
CREATE TABLE IF NOT EXISTS `wechat_temp_media` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `media_type` varchar(20) NOT NULL COMMENT '素材类型：image-图片、voice-语音、video-视频、thumb-缩略图',
  `media_id` varchar(255) NOT NULL COMMENT '微信返回的media_id（临时素材标识，3天有效）',
  `description` varchar(100) NOT NULL COMMENT '素材用途说明（如：欢迎语小程序码）',
  `file_name` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径（本地存储路径，用于重新上传）',
  `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小（字节）',
  `created_at` datetime NOT NULL COMMENT 'media_id创建时间',
  `expired_at` datetime NOT NULL COMMENT 'media_id过期时间（创建时间+3天）',
  `updated_at` datetime DEFAULT NULL COMMENT '最后更新时间',
  `status` int(1) NOT NULL DEFAULT '1' COMMENT '状态：1-有效、0-已过期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_description` (`description`) COMMENT '用途唯一索引',
  KEY `idx_status_expired` (`status`, `expired_at`) COMMENT '状态和过期时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信临时素材表';

-- 插入示例数据（需要先手动上传获取真实的media_id）
-- INSERT INTO `wechat_temp_media` VALUES 
-- (1, 'image', 'MEDIA_ID_PLACEHOLDER', '小程序二维码', 'miniapp_qrcode.jpg', 
--  'd:/temp/wechat/media/小程序二维码_1234567890_miniapp_qrcode.jpg', 102400,
--  NOW(), DATE_ADD(NOW(), INTERVAL 3 DAY), NOW(), 1, '用于用户关注时发送的小程序二维码');
