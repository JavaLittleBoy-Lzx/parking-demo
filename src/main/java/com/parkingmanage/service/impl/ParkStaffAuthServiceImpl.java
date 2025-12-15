package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.parkingmanage.entity.ParkStaff;
import com.parkingmanage.mapper.ParkStaffMapper;
import com.parkingmanage.mapper.UserTokenMapper;
import com.parkingmanage.service.ParkStaffAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 车场人员认证服务实现类
 * 
 * @author parking-system
 * @version 1.0
 */
@Service
@Transactional
public class ParkStaffAuthServiceImpl implements ParkStaffAuthService {

    @Autowired
    private ParkStaffMapper parkStaffMapper;

    @Autowired
    private UserTokenMapper userTokenMapper;

    @Override
    public ParkStaff findByUsername(String username) {
        QueryWrapper<ParkStaff> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return parkStaffMapper.selectOne(queryWrapper);
    }

    @Override
    public void updateLastLogin(Integer userId, String loginIp) {
        UpdateWrapper<ParkStaff> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId)
                    .set("last_login_time", LocalDateTime.now())
                    .set("last_login_ip", loginIp)
                    // 登录成功，重置失败次数和锁定时间
                    .set("failed_login_count", 0)
                    .set("lock_time", null);
        parkStaffMapper.update(null, updateWrapper);
    }

    @Override
    public void saveUserToken(Integer userId, String token) {
        try {
            // 计算token的hash值
            String tokenHash = hashToken(token);
            
            // 设置过期时间（24小时后）
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
            
            // 先使该用户的所有旧token失效
            invalidateUserTokens(userId);
            
            // 插入新token
            Map<String, Object> tokenRecord = new HashMap<>();
            tokenRecord.put("user_id", userId);
            tokenRecord.put("token_hash", tokenHash);
            tokenRecord.put("expires_at", expiresAt);
            tokenRecord.put("created_at", LocalDateTime.now());
            tokenRecord.put("is_active", 1);
            
            userTokenMapper.insert(tokenRecord);
            
        } catch (Exception e) {
            // 记录日志，但不影响登录流程
            System.err.println("保存token失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            String tokenHash = hashToken(token);
            
            // 查询数据库中是否存在有效的token
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put("token_hash", tokenHash);
            queryParams.put("is_active", 1);
            
            Map<String, Object> tokenRecord = userTokenMapper.selectByTokenHash(tokenHash);
            
            if (tokenRecord == null) {
                return false;
            }
            
            // 检查是否过期
            LocalDateTime expiresAt = (LocalDateTime) tokenRecord.get("expires_at");
            return expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
            
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void invalidateToken(String token) {
        try {
            String tokenHash = hashToken(token);
            userTokenMapper.invalidateToken(tokenHash);
        } catch (Exception e) {
            // 记录日志但不抛出异常
            System.err.println("使token失效失败: " + e.getMessage());
        }
    }

    /**
     * 使用户的所有token失效
     * 
     * @param userId 用户ID
     */
    private void invalidateUserTokens(Integer userId) {
        userTokenMapper.invalidateUserTokens(userId);
    }

    /**
     * 计算token的hash值
     * 
     * @param token 原始token
     * @return hash值
     */
    private String hashToken(String token) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(token.getBytes());
        
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
} 