package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Department;
import com.parkingmanage.entity.Role;
import com.parkingmanage.entity.User;
import com.parkingmanage.service.DepartmentService;
import com.parkingmanage.service.RoleService;
import com.parkingmanage.service.UserService;
import com.parkingmanage.service.ActivityLogService;
import com.parkingmanage.entity.ActivityLog;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 <p>
 前端控制器
 </p>

 @author yuli
 * @since 2022-02-27
 */
@Slf4j
@RestController
@RequestMapping("/parking/user")
@CrossOrigin
@Api(tags = "用户管理")
public class UserController {
    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;
    @Resource
    private DepartmentService departmentService;
    
    @Resource
    private ActivityLogService activityLogService;
    
    @Resource
    private com.parkingmanage.service.VehicleReservationService vehicleReservationService;

    @ApiOperation("新增用户")
    @PostMapping
    public ResponseEntity<Result> saveUser(@RequestBody User user, HttpServletRequest request) {
        try {
            userService.save(user);
            // 记录新增用户日志 - 使用当前登录管理员的ID，而不是新创建用户的ID
            Integer currentUserId = getCurrentUserId(request);
            User currentUser = userService.getById(currentUserId);
            // 只使用 login_name，不使用 user_name
            String adminLoginName = currentUser != null && currentUser.getLoginName() != null 
                                  ? currentUser.getLoginName() 
                                  : "管理员";
            String newUserLoginName = user.getLoginName() != null ? user.getLoginName() : "新用户";
            String description = "管理员 " + adminLoginName + " 新增了用户：" + newUserLoginName;
            recordUserOperation(currentUserId, adminLoginName, "用户管理", "新增用户", description, request);
            return ResponseEntity.ok(Result.success());
        } catch (Exception e) {
            log.error("新增用户失败", e);
            return ResponseEntity.ok(Result.error("新增用户失败"));
        }
    }
    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> updateUser(@RequestBody User user, HttpServletRequest request) {
        try {
            // 获取更新前的用户信息用于日志记录
            User oldUser = userService.getById(user.getUserId());
            if (oldUser == null) {
                return ResponseEntity.ok(Result.error("用户不存在"));
            }
            
            // 获取当前登录用户ID
            Integer currentUserId = getCurrentUserId(request);
            
            // 判断是用户自己修改还是管理员修改
            boolean isSelfUpdate = currentUserId != null && currentUserId.equals(user.getUserId());
            
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
            
            // 执行更新
            userService.updateById(user);
            
            // 记录日志 - 只使用 login_name，不使用 user_name
            String loginName = oldUser.getLoginName() != null ? oldUser.getLoginName() : "未知用户";
            String module = isSelfUpdate ? "个人中心" : "用户管理";
            String action = isSelfUpdate ? "更新个人信息" : "修改用户";
            
            String description;
            String operatorName; // 操作者的名字（使用 login_name）
            
            if (isSelfUpdate) {
                // 用户自己修改
                operatorName = loginName;
                if (hasChanges) {
                    description = "用户 " + loginName + " 更新了个人信息：" + changeDetails.toString();
                } else {
                    description = "用户 " + loginName + " 更新了个人信息（无字段变更）";
                }
            } else {
                // 管理员修改其他用户
                User currentUser = userService.getById(currentUserId);
                String adminLoginName = currentUser != null && currentUser.getLoginName() != null 
                                      ? currentUser.getLoginName() 
                                      : "管理员";
                operatorName = adminLoginName;
                if (hasChanges) {
                    description = "管理员 " + adminLoginName + " 修改了用户 " + loginName + " 的信息：" + changeDetails.toString();
                } else {
                    description = "管理员 " + adminLoginName + " 修改了用户 " + loginName + " 的信息（无字段变更）";
                }
            }
            
            // 记录操作日志（传递操作者的名字）
            recordUserOperation(currentUserId, operatorName, module, action, description, request);
            
            return ResponseEntity.ok(Result.success());
        } catch (Exception e) {
            log.error("修改用户失败", e);
            return ResponseEntity.ok(Result.error("修改用户失败"));
        }
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public ResponseEntity<Result> delete(@PathVariable Integer id, HttpServletRequest request) {
        try {
            // 获取删除前的用户信息用于日志记录
            User user = userService.getById(id);
            boolean success = userService.removeById(id);
            if (success) {
                // 记录删除用户日志 - 使用当前登录管理员的ID，只使用 login_name
                Integer currentUserId = getCurrentUserId(request);
                User currentUser = userService.getById(currentUserId);
                String adminLoginName = currentUser != null && currentUser.getLoginName() != null 
                                      ? currentUser.getLoginName() 
                                      : "管理员";
                String deletedUserLoginName = user != null && user.getLoginName() != null 
                                            ? user.getLoginName() 
                                            : "ID:" + id;
                String description = "管理员 " + adminLoginName + " 删除了用户：" + deletedUserLoginName;
                recordUserOperation(currentUserId, adminLoginName, "用户管理", "删除用户", description, request);
                return ResponseEntity.ok(Result.success());
            } else {
                return ResponseEntity.ok(Result.error("删除失败"));
            }
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return ResponseEntity.ok(Result.error("删除用户失败"));
        }
    }

    @ApiOperation("查询单条")
    @GetMapping("/{id}")
    public User findById(@PathVariable Integer id) {
        return userService.getById(id);
    }

    @ApiOperation("获取当前用户信息")
    @GetMapping("/current")
    public ResponseEntity<Result> getCurrentUser(HttpServletRequest request) { 
        try {
            // 从请求头获取用户ID
            String userIdStr = request.getHeader("userId");
            if (StringUtils.isEmpty(userIdStr)) {
                return ResponseEntity.ok(Result.error("用户未登录"));
            }
            
            Integer userId = Integer.parseInt(userIdStr);
            User user = userService.getById(userId);
            if (user == null) {
                return ResponseEntity.ok(Result.error("用户不存在"));
            }
            
            // 获取部门信息
            if (user.getDepartmentId() != null) {
                Department department = departmentService.getById(user.getDepartmentId());
                if (ObjectUtils.isNotEmpty(department)) {
                    user.setDepartmentName(department.getDepartmentName());
                }
            }
            
            // 获取角色信息（包含权限数据）
            if (user.getRoleId() != null) {
                Role role = roleService.getById(user.getRoleId());
                if (ObjectUtils.isNotEmpty(role)) {
                    user.setRoleName(role.getName());
                    // 🆕 将角色的完整信息（包括PERMISSION字段）封装到响应中
                    // 前端可以通过 userData.roles[0].permission 获取权限JSON
                    List<Role> roles = new ArrayList<>();
                    roles.add(role);
                    user.setRoles(roles);
                    
                    log.info("用户 {} 的角色信息: ID={}, Name={}, Permission={}", 
                            user.getLoginName(), role.getId(), role.getName(),
                            role.getPermission() != null ? role.getPermission().substring(0, Math.min(50, role.getPermission().length())) + "..." : "null");
                }
            }
            
            return ResponseEntity.ok(Result.success(user));
        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
            return ResponseEntity.ok(Result.error("获取用户信息失败"));
        }
    }

    @ApiOperation("查询所有")
    @GetMapping("/listAll")
    public List<User> findAll() {
        return userService.list();
    }

    @ApiOperation("登录")
    @GetMapping("/login")
    public ResponseEntity<Result> login(@RequestParam(value = "username") String loginName,
                                        @RequestParam(value = "password") String password,
                                        HttpServletRequest request) {
        try {
            User user = userService.login(loginName, password);
            
            // 🔄 更新用户登录信息（最后登录时间、IP、登录次数，重置失败次数）
            try {
                String clientIp = getClientIpAddress(request);
                Integer loginCount = user.getLoginCount() != null ? user.getLoginCount() + 1 : 1;
                
                User updateUser = new User();
                updateUser.setUserId(user.getUserId());
                updateUser.setLastLoginTime(LocalDateTime.now());
                updateUser.setLastLoginIp(clientIp);
                updateUser.setLoginCount(loginCount);
                // 登录成功，重置失败次数和锁定时间
                updateUser.setFailedLoginCount(0);
                updateUser.setLockTime(null);
                
                userService.updateById(updateUser);
                
                // 更新返回对象的登录信息
                user.setLastLoginTime(updateUser.getLastLoginTime());
                user.setLastLoginIp(clientIp);
                user.setLoginCount(loginCount);
                
                log.info("✅ [登录] 用户：{}，IP：{}，登录次数：{}", loginName, clientIp, loginCount);
            } catch (Exception updateEx) {
                // 更新登录信息失败不影响登录流程
                log.warn("⚠️ 更新用户登录信息失败：{}", updateEx.getMessage());
            }
            
            // 获取部门信息
            if (user.getDepartmentId() != null) {
                Department department = departmentService.getById(user.getDepartmentId());
                if (ObjectUtils.isNotEmpty(department)) {
                    user.setDepartmentName(department.getDepartmentName());
                }
            }
            
            // 获取角色信息
            if (user.getRoleId() != null) {
                Role role = roleService.getById(user.getRoleId());
                if (ObjectUtils.isNotEmpty(role)) {
                    user.setRoleName(role.getName());
                }
            }
            
            // 记录登录日志 - 使用 user 对象的 login_name 确保正确
            String userLoginName = user.getLoginName() != null ? user.getLoginName() : loginName;
            recordLoginLog(user.getUserId(), userLoginName, "用户登录", "success", request);
            
            return ResponseEntity.ok(Result.success(user));
        } catch (Exception e) {
            // 记录登录失败日志
            recordLoginLog(null, loginName, "用户登录", "failed", request);
            throw e;
        }
    }

    @ApiOperation("退出登录")
    @PostMapping("/logout")
    public ResponseEntity<Result> logout(HttpServletRequest request) {
        try {
            // 从请求头获取用户ID
            String userIdStr = request.getHeader("userId");
            if (StringUtils.isEmpty(userIdStr)) {
                return ResponseEntity.ok(Result.error("用户未登录"));
            }
            
            Integer userId = Integer.parseInt(userIdStr);
            User user = userService.getById(userId);
            if (user == null) {
                return ResponseEntity.ok(Result.error("用户不存在"));
            }
            
            // 使用 login_name 记录退出日志
            String loginName = user.getLoginName() != null ? user.getLoginName() : "未知用户";
            String userName = user.getUserName() != null ? user.getUserName() : "";
            
            // 记录退出登录日志
            String description;
            if (!userName.isEmpty()) {
                description = "用户 " + userName + " (" + loginName + ") 退出系统";
            } else {
                description = "用户 " + loginName + " 退出系统";
            }
            
            recordLoginLog(userId, loginName, "用户退出", "success", request);
            
            log.info("✅ [退出登录] 用户：{}，IP：{}", loginName, getClientIpAddress(request));
            
            return ResponseEntity.ok(Result.success("退出登录成功"));
        } catch (Exception e) {
            log.error("退出登录失败", e);
            return ResponseEntity.ok(Result.error("退出登录失败：" + e.getMessage()));
        }
    }

    @ApiOperation("按照不同条件分页查询")
    @GetMapping("/page")
    public ResponseEntity<Result> getUser(@RequestParam(required = false,value = "userName") String userName,
                                          @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                          @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<User> users = userService.list(Wrappers.<User>lambdaQuery().eq(StringUtils.hasLength(userName), User::getUserName, userName));
        for (User user : users) {
            Department department = departmentService.getById(user.getDepartmentId());
            Role role = roleService.getById(user.getRoleId());
            if (ObjectUtils.isNotEmpty(department)) {
                user.setDepartmentName(department.getDepartmentName());
            }
            if (ObjectUtils.isNotEmpty(role)) {
                user.setRoleName(role.getName());
            }
        }
//        System.out.println("users = " + users);
        return ResponseEntity.ok(Result.success(PageUtils.getPage(users, pageNum, pageSize)));

    }

    /**
     * 记录用户操作日志
     * @param userId 用户ID
     * @param username 用户名（loginName）
     * @param module 模块名
     * @param action 操作类型
     * @param description 详细描述
     * @param request HTTP请求
     */
    private void recordUserOperation(Integer userId, String username, String module, String action, String description, HttpServletRequest request) {
        try {
            ActivityLog activityLog = new ActivityLog();
            activityLog.setUserId(userId != null ? userId.toString() : "system");
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
     * 记录登录日志
     * 使用 login_name 作为 username，确保日志记录正确的登录账号
     */
    private void recordLoginLog(Integer userId, String username, String action, String status, HttpServletRequest request) {
        try {
            ActivityLog activityLog = new ActivityLog();
            activityLog.setUserId(userId != null ? userId.toString() : "unknown");
            
            // 确保 username 是 login_name，如果 userId 存在，从数据库获取正确的 login_name
            String loginName = username;
            String description = "用户登录操作";
            
            if (userId != null) {
                try {
                    User user = userService.getById(userId);
                    if (user != null) {
                        // 使用数据库中的 login_name，确保正确
                        if (user.getLoginName() != null && !user.getLoginName().trim().isEmpty()) {
                            loginName = user.getLoginName();
                        }
                        
                        // 构建描述：包含 user_name 和 login_name
                        String userName = user.getUserName() != null ? user.getUserName() : "";
                        if ("用户退出".equals(action)) {
                            // 退出登录的描述
                            if (!userName.isEmpty()) {
                                description = "用户 " + userName + " (" + loginName + ") 退出系统";
                            } else {
                                description = "用户 " + loginName + " 退出系统";
                            }
                        } else {
                            // 登录的描述
                            if (!userName.isEmpty()) {
                                description = "用户 " + userName + " (" + loginName + ") 登录系统";
                            } else {
                                description = "用户 " + loginName + " 登录系统";
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("获取用户信息失败，使用传入的 username: {}", username);
                }
            }
            
            activityLog.setUsername(loginName);
            activityLog.setModule("用户认证");
            activityLog.setAction(action);
            activityLog.setDescription(description);
            activityLog.setStatus(status);
            activityLog.setCreatedAt(LocalDateTime.now());
            activityLog.setIpAddress(getClientIpAddress(request));
            activityLog.setUserAgent(request.getHeader("User-Agent"));
            
            activityLogService.save(activityLog);
            
            log.info("📝 [登录日志] 用户ID: {}, login_name: {}, 状态: {}", userId, loginName, status);
        } catch (Exception e) {
            // 记录日志失败不影响主业务
            log.warn("记录登录日志失败", e);
        }
    }

    /**
     * 获取当前登录用户ID（从请求头中获取）
     */
    private Integer getCurrentUserId(HttpServletRequest request) {
        try {
            String userIdHeader = request.getHeader("userId");
            if (userIdHeader != null && !userIdHeader.trim().isEmpty()) {
                return Integer.parseInt(userIdHeader.trim());
            }
            // 如果请求头中没有userId，返回null
            log.warn("无法从请求头获取userId");
            return null;
        } catch (NumberFormatException e) {
            log.error("解析userId失败", e);
            return null;
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
     * 获取所有车场列表
     * 从vehicle_reservation表中获取所有不重复的车场名称
     * 用于前端用户管理页面的车场选择下拉框
     */
    @ApiOperation("获取所有车场列表")
    @GetMapping("/parking/listAll")
    public ResponseEntity<Result> listAllParks() {
        try {
            // 从VehicleReservationService获取所有不重复的车场名称
            List<String> parkNames = vehicleReservationService.getAllDistinctYardNames();
            
            log.info("获取车场列表成功，共{}个车场", parkNames != null ? parkNames.size() : 0);
            
            return ResponseEntity.ok(Result.success(parkNames));
        } catch (Exception e) {
            log.error("获取车场列表失败", e);
            return ResponseEntity.ok(Result.error("获取车场列表失败"));
        }
    }

    /**
     * 获取当前用户的操作历史日志
     * 根据当前登录用户的 login_name 查询操作历史，而不是 user_name
     */
    @ApiOperation("获取当前用户的操作历史日志")
    @GetMapping("/current/operation-history")
    public ResponseEntity<Result> getCurrentUserOperationHistory(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam("页大小") @RequestParam(defaultValue = "20") Integer pageSize,
            @ApiParam("操作模块") @RequestParam(required = false) String module,
            @ApiParam("操作动作") @RequestParam(required = false) String action,
            @ApiParam("操作状态") @RequestParam(required = false) String status,
            @ApiParam("开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam("结束时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            HttpServletRequest request) {
        try {
            // 从请求头获取用户ID
            String userIdStr = request.getHeader("userId");
            if (StringUtils.isEmpty(userIdStr)) {
                return ResponseEntity.ok(Result.error("用户未登录"));
            }
            
            Integer userId = Integer.parseInt(userIdStr);
            User user = userService.getById(userId);
            if (user == null) {
                return ResponseEntity.ok(Result.error("用户不存在"));
            }
            
            // 使用 login_name 而不是 user_name 来查询操作历史
            String loginName = user.getLoginName();
            if (StringUtils.isEmpty(loginName)) {
                log.warn("用户ID {} 的 login_name 为空，无法查询操作历史", userId);
                return ResponseEntity.ok(Result.error("用户登录账号为空，无法查询操作历史"));
            }
            
            log.info("查询当前用户操作历史 - userId: {}, loginName: {}", userId, loginName);
            
            // 调用 ActivityLogService 查询操作历史，使用 login_name 作为 username 参数
            Page<ActivityLog> page = new Page<>(pageNum, pageSize);
            IPage<ActivityLog> result = activityLogService.getActivityLogPage(
                    page, null, loginName, module, action, status, startTime, endTime);
            
            log.info("查询当前用户操作历史成功 - userId: {}, loginName: {}, 总数: {}", 
                    userId, loginName, result.getTotal());
            
            return ResponseEntity.ok(Result.success(result));
        } catch (Exception e) {
            log.error("获取当前用户操作历史失败", e);
            return ResponseEntity.ok(Result.error("获取操作历史失败：" + e.getMessage()));
        }
    }

}

