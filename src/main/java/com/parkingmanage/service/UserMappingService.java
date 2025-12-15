package com.parkingmanage.service;

import com.parkingmanage.entity.UserMapping;

/**
 * 用户映射服务接口
 * 
 * @author MLH
 * @since 2025-01-28
 */
public interface UserMappingService {
    
    /**
     * 根据openid查询用户映射
     */
    UserMapping getByOpenid(String openid);
    
    /**
     * 根据手机号查询用户映射
     */
    UserMapping getByPhone(String phone);
    
    /**
     * 保存或更新用户映射（网页授权时调用）
     * 如果用户不存在则创建，存在则更新基本信息
     * @param openid 微信openid
     * @param nickname 用户昵称
     * @param avatarUrl 用户头像URL
     * @return 保存或更新后的用户映射对象
     */
    UserMapping saveOrUpdateFromWebAuth(String openid, String nickname, String avatarUrl);
    
    /**
     * 更新关注状态
     * @return 更新后的用户映射对象
     */
    UserMapping updateFollowStatus(String openid, Integer isFollowed);
    
    /**
     * 插入新用户映射
     * @return 插入后的用户映射对象（包含自动生成的ID）
     */
    UserMapping insertUserMapping(UserMapping userMapping);
    
    /**
     * 更新用户映射
     * @return 更新后的用户映射对象
     */
    UserMapping updateUserMapping(UserMapping userMapping);
} 