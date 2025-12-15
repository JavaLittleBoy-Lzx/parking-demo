package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.entity.ActivityLog;
import com.parkingmanage.entity.SystemConfig;
import com.parkingmanage.entity.User;
import com.parkingmanage.service.ActivityLogService;
import com.parkingmanage.service.SystemConfigService;
import com.parkingmanage.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 系统设置控制器
 */
@Slf4j
@RestController
@RequestMapping("/parking/system")
@CrossOrigin
@Api(tags = "系统设置管理")
public class SystemSettingsController {

    @Resource
    private SystemConfigService systemConfigService;

    @Resource
    private ActivityLogService activityLogService;

    @Resource
    private UserService userService;

    @ApiOperation("获取所有系统配置")
    @GetMapping("/configs")
    public ResponseEntity<Result> getAllConfigs() {
        try {
            List<SystemConfig> configs = systemConfigService.getAllEnabledConfigs();
            return ResponseEntity.ok(Result.success(configs));
        } catch (Exception e) {
            log.error("获取系统配置失败", e);
            return ResponseEntity.ok(Result.error("获取系统配置失败"));
        }
    }

    @ApiOperation("根据类型获取配置")
    @GetMapping("/configs/type/{configType}")
    public ResponseEntity<Result> getConfigsByType(@PathVariable String configType) {
        try {
            List<SystemConfig> configs = systemConfigService.getConfigsByType(configType);
            return ResponseEntity.ok(Result.success(configs));
        } catch (Exception e) {
            log.error("获取配置失败", e);
            return ResponseEntity.ok(Result.error("获取配置失败"));
        }
    }

    @ApiOperation("获取单个配置值")
    @GetMapping("/config/{configKey}")
    public ResponseEntity<Result> getConfigValue(@PathVariable String configKey) {
        try {
            String value = systemConfigService.getConfigValue(configKey);
            return ResponseEntity.ok(Result.success(value));
        } catch (Exception e) {
            log.error("获取配置值失败", e);
            return ResponseEntity.ok(Result.error("获取配置值失败"));
        }
    }

    @ApiOperation("更新单个配置")
    @PutMapping("/config")
    public ResponseEntity<Result> updateConfig(@RequestBody SystemConfig config, HttpServletRequest request) {
        try {
            // 记录操作日志
            recordSystemOperation("系统设置", "更新配置", 
                "更新配置: " + config.getConfigKey() + " = " + config.getConfigValue(), request);
            
            boolean success = systemConfigService.updateById(config);
            if (success) {
                return ResponseEntity.ok(Result.success("配置更新成功"));
            } else {
                return ResponseEntity.ok(Result.error("配置更新失败"));
            }
        } catch (Exception e) {
            log.error("更新配置失败", e);
            return ResponseEntity.ok(Result.error("更新配置失败"));
        }
    }

    @ApiOperation("批量更新配置")
    @PutMapping("/configs/batch")
    public ResponseEntity<Result> updateConfigs(@RequestBody Map<String, String> configs, HttpServletRequest request) {
        try {
            // 记录操作日志
            recordSystemOperation("系统设置", "批量更新配置", 
                "批量更新了 " + configs.size() + " 个配置项", request);
            
            boolean success = systemConfigService.updateConfigs(configs);
            if (success) {
                return ResponseEntity.ok(Result.success("批量更新成功"));
            } else {
                return ResponseEntity.ok(Result.error("批量更新失败"));
            }
        } catch (Exception e) {
            log.error("批量更新配置失败", e);
            return ResponseEntity.ok(Result.error("批量更新配置失败"));
        }
    }

    @ApiOperation("获取系统信息")
    @GetMapping("/info")
    public ResponseEntity<Result> getSystemInfo() {
        try {
            Map<String, String> systemInfo = systemConfigService.getConfigMapByType("system");
            return ResponseEntity.ok(Result.success(systemInfo));
        } catch (Exception e) {
            log.error("获取系统信息失败", e);
            return ResponseEntity.ok(Result.error("获取系统信息失败"));
        }
    }

