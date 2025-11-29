package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.entity.ActivityLog;
import com.parkingmanage.entity.User;
import com.parkingmanage.service.ActivityLogService;
import com.parkingmanage.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 个人中心控制器
 */
@Slf4j
@RestController
@RequestMapping("/parking/personal")
@CrossOrigin
@Api(tags = "个人中心管理")
public class PersonalCenterController {

    @Resource
    private UserService userService;

    @Resource
    private ActivityLogService activityLogService;
    
    // 📁 从配置文件读取上传路径（与 FileUploadController 保持一致）
    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @ApiOperation("获取个人信息")
    @GetMapping("/profile/{userId}")
    public ResponseEntity<Result> getProfile(@PathVariable Integer userId) {
        try {
            User user = userService.getById(userId);
            if (user == null) {
                return ResponseEntity.ok(Result.error("用户不存在"));
            }
            
            // 隐藏敏感信息
            user.setPassword(null);
            user.setToken(null);
            
            return ResponseEntity.ok(Result.success(user));
        } catch (Exception e) {
            log.error("获取个人信息失败", e);
            return ResponseEntity.ok(Result.error("获取个人信息失败"));
        }
    }

    @ApiOperation("更新个人信息")
    @PutMapping("/profile")
    public ResponseEntity<Result> updateProfile(@RequestBody User user, HttpServletRequest request) {
        try {
            // 获取更新前的用户信息，用于记录详细的修改内容
            User oldUser = userService.getById(user.getUserId());
            if (oldUser == null) {
                return ResponseEntity.ok(Result.error("用户不存在"));
            }
            
            // 获取登录名
            String loginName = oldUser.getLoginName() != null ? oldUser.getLoginName() : oldUser.getUserName();
            
            // 构建详细的修改描述
            StringBuilder changeDetails = new StringBuilder();
            boolean hasChanges = false;
            
            if (user.getUserName() != null && !user.getUserName().equals(oldUser.getUserName())) {
                changeDetails.append("姓名从\"").append(oldUser.getUserName())
                           .append("\"改为\"").append(user.getUserName()).append("\"；");
                hasChanges = true;
            }
            if (user.getEmail() != null && !user.getEmail().equals(oldUser.getEmail())) {
                changeDetails.append("邮箱从\"").append(oldUser.getEmail())
                           .append("\"改为\"").append(user.getEmail()).append("\"；");
                hasChanges = true;
            }
            if (user.getTelephone() != null && !user.getTelephone().equals(oldUser.getTelephone())) {
                changeDetails.append("电话从\"").append(oldUser.getTelephone())
                           .append("\"改为\"").append(user.getTelephone()).append("\"；");
                hasChanges = true;
            }
            
            // 构建完整的日志描述
            String description;
            if (hasChanges) {
                description = "用户 " + loginName + " 更新了个人信息：" + changeDetails.toString();
            } else {
                description = "用户 " + loginName + " 更新了个人信息（无字段变更）";
            }
            
            // 记录操作日志（使用登录名 loginName）
            recordUserOperation(user.getUserId(), loginName, "个人中心", "更新个人信息", 
                              description, request);
            
            // 不允许通过此接口更新敏感字段
            user.setPassword(null);
            user.setLoginName(null);
            user.setDepartmentId(null);
            user.setRoleId(null);
            user.setStatus(null);
            user.setLoginCount(null);
            user.setLastLoginTime(null);
            user.setLastLoginIp(null);
            
            boolean success = userService.updateById(user);
            if (success) {
                return ResponseEntity.ok(Result.success("更新成功"));
            } else {
                return ResponseEntity.ok(Result.error("更新失败"));
            }
        } catch (Exception e) {
            log.error("更新个人信息失败", e);
            return ResponseEntity.ok(Result.error("更新个人信息失败"));
        }
    }

    @ApiOperation("修改密码")
    @PutMapping("/change-password")
    public ResponseEntity<Result> changePassword(@RequestBody Map<String, String> params, HttpServletRequest request) {
        try {
            Integer userId = Integer.parseInt(params.get("userId"));
            String oldPassword = params.get("oldPassword");
            String newPassword = params.get("newPassword");
            
            User user = userService.getById(userId);
            if (user == null) {
                return ResponseEntity.ok(Result.error("用户不存在"));
            }
            
            // 验证旧密码
            if (!userService.verifyPassword(user, oldPassword)) {
                return ResponseEntity.ok(Result.error("旧密码错误"));
            }
            
            // 更新密码
            user.setPassword(newPassword);
            boolean success = userService.updateById(user);
            
            if (success) {
                // 使用登录名 loginName
                String loginName = user.getLoginName() != null ? user.getLoginName() : user.getUserName();
                recordUserOperation(userId, loginName, "个人中心", "修改密码", 
                                  "用户 " + loginName + " 修改了登录密码", request);
                return ResponseEntity.ok(Result.success("密码修改成功"));
            } else {
                return ResponseEntity.ok(Result.error("密码修改失败"));
            }
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return ResponseEntity.ok(Result.error("修改密码失败"));
        }
    }

