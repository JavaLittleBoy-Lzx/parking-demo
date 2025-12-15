package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.VisitorApplication;
import com.parkingmanage.service.VisitorApplicationService;
import com.parkingmanage.utils.PageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 访客申请控制器
 * 处理访客申请提交、状态查询等功能
 */
@RestController
@RequestMapping("/parking/visitor")
@CrossOrigin(origins = "*")
public class VisitorApplicationController {
    
    private static final Logger logger = LoggerFactory.getLogger(VisitorApplicationController.class);
    
    @Autowired
    private VisitorApplicationService visitorApplicationService;
    
    /**
     * 提交访客申请
     * 
     * @param applicationData 申请数据
     * @return 提交结果
     */
    @PostMapping("/apply")
    public Result<Map<String, Object>> submitApplication(@RequestBody Map<String, Object> applicationData) {
        logger.info("📝 收到访客申请请求: {}", applicationData);
        
        try {
            // 验证必要字段
            String nickname = (String) applicationData.get("nickname");
            String phone = (String) applicationData.get("phone");
            String ownerPhone = (String) applicationData.get("ownerPhone");
            String ownerName = (String) applicationData.get("ownerName");
            String reason = (String) applicationData.get("reason");
            String fullAddress = (String) applicationData.get("fullAddress");
            
            if (nickname == null || nickname.trim().isEmpty()) {
                return Result.error("姓名不能为空");
            }
            
            if (phone == null || phone.trim().isEmpty()) {
                return Result.error("手机号不能为空");
            }
            
            if (ownerPhone == null || ownerPhone.trim().isEmpty()) {
                return Result.error("业主手机号不能为空");
            }
            
            if (reason == null || reason.trim().isEmpty()) {
                return Result.error("申请原因不能为空");
            }
            
            if (fullAddress == null || fullAddress.trim().isEmpty()) {
                return Result.error("访问地址不能为空");
            }
            
            // 检查是否已有申请
            VisitorApplication existingApplication = visitorApplicationService.getByPhone(phone);
            if (existingApplication != null && "待审批".equals(existingApplication.getAuditstatus())) {
                return Result.error("您已有待审批的申请，请耐心等待审核结果");
            }
            
            // 创建访客申请实体
            VisitorApplication visitorApplication = new VisitorApplication();
            visitorApplication.setApplicationNo(visitorApplicationService.generateApplicationNo());
            visitorApplication.setNickname(nickname.trim());
            visitorApplication.setPhone(phone.trim());
            visitorApplication.setOwnerPhone(ownerPhone.trim());
            visitorApplication.setOwnerName(ownerName != null ? ownerName.trim() : null);
            visitorApplication.setGender((String) applicationData.get("gender"));
            visitorApplication.setIdCard((String) applicationData.get("idCard"));
            visitorApplication.setReason(reason.trim());
            visitorApplication.setFullAddress(fullAddress.trim());
            
            // 解析地址信息 - 前端直接发送这些字段，不是嵌套对象
            String province = (String) applicationData.get("province");
            String city = (String) applicationData.get("city");
            String district = (String) applicationData.get("district");
            String community = (String) applicationData.get("community");
            String building = (String) applicationData.get("building");
            Object unitsObj = applicationData.get("units");
            Object floorObj = applicationData.get("floor");
            Object roomnumberObj = applicationData.get("roomnumber");
            
            // 设置省市区信息（使用前端传递的真实数据）
            visitorApplication.setProvince(province != null ? province : "");
            visitorApplication.setCity(city != null ? city : "");
            visitorApplication.setDistrict(district != null ? district : "");
            visitorApplication.setCommunity(community != null ? community : "四季上东");
            
            // 设置用户选择的具体地址信息
            visitorApplication.setBuilding(building);
            
            // 处理数字类型转换
            if (unitsObj != null) {
                if (unitsObj instanceof Integer) {
                    visitorApplication.setUnits(String.valueOf(unitsObj));
                } else if (unitsObj instanceof String && !((String) unitsObj).isEmpty()) {
                    visitorApplication.setUnits((String) unitsObj);
                }
            }
            
            if (floorObj != null) {
                if (floorObj instanceof Integer) {
                    visitorApplication.setFloor(String.valueOf(floorObj));
                } else if (floorObj instanceof String && !((String) floorObj).isEmpty()) {
                    visitorApplication.setFloor((String) floorObj);
                }
            }
            
            if (roomnumberObj != null) {
                if (roomnumberObj instanceof Integer) {
                    visitorApplication.setRoomnumber(String.valueOf(roomnumberObj));
                } else if (roomnumberObj instanceof String && !((String) roomnumberObj).isEmpty()) {
                    visitorApplication.setRoomnumber((String) roomnumberObj);
                }
            }
            
            logger.info("📍 完整地址信息: province={}, city={}, district={}, community={}, building={}, units={}, floor={}, roomnumber={}", 
                province, city, district, community, building, visitorApplication.getUnits(), visitorApplication.getFloor(), visitorApplication.getRoomnumber());
            
            visitorApplication.setUserkind("访客");
            visitorApplication.setAuditstatus("待审批");
            visitorApplication.setApplydate(LocalDateTime.now());
            visitorApplication.setCreateTime(LocalDateTime.now());
            visitorApplication.setUpdateTime(LocalDateTime.now());
            
            // 保存到数据库
            boolean saved = visitorApplicationService.save(visitorApplication);
            if (!saved) {
                return Result.error("申请提交失败，请重试");
            }
            
            logger.info("✅ 访客申请保存成功: 申请编号={}, 姓名={}, 手机号={}, 业主姓名={}, 业主手机号={}", 
                visitorApplication.getApplicationNo(), nickname, phone, ownerName, ownerPhone);
            
            // 返回成功结果
            Map<String, Object> result = new HashMap<>();
            result.put("applicationId", visitorApplication.getId());
            result.put("applicationNo", visitorApplication.getApplicationNo());
            result.put("status", "待审批");
            result.put("message", "申请提交成功，管理员将在1-3个工作日内审核");
            result.put("estimatedReviewTime", "1-3个工作日");
            
            return Result.success(result);
            
        } catch (Exception e) {
            logger.error("❌ 访客申请提交失败", e);
            return Result.error("申请提交失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询申请状态
     * 
     * @param phone 手机号
     * @return 申请状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> checkApplicationStatus(@RequestParam String phone) {
        logger.info("📱 查询申请状态: phone={}", phone);
        
        try {
            if (phone == null || phone.trim().isEmpty()) {
                return Result.error("手机号不能为空");
            }
            
            // 从数据库查询申请记录
            VisitorApplication application = visitorApplicationService.getByPhone(phone);
            
            Map<String, Object> applicationStatus = new HashMap<>();
            applicationStatus.put("phone", phone);
            
            if (application != null) {
                applicationStatus.put("hasApplication", true);
                applicationStatus.put("applicationNo", application.getApplicationNo());
                applicationStatus.put("status", application.getAuditstatus());
                applicationStatus.put("submitTime", application.getApplydate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                applicationStatus.put("fullAddress", application.getFullAddress());
                
                // 根据状态设置消息
                String message;
                switch (application.getAuditstatus()) {
                    case "待审批":
                        message = "您的申请正在审核中，请耐心等待";
                        break;
                    case "已通过":
                        message = "恭喜！您的申请已通过审核，可以使用系统了";
                        break;
                    case "未通过":
                        message = "很抱歉，您的申请未通过审核。原因：" + (application.getRefusereason() != null ? application.getRefusereason() : "无");
                        break;
                    default:
                        message = "申请状态异常，请联系管理员";
                }
                applicationStatus.put("message", message);
                
                if (application.getAuditdate() != null) {
                    applicationStatus.put("auditTime", application.getAuditdate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
                if (application.getAuditusername() != null) {
                    applicationStatus.put("auditUser", application.getAuditusername());
                }
            } else {
                applicationStatus.put("hasApplication", false);
                applicationStatus.put("message", "未找到您的申请记录");
            }
            
            logger.info("✅ 申请状态查询成功: {}", applicationStatus);
            
            return Result.success(applicationStatus);
            
        } catch (Exception e) {
            logger.error("❌ 查询申请状态失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新申请信息
     * 
     * @param applicationData 更新数据
     * @return 更新结果
     */
    @PutMapping("/update")
    public Result<Map<String, Object>> updateApplication(@RequestBody Map<String, Object> applicationData) {
        logger.info("🔄 更新访客申请: {}", applicationData);
        
        try {
            String applicationId = (String) applicationData.get("applicationId");
            
            if (applicationId == null || applicationId.trim().isEmpty()) {
                return Result.error("申请ID不能为空");
            }
            
            // 模拟更新数据库
            Map<String, Object> updatedApplication = new HashMap<>();
            updatedApplication.put("applicationId", applicationId);
            updatedApplication.put("updateTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            updatedApplication.put("message", "申请信息更新成功");
            
            logger.info("✅ 申请信息更新成功: applicationId={}", applicationId);
            
            return Result.success(updatedApplication);
            
        } catch (Exception e) {
            logger.error("❌ 更新申请信息失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取申请表单选项
     * 
     * @return 表单选项
     */
    @GetMapping("/form-options")
    public Result<Map<String, Object>> getFormOptions() {
        logger.info("📋 获取申请表单选项");
        
        try {
            Map<String, Object> options = new HashMap<>();
            
            // 性别选项
            options.put("genderOptions", new String[]{"男", "女"});
            
            // 申请原因模板
            options.put("reasonTemplates", new String[]{
                "探访朋友",
                "送货服务", 
                "维修服务",
                "商务拜访",
                "其他事务"
            });
            
            return Result.success(options);
            
        } catch (Exception e) {
            logger.error("❌ 获取表单选项失败", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 审批访客申请
     * 
     * @param visitorApplication 审批数据
     * @return 审批结果
     */
    @PutMapping("/audit")
    public Result<Map<String, Object>> auditVisitorApplication(@RequestBody VisitorApplication visitorApplication) {
        logger.info("🔍 审批访客申请: id={}, status={}", visitorApplication.getId(), visitorApplication.getAuditstatus());
        
        try {
            if (visitorApplication.getId() == null) {
                return Result.error("申请ID不能为空");
            }
            
            if (visitorApplication.getAuditstatus() == null || visitorApplication.getAuditstatus().trim().isEmpty()) {
                return Result.error("审批状态不能为空");
            }
            
            // 设置审批信息
            visitorApplication.setAuditdate(LocalDateTime.now());
            visitorApplication.setUpdateTime(LocalDateTime.now());
            
            // 更新申请状态
            boolean updated = visitorApplicationService.updateVisitorApplication(visitorApplication);
            if (!updated) {
                return Result.error("审批失败，请重试");
            }
            
            logger.info("✅ 访客申请审批成功: id={}, status={}", visitorApplication.getId(), visitorApplication.getAuditstatus());
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "审批成功");
            result.put("status", visitorApplication.getAuditstatus());
            
            return Result.success(result);
            
        } catch (Exception e) {
            logger.error("❌ 审批访客申请失败", e);
            return Result.error("审批失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据手机号查询访客的所有申请记录（用于移动端）
     * 
     * @param phone 手机号
     * @return 申请记录列表
     */
    @GetMapping("/records")
    public Result<List<VisitorApplication>> getVisitorRecords(@RequestParam String phone) {
        logger.info("📋 查询访客申请记录列表: phone={}", phone);
        
        try {
            if (phone == null || phone.trim().isEmpty()) {
                return Result.error("手机号不能为空");
            }
            
            // 查询该手机号的所有申请记录
            List<VisitorApplication> records = visitorApplicationService.getRecordsByPhone(phone);
            
            // 按申请时间倒序排列（最新的在前面）
            records.sort((a, b) -> {
                LocalDateTime timeA = a.getApplydate() != null ? a.getApplydate() : a.getCreateTime();
                LocalDateTime timeB = b.getApplydate() != null ? b.getApplydate() : b.getCreateTime();
                return timeB.compareTo(timeA);
            });
            
            logger.info("✅ 查询到 {} 条申请记录", records.size());
            
            return Result.success(records);
            
        } catch (Exception e) {
            logger.error("❌ 查询访客申请记录失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取审核通过的申请记录（用于预约时获取详细地址）
     * 
     * @param phone       手机号
     * @param auditstatus 审核状态（默认"已通过"）
     * @return 审核通过的申请记录列表
     */
    @GetMapping("/getApprovedApplications")
    public Result<List<VisitorApplication>> getApprovedApplications(
            @RequestParam String phone,
            @RequestParam(required = false, defaultValue = "已通过") String auditstatus) {
        
        logger.info("🔍 查询审核通过的访客申请记录: phone={}, auditstatus={}", phone, auditstatus);
        
        try {
            if (phone == null || phone.trim().isEmpty()) {
                return Result.error("手机号不能为空");
            }
            
            // 查询该手机号的审核通过记录
            List<VisitorApplication> approvedApplications = visitorApplicationService.getApprovedApplicationsByPhone(phone, auditstatus);
            
            if (approvedApplications == null || approvedApplications.isEmpty()) {
                logger.info("📍 未找到审核通过的访客申请记录: phone={}", phone);
                return Result.success(approvedApplications);
            }
            
            // 按申请时间倒序排列（最新的在前面），用于获取最新的地址信息
            approvedApplications.sort((a, b) -> {
                LocalDateTime timeA = a.getApplydate() != null ? a.getApplydate() : a.getCreateTime();
                LocalDateTime timeB = b.getApplydate() != null ? b.getApplydate() : b.getCreateTime();
                return timeB.compareTo(timeA);
            });
            
            // 🔧 增强：记录详细的地址信息用于调试
            if (!approvedApplications.isEmpty()) {
                System.out.println("===========================================");
                System.out.println("🔍 [访客申请查询] 详细地址信息检查:");
                System.out.println("📞 查询手机号: " + phone);
                System.out.println("📊 找到审核通过记录数量: " + approvedApplications.size());
                System.out.println("===========================================");
                
                // 显示前3条记录的详细地址信息
                for (int i = 0; i < Math.min(3, approvedApplications.size()); i++) {
                    VisitorApplication record = approvedApplications.get(i);
                    System.out.println("📋 [访客申请查询] 记录" + (i+1) + " 详情:");
                    System.out.println("  ID: " + record.getId());
                    System.out.println("  申请编号: " + record.getApplicationNo());
                    System.out.println("  申请人: " + record.getNickname());
                    System.out.println("  手机号: " + record.getPhone());
                    System.out.println("  审核状态: " + record.getAuditstatus());
                    System.out.println("  申请时间: " + record.getApplydate());
                    System.out.println("  地址信息:");
                    System.out.println("    省份: '" + record.getProvince() + "'");
                    System.out.println("    城市: '" + record.getCity() + "'");
                    System.out.println("    区域: '" + record.getDistrict() + "'");
                    System.out.println("    小区: '" + record.getCommunity() + "'");
                    System.out.println("    栋号: '" + record.getBuilding() + "' (类型: " + (record.getBuilding() != null ? record.getBuilding().getClass().getSimpleName() : "null") + ")");
                    System.out.println("    单元: " + record.getUnits() + " (类型: " + (record.getUnits() != null ? record.getUnits().getClass().getSimpleName() : "null") + ")");
                    System.out.println("    楼层: " + record.getFloor() + " (类型: " + (record.getFloor() != null ? record.getFloor().getClass().getSimpleName() : "null") + ")");
                    System.out.println("    房间号: " + record.getRoomnumber() + " (类型: " + (record.getRoomnumber() != null ? record.getRoomnumber().getClass().getSimpleName() : "null") + ")");
                    System.out.println("    完整地址: '" + record.getFullAddress() + "'");
                    System.out.println("  地址字段检查:");
                    System.out.println("    栋号为空: " + (record.getBuilding() == null || record.getBuilding().trim().isEmpty()));
                    System.out.println("    单元为空: " + (record.getUnits() == null));
                    System.out.println("    楼层为空: " + (record.getFloor() == null));
                    System.out.println("    房间号为空: " + (record.getRoomnumber() == null));
                    System.out.println("-------------------------------------------");
                }
                
                // 检查最新记录的地址完整性
                VisitorApplication latest = approvedApplications.get(0);
                boolean hasCompleteAddress = latest.getBuilding() != null && !latest.getBuilding().trim().isEmpty() &&
                                           latest.getUnits() != null &&
                                           latest.getFloor() != null &&
                                           latest.getRoomnumber() != null;
                
                if (hasCompleteAddress) {
                    logger.info("✅ [访客申请查询] 最新记录地址信息完整");
                } else {
                    logger.warn("⚠️ [访客申请查询] 最新记录地址信息不完整！");
                    System.out.println("🚨 [访客申请查询] 地址信息缺失详情:");
                    if (latest.getBuilding() == null || latest.getBuilding().trim().isEmpty()) {
                        System.out.println("  - 缺少栋号");
                    }
                    if (latest.getUnits() == null) {
                        System.out.println("  - 缺少单元号");
                    }
                    if (latest.getFloor() == null) {
                        System.out.println("  - 缺少楼层号");
                    }
                    if (latest.getRoomnumber() == null) {
                        System.out.println("  - 缺少房间号");
                    }
                }
                
                System.out.println("===========================================");
                
                // 保持原有的日志格式
                logger.info("📍 最新审核通过记录的地址信息: province={}, city={}, district={}, community={}, building={}, units={}, floor={}, roomnumber={}, fullAddress={}",
                    latest.getProvince(), latest.getCity(), latest.getDistrict(), latest.getCommunity(),
                    latest.getBuilding(), latest.getUnits(), latest.getFloor(), latest.getRoomnumber(),
                    latest.getFullAddress());
            } else {
                System.out.println("⚠️ [访客申请查询] 未找到任何审核通过的记录");
                System.out.println("📞 查询条件: phone=" + phone + ", auditstatus=" + auditstatus);
            }
            
            logger.info("✅ 查询到 {} 条审核通过的申请记录", approvedApplications.size());
            
            return Result.success(approvedApplications);
            
        } catch (Exception e) {
            logger.error("❌ 查询审核通过的访客申请记录失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页查询访客申请列表（用于管理后台）
     * 
     * @param nickname  访客姓名
     * @param community 小区名称
     * @param applydate 申请日期
     * @param pageNum   页码
     * @param pageSize  页大小
     * @return 分页结果
     */
    @GetMapping("/mypage")
    public IPage<VisitorApplication> myFindPage(
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false, value = "community") String community,
            @RequestParam(required = false, value = "applydate") String applydate,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        
        logger.info("📋 分页查询访客申请: nickname={}, community={}, applydate={}, pageNum={}, pageSize={}", 
            nickname, community, applydate, pageNum, pageSize);
        
        try {
            List<VisitorApplication> applicationList = visitorApplicationService.queryListVisitorApplication(nickname, community, applydate);
            
            // 按照姓名和申请日期排序
            List<VisitorApplication> sortedApplications = applicationList.stream()
                .sorted(Comparator.comparing(VisitorApplication::getNickname)
                    .thenComparing(VisitorApplication::getApplydate))
                .collect(Collectors.toList());
            
            IPage<VisitorApplication> page = PageUtils.getPage(sortedApplications, pageNum, pageSize);
            
            logger.info("✅ 查询到 {} 条访客申请记录", applicationList.size());
            
            return page;
            
        } catch (Exception e) {
            logger.error("❌ 分页查询访客申请失败", e);
            throw e;
        }
    }
} 