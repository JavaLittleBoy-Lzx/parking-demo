package com.parkingmanage.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.common.HttpClientUtil;
import com.parkingmanage.entity.UserMapping;
import com.parkingmanage.mapper.UserMappingMapper;
import com.parkingmanage.service.WeChatTemplateMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信模板消息服务实现类
 */
@Slf4j
@Service
public class WeChatTemplateMessageServiceImpl implements WeChatTemplateMessageService {
    
    private static final String TEMPLATE_MESSAGE_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=";
    
    // 微信公众号配置
    @Value("${wechat.public.appid:}")
    private String appId;
    
    @Value("${wechat.public.secret:}")
    private String secret;
    
    // 微信小程序配置
    @Value("${wechat.miniapp.appid:}")
    private String miniappAppId;
    
    // 模板ID - 这些需要在微信公众平台申请对应的模板
    @Value("${wechat.template.parking.enter:}")
    private String parkingEnterTemplateId;
    
    @Value("${wechat.template.parking.leave:}")
    private String parkingLeaveTemplateId;
    
    @Value("${wechat.template.parking.timeout}")
    private String parkingTimeoutTemplateId;
    // 车辆违规停车告警通知模板ID
    @Value("${wechat.template.parking.violation:}")
    private String parkingViolationTemplateId;
    // 预约车辆待审核提醒模板ID  
    @Value("${wechat.template.booking.pending:}")
    private String bookingPendingTemplateId;
    
    // 预约审核通过通知模板ID
    @Value("${wechat.template.appointment.approved:}")
    private String appointmentApprovedTemplateId;
    
    // 预约审核驳回通知模板ID  
    @Value("${wechat.template.appointment.rejected:}")
    private String appointmentRejectedTemplateId;
    
    // 预约成功通知模板ID
    @Value("${wechat.template.appointment.success:}")
    private String appointmentSuccessTemplateId;
    
    // 车辆加入黑名单成功通知模板ID
    @Value("${wechat.template.blacklist.add:VULfsba-5E9TXmEwZQZN57zg4rXh46xmka_wfoMpAnk}")
    private String blacklistAddTemplateId;
    
    // 车辆滞留通知模板ID
    @Value("${wechat.template.parking.retention:}")
    private String parkingRetentionTemplateId;
    
    @Resource
    private UserMappingMapper userMappingMapper;
    
    @Override
    public Map<String, Object> sendTemplateMessage(String openid, String templateId, Map<String, Object> data, String url) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取access_token
            String accessToken = getAccessToken();
            if (StringUtils.isEmpty(accessToken)) {
                log.error("❌ 获取access_token失败");
                result.put("success", false);
                result.put("message", "获取access_token失败");
                return result;
            }
            
            // 构建请求参数
            JSONObject requestBody = new JSONObject();
            requestBody.put("touser", openid);
            requestBody.put("template_id", templateId);
            if (!StringUtils.isEmpty(url)) {
                requestBody.put("url", url);
            }
            requestBody.put("data", data);
            
            // 发送请求
            String requestUrl = TEMPLATE_MESSAGE_URL + accessToken;
            String response = HttpClientUtil.doPostJson(requestUrl, requestBody.toJSONString());
            
            log.info("📤 发送模板消息 - openid: {}, templateId: {}, response: {}", openid, templateId, response);
            
