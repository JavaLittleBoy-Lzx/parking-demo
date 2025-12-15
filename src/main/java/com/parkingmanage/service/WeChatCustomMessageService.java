package com.parkingmanage.service;

import java.util.Map;

/**
 * 微信客服消息服务接口
 * 
 * @author System
 * @since 2024-01-01
 */
public interface WeChatCustomMessageService {
    
    /**
     * 发送文本消息
     * 
     * @param openId 接收者openId
     * @param content 文本内容
     * @return 发送结果
     */
    boolean sendTextMessage(String openId, String content);
    
    /**
     * 发送图片消息
     * 
     * @param openId 接收者openId
     * @param mediaId 图片素材的media_id
     * @return 发送结果
     */
    boolean sendImageMessage(String openId, String mediaId);
    
    /**
     * 发送小程序卡片
     * 
     * @param openId 接收者openId
     * @param title 小程序卡片标题
     * @param appId 小程序appid
     * @param pagePath 小程序页面路径
     * @param thumbMediaId 小程序卡片图片的media_id
     * @return 发送结果
     */
    boolean sendMiniprogramCard(String openId, String title, String appId, 
                                String pagePath, String thumbMediaId);
    
    /**
     * 发送欢迎消息（文本+图片+小程序卡片）
     * 
     * @param openId 接收者openId
     * @return 发送结果
     */
    boolean sendWelcomeMessage(String openId);
}
