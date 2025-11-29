package com.parkingmanage.service;

/**
 * 微信事件处理服务接口
 * 用于处理微信公众号推送的各种事件
 * 
 * @author MLH
 * @since 2024-01-01
 */
public interface WeChatEventService {
    
    /**
     * 处理微信事件推送
     * @param xmlData 微信推送的XML数据
     * @return 处理结果，返回"success"表示处理成功
     */
    String handleWeChatEvent(String xmlData);
    
    /**
     * 处理用户关注事件
     * @param openId 用户openId
     * @param eventTime 事件时间
     */
    void handleSubscribeEvent(String openId, Long eventTime);
    
    /**
     * 处理用户取消关注事件
     * @param openId 用户openId  
     * @param eventTime 事件时间
     */
    void handleUnsubscribeEvent(String openId, Long eventTime);
    
    /**
     * 异步处理微信事件推送
     * 根据官方文档，为避免5秒超时，使用异步方式处理
     * 
     * @param xmlData 微信推送的XML数据
     */
    void processEventAsync(String xmlData);
} 