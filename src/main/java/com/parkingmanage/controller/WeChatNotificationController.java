package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.service.WeChatTemplateMessageService;
import com.parkingmanage.service.ButlerService;
import com.parkingmanage.service.UserMappingService;
import com.parkingmanage.entity.Butler;
import com.parkingmanage.entity.UserMapping;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信通知控制器
 * 提供微信模板消息通知相关接口
 * 
 * @author MLH
 * @since 2025-01-31
 */
@Slf4j
@RestController
@RequestMapping("/api/wechat")
@Api(tags = "微信通知接口")
public class WeChatNotificationController {
    
    @Resource
    private WeChatTemplateMessageService weChatTemplateMessageService;
    
    @Resource
    private ButlerService butlerService;
    
    @Resource
    private UserMappingService userMappingService;
    
    /**
     * 根据车场名称查询管家昵称
     * 流程：查询车场管家的手机号 -> 通过手机号在user_mapping中查询nickname
     * 如果管家未关注公众号，返回未关注的管家列表
     */
    @GetMapping("/butler-nickname/{community}")
    @ApiOperation("根据车场名称查询管家昵称")
    public Result<Map<String, Object>> getButlerNicknameByCommunity(
            @PathVariable @ApiParam("车场名称") String community) {
        log.info("🔍 查询管家昵称 - 车场: {}", community);
        
        try {
            // 参数校验
            if (community == null || community.trim().isEmpty()) {
                return Result.error("车场名称不能为空");
            }
            
            // 1. 查询该车场的所有管家
            List<Butler> butlers = butlerService.getAllButlersByCommunity(community);
            
            if (butlers == null || butlers.isEmpty()) {
                log.warn("⚠️ 未找到车场管家 - 车场: {}", community);
                return Result.error("未找到该车场的管家信息");
            }
            
            // 2. 遍历所有管家，查找已关注公众号的管家
            List<Map<String, String>> unfollowedButlers = new java.util.ArrayList<>();
            UserMapping foundUserMapping = null;
            Butler foundButler = null;
            
            for (Butler butler : butlers) {
                String phone = butler.getPhone();
                
                if (phone == null || phone.trim().isEmpty()) {
                    log.warn("⚠️ 管家手机号为空 - 车场: {}, 管家: {}", community, butler.getUsername());
                    continue;
                }
                
                // 通过手机号在user_mapping表中查询
                UserMapping userMapping = userMappingService.getByPhone(phone);
                
                if (userMapping != null && userMapping.getNickname() != null && !userMapping.getNickname().trim().isEmpty()) {
                    // 找到已关注的管家
                    foundUserMapping = userMapping;
                    foundButler = butler;
                    log.info("✅ 找到已关注管家 - 管家: {}, 昵称: {}", butler.getUsername(), userMapping.getNickname());
                    break;
                } else {
                    // 记录未关注的管家
                    Map<String, String> unfollowedButler = new HashMap<>();
                    unfollowedButler.put("butlerName", butler.getUsername());
                    unfollowedButler.put("phone", phone);
                    unfollowedButlers.add(unfollowedButler);
                    log.warn("⚠️ 管家未关注公众号 - 管家: {}, 手机: {}", butler.getUsername(), phone);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            
            // 3. 如果找到已关注的管家，返回其昵称
            if (foundUserMapping != null && foundButler != null) {
                log.info("✅ 查询管家昵称成功 - 车场: {}, 管家: {}, 昵称: {}", 
                        community, foundButler.getUsername(), foundUserMapping.getNickname());
                
                result.put("success", true);
                result.put("nickname", foundUserMapping.getNickname());
                result.put("butlerName", foundButler.getUsername());
                result.put("phone", foundButler.getPhone());
                result.put("community", community);
                result.put("hasUnfollowedButlers", !unfollowedButlers.isEmpty());
                result.put("unfollowedButlers", unfollowedButlers);
                
                return Result.success(result);
            }
            
            // 4. 如果所有管家都未关注公众号，返回未关注列表
            log.warn("⚠️ 所有管家都未关注公众号 - 车场: {}, 未关注管家数: {}", community, unfollowedButlers.size());
            
            result.put("success", false);
            result.put("message", "该车场的管家尚未关注公众号，无法接收通知");
            result.put("unfollowedButlers", unfollowedButlers);
            result.put("community", community);
            result.put("totalButlers", butlers.size());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("❌ 查询管家昵称异常 - 车场: {}, 错误: {}", community, e.getMessage(), e);
            return Result.error("查询异常: " + e.getMessage());
        }
    }
    
    /**
     * 发送车辆违规停车告警通知
     */
    @PostMapping("/send/violation-notification")
    @ApiOperation("发送车辆违规停车告警通知")
    public Result<Map<String, Object>> sendViolationNotification(@RequestBody ViolationNotificationRequest request) {
        log.info("🚨 收到违规停车告警通知请求 - 车牌: {}, 停车场: {}, 管家: {}", 
                request.getPlateNumber(), request.getParkName(), request.getManagerNickname());
        
        try {
            // 参数校验
            if (request.getPlateNumber() == null || request.getPlateNumber().trim().isEmpty()) {
                return Result.error("车牌号不能为空");
            }
            if (request.getParkName() == null || request.getParkName().trim().isEmpty()) {
                return Result.error("停车场名称不能为空");
            }
            if (request.getManagerNickname() == null || request.getManagerNickname().trim().isEmpty()) {
                return Result.error("管家昵称不能为空");
            }
            if (request.getViolationLocation() == null || request.getViolationLocation().trim().isEmpty()) {
                return Result.error("违规位置不能为空");
            }
            if (request.getParkingDuration() == null || request.getParkingDuration().trim().isEmpty()) {
                return Result.error("停车时长不能为空");
            }
            
            // 调用服务层发送通知
            Map<String, Object> result = weChatTemplateMessageService.sendParkingViolationNotification(
                    request.getPlateNumber(),
                    request.getParkName(),
                    request.getViolationLocation(),
                    request.getParkingDuration(),
                    request.getManagerNickname()
            );
            
            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                log.info("✅ 违规停车告警通知发送成功 - 车牌: {}", request.getPlateNumber());
                return Result.success(result);
            } else {
                String message = result != null ? (String) result.get("message") : "发送失败";
                log.warn("⚠️ 违规停车告警通知发送失败 - 车牌: {}, 原因: {}", request.getPlateNumber(), message);
                return Result.error("发送失败: " + message);
            }
            
        } catch (Exception e) {
            log.error("❌ 发送违规停车告警通知异常 - 车牌: {}, 错误: {}", request.getPlateNumber(), e.getMessage(), e);
            return Result.error("发送异常: " + e.getMessage());
        }
    }
    
    /**
     * 发送预约车辆待审核提醒
     */
    @PostMapping("/send/booking-pending-notification")
    @ApiOperation("发送预约车辆待审核提醒")
    public Result<Map<String, Object>> sendBookingPendingNotification(@RequestBody BookingPendingNotificationRequest request) {
        log.info("📝 收到预约待审核提醒请求 - 车牌: {}, 停车场: {}, 预约人: {}, 管家: {}", 
                request.getPlateNumber(), request.getParkName(), request.getBookerName(), request.getManagerNickname());
        
        try {
            // 参数校验
            if (request.getPlateNumber() == null || request.getPlateNumber().trim().isEmpty()) {
                return Result.error("车牌号不能为空");
            }
            if (request.getParkName() == null || request.getParkName().trim().isEmpty()) {
                return Result.error("停车场名称不能为空");
            }
            if (request.getBookerName() == null || request.getBookerName().trim().isEmpty()) {
                return Result.error("预约人姓名不能为空");
            }
            if (request.getContactPhone() == null || request.getContactPhone().trim().isEmpty()) {
                return Result.error("联系电话不能为空");
            }
            if (request.getManagerNickname() == null || request.getManagerNickname().trim().isEmpty()) {
                return Result.error("管家昵称不能为空");
            }
            
            // 调用服务层发送通知
            Map<String, Object> result = weChatTemplateMessageService.sendBookingPendingNotification(
                    request.getPlateNumber(),
                    request.getParkName(),
                    request.getContactPhone(),
                    request.getManagerNickname()
            );
            
            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                log.info("✅ 预约待审核提醒发送成功 - 车牌: {}", request.getPlateNumber());
                return Result.success(result);
            } else {
                String message = result != null ? (String) result.get("message") : "发送失败";
                log.warn("⚠️ 预约待审核提醒发送失败 - 车牌: {}, 原因: {}", request.getPlateNumber(), message);
                return Result.error("发送失败: " + message);
            }
            
        } catch (Exception e) {
            log.error("❌ 发送预约待审核提醒异常 - 车牌: {}, 错误: {}", request.getPlateNumber(), e.getMessage(), e);
            return Result.error("发送异常: " + e.getMessage());
        }
    }
    
