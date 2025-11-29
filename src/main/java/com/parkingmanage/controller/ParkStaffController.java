package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.entity.ParkStaff;
import com.parkingmanage.entity.ActivityLog;
import com.parkingmanage.service.ParkStaffService;
import com.parkingmanage.service.ActivityLogService;
import com.parkingmanage.common.Result;
import com.parkingmanage.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 车场人员管理控制器
 * 提供人员的增删改查等管理功能
 * 
 * @author parking-system
 * @version 1.0
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/staff")
@CrossOrigin(origins = "*")
public class ParkStaffController {

    @Autowired
    private ParkStaffService parkStaffService;
    
    @Autowired
    private ActivityLogService activityLogService;

    /**
     * 获取人员列表（分页）
     * 
     * @param page 页码
     * @param size 每页大小
     * @param username 用户名（可选）
     * @param realName 真实姓名（可选）
     * @param parkName 车场名称（可选）
     * @param status 状态（可选）
     * @return 分页数据
     */
    @GetMapping("/list")
    public Result<IPage<ParkStaff>> getStaffList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String parkName,
            @RequestParam(required = false) String status) {
        
        try {
            // 创建分页对象
            Page<ParkStaff> pageObj = new Page<>(page, size);
            
            // 构建查询条件
            QueryWrapper<ParkStaff> queryWrapper = new QueryWrapper<>();
            
            if (StringUtils.hasText(username)) {
                queryWrapper.like("username", username);
            }
            if (StringUtils.hasText(realName)) {
                queryWrapper.like("real_name", realName);
            }
            if (StringUtils.hasText(parkName)) {
                queryWrapper.like("park_name", parkName);
            }
            if (StringUtils.hasText(status)) {
                queryWrapper.eq("status", Integer.parseInt(status));
            }
            
            // 按创建时间倒序排列
            queryWrapper.orderByDesc("created_time");
            
            // 执行分页查询
            IPage<ParkStaff> result = parkStaffService.page(pageObj, queryWrapper);
            
            return Result.success(result);
            
        } catch (Exception e) {
            return Result.error("500", "获取人员列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取人员详情
     * 
     * @param id 人员ID
     * @return 人员详情
     */
    @GetMapping("/{id}")
    public Result<ParkStaff> getStaffDetail(@PathVariable Integer id) {
        try {
            ParkStaff staff = parkStaffService.getById(id);
            if (staff == null) {
                return Result.error("404", "人员不存在");
            }
            return Result.success(staff);
        } catch (Exception e) {
            return Result.error("500", "获取人员详情失败：" + e.getMessage());
        }
    }

    /**
     * 新增人员
     * 
     * @param staff 人员信息
     * @return 操作结果
     */
    @PostMapping("/add")
    public Result<String> addStaff(@Valid @RequestBody ParkStaff staff, HttpServletRequest request) {
        try {
            // 检查用户名是否已存在
            QueryWrapper<ParkStaff> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", staff.getUsername());
            ParkStaff existingStaff = parkStaffService.getOne(queryWrapper);
            
            if (existingStaff != null) {
                return Result.error("400", "用户名已存在");
            }
            
            // 设置创建时间和更新时间
            staff.setCreatedTime(LocalDateTime.now());
            staff.setUpdatedTime(LocalDateTime.now());
            
            // 如果没有设置状态，默认为正常
            if (staff.getStatus() == null) {
                staff.setStatus(1);
            }
            
            // 如果没有设置密码，设置默认密码
            if (staff.getPassword() == null || staff.getPassword().trim().isEmpty()) {
                staff.setPassword("123456"); // 默认密码
            }
            
            // 使用BCrypt加密密码
            staff.setPassword(PasswordUtil.encodePassword(staff.getPassword()));
            
            // 保存人员信息
            boolean success = parkStaffService.save(staff);
            
            if (success) {
                // 记录操作日志
                com.parkingmanage.entity.User currentUser = getCurrentUser(request);
                String username = currentUser != null && currentUser.getLoginName() != null 
                                ? currentUser.getLoginName() 
                                : (currentUser != null && currentUser.getUserName() != null 
                                    ? currentUser.getUserName() 
                                    : "未知用户");
                String description = String.format("用户 %s 新增了车场人员：用户名 %s，姓名 %s，车场 %s，电话 %s", 
                                                  username,
                                                  staff.getUsername(),
                                                  staff.getRealName() != null ? staff.getRealName() : "未填写",
                                                  staff.getParkName() != null ? staff.getParkName() : "未填写",
                                                  staff.getPhone() != null ? staff.getPhone() : "未填写");
                recordStaffOperation(currentUser != null ? currentUser.getUserId() : null, username, "停车人员管理", "新增人员", description, request);
                return Result.success("新增人员成功");
            } else {
                return Result.error("500", "新增人员失败");
            }
            
        } catch (Exception e) {
            return Result.error("500", "新增人员失败：" + e.getMessage());
        }
    }

    /**
     * 更新人员信息
     * 
     * @param staff 人员信息
     * @return 操作结果
     */
    @PutMapping("/update")
    public Result<String> updateStaff(@Valid @RequestBody ParkStaff staff, HttpServletRequest request) {
        try {
            if (staff.getId() == null) {
                return Result.error("400", "人员ID不能为空");
            }
            
            // 检查人员是否存在
            ParkStaff existingStaff = parkStaffService.getById(staff.getId());
            if (existingStaff == null) {
                return Result.error("404", "人员不存在");
            }
            
            // 如果更新用户名，检查是否与其他人员重复
            if (!existingStaff.getUsername().equals(staff.getUsername())) {
                QueryWrapper<ParkStaff> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("username", staff.getUsername());
                queryWrapper.ne("id", staff.getId());
                ParkStaff duplicateStaff = parkStaffService.getOne(queryWrapper);
                
                if (duplicateStaff != null) {
                    return Result.error("400", "用户名已存在");
                }
            }
            
            // 设置更新时间
            staff.setUpdatedTime(LocalDateTime.now());
            
            // 处理密码更新逻辑
            if (staff.getPassword() != null && !staff.getPassword().trim().isEmpty()) {
                // 如果提供了新密码，则加密后更新
                staff.setPassword(PasswordUtil.encodePassword(staff.getPassword()));
            } else {
                // 如果没有提供密码，检查原始密码是否需要加密
                String existingPassword = existingStaff.getPassword();
                
                // 如果原始密码为空，设置默认密码并加密
                if (existingPassword == null || existingPassword.trim().isEmpty()) {
                    staff.setPassword(PasswordUtil.encodePassword("123456"));
                }
                // 如果原始密码是明文（不是BCrypt格式），则加密
                else if (!existingPassword.startsWith("$2a$") && 
                         !existingPassword.startsWith("$2b$") && 
                         !existingPassword.startsWith("$2y$")) {
                    staff.setPassword(PasswordUtil.encodePassword(existingPassword));
                } else {
                    // 保留已加密的密码
                    staff.setPassword(existingPassword);
                }
            }
            
            // 更新人员信息
            boolean success = parkStaffService.updateById(staff);
            
            if (success) {
                // 记录操作日志（包含具体修改内容）
                com.parkingmanage.entity.User currentUser = getCurrentUser(request);
                String username = currentUser != null && currentUser.getLoginName() != null 
                                ? currentUser.getLoginName() 
                                : (currentUser != null && currentUser.getUserName() != null 
                                    ? currentUser.getUserName() 
                                    : "未知用户");
                
                StringBuilder changeDetails = new StringBuilder();
                if (staff.getRealName() != null && !staff.getRealName().equals(existingStaff.getRealName())) {
                    changeDetails.append("姓名从\"").append(existingStaff.getRealName())
                               .append("\"改为\"").append(staff.getRealName()).append("\"；");
                }
                if (staff.getPhone() != null && !staff.getPhone().equals(existingStaff.getPhone())) {
                    changeDetails.append("电话从\"").append(existingStaff.getPhone())
                               .append("\"改为\"").append(staff.getPhone()).append("\"；");
                }
                if (staff.getParkName() != null && !staff.getParkName().equals(existingStaff.getParkName())) {
                    changeDetails.append("车场从\"").append(existingStaff.getParkName())
                               .append("\"改为\"").append(staff.getParkName()).append("\"；");
                }
                
                String description = changeDetails.length() > 0 
                    ? String.format("用户 %s 修改了车场人员（%s）的信息：%s", username, staff.getUsername(), changeDetails.toString())
                    : String.format("用户 %s 修改了车场人员（%s）的信息（无字段变更）", username, staff.getUsername());
                
                recordStaffOperation(currentUser != null ? currentUser.getUserId() : null, username, "停车人员管理", "修改人员", description, request);
                return Result.success("更新人员信息成功");
            } else {
                return Result.error("500", "更新人员信息失败");
            }
            
        } catch (Exception e) {
            return Result.error("500", "更新人员信息失败：" + e.getMessage());
        }
    }

    /**
     * 删除人员
     * 
     * @param id 人员ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteStaff(@PathVariable Integer id, HttpServletRequest request) {
        try {
            // 检查人员是否存在
            ParkStaff staff = parkStaffService.getById(id);
            if (staff == null) {
                return Result.error("404", "人员不存在");
            }
            
            // 删除人员
            boolean success = parkStaffService.removeById(id);
            
            if (success) {
                // 记录操作日志
                com.parkingmanage.entity.User currentUser = getCurrentUser(request);
                String username = currentUser != null && currentUser.getLoginName() != null 
                                ? currentUser.getLoginName() 
                                : (currentUser != null && currentUser.getUserName() != null 
                                    ? currentUser.getUserName() 
                                    : "未知用户");
                String description = String.format("用户 %s 删除了车场人员：用户名 %s，姓名 %s，车场 %s", 
                                                  username,
                                                  staff.getUsername(),
                                                  staff.getRealName() != null ? staff.getRealName() : "未知",
                                                  staff.getParkName() != null ? staff.getParkName() : "未知");
                recordStaffOperation(currentUser != null ? currentUser.getUserId() : null, username, "停车人员管理", "删除人员", description, request);
                return Result.success("删除人员成功");
            } else {
                return Result.error("500", "删除人员失败");
            }
            
        } catch (Exception e) {
            return Result.error("500", "删除人员失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除人员
     * 
     * @param request 包含ids数组的请求体
     * @return 操作结果
     */
    @DeleteMapping("/batch-delete")
    public Result<String> batchDeleteStaff(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> ids = (List<Integer>) request.get("ids");
            
            if (ids == null || ids.isEmpty()) {
                return Result.error("400", "请选择要删除的人员");
            }
            
            // 批量删除
            boolean success = parkStaffService.removeByIds(ids);
            
            if (success) {
                return Result.success("批量删除成功");
            } else {
                return Result.error("500", "批量删除失败");
            }
            
        } catch (Exception e) {
            return Result.error("500", "批量删除失败：" + e.getMessage());
        }
    }

    /**
     * 更新人员状态
     * 
     * @param id 人员ID
     * @param request 包含status的请求体
     * @return 操作结果
     */
    @PutMapping("/status/{id}")
    public Result<String> updateStaffStatus(@PathVariable Integer id, 
                                          @RequestBody Map<String, Object> request,
                                          HttpServletRequest httpRequest) {
        try {
            Integer status = (Integer) request.get("status");
            if (status == null) {
                return Result.error("400", "状态值不能为空");
            }
            
            // 检查人员是否存在
            ParkStaff staff = parkStaffService.getById(id);
            if (staff == null) {
                return Result.error("404", "人员不存在");
            }
            
            // 获取禁用原因（如果状态为禁用）
            String disableReason = null;
            if (request.containsKey("disableReason")) {
                Object reasonObj = request.get("disableReason");
                if (reasonObj != null) {
                    disableReason = reasonObj.toString();
                }
            }
            
            // 更新状态
            staff.setStatus(status);
            staff.setUpdatedTime(LocalDateTime.now());
            
            // 如果状态为禁用（0），设置禁用原因和禁用时间
            if (status == 0) {
                staff.setDisableReason(disableReason);
                staff.setDisableTime(LocalDateTime.now());
            } else {
                // 如果状态为启用（1），清除禁用原因、禁用时间、锁定次数和锁定时间
                staff.setDisableReason(null);
                staff.setDisableTime(null);
                staff.setLockCount(0); // 启用后，锁定次数清零
                staff.setLockTime(null); // 清除锁定时间（禁用和锁定不能同时进行）
                staff.setFailedLoginCount(0); // 清除失败次数
            }
            
            boolean success = parkStaffService.updateById(staff);
            
            if (success) {
                // 记录操作日志
                com.parkingmanage.entity.User currentUser = getCurrentUser(httpRequest);
                String username = currentUser != null && currentUser.getLoginName() != null 
                                ? currentUser.getLoginName() 
                                : (currentUser != null && currentUser.getUserName() != null 
                                    ? currentUser.getUserName() 
                                    : "未知用户");
                String action = status == 0 ? "禁用人员" : "启用人员";
                String description = String.format("用户 %s %s：%s（%s）", 
                                                  username,
                                                  action,
                                                  staff.getUsername(),
                                                  staff.getRealName() != null ? staff.getRealName() : "未知");
                if (status == 0 && disableReason != null && !disableReason.isEmpty()) {
                    description += "，禁用原因：" + disableReason;
                }
                recordStaffOperation(currentUser != null ? currentUser.getUserId() : null, username, "停车人员管理", action, description, httpRequest);
                return Result.success("状态更新成功");
            } else {
                return Result.error("500", "状态更新失败");
            }
            
        } catch (Exception e) {
            return Result.error("500", "状态更新失败：" + e.getMessage());
        }
    }

    /**
     * 重置密码
     * 
     * @param id 人员ID
     * @param request 包含password的请求体
     * @return 操作结果
     */
    @PutMapping("/reset-password/{id}")
    public Result<String> resetPassword(@PathVariable Integer id, 
                                      @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("password");
            
            // 如果没有提供新密码，使用默认密码123456
            if (!StringUtils.hasText(newPassword)) {
                newPassword = "123456";
            }
            
            // 检查人员是否存在
            ParkStaff staff = parkStaffService.getById(id);
            if (staff == null) {
                return Result.error("404", "人员不存在");
            }
            
            // 更新密码并加密
            staff.setPassword(PasswordUtil.encodePassword(newPassword));
            staff.setUpdatedTime(LocalDateTime.now());
            boolean success = parkStaffService.updateById(staff);
            
            if (success) {
                String message = request.get("password") != null ? 
                    "密码重置成功" : "密码已重置为默认密码(123456)";
                return Result.success(message);
            } else {
                return Result.error("500", "密码重置失败");
            }
            
        } catch (Exception e) {
            return Result.error("500", "密码重置失败：" + e.getMessage());
        }
    }

    /**
     * 导出人员数据
     * 
     * @param username 用户名（可选）
     * @param realName 真实姓名（可选）
     * @param parkName 车场名称（可选）
     * @param status 状态（可选）
     * @param response HTTP响应对象
     */
    @GetMapping("/export")
    public void exportStaffData(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String parkName,
            @RequestParam(required = false) String status,
            HttpServletResponse response) {
        
        try {
            // 构建查询条件
            QueryWrapper<ParkStaff> queryWrapper = new QueryWrapper<>();
            
            if (StringUtils.hasText(username)) {
                queryWrapper.like("username", username);
            }
            if (StringUtils.hasText(realName)) {
                queryWrapper.like("real_name", realName);
            }
            if (StringUtils.hasText(parkName)) {
                queryWrapper.like("park_name", parkName);
            }
            if (StringUtils.hasText(status)) {
                queryWrapper.eq("status", Integer.parseInt(status));
            }
            
            queryWrapper.orderByDesc("created_time");
            
            // 获取数据
            List<ParkStaff> staffList = parkStaffService.list(queryWrapper);
            
            log.info("📊 导出巡检人员数据，共 {} 条记录", staffList.size());
            
            // 生成CSV内容（带BOM头解决Excel打开中文乱码问题）
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("\uFEFF"); // UTF-8 BOM
            csvContent.append("用户名,真实姓名,车场名称,职位,手机号码,邮箱,状态,创建时间\n");
            
            for (ParkStaff staff : staffList) {
                csvContent.append(escapeCSV(staff.getUsername())).append(",")
                          .append(escapeCSV(staff.getRealName())).append(",")
                          .append(escapeCSV(staff.getParkName())).append(",")
                          .append(escapeCSV(staff.getPosition())).append(",")
                          .append(escapeCSV(staff.getPhone() != null ? staff.getPhone() : "")).append(",")
                          .append(escapeCSV(staff.getEmail() != null ? staff.getEmail() : "")).append(",")
                          .append(staff.getStatus() == 1 ? "正常" : "禁用").append(",")
                          .append(staff.getCreatedTime() != null ? staff.getCreatedTime().toString() : "").append("\n");
            }
            
            // 设置响应头
            String fileName = "巡检人员数据_" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + new String(fileName.getBytes("UTF-8"), "ISO-8859-1") + "\"");
            
            // 写入响应流
            byte[] content = csvContent.toString().getBytes("UTF-8");
            response.getOutputStream().write(content);
            response.getOutputStream().flush();
            
            log.info("✅ 导出巡检人员数据成功");
                    
        } catch (Exception e) {
            log.error("❌ 导出巡检人员数据失败", e);
            try {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                String errorMsg = "{\"code\":\"500\",\"message\":\"导出失败：" + e.getMessage().replace("\"", "'") + "\"}";
                response.getWriter().write(errorMsg);
                response.getWriter().flush();
            } catch (Exception ex) {
                log.error("❌ 写入错误响应失败", ex);
            }
        }
    }
    
    /**
     * CSV字段转义（处理逗号、引号、换行符）
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        // 如果包含逗号、引号或换行符，需要用引号包裹，并且引号要转义
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * 根据用户名查询车场名称
     * 
     * @param username 用户名
     * @return 车场信息（包含park_name等）
     */
    @GetMapping("/park-info/{username}")
    public Result<Map<String, String>> getParkInfoByUsername(@PathVariable String username) {
        try {
            if (!StringUtils.hasText(username)) {
                return Result.error("400", "用户名不能为空");
            }
            
            // 根据用户名查询人员信息
            QueryWrapper<ParkStaff> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", username);
            ParkStaff staff = parkStaffService.getOne(queryWrapper);
            
            if (staff == null) {
                return Result.error("404", "未找到该用户");
            }
            
            // 构建返回数据
            Map<String, String> parkInfo = new HashMap<>();
            parkInfo.put("parkName", staff.getParkName());
            parkInfo.put("realName", staff.getRealName());
            parkInfo.put("position", staff.getPosition());
            
            return Result.success(parkInfo);
            
        } catch (Exception e) {
            return Result.error("500", "查询车场信息失败：" + e.getMessage());
        }
    }

    /**
     * 记录车场人员操作日志
     */
    /**
     * 获取当前登录用户
     */
    private com.parkingmanage.entity.User getCurrentUser(HttpServletRequest request) {
        try {
            return com.parkingmanage.utils.TokenUtils.getCurrentUser();
        } catch (Exception e) {
            log.warn("获取当前用户失败", e);
            return null;
        }
    }

    private void recordStaffOperation(Integer userId, String username, String module, String action, String description, HttpServletRequest request) {
        try {
            ActivityLog activityLog = new ActivityLog();
            activityLog.setUserId(userId != null ? userId.toString() : "system");
            activityLog.setUsername(username != null ? username : "系统");
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
            log.warn("记录车场人员操作日志失败", e);
        }
    }

    /**
     * 获取客户端IP地址
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

    // ==================== 📊 巡检人员统计API ====================

    /**
     * 6. 巡检人员状态统计
     * GET /api/staff/stats/status
     */
    @GetMapping("/stats/status")
    public Result<List<Map<String, Object>>> getStaffStatusStats() {
        try {
            log.info("📊 [统计接口] 巡检人员状态统计");
            
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            
            // 统计启用和禁用人数
            QueryWrapper<ParkStaff> enabledWrapper = new QueryWrapper<>();
            enabledWrapper.eq("status", 1);
            long enabledCount = parkStaffService.count(enabledWrapper);
            
            QueryWrapper<ParkStaff> disabledWrapper = new QueryWrapper<>();
            disabledWrapper.eq("status", 0);
            long disabledCount = parkStaffService.count(disabledWrapper);
            
            // 构建前端期望的数组格式（改为"正常"和"禁用"）
            if (enabledCount > 0) {
                Map<String, Object> enabledItem = new HashMap<>();
                enabledItem.put("statusName", "正常");
                enabledItem.put("status", 1);
                enabledItem.put("count", enabledCount);
                result.add(enabledItem);
            }
            
            if (disabledCount > 0) {
                Map<String, Object> disabledItem = new HashMap<>();
                disabledItem.put("statusName", "禁用");
                disabledItem.put("status", 0);
                disabledItem.put("count", disabledCount);
                result.add(disabledItem);
            }
            
            // 如果没有任何数据，返回默认值
            if (result.isEmpty()) {
                Map<String, Object> emptyItem = new HashMap<>();
                emptyItem.put("statusName", "暂无数据");
                emptyItem.put("status", -1);
                emptyItem.put("count", 0);
                result.add(emptyItem);
            }
            
            log.info("✅ [统计接口] 巡检人员状态统计成功 - 在职:{}, 离职:{}", 
                    enabledCount, disabledCount);
            return Result.success(result);
        } catch (Exception e) {
            log.error("❌ [统计接口] 巡检人员状态统计失败", e);
            return Result.error("500", "统计失败：" + e.getMessage());
        }
    }

    /**
     * 7. 巡检员发现问题类型分布
     * GET /api/staff/stats/problem-types?days=30
     */
    @GetMapping("/stats/problem-types")
    public Result<List<Map<String, Object>>> getStaffProblemTypes(
            @RequestParam(defaultValue = "30") Integer days) {
        try {
            log.info("📊 [统计接口] 巡检员发现问题类型分布 - 近{}天", days);
            
            // 这里需要关联violations表统计
            // 假设violations表有created_by字段和violation_type字段
            List<Map<String, Object>> result = parkStaffService.getProblemTypeDistribution(days);
            
            log.info("✅ [统计接口] 巡检员问题类型统计成功 - 返回{}条", result.size());
            return Result.success(result);
        } catch (Exception e) {
            log.error("❌ [统计接口] 巡检员问题类型统计失败", e);
            return Result.error("500", "统计失败：" + e.getMessage());
        }
    }
} 