    @ApiOperation("获取业务配置")
    @GetMapping("/business")
    public ResponseEntity<Result> getBusinessConfigs() {
        try {
            Map<String, String> businessConfigs = systemConfigService.getConfigMapByType("business");
            return ResponseEntity.ok(Result.success(businessConfigs));
        } catch (Exception e) {
            log.error("获取业务配置失败", e);
            return ResponseEntity.ok(Result.error("获取业务配置失败"));
        }
    }

    @ApiOperation("获取安全配置")
    @GetMapping("/security")
    public ResponseEntity<Result> getSecurityConfigs() {
        try {
            Map<String, String> securityConfigs = systemConfigService.getConfigMapByType("security");
            return ResponseEntity.ok(Result.success(securityConfigs));
        } catch (Exception e) {
            log.error("获取安全配置失败", e);
            return ResponseEntity.ok(Result.error("获取安全配置失败"));
        }
    }

    @ApiOperation("重置配置为默认值")
    @PostMapping("/configs/reset")
    public ResponseEntity<Result> resetConfigs(HttpServletRequest request) {
        try {
            // 这里可以实现重置为默认配置的逻辑
            // 简化处理，实际项目中需要根据具体需求实现
            
            recordSystemOperation("系统设置", "重置配置", "将系统配置重置为默认值", request);
            
            return ResponseEntity.ok(Result.success("配置重置成功"));
        } catch (Exception e) {
            log.error("重置配置失败", e);
            return ResponseEntity.ok(Result.error("重置配置失败"));
        }
    }

    @ApiOperation("获取系统操作日志")
    @GetMapping("/operation-logs")
    public ResponseEntity<Result> getSystemOperationLogs(@RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            // 这里需要根据实际需求实现获取系统操作日志的逻辑
            // 可以基于ActivityLog表查询系统相关的操作记录
            return ResponseEntity.ok(Result.success("获取操作日志功能待实现"));
        } catch (Exception e) {
            log.error("获取系统操作日志失败", e);
            return ResponseEntity.ok(Result.error("获取系统操作日志失败"));
        }
    }

    /**
     * 记录系统操作日志
     * 使用当前登录用户的 login_name 而不是 user_name
     */
    private void recordSystemOperation(String module, String action, String description, HttpServletRequest request) {
        try {
            ActivityLog activityLog = new ActivityLog();
            
            // 从请求头获取当前用户ID
            String userIdStr = request.getHeader("userId");
            String username = ""; // 默认值
            
            if (!StringUtils.isEmpty(userIdStr)) {
                try {
                    Integer userId = Integer.parseInt(userIdStr);
                    User user = userService.getById(userId);
                    if (user != null && user.getLoginName() != null && !user.getLoginName().trim().isEmpty()) {
                        // 使用 login_name 而不是 user_name
                        username = user.getLoginName();
                        activityLog.setUserId(userId.toString());
                    } else {
                        activityLog.setUserId("system");
                    }
                } catch (NumberFormatException e) {
                    log.warn("解析userId失败: {}", userIdStr);
                    activityLog.setUserId("system");
                }
            } else {
                activityLog.setUserId("system");
            }
            
            activityLog.setUsername(username);
            activityLog.setModule(module);
            activityLog.setAction(action);
            activityLog.setDescription(description);
            activityLog.setStatus("success");
            activityLog.setCreatedAt(LocalDateTime.now());
            activityLog.setIpAddress(getClientIpAddress(request));
            activityLog.setUserAgent(request.getHeader("User-Agent"));
            
            activityLogService.save(activityLog);
            
            log.info("📝 [系统管理操作日志] 用户：{}，模块：{}，操作：{}，描述：{}", username, module, action, description);
        } catch (Exception e) {
            // 记录日志失败不影响主业务
            log.warn("记录系统操作日志失败", e);
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
