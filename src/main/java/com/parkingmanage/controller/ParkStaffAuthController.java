package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkingmanage.entity.ParkStaff;
import com.parkingmanage.entity.ActivityLog;
import com.parkingmanage.entity.YardInfo;
import com.parkingmanage.service.ParkStaffAuthService;
import com.parkingmanage.service.ParkStaffService;
import com.parkingmanage.service.ActivityLogService;
import com.parkingmanage.mapper.YardInfoMapper;
import com.parkingmanage.utils.JwtUtil;
import com.parkingmanage.util.PasswordUtil;
import com.parkingmanage.common.Result;
import com.parkingmanage.dto.LoginRequest;
import com.parkingmanage.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 车场人员认证控制器
 * 提供登录、登出、令牌验证等认证相关接口
 * 
 * @author parking-system
 * @version 1.0
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class ParkStaffAuthController {

    @Autowired
    private ParkStaffAuthService parkStaffAuthService;

    @Autowired
    private ParkStaffService parkStaffService;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    @Autowired
    private YardInfoMapper yardInfoMapper;

    /**
     * 验证密码
     * 使用BCrypt进行密码验证，同时兼容旧的明文密码
     * 
     * @param rawPassword 原始密码
     * @param encodedPassword 数据库中存储的密码
     * @return 验证结果
     */
    private boolean verifyPassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        
        // 检查是否是BCrypt加密的密码（BCrypt密码以$2a$开头）
        if (encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$")) {
            // 使用BCrypt验证
            return PasswordUtil.matches(rawPassword, encodedPassword);
        } else {
            // 兼容旧的明文密码（建议逐步迁移到BCrypt）
            return rawPassword.equals(encodedPassword);
        }
    }
    /**
     * 用户登录接口
     * 
     * @param loginRequest 登录请求体，包含用户名和密码
     * @param request HTTP请求对象，用于获取客户端IP
     * @return 登录结果，成功返回token和用户信息
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, 
                                     HttpServletRequest request) {
        try {
            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();
            
            // 验证用户名和密码
            ParkStaff staff = parkStaffAuthService.findByUsername(username);
            if (staff == null) {
                return Result.error("401", "用户名或密码错误");
            }
            
            // 先检查用户状态（禁用和锁定不能同时进行）
            if (staff.getStatus() != 1) {
                String disableInfo = "";
                if (staff.getDisableReason() != null && !staff.getDisableReason().isEmpty()) {
                    disableInfo = "，原因：" + staff.getDisableReason();
                }
                return Result.error("403", "账户已被禁用，请联系管理员" + disableInfo);
            }
            
            // 检查账户是否被锁定（只有在未禁用的情况下才检查锁定）
            if (staff.getLockTime() != null) {
                LocalDateTime now = LocalDateTime.now();
                long minutesLocked = ChronoUnit.MINUTES.between(staff.getLockTime(), now);
                
                if (minutesLocked < 10) {
                    // 还在锁定期内
                    long remainingMinutes = 10 - minutesLocked;
                    return Result.error("403", "账户已被锁定，请" + remainingMinutes + "分钟后再试");
                } else {
                    // 锁定时间已过，解除锁定（但不清除锁定次数和失败次数）
                    // 注意：不清除失败次数，因为失败次数应该持续累加，直到登录成功或达到5次
                    staff.setLockTime(null);
                    // 不再重置失败次数，让失败次数持续累加
                    parkStaffService.updateById(staff);
                    log.info("🔓 [登录] 账户 {} 锁定时间已过，解除锁定，当前失败次数：{}", 
                        staff.getUsername(), staff.getFailedLoginCount());
                }
            }
            
            // 验证密码
            if (!verifyPassword(password, staff.getPassword())) {
                // 密码错误，增加失败次数
                int failedCount = (staff.getFailedLoginCount() != null ? staff.getFailedLoginCount() : 0) + 1;
                staff.setFailedLoginCount(failedCount);
                
                // 如果失败次数达到5次，锁定账户10分钟
                if (failedCount == 5) {
                    // 增加锁定次数
                    int lockCount = (staff.getLockCount() != null ? staff.getLockCount() : 0) + 1;
                    staff.setLockCount(lockCount);
                    // 被锁定一次就清零失败次数
                    staff.setFailedLoginCount(0);
                    
                    // 检查锁定次数是否达到5次，如果达到则禁用账户
                    if (lockCount == 5) {
                        // 禁用账户，清除锁定状态和锁定次数
                        staff.setStatus(0);
                        staff.setLockTime(null); // 清除锁定时间（禁用和锁定不能同时进行）
                        staff.setLockCount(0); // 禁用后，锁定次数清零
                        // 设置禁用原因
                        String disableReason = String.format("账号锁定5次（累计锁定次数：%d次，本次失败次数：%d次，禁用时间：%s）", 
                            5, // 显示累计锁定5次
                            5, // 显示失败5次（锁定前的失败次数）
                            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        staff.setDisableReason(disableReason);
                        staff.setDisableTime(LocalDateTime.now());
                        parkStaffService.updateById(staff);
                        log.warn("🔒 [登录] 账户 {} 因累计锁定5次被禁用，锁定前的失败次数：5次，禁用原因：{}", 
                            staff.getUsername(), disableReason);
                        return Result.error("403", "账户累计锁定次数过多，已被禁用，请联系管理员");
                    } else {
                        // 锁定次数未达到5次，只锁定不禁用
                        staff.setLockTime(LocalDateTime.now());
                        parkStaffService.updateById(staff);
                        log.warn("🔒 [登录] 账户 {} 因连续5次密码错误被锁定（累计锁定{}次），锁定10分钟，失败次数已清零", 
                            staff.getUsername(), lockCount);
                        return Result.error("403", "密码错误次数过多，账户已被锁定10分钟，请稍后再试");
                    }
                } else {
                    // 更新失败次数
                    parkStaffService.updateById(staff);
                    int remainingAttempts = 5 - failedCount;
                    return Result.error("401", "用户名或密码输入错误，还可尝试" + remainingAttempts + "次");
                }
            }
            
            // 登录成功，重置失败次数和锁定时间
            if (staff.getFailedLoginCount() != null && staff.getFailedLoginCount() > 0) {
                staff.setFailedLoginCount(0);
                staff.setLockTime(null);
                parkStaffService.updateById(staff);
            }
            
            // 生成JWT token
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", staff.getId());
            claims.put("username", staff.getUsername());
            claims.put("parkName", staff.getParkName());
            claims.put("position", staff.getPosition());
            
            String token = jwtUtil.generateToken(claims);
            
            // 更新最后登录信息（同时重置失败次数和锁定时间）
            String clientIp = getClientIpAddress(request);
            parkStaffAuthService.updateLastLogin(staff.getId(), clientIp);
            
            // 保存token到数据库（可选，用于token管理）
            parkStaffAuthService.saveUserToken(staff.getId(), token);
            
            // 记录登录成功日志
            recordLoginLog(staff.getId(), staff.getUsername(), "车场人员登录", "success", request);
            
            // 构建返回结果
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUser(buildUserInfo(staff));
            
            return Result.success(response);
            
        } catch (Exception e) {
            // 记录登录失败日志
            recordLoginLog(null, loginRequest.getUsername(), "车场人员登录", "failed", request);
            return Result.error("500", "登录失败：" + e.getMessage());
        }
    }

    /**
     * 验证token有效性
     * 
     * @param request HTTP请求对象
     * @return 验证结果和用户信息
     */
    @GetMapping("/verify")
    public Result<Map<String, Object>> verifyToken(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token == null || token.isEmpty()) {
                return Result.error("401", "未提供认证令牌");
            }
            
            // 验证token
            if (!jwtUtil.validateToken(token)) {
                return Result.error("401", "令牌无效或已过期");
            }
            
            // 检查token是否在数据库中存在且有效
            if (!parkStaffAuthService.isTokenValid(token)) {
                return Result.error("401", "令牌已失效");
            }
            
            // 获取用户信息
            String username = jwtUtil.getUsernameFromToken(token);
            ParkStaff staff = parkStaffAuthService.findByUsername(username);
            
            if (staff == null || staff.getStatus() != 1) {
                return Result.error("401", "用户不存在或已被禁用");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("valid", true);
            result.put("user", buildUserInfo(staff));
            
            return Result.success(result);
            
        } catch (Exception e) {
            return Result.error("500", "令牌验证失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息
     * 
     * @param request HTTP请求对象
     * @return 用户信息
     */
    @GetMapping("/userInfo")
    public Result<Map<String, Object>> getCurrentUser(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                return Result.error("401", "未授权访问");
            }
            
            String username = jwtUtil.getUsernameFromToken(token);
            ParkStaff staff = parkStaffAuthService.findByUsername(username);
            
            if (staff == null) {
                return Result.error("404", "用户不存在");
            }
            
            return Result.success(buildUserInfo(staff));
            
        } catch (Exception e) {
            return Result.error("500", "获取用户信息失败：" + e.getMessage());
        }
    }

    /**
     * 用户登出接口
     * 
     * @param request HTTP请求对象
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token != null && !token.isEmpty()) {
                // 获取用户信息用于日志记录
                String username = jwtUtil.getUsernameFromToken(token);
                Integer userId = null;
                try {
                    // 从token中获取用户ID，这里需要根据实际的JwtUtil实现来调整
                    // userId = (Integer) jwtUtil.getClaimFromToken(token, "userId");
                    userId = null; // 暂时设为null，避免编译错误
                } catch (Exception e) {
                    // 忽略获取用户ID失败的情况
                }
                
                // 将token标记为无效
                parkStaffAuthService.invalidateToken(token);
                
                // 记录登出日志
                recordLoginLog(userId, username, "车场人员登出", "success", request);
            }
            
            return Result.success();
            
        } catch (Exception e) {
            return Result.error("500", "退出登录失败：" + e.getMessage());
        }
    }

    /**
     * 刷新token接口
     * 
     * @param request HTTP请求对象
     * @return 新的token
     */
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshToken(HttpServletRequest request) {
        try {
            String oldToken = extractTokenFromRequest(request);
            if (oldToken == null || !jwtUtil.validateToken(oldToken)) {
                return Result.error("401", "原token无效");
            }
            
            String username = jwtUtil.getUsernameFromToken(oldToken);
            ParkStaff staff = parkStaffAuthService.findByUsername(username);
            
            if (staff == null || staff.getStatus() != 1) {
                return Result.error("401", "用户不存在或已被禁用");
            }
            
            // 生成新token
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", staff.getId());
            claims.put("username", staff.getUsername());
            claims.put("parkName", staff.getParkName());
            claims.put("position", staff.getPosition());
            
            String newToken = jwtUtil.generateToken(claims);
            
            // 更新数据库中的token
            parkStaffAuthService.invalidateToken(oldToken);
            parkStaffAuthService.saveUserToken(staff.getId(), newToken);
            
            Map<String, Object> result = new HashMap<>();
            result.put("token", newToken);
            result.put("user", buildUserInfo(staff));
            
            return Result.success(result);
            
        } catch (Exception e) {
            return Result.error("500", "token刷新失败：" + e.getMessage());
        }
    }

    /**
     * 检查用户名是否存在
     * 
     * @param username 用户名
     * @return 检查结果
     */
    @GetMapping("/check-username")
    public Result<Map<String, Object>> checkUsername(@RequestParam String username) {
        try {
            boolean exists = parkStaffAuthService.findByUsername(username) != null;
            
            Map<String, Object> result = new HashMap<>();
            result.put("exists", exists);
            result.put("available", !exists);
            
            return Result.success(result);
            
        } catch (Exception e) {
            return Result.error("500", "用户名检查失败：" + e.getMessage());
        }
    }

    /**
     * 从请求中提取token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        // 也支持从查询参数中获取token（适用于某些特殊情况）
        return request.getParameter("token");
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 构建用户信息返回对象（敏感信息脱敏）
     */
    private Map<String, Object> buildUserInfo(ParkStaff staff) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", staff.getId());
        userInfo.put("username", staff.getUsername());
        userInfo.put("realName", staff.getRealName());
        userInfo.put("parkName", staff.getParkName());
        userInfo.put("position", staff.getPosition());
        userInfo.put("phone", staff.getPhone());
        userInfo.put("email", staff.getEmail());
        userInfo.put("status", staff.getStatus());
        userInfo.put("lastLoginTime", staff.getLastLoginTime());
        
        // 🆕 根据车场名称查询车场ID
        try {
            if (staff.getParkName() != null && !staff.getParkName().isEmpty()) {
                LambdaQueryWrapper<YardInfo> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(YardInfo::getYardName, staff.getParkName())
                       .eq(YardInfo::getDeleted, 0);
                YardInfo yardInfo = yardInfoMapper.selectOne(wrapper);
                
                if (yardInfo != null && yardInfo.getId() != null) {
                    userInfo.put("yardId", yardInfo.getId());
                    log.info("📋 [登录] 用户 {} 的车场 {} 对应的ID: {}", 
                            staff.getUsername(), staff.getParkName(), yardInfo.getId());
                } else {
                    log.warn("⚠️ [登录] 未找到车场 {} 的信息", staff.getParkName());
                    userInfo.put("yardId", null);
                }
            } else {
                log.warn("⚠️ [登录] 用户 {} 没有关联车场", staff.getUsername());
                userInfo.put("yardId", null);
            }
        } catch (Exception e) {
            log.error("❌ [登录] 查询车场ID失败: {}", e.getMessage(), e);
            userInfo.put("yardId", null);
        }
        
        // 注意：不返回密码等敏感信息
        return userInfo;
    }

    /**
     * 记录登录日志
     */
    private void recordLoginLog(Integer userId, String username, String action, String status, HttpServletRequest request) {
        try {
            ActivityLog log = new ActivityLog();
            log.setUserId(userId != null ? userId.toString() : "unknown");
            log.setUsername(username);
            log.setModule("车场人员认证");
            log.setAction(action);
            log.setDescription("车场人员登录/登出操作");
            log.setStatus(status);
            log.setCreatedAt(LocalDateTime.now());
            log.setIpAddress(getClientIpAddress(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            
            activityLogService.save(log);
        } catch (Exception e) {
            // 记录日志失败不影响主业务
            log.warn("记录登录日志失败", e);
        }
    }
} 