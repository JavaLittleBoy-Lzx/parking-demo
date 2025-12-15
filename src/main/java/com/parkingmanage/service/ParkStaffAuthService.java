package com.parkingmanage.service;

import com.parkingmanage.entity.ParkStaff;

/**
 * 车场人员认证服务接口
 * 
 * @author parking-system
 * @version 1.0
 */
public interface ParkStaffAuthService {

    /**
     * 根据用户名查找车场人员
     * 
     * @param username 用户名
     * @return 车场人员信息
     */
    ParkStaff findByUsername(String username);

    /**
     * 更新最后登录信息
     * 
     * @param userId 用户ID
     * @param loginIp 登录IP
     */
    void updateLastLogin(Integer userId, String loginIp);

    /**
     * 保存用户token
     * 
     * @param userId 用户ID
     * @param token token值
     */
    void saveUserToken(Integer userId, String token);

    /**
     * 验证token是否有效
     * 
     * @param token token值
     * @return 是否有效
     */
    boolean isTokenValid(String token);

    /**
     * 使token失效
     * 
     * @param token token值
     */
    void invalidateToken(String token);
} 