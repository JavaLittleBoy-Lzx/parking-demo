-- 为 appointment 表添加 owneropenid 字段
-- 用于存储业主的微信openid，方便后续推送消息给业主

USE parking;

-- 添加 owneropenid 字段到 appointment 表
ALTER TABLE appointment 
ADD COLUMN owneropenid VARCHAR(100) COMMENT '业主openid（用于业主预约）' AFTER ownerphone;

-- 查看表结构确认修改成功
DESC appointment;

-- 说明：
-- 1. 该字段用于业主预约时存储业主的微信openid
-- 2. 通过业主手机号查询 user_mapper 表获取对应的openid
-- 3. 方便后续向业主推送预约状态变更等消息
