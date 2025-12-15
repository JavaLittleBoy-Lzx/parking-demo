package com.parkingmanage.controller;

import com.parkingmanage.util.PasswordUtil;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.ParkStaff;
import com.parkingmanage.service.ParkStaffService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试控制器
 * 用于测试密码加密等功能
 * 
 * @author parking-system
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    @Resource
    private ParkStaffService parkStaffService;

    /**
     * 测试密码加密
     * 
     * @param password 原始密码
     * @return 加密结果
     */
    @GetMapping("/encrypt-password")
    public Result<Map<String, String>> testPasswordEncryption(@RequestParam String password) {
        try {
            String encryptedPassword = PasswordUtil.encodePassword(password);
            boolean matches = PasswordUtil.matches(password, encryptedPassword);
            
            Map<String, String> result = new HashMap<>();
            result.put("originalPassword", password);
            result.put("encryptedPassword", encryptedPassword);
            result.put("verificationResult", String.valueOf(matches));
            
            return Result.success(result);
            
        } catch (Exception e) {
            return Result.error("500", "密码加密测试失败：" + e.getMessage());
        }
    }
    
    /**
     * 测试密码验证
     * 
     * @param rawPassword 原始密码
     * @param encryptedPassword 加密密码
     * @return 验证结果
     */
    @GetMapping("/verify-password")
    public Result<Map<String, Object>> testPasswordVerification(
            @RequestParam String rawPassword,
            @RequestParam String encryptedPassword) {
        try {
            boolean matches = PasswordUtil.matches(rawPassword, encryptedPassword);
            
            Map<String, Object> result = new HashMap<>();
            result.put("rawPassword", rawPassword);
            result.put("encryptedPassword", encryptedPassword);
            result.put("matches", matches);
            
            return Result.success(result);
            
        } catch (Exception e) {
            return Result.error("500", "密码验证测试失败：" + e.getMessage());
        }
    }
    
    /**
     * 迁移数据库中的明文密码为加密密码
     * 主要用于将默认密码123456和其他明文密码加密
     * 
     * @return 迁移结果
     */
    @PostMapping("/migrate-passwords")
    public Result<Map<String, Object>> migratePasswords() {
        try {
            // 获取所有人员
            List<ParkStaff> allStaff = parkStaffService.list();
            
            int totalCount = 0;
            int migratedCount = 0;
            int errorCount = 0;
            
            for (ParkStaff staff : allStaff) {
                totalCount++;
                
                String currentPassword = staff.getPassword();
                
                // 跳过已经加密的密码（以$2a$开头）
                if (currentPassword != null && 
                    (currentPassword.startsWith("$2a$") || 
                     currentPassword.startsWith("$2b$") || 
                     currentPassword.startsWith("$2y$"))) {
                    continue;
                }
                
                try {
                    // 如果密码为空或null，设置默认密码
                    if (currentPassword == null || currentPassword.trim().isEmpty()) {
                        currentPassword = "123456";
                    }
                    
                    // 加密密码
                    String encryptedPassword = PasswordUtil.encodePassword(currentPassword);
                    
                    // 更新数据库
                    staff.setPassword(encryptedPassword);
                    boolean success = parkStaffService.updateById(staff);
                    
                    if (success) {
                        migratedCount++;
                    } else {
                        errorCount++;
                    }
                    
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("迁移用户 " + staff.getUsername() + " 密码失败: " + e.getMessage());
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", totalCount);
            result.put("migratedCount", migratedCount);
            result.put("errorCount", errorCount);
            result.put("alreadyEncryptedCount", totalCount - migratedCount - errorCount);
            result.put("message", String.format("密码迁移完成！总数: %d, 已迁移: %d, 错误: %d, 已加密: %d", 
                totalCount, migratedCount, errorCount, totalCount - migratedCount - errorCount));
            
            return Result.success(result);
            
        } catch (Exception e) {
            return Result.error("500", "密码迁移失败：" + e.getMessage());
        }
    }
} 