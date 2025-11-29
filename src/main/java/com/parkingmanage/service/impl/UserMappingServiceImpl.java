package com.parkingmanage.service.impl;

import com.parkingmanage.entity.UserMapping;
import com.parkingmanage.mapper.UserMappingMapper;
import com.parkingmanage.service.UserMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 用户映射服务实现类
 * 
 * @author MLH
 * @since 2025-01-28
 */
@Service
public class UserMappingServiceImpl implements UserMappingService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserMappingServiceImpl.class);
    
    @Resource
    private UserMappingMapper userMappingMapper;
    
    @Override
    public UserMapping getByOpenid(String openid) {
        return userMappingMapper.findByOpenid(openid);
    }
    
    @Override
    public UserMapping getByPhone(String phone) {
        List<UserMapping> list = userMappingMapper.findByPhone(phone);
        return list.isEmpty() ? null : list.get(0);
    }
    
    @Override
    public UserMapping saveOrUpdateFromWebAuth(String openid, String nickname, String avatarUrl) {
        try {
            UserMapping existingUser = userMappingMapper.findByOpenid(openid);
            
            if (existingUser != null) {
                // 用户已存在，更新昵称、头像和更新时间
                existingUser.setNickname(nickname);
                existingUser.setAvatarUrl(avatarUrl);
                existingUser.setUpdateTime(new Date());
                userMappingMapper.updateUserMapping(existingUser);
                
                logger.info("✅ 更新已存在用户信息 - openid: {}, nickname: {}, avatarUrl: {}", 
                    openid, nickname, avatarUrl != null ? "已设置" : "未设置");
                return existingUser;
            } else {
                // 用户不存在，创建新记录，默认未关注
                UserMapping newUser = new UserMapping();
                newUser.setOpenid(openid);
                newUser.setNickname(nickname);
                newUser.setAvatarUrl(avatarUrl);
                newUser.setIsFollowed(0); // 默认未关注
                newUser.setCreateTime(new Date());
                newUser.setUpdateTime(new Date());
                
                userMappingMapper.insertUserMapping(newUser);
                
                logger.info("✅ 创建新用户记录 - openid: {}, nickname: {}, avatarUrl: {}", 
                    openid, nickname, avatarUrl != null ? "已设置" : "未设置");
                return newUser;
            }
            
        } catch (Exception e) {
            logger.error("❌ 保存或更新用户映射失败 - openid: {}, nickname: {}, avatarUrl: {}", 
                openid, nickname, avatarUrl, e);
            throw new RuntimeException("保存用户信息失败", e);
        }
    }
    
    @Override
    public UserMapping updateFollowStatus(String openid, Integer isFollowed) {
        try {
            UserMapping existingUser = userMappingMapper.findByOpenid(openid);
            
            if (existingUser != null) {
                existingUser.setIsFollowed(isFollowed);
                existingUser.setUpdateTime(new Date());
                
                if (isFollowed == 1) {
                    existingUser.setFollowTime(new Date());
                    existingUser.setUnfollowTime(null);
                } else {
                    existingUser.setUnfollowTime(new Date());
                }
                
                userMappingMapper.updateUserMapping(existingUser);
                
                logger.info("✅ 更新用户关注状态 - openid: {}, isFollowed: {}", openid, isFollowed);
                return existingUser;
            } else {
                logger.warn("⚠️ 更新关注状态时未找到用户 - openid: {}", openid);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("❌ 更新用户关注状态失败 - openid: {}, isFollowed: {}", openid, isFollowed, e);
            throw new RuntimeException("更新关注状态失败", e);
        }
    }
    
    @Override
    public UserMapping insertUserMapping(UserMapping userMapping) {
        userMappingMapper.insertUserMapping(userMapping);
        return userMapping;
    }
    
    @Override
    public UserMapping updateUserMapping(UserMapping userMapping) {
        userMappingMapper.updateUserMapping(userMapping);
        return userMapping;
    }
} 