    @ApiOperation("上传头像")
    @PostMapping("/upload-avatar")
    public ResponseEntity<Result> uploadAvatar(@RequestParam("file") MultipartFile file,
                                             @RequestParam("userId") String userId,
                                             HttpServletRequest request) {
        try {
            log.info("📸 [头像上传] 开始处理 - userId: {}, 文件名: {}, 文件大小: {} bytes", 
                    userId, file.getOriginalFilename(), file.getSize());
            
            // 验证文件
            if (file.isEmpty()) {
                log.warn("❌ [头像上传] 文件为空");
                return ResponseEntity.ok(Result.error("文件不能为空"));
            }
            
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && 
                !contentType.equals("image/png") && !contentType.equals("image/jpg"))) {
                log.warn("❌ [头像上传] 不支持的文件类型: {}", contentType);
                return ResponseEntity.ok(Result.error("只支持 JPG、PNG 格式的图片"));
            }
            
            // 验证文件大小（2MB）
            if (file.getSize() > 2 * 1024 * 1024) {
                log.warn("❌ [头像上传] 文件过大: {} bytes", file.getSize());
                return ResponseEntity.ok(Result.error("文件大小不能超过 2MB"));
            }
            
            // 生成文件名和路径
            String timestamp = String.valueOf(System.currentTimeMillis());
            String extension = ".jpg"; // 统一使用jpg扩展名
            String fileName = "avatar_" + userId + "_" + timestamp + extension;
            
            // 🔧 使用绝对路径（项目根目录 + uploads）
            String projectRoot = System.getProperty("user.dir");
            String uploadsRoot = projectRoot + File.separator + "uploads";
            String avatarDir = uploadsRoot + File.separator + "avatars";
            