    /**
     * 违规停车告警通知请求参数
     */
    public static class ViolationNotificationRequest {
        private String plateNumber;      // 车牌号
        private String parkName;         // 停车场名称
        private String violationLocation; // 违规位置
        private String parkingDuration;   // 停车时长
        private String managerNickname;   // 管家昵称
        
        // Getters and Setters
        public String getPlateNumber() {
            return plateNumber;
        }
        
        public void setPlateNumber(String plateNumber) {
            this.plateNumber = plateNumber;
        }
        
        public String getParkName() {
            return parkName;
        }
        
        public void setParkName(String parkName) {
            this.parkName = parkName;
        }
        
        public String getViolationLocation() {
            return violationLocation;
        }
        
        public void setViolationLocation(String violationLocation) {
            this.violationLocation = violationLocation;
        }
        
        public String getParkingDuration() {
            return parkingDuration;
        }
        
        public void setParkingDuration(String parkingDuration) {
            this.parkingDuration = parkingDuration;
        }
        
        public String getManagerNickname() {
            return managerNickname;
        }
        
        public void setManagerNickname(String managerNickname) {
            this.managerNickname = managerNickname;
        }
    }
    
    /**
     * 预约待审核提醒请求参数
     */
    public static class BookingPendingNotificationRequest {
        private String plateNumber;      // 车牌号
        private String parkName;         // 停车场名称
        private String bookerName;       // 预约人姓名
        private String contactPhone;     // 联系电话
        private String managerNickname;  // 管家昵称
        
        // Getters and Setters
        public String getPlateNumber() {
            return plateNumber;
        }
        
        public void setPlateNumber(String plateNumber) {
            this.plateNumber = plateNumber;
        }
        
        public String getParkName() {
            return parkName;
        }
        
        public void setParkName(String parkName) {
            this.parkName = parkName;
        }
        
        public String getBookerName() {
            return bookerName;
        }
        
        public void setBookerName(String bookerName) {
            this.bookerName = bookerName;
        }
        
        public String getContactPhone() {
            return contactPhone;
        }
        
        public void setContactPhone(String contactPhone) {
            this.contactPhone = contactPhone;
        }
        
        public String getManagerNickname() {
            return managerNickname;
        }
        
        public void setManagerNickname(String managerNickname) {
            this.managerNickname = managerNickname;
        }
    }
} 