            JSONObject responseJson = JSONObject.parseObject(response);
            if (responseJson.getInteger("errcode") == 0) {
                result.put("success", true);
                result.put("message", "发送成功");
                result.put("msgid", responseJson.getLong("msgid"));
            } else {
                result.put("success", false);
                result.put("message", "发送失败: " + responseJson.getString("errmsg"));
                result.put("errcode", responseJson.getInteger("errcode"));
            }
            
        } catch (Exception e) {
            log.error("❌ 发送模板消息异常", e);
            result.put("success", false);
            result.put("message", "发送异常: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> sendTemplateMessageWithMiniprogram(String openid, String templateId, 
            Map<String, Object> data, String miniprogramPath, String miniprogramAppid) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取access_token
            String accessToken = getAccessToken();
            if (StringUtils.isEmpty(accessToken)) {
                log.error("❌ 获取access_token失败");
                result.put("success", false);
                result.put("message", "获取access_token失败");
                return result;
            }
            
            // 构建请求参数
            JSONObject requestBody = new JSONObject();
            requestBody.put("touser", openid);
            requestBody.put("template_id", templateId);
            requestBody.put("data", data);
            
            // 添加小程序跳转配置
            if (!StringUtils.isEmpty(miniprogramPath)) {
                JSONObject miniprogram = new JSONObject();
                if (!StringUtils.isEmpty(miniprogramAppid)) {
                    miniprogram.put("appid", miniprogramAppid);
                }
                miniprogram.put("pagepath", miniprogramPath);
                requestBody.put("miniprogram", miniprogram);
                
                log.info("🎯 配置小程序跳转 - path: {}, appid: {}", miniprogramPath, miniprogramAppid);
            }
            
            // 发送请求
            String requestUrl = TEMPLATE_MESSAGE_URL + accessToken;
            String response = HttpClientUtil.doPostJson(requestUrl, requestBody.toJSONString());
            
            log.info("📤 发送模板消息(小程序跳转) - openid: {}, templateId: {}, response: {}", openid, templateId, response);
            
            JSONObject responseJson = JSONObject.parseObject(response);
            if (responseJson.getInteger("errcode") == 0) {
                result.put("success", true);
                result.put("message", "发送成功");
                result.put("msgid", responseJson.getLong("msgid"));
            } else {
                result.put("success", false);
                result.put("message", "发送失败: " + responseJson.getString("errmsg"));
                result.put("errcode", responseJson.getInteger("errcode"));
            }
            
        } catch (Exception e) {
            log.error("❌ 发送模板消息异常", e);
            result.put("success", false);
            result.put("message", "发送异常: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> sendParkingEnterNotification(String plateNumber, String parkName, String enterChannel, String enterTime, String managerNickname) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("🔔 开始发送停车进场通知 - 车牌: {}, 停车场: {}, 管家: {}", plateNumber, parkName, managerNickname);
            
            // 通过管家昵称获取openid
            String openid = getOpenidByNickname(managerNickname);
            if (!StringUtils.hasText(openid)) {
                result.put("success", false);
                result.put("message", "未找到管家对应的微信openid");
                return result;
            }
            
            // 构建模板消息数据 - 根据实际模板格式
            Map<String, Object> templateData = new HashMap<>();
            
            // 停车场
            Map<String, String> thing2 = new HashMap<>();
            thing2.put("value", parkName);
            templateData.put("thing2", thing2);
            
            // 通道名称
            Map<String, String> thing21 = new HashMap<>();
            thing21.put("value", enterChannel);
            templateData.put("thing21", thing21);
            
            // 车牌号
            Map<String, String> carNumber1 = new HashMap<>();
            carNumber1.put("value", plateNumber);
            templateData.put("car_number1", carNumber1);
            
            // 入场时间
            Map<String, String> time4 = new HashMap<>();
            time4.put("value", enterTime);
            templateData.put("time4", time4);
            
            // 发送模板消息
            return sendTemplateMessage(openid, parkingEnterTemplateId, templateData, null);
            
        } catch (Exception e) {
            log.error("❌ 发送进场通知异常", e);
            result.put("success", false);
            result.put("message", "发送异常: " + e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> sendParkingLeaveNotification(String plateNumber, String parkName, String leaveTime, String enterTime, String managerNickname,String leaveChannel) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("🔔 开始发送停车离场通知 - 车牌: {}, 停车场: {}, 管家: {}", plateNumber, parkName, managerNickname);
            
            // 通过管家昵称获取openid
            String openid = getOpenidByNickname(managerNickname);
            if (!StringUtils.hasText(openid)) {
                result.put("success", false);
                result.put("message", "未找到管家对应的微信openid");
                return result;
            }
            
            // 查询进场时间和计算停车时长
            String parkingDuration = "";
            
            try {
                // 格式化enterTime, leaveTime
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime arriveDateTime = LocalDateTime.parse(enterTime, formatter);
                LocalDateTime leaveDateTime = LocalDateTime.parse(leaveTime, formatter);
                // 计算停车时长
                Duration duration = Duration.between(arriveDateTime,leaveDateTime);
                long hours = Math.abs(duration.toHours());
                long minutes = Math.abs(duration.toMinutes() % 60);
                parkingDuration = hours + "小时" + minutes + "分钟";
                
            } catch (Exception e) {
                log.warn("⚠️ 计算停车时长失败，使用默认值", e);
                enterTime = leaveTime;
                parkingDuration = "未知";
            }
            
            // 构建模板消息数据 - 根据实际模板格式
            Map<String, Object> templateData = new HashMap<>();
            
            // 停车场
            Map<String, String> thing2 = new HashMap<>();
            thing2.put("value", parkName);
            templateData.put("thing2", thing2);
            
            // 车牌号
            Map<String, String> carNumber1 = new HashMap<>();
            carNumber1.put("value", plateNumber);
            templateData.put("car_number1", carNumber1);
            
            // 通道名称
            Map<String, String> thing29 = new HashMap<>();
            thing29.put("value", leaveChannel);
            templateData.put("thing29", thing29);
            
            // 出场时间
            Map<String, String> time4 = new HashMap<>();
            time4.put("value", leaveTime);
            templateData.put("time4", time4);
            
            // 停车时长
            Map<String, String> thing9 = new HashMap<>();
            thing9.put("value", parkingDuration);
            templateData.put("thing9", thing9);
            // 发送模板消息
            return sendTemplateMessage(openid, parkingLeaveTemplateId, templateData, null);
            
        } catch (Exception e) {
            log.error("❌ 发送离场通知异常", e);
            result.put("success", false);
            result.put("message", "发送异常: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 通过昵称获取openid
     */
    private String getOpenidByNickname(String nickname) {
        if (StringUtils.isEmpty(nickname)) {
            return null;
        }
        
        try {
            List<UserMapping> userMappings = userMappingMapper.findByNickname(nickname);
            if (userMappings != null && !userMappings.isEmpty()) {
                return userMappings.get(0).getOpenid();
            }
        } catch (Exception e) {
            log.error("❌ 根据昵称查询openid异常 - nickname: {}", nickname, e);
        }
        
        return null;
    }
    
    /**
     * 获取微信access_token
     */
    private String getAccessToken() {
        try {
            if (StringUtils.isEmpty(appId) || StringUtils.isEmpty(secret)) {
                log.error("❌ 微信公众号配置不完整 - appId: {}, secret: {}", appId, StringUtils.isEmpty(secret) ? "未配置" : "已配置");
                return null;
            }
            
            // 构建请求参数
            Map<String, String> params = new HashMap<>();
            params.put("grant_type", "client_credential");
            params.put("appid", appId);
            params.put("secret", secret);
            
            // 调用微信API
            String url = "https://api.weixin.qq.com/cgi-bin/token";
            String response = HttpClientUtil.doGet(url, params);
            
            log.info("📥 获取access_token响应: {}", response);
            
            // 解析响应
            JSONObject jsonResponse = JSONObject.parseObject(response);
            
            if (jsonResponse.containsKey("access_token")) {
                return jsonResponse.getString("access_token");
            } else {
                Integer errcode = jsonResponse.getInteger("errcode");
                String errmsg = jsonResponse.getString("errmsg");
                log.error("❌ 获取access_token失败 - 错误码: {}, 错误信息: {}", errcode, errmsg);
                return null;
            }
            
        } catch (Exception e) {
            log.error("❌ 获取access_token异常", e);
            return null;
        }
    }
    
    /**
     * 给管家发送停车超时通知
     */
    @Override
    public Map<String, Object> sendParkingTimeoutNotification(String plateNumber, String parkName, 
        String enterTime, String managerNickname, long overtimeMinutes) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("🔔 开始发送停车超时通知 - 车牌: {}, 停车场: {}, 超时: {}分钟", 
                plateNumber, parkName, overtimeMinutes);
            
            // 获取openid
            String openid = getOpenidByNickname(managerNickname);
            if (!StringUtils.hasText(openid)) {
                result.put("success", false);
                result.put("message", "未找到管家对应的微信openid");
                return result;
            }
            
            // 格式化超时时长
            long hours = overtimeMinutes / 60;
            long minutes = overtimeMinutes % 60;
            String overtimeText = hours > 0 ? hours + "小时" + minutes + "分钟" : minutes + "分钟";
            
            // 构建模板消息数据
            Map<String, Object> templateData = new HashMap<>();
            
            // 车牌号
            Map<String, String> carNumber = new HashMap<>();
            carNumber.put("value", plateNumber);
            templateData.put("car_number1", carNumber);
            
            // 停车场
            Map<String, String> parkingLot = new HashMap<>();
            parkingLot.put("value", parkName);
            templateData.put("thing2", parkingLot);
            
            // 进场时间
            Map<String, String> enterTimeMap = new HashMap<>();
            enterTimeMap.put("value", enterTime);
            templateData.put("time4", enterTimeMap);
            
            // 超时时长
            Map<String, String> overtimeMap = new HashMap<>();
            overtimeMap.put("value", overtimeText);
            templateData.put("thing9", overtimeMap);
            
            // 温馨提示
            Map<String, String> remarkMap = new HashMap<>();
            remarkMap.put("value", "请及时联系车主处理，避免影响其他车辆停车");
            templateData.put("thing10", remarkMap);
            
            // 发送模板消息（使用进场通知的模板ID，或者配置新的超时模板ID）
            return sendTemplateMessage(openid, parkingEnterTemplateId, templateData, null);
            
        } catch (Exception e) {
            log.error("❌ 发送超时通知异常", e);
            result.put("success", false);
            result.put("message", "发送异常: " + e.getMessage());
            return result;
        }
    }
    
    @Override
    public Map<String, Object> sendParkingAlmostTimeoutNotification(String openid, String plateNumber, 
        String parkName, String enterTime, long remainingMinutes) {
        
        Map<String, Object> result = new HashMap<>();
        
        // 🔍 添加详细日志诊断进场时间问题
        log.info("🔍 [超时通知] 接收参数 - 车牌: {}, 进场时间: {}, 剩余分钟: {}, openid: {}", 
            plateNumber, enterTime, remainingMinutes, openid);
        
        try {
            log.info(" 检查即将超时提醒 - 车牌: {}, 停车场: {}, 剩余: {}分钟", 
                plateNumber, parkName, remainingMinutes);
            
            if (!StringUtils.hasText(openid)) {
                result.put("success", false);
                result.put("message", "访客微信openid为空");
                return result;
            }
            
            // 只有在15分钟、5分钟、1分钟时才发送通知，避免频繁调用微信接口
            String remainingText;
            if (remainingMinutes == 30) {
                remainingText = "30分钟";
            } else if (remainingMinutes == 15) {
                remainingText = "15分钟";
            } else {
                // 其他时间点不发送通知
                log.debug(" 剩余时长{}分钟，不在发送范围内(15/5/1分钟)，跳过发送", remainingMinutes);
                result.put("success", true);
                result.put("message", "剩余时长不在发送范围内，跳过发送");
                return result;
            }
            
            log.info(" 开始发送即将超时提醒 - 车牌: {}, 停车场: {}, 剩余: {}", 
                plateNumber, parkName, remainingText);
            
            // 构建模板消息数据 - 根据您提供的模板格式
            Map<String, Object> templateData = new HashMap<>();
            
            // 停车场名称 -> thing2.DATA
            Map<String, String> parkingLot = new HashMap<>();
            String shortParkName = parkName.length() > 20 ? parkName.substring(0, 17) + "..." : parkName;
            parkingLot.put("value", shortParkName);
            templateData.put("thing2", parkingLot);
            
            // 车牌号 -> car_number5.DATA  
            Map<String, String> carNumber = new HashMap<>();
            carNumber.put("value", plateNumber);
            templateData.put("car_number5", carNumber);
            
            // 入场时间 -> time9.DATA
            Map<String, String> enterTimeMap = new HashMap<>();
            enterTimeMap.put("value", enterTime);
            templateData.put("time9", enterTimeMap);
            
            // 剩余时长 -> const13.DATA（纯数字格式）
            Map<String, String> remainingTimeMap = new HashMap<>();
            System.out.println("remainingTimeMap = " + remainingTimeMap);
            remainingTimeMap.put("value", remainingText);
            templateData.put("const13", remainingTimeMap);
            
            log.info(" 发送即将超时提醒模板数据: {}", templateData);
            
            // 构建小程序跳转路径：跳转到预约查询页面，带上车牌号和进场时间参数
            // 参数说明：keyword=车牌号&enterTime=进场时间&autoExpand=true（自动展开该记录）
            try {
                String miniprogramPath = String.format(
                    "pagesA/reservation/dataList/dataList?keyword=%s&enterTime=%s&autoExpand=true",
                    java.net.URLEncoder.encode(plateNumber, "UTF-8"),
                    java.net.URLEncoder.encode(enterTime, "UTF-8")
                );
                
                log.info(" 配置小程序跳转路径: {}", miniprogramPath);
                
                // 使用支持小程序跳转的方法发送消息（传入小程序appid）
                return sendTemplateMessageWithMiniprogram(openid, parkingTimeoutTemplateId, templateData, miniprogramPath, miniappAppId);
                
            } catch (Exception e) {
                log.warn(" 构建小程序跳转路径失败，降级为普通消息: {}", e.getMessage());
                // 降级：如果构建跳转路径失败，则发送不带跳转的普通消息
                return sendTemplateMessage(openid, parkingTimeoutTemplateId, templateData, null);
            }
            
        } catch (Exception e) {
            log.error(" 发送即将超时提醒异常", e);
            result.put("success", false);
            result.put("message", "发送异常: " + e.getMessage());
            return result;
        }
    }

    /**
     * 创建模板数据项
     */
    private Map<String, String> createTemplateValue(String value) {
        Map<String, String> item = new HashMap<>();
        item.put("value", value);
        return item;
    }
    
    /**
     * 将停车时长转换为 HH:mm:ss 格式
     * @param parkingDuration 停车时长，如 "2小时30分钟45秒" 或 "30分钟15秒" 或 "45秒" 或直接的时间格式
     * @return HH:mm:ss 格式的时间字符串
     */
    private String formatDurationToTimeString(String parkingDuration) {
        if (StringUtils.isEmpty(parkingDuration)) {
            return "00:00:00";
        }
        
        try {
            // 如果已经是 HH:mm:ss 格式，直接返回
            if (parkingDuration.matches("\\d{2}:\\d{2}:\\d{2}")) {
                return parkingDuration;
            }
            
            // 解析中文格式的时长，如 "2小时30分钟45秒" 或 "30分钟15秒" 或 "45秒"
            int hours = 0;
            int minutes = 0;
            int seconds = 0;
            
            // 提取小时
            if (parkingDuration.contains("小时")) {
                String hourStr = parkingDuration.substring(0, parkingDuration.indexOf("小时"));
                hours = Integer.parseInt(hourStr);
            }
            
            // 提取分钟
            if (parkingDuration.contains("分钟")) {
                String minuteStr;
                if (parkingDuration.contains("小时")) {
                    minuteStr = parkingDuration.substring(parkingDuration.indexOf("小时") + 2, parkingDuration.indexOf("分钟"));
                } else {
                    minuteStr = parkingDuration.substring(0, parkingDuration.indexOf("分钟"));
                }
                minutes = Integer.parseInt(minuteStr);
            }
            
            // 提取秒数
            if (parkingDuration.contains("秒")) {
                String secondStr;
                if (parkingDuration.contains("分钟")) {
                    secondStr = parkingDuration.substring(parkingDuration.indexOf("分钟") + 2, parkingDuration.indexOf("秒"));
                } else if (parkingDuration.contains("小时")) {
                    secondStr = parkingDuration.substring(parkingDuration.indexOf("小时") + 2, parkingDuration.indexOf("秒"));
                } else {
                    secondStr = parkingDuration.substring(0, parkingDuration.indexOf("秒"));
                }
                seconds = Integer.parseInt(secondStr);
            }
            
            // 格式化为 HH:mm:ss
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
            
        } catch (Exception e) {
            log.warn("⚠️ 格式化停车时长失败: {}, 使用默认值", parkingDuration, e);
            return "00:00:00";
        }
    }
    
    @Override
    public Map<String, Object> sendParkingViolationNotification(String plateNumber, String parkName, 
        String violationLocation, String parkingDuration, String openid) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("🚨 开始发送车辆违规停车告警通知 - 车牌: {}, 停车场: {}, openid: {}", 
                plateNumber, parkName, openid);
            
            // 直接使用传入的openid
            if (!StringUtils.hasText(openid)) {
                result.put("success", false);
                result.put("message", "openid为空，无法发送通知");
                return result;
            }
            
            // 构建模板消息数据 - 根据图2所示格式
            Map<String, Object> templateData = new HashMap<>();
            
            // 停车场 -> thing3.DATA
            Map<String, String> parkingLot = new HashMap<>();
            parkingLot.put("value", parkName);
            templateData.put("thing3", parkingLot);
            
            // 车牌号 -> car_number1.DATA
            Map<String, String> carNumber = new HashMap<>();
            carNumber.put("value", plateNumber);
            templateData.put("car_number1", carNumber);
            
            // 发生地点 -> thing6.DATA
            Map<String, String> location = new HashMap<>();
            location.put("value", violationLocation);
            templateData.put("thing6", location);
            
            // 停车时长 -> time7.DATA (格式: HH:mm:ss)
            Map<String, String> duration = new HashMap<>();
            // 将停车时长转换为 HH:mm:ss 格式
            String formattedDuration = formatDurationToTimeString(parkingDuration);
            System.out.println("formattedDuration = " + formattedDuration);
            duration.put("value", formattedDuration);
            templateData.put("time7", duration);
            
            log.info("📤 发送违规停车告警模板数据: {}", templateData);
            
            // 发送模板消息
            return sendTemplateMessage(openid, parkingViolationTemplateId, templateData, null);
            
        } catch (Exception e) {
            log.error("❌ 发送违规停车告警通知异常", e);
            result.put("success", false);
            result.put("message", "发送异常: " + e.getMessage());
            return result;
        }
    }
    
    @Override
    public Map<String, Object> sendBookingPendingNotification(String plateNumber, String parkName,String contactPhone, String managerNickname) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("📝 开始发送预约车辆待审核提醒 - 车牌: {}, 停车场: {}, 管家: {}",
                plateNumber, parkName, managerNickname);
            
            // 通过管家昵称获取openid
            String openid = getOpenidByNickname(managerNickname);
            if (!StringUtils.hasText(openid)) {
                result.put("success", false);
                result.put("message", "未找到管家对应的微信openid");
                return result;
            }
            
            // 构建模板消息数据 - 根据图3所示格式
            Map<String, Object> templateData = new HashMap<>();
            
            // 停车场 -> thing1.DATA
            Map<String, String> parkingLot = new HashMap<>();
            parkingLot.put("value", parkName);
            templateData.put("thing1", parkingLot);
            
            // 车牌号 -> car_number2.DATA
            Map<String, String> carNumber = new HashMap<>();
            carNumber.put("value", plateNumber);
            templateData.put("car_number2", carNumber);
            
            // 联系电话 -> phone_number8.DATA
            Map<String, String> phone = new HashMap<>();
            phone.put("value", contactPhone);
            templateData.put("phone_number8", phone);
            
            log.info("📤 发送预约待审核提醒模板数据: {}", templateData);
            
            // 发送模板消息
            return sendTemplateMessage(openid, bookingPendingTemplateId, templateData, null);
            
        } catch (Exception e) {
            log.error("❌ 发送预约待审核提醒异常", e);
            result.put("success", false);
            result.put("message", "发送异常: " + e.getMessage());
            return result;
        }
    }

    /**
     * 🔔 发送预约审核结果通知给访客
     * @param plateNumber 车牌号
     * @param parkName 停车场名称  
     * @param auditResult 审核结果（已通过/未通过）
     * @param auditReason 审核备注（通过时为空，驳回时为驳回原因）
     * @param appointmentTime 预约时间
     * @param visitorName 访客姓名（用于查询openid）
     * @param managerName 审核管家姓名
     * @return 发送结果
     */
    @Override
    public Map<String, Object> sendAppointmentAuditResultNotification(String plateNumber, String parkName,
            String auditResult, String auditReason, String appointmentTime, String visitorName, String managerName) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("🔔 开始发送预约审核结果通知 - 车牌: {}, 停车场: {}, 审核结果: {}, 访客: {}", 
                plateNumber, parkName, auditResult, visitorName);
            
            // 通过访客姓名获取openid
            String visitorOpenid = getOpenidByVisitorName(visitorName);
            if (!StringUtils.hasText(visitorOpenid)) {
                result.put("success", false);
                result.put("message", "未找到访客对应的微信openid");
                log.warn("⚠️ 未找到访客微信openid - 访客姓名: {}", visitorName);
                return result;
            }
            
            // 构建模板消息数据 - 根据预约结果模板格式
            Map<String, Object> templateData = new HashMap<>();
            
            // 园区名称 -> thing1.DATA
            Map<String, String> parkingLot = new HashMap<>();
            parkingLot.put("value", parkName);
            templateData.put("thing1", parkingLot);
            
            // 车牌号 -> car_number2.DATA
            Map<String, String> carNumber = new HashMap<>();
            carNumber.put("value", plateNumber);
            templateData.put("car_number2", carNumber);
            
            // 审核结果 -> thing3.DATA
            Map<String, String> auditStatus = new HashMap<>();
            auditStatus.put("value", auditResult);
            templateData.put("const4", auditStatus);
            
            log.info("📤 发送预约审核结果通知模板数据: {}", templateData);
            
            // 发送模板消息 - 使用预约审核结果通知模板
            String templateId = appointmentApprovedTemplateId; // 使用统一的预约结果通知模板
            if (!StringUtils.hasText(templateId)) {
                // 如果没有配置专门的模板ID，使用预约待审核的模板ID作为备用
                templateId = bookingPendingTemplateId;
            }
            
            return sendTemplateMessage(visitorOpenid, templateId, templateData, null);
            
        } catch (Exception e) {
            log.error("❌ 发送预约审核结果通知异常", e);
            result.put("success", false);
            result.put("message", "发送异常: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 通过访客姓名获取微信openid
     * @param visitorName 访客姓名
     * @return 微信openid，如果未找到则返回null
     */
    private String getOpenidByVisitorName(String visitorName) {
        if (StringUtils.isEmpty(visitorName)) {
            return null;
        }
        
        try {
            // 方案1：通过user_mapping表的nickname字段查询
            List<UserMapping> userMappings = userMappingMapper.findByNickname(visitorName);
            if (userMappings != null && !userMappings.isEmpty()) {
                log.info("✅ 通过访客姓名在user_mapping表中找到openid - 访客: {}, openid: {}", 
                    visitorName, userMappings.get(0).getOpenid());
                return userMappings.get(0).getOpenid();
            }
            
            // 方案2：如果user_mapping中没找到，可以尝试其他方式
            // 例如通过appointment表中的openid字段
            log.warn("⚠️ 在user_mapping表中未找到访客 {} 的记录", visitorName);
            
        } catch (Exception e) {
            log.error("❌ 根据访客姓名查询openid异常 - visitorName: {}", visitorName, e);
        }
        
        return null;
    }
    
    @Override
    public Map<String, Object> sendAppointmentSuccessNotification(String openid, String plateNumbers,
            String parkName, String appointmentTime, String ownerName) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("✅ 开始发送车辆预约成功通知 - 车牌: {}, 停车场: {}, 车主姓名: {}",
                plateNumbers, parkName, ownerName);
            
            if (!StringUtils.hasText(openid)) {
                result.put("success", false);
                result.put("message", "接收者openid为空");
                log.warn("⚠️ 发送预约成功通知失败 - openid为空");
                return result;
            }
            
            // 处理车牌号列表 - 支持多个车牌
            String displayPlateNumbers = plateNumbers;
            // 如果车牌号过长（超过20个字符），需要截断
            if (plateNumbers.length() > 20) {
                displayPlateNumbers = plateNumbers.substring(0, 17) + "...";
            }
            
            // 构建模板消息数据 - 按照模板要求的字段映射
            Map<String, Object> templateData = new HashMap<>();
            
            // 停车场 -> thing8.DATA
            Map<String, String> parkingLot = new HashMap<>();
            String shortParkName = parkName.length() > 20 ? parkName.substring(0, 17) + "..." : parkName;
            parkingLot.put("value", shortParkName);
            templateData.put("thing8", parkingLot);
            
            // 车主姓名 -> thing10.DATA
            Map<String, String> ownerNameMap = new HashMap<>();
            String shortOwnerName = ownerName.length() > 20 ? ownerName.substring(0, 17) + "..." : ownerName;
            ownerNameMap.put("value", shortOwnerName);
            templateData.put("thing10", ownerNameMap);
            
            // 车牌号 -> car_number9.DATA
            Map<String, String> carNumber = new HashMap<>();
            carNumber.put("value", displayPlateNumbers);
            templateData.put("car_number9", carNumber);
            
            // 预约时间 -> time2.DATA
            Map<String, String> appointTime = new HashMap<>();
            appointTime.put("value", appointmentTime);
            templateData.put("time2", appointTime);
            
            log.info("📤 发送预约成功通知模板数据: {}", templateData);
            
            // 发送模板消息
            return sendTemplateMessage(openid, appointmentSuccessTemplateId, templateData, null);
            
        } catch (Exception e) {
            log.error("❌ 发送预约成功通知异常", e);
            result.put("success", false);
            result.put("message", "发送异常: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 通过手机号获取微信openid
     * @param phone 手机号
     * @return 微信openid，如果未找到则返回null
     */
    private String getOpenidByPhone(String phone) {
        if (StringUtils.isEmpty(phone)) {
            return null;
        }
        
        try {
            List<UserMapping> userMappings = userMappingMapper.findByPhone(phone);
            if (userMappings != null && !userMappings.isEmpty()) {
                log.info("✅ 通过手机号在user_mapping表中找到openid - 手机号: {}, openid: {}", 
                    phone, userMappings.get(0).getOpenid());
                return userMappings.get(0).getOpenid();
            }
            
            log.warn("⚠️ 在user_mapping表中未找到手机号 {} 的记录", phone);
            
        } catch (Exception e) {
            log.error("❌ 根据手机号查询openid异常 - phone: {}", phone, e);
        }
        
        return null;
    }
    
    @Override
    public Map<String, Object> sendBlacklistAddNotification(String openid, String plateNumber, 
            String parkName, String enterTime, String parkingDuration, Integer blacklistDays) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("🚨 开始发送车辆加入黑名单成功通知 - 车牌: {}, 停车场: {}, openid: {}", 
                plateNumber, parkName, openid);
            
            // 直接使用传入的openid
            if (!StringUtils.hasText(openid)) {
                result.put("success", false);
                result.put("message", "openid为空，无法发送通知");
                return result;
            }
            
            // 构建模板消息数据 - 根据图片所示格式
            Map<String, Object> templateData = new HashMap<>();
            
            // 停车场 -> thing1.DATA
            Map<String, String> thing1 = new HashMap<>();
            thing1.put("value", parkName);
            templateData.put("thing1", thing1);
            
            // 车牌号码 -> car_number2.DATA
            Map<String, String> carNumber2 = new HashMap<>();
            carNumber2.put("value", plateNumber);
            templateData.put("car_number2", carNumber2);
            
            // 进场时间 -> time3.DATA
            Map<String, String> time3 = new HashMap<>();
            time3.put("value", enterTime);
            templateData.put("time3", time3);
            
            // 停车时长 -> time4.DATA
            Map<String, String> time4 = new HashMap<>();
            time4.put("value", parkingDuration);
            templateData.put("time4", time4);
            
            // 拉黑天数 -> character_string5.DATA
            Map<String, String> characterString5 = new HashMap<>();
            characterString5.put("value", String.valueOf(blacklistDays));
            templateData.put("character_string5", characterString5);
            
            log.info("📤 发送黑名单通知模板数据: {}", templateData);
            
            // 发送模板消息
            return sendTemplateMessage(openid, blacklistAddTemplateId, templateData, null);
            
        } catch (Exception e) {
            log.error("❌ 发送黑名单通知异常", e);
            result.put("success", false);
            result.put("message", "发送异常: " + e.getMessage());
            return result;
        }
    }
    
    @Override
    public Map<String, Object> sendParkingRetentionNotification(String openid, String plateNumber, 
            String parkName, String enterTime, String retentionTime) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("🚗 开始发送车辆滞留通知 - 车牌: {}, 停车场: {}, 滞留时长: {}", 
                plateNumber, parkName, retentionTime);
            
            if (!StringUtils.hasText(openid)) {
                result.put("success", false);
                result.put("message", "openid为空，无法发送通知");
                return result;
            }
            
            // 构建模板消息数据 - 根据图片所示模板格式
            Map<String, Object> templateData = new HashMap<>();
            
            // 停车场 -> thing1.DATA
            Map<String, String> thing1 = new HashMap<>();
            String shortParkName = parkName.length() > 20 ? parkName.substring(0, 17) + "..." : parkName;
            thing1.put("value", shortParkName);
            templateData.put("thing1", thing1);
            
            // 车牌号 -> car_number3.DATA
            Map<String, String> carNumber3 = new HashMap<>();
            carNumber3.put("value", plateNumber);
            templateData.put("car_number3", carNumber3);
            
            // 入场时间 -> time9.DATA
            Map<String, String> time9 = new HashMap<>();
            time9.put("value", enterTime);
            templateData.put("time9", time9);
            
            // 滞留时间 -> time4.DATA
            Map<String, String> time4 = new HashMap<>();
            // 格式化滞留时间为HH:mm:ss格式
            String formattedRetentionTime = formatRetentionTimeToTimeString(retentionTime);
            time4.put("value", formattedRetentionTime);
            templateData.put("time4", time4);
            
            log.info("📤 发送滞留通知模板数据: {}", templateData);
            
            // 发送模板消息
            return sendTemplateMessage(openid, parkingRetentionTemplateId, templateData, null);
            
        } catch (Exception e) {
            log.error("❌ 发送滞留通知异常", e);
            result.put("success", false);
            result.put("message", "发送异常: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 将滞留时间转换为HH:mm:ss格式
     * @param retentionTime 滞留时间，如"30分钟"、"1小时"或已经是"01:00:00"格式
     * @return HH:mm:ss格式的时间字符串
     */
    private String formatRetentionTimeToTimeString(String retentionTime) {
        if (StringUtils.isEmpty(retentionTime)) {
            return "00:00:00";
        }
        
        try {
            // 如果已经是HH:mm:ss格式，直接返回
            if (retentionTime.matches("\\d{2}:\\d{2}:\\d{2}")) {
                return retentionTime;
            }
            
            // 解析中文格式，如"30分钟"、"1小时"、"1小时30分钟"
            int hours = 0;
            int minutes = 0;
            
            if (retentionTime.contains("小时")) {
                String hourStr = retentionTime.substring(0, retentionTime.indexOf("小时"));
                hours = Integer.parseInt(hourStr.trim());
            }
            
            if (retentionTime.contains("分钟")) {
                String minuteStr;
                if (retentionTime.contains("小时")) {
                    minuteStr = retentionTime.substring(retentionTime.indexOf("小时") + 2, retentionTime.indexOf("分钟"));
                } else {
                    minuteStr = retentionTime.substring(0, retentionTime.indexOf("分钟"));
                }
                minutes = Integer.parseInt(minuteStr.trim());
            }
            
            // 格式化为HH:mm:ss
            return String.format("%02d:%02d:00", hours, minutes);
            
        } catch (Exception e) {
            log.warn("⚠️ 格式化滞留时间失败: {}, 使用默认值", retentionTime, e);
            return "00:00:00";
        }
    }
}