            // 创建目录（如果不存在）
            File directory = new File(avatarDir);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                log.info("📁 [头像上传] 创建目录: {}, 结果: {}", avatarDir, created);
            }
            
            // 完整文件路径
            String filePath = avatarDir + File.separator + fileName;
            File destFile = new File(filePath);
            
            // 保存文件到磁盘
            file.transferTo(destFile);
            
            log.info("💾 [头像上传] 文件已保存到: {}", destFile.getAbsolutePath());
            log.info("📂 [头像上传] 项目根目录: {}", projectRoot);
            log.info("📂 [头像上传] uploads目录: {}", uploadsRoot);
            
            // 生成访问URL（使用相对路径，统一使用正斜杠）
            String avatarUrl = "/uploads/avatars/" + fileName;
            
            log.info("✅ [头像上传] 生成头像URL: {}", avatarUrl);
            
            // 更新用户头像URL
            Integer userIdInt = Integer.parseInt(userId);
            User user = userService.getById(userIdInt);
            
            if (user == null) {
                log.warn("❌ [头像上传] 用户不存在: {}", userId);
                return ResponseEntity.ok(Result.error("用户不存在"));
            }
            
            user.setAvatar(avatarUrl);
            boolean success = userService.updateById(user);
            
            if (success) {
                // 记录操作日志（使用登录名 loginName 而不是 userName）
                String loginName = user.getLoginName() != null ? user.getLoginName() : user.getUserName();
                String description = String.format("用户 %s 上传了新的头像（文件名：%s，大小：%.2fKB）", 
                                                  loginName, fileName, file.getSize() / 1024.0);
                recordUserOperation(userIdInt, loginName, "个人中心", "上传头像", description, request);
                
                Map<String, Object> result = new HashMap<>();
                result.put("avatarUrl", avatarUrl);
                result.put("url", avatarUrl);
                
                log.info("🎉 [头像上传] 上传成功 - loginName: {}, avatarUrl: {}", loginName, avatarUrl);
                return ResponseEntity.ok(Result.success(result));
            } else {
                log.error("❌ [头像上传] 更新用户头像失败");
                return ResponseEntity.ok(Result.error("更新用户头像失败"));
            }
        } catch (NumberFormatException e) {
            log.error("❌ [头像上传] userId格式错误: {}", userId, e);
            return ResponseEntity.ok(Result.error("用户ID格式错误"));
        } catch (Exception e) {
            log.error("❌ [头像上传] 上传失败", e);
            return ResponseEntity.ok(Result.error("头像上传失败: " + e.getMessage()));
        }
    }

    @ApiOperation("获取用户操作记录")
    @GetMapping("/operation-logs/{userId}")
    public ResponseEntity<Result> getOperationLogs(@PathVariable Integer userId,
                                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            List<ActivityLog> logs = activityLogService.getUserOperationLogs(userId, pageNum, pageSize);
            return ResponseEntity.ok(Result.success(logs));
        } catch (Exception e) {
            log.error("获取操作记录失败", e);
            return ResponseEntity.ok(Result.error("获取操作记录失败"));
        }
    }

    @ApiOperation("获取用户统计信息")
    @GetMapping("/statistics/{userId}")
    public ResponseEntity<Result> getUserStatistics(@PathVariable Integer userId) {
        try {
            User user = userService.getById(userId);
            if (user == null) {
                return ResponseEntity.ok(Result.error("用户不存在"));
            }
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("loginCount", user.getLoginCount() != null ? user.getLoginCount() : 0);
            statistics.put("lastLoginTime", user.getLastLoginTime());
            statistics.put("createdTime", user.getCreatedTime());
            
            // 获取操作记录统计
            long totalOperations = activityLogService.getUserOperationCount(userId);
            statistics.put("totalOperations", totalOperations);
            
            return ResponseEntity.ok(Result.success(statistics));
        } catch (Exception e) {
            log.error("获取用户统计信息失败", e);
            return ResponseEntity.ok(Result.error("获取用户统计信息失败"));
        }
    }

    /**
     * 记录用户操作日志
     * @param userId 用户ID
     * @param username 用户名
     * @param module 模块名
     * @param action 操作类型
     * @param description 详细描述
     * @param request HTTP请求
     */
    private void recordUserOperation(Integer userId, String username, String module, String action, String description, HttpServletRequest request) {
        try {
            ActivityLog activityLog = new ActivityLog();
            activityLog.setUserId(userId != null ? userId.toString() : "unknown");
            activityLog.setUsername(username != null ? username : "未知用户");
            activityLog.setModule(module);
            activityLog.setAction(action);
            activityLog.setDescription(description);
            activityLog.setStatus("success");
            activityLog.setCreatedAt(LocalDateTime.now());
            activityLog.setIpAddress(getClientIpAddress(request));
            activityLog.setUserAgent(request.getHeader("User-Agent"));
            
            activityLogService.save(activityLog);
            
            log.info("📝 [操作日志] 用户：{}，模块：{}，操作：{}，描述：{}", username, module, action, description);
        } catch (Exception e) {
            // 记录日志失败不影响主业务
            log.warn("记录用户操作日志失败", e);
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // 优先从代理服务器转发的header中获取
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }
        
        String proxyClientIp = request.getHeader("Proxy-Client-IP");
        if (proxyClientIp != null && !proxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(proxyClientIp)) {
            return proxyClientIp.trim();
        }
        
        String wlProxyClientIp = request.getHeader("WL-Proxy-Client-IP");
        if (wlProxyClientIp != null && !wlProxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(wlProxyClientIp)) {
            return wlProxyClientIp.trim();
        }
        
        // 获取远程地址
        String remoteAddr = request.getRemoteAddr();
        
        // 如果是IPv6的www.xuerparking.cn地址，转换为IPv4的www.xuerparking.cn
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            remoteAddr = "127.0.0.1";
        }
        
        return remoteAddr;
    }
    
    /**
     * 🔍 测试端点：检查上传目录和文件
     */
    @ApiOperation("检查上传目录和文件")
    @GetMapping("/check-uploads")
    public ResponseEntity<Result> checkUploads() {
        Map<String, Object> info = new HashMap<>();
        
        // 项目根目录
        String projectRoot = System.getProperty("user.dir");
        info.put("projectRoot", projectRoot);
        
        // uploads目录
        String uploadsPath = projectRoot + java.io.File.separator + "uploads";
        java.io.File uploadsDir = new java.io.File(uploadsPath);
        info.put("uploadsPath", uploadsPath);
        info.put("uploadsExists", uploadsDir.exists());
        info.put("uploadsIsDirectory", uploadsDir.isDirectory());
        
        // avatars目录
        String avatarsPath = uploadsPath + java.io.File.separator + "avatars";
        java.io.File avatarsDir = new java.io.File(avatarsPath);
        info.put("avatarsPath", avatarsPath);
        info.put("avatarsExists", avatarsDir.exists());
        info.put("avatarsIsDirectory", avatarsDir.isDirectory());
        
        // 列出avatars目录中的文件
        if (avatarsDir.exists() && avatarsDir.isDirectory()) {
            java.io.File[] files = avatarsDir.listFiles();
            if (files != null) {
                java.util.List<Map<String, Object>> fileList = new java.util.ArrayList<>();
                for (java.io.File file : files) {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", file.getName());
                    fileInfo.put("size", file.length());
                    fileInfo.put("canRead", file.canRead());
                    fileInfo.put("path", file.getAbsolutePath());
                    fileList.add(fileInfo);
                }
                info.put("files", fileList);
                info.put("fileCount", files.length);
            }
        }
        
        log.info("🔍 [检查上传目录] {}", info);
        return ResponseEntity.ok(Result.success(info));
    }
}
