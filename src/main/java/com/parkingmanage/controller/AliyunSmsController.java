package com.parkingmanage.controller;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponse;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.ViolationReminder;
import com.parkingmanage.service.ViolationReminderService;
import darabonba.core.client.ClientOverrideConfiguration;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * 阿里云短信发送控制器
 * 
 * @author parking-system
 * @since 2024
 */
@RestController
@RequestMapping("/parking/sms")
@Api(tags = "阿里云短信服务")
public class AliyunSmsController {
    
    private static final Logger logger = LoggerFactory.getLogger(AliyunSmsController.class);
    
    // 从配置文件读取阿里云配置信息
    @Value("${aliyun.sms.accessKeyId:your_access_key_id}")
    private String accessKeyId;
    
    @Value("${aliyun.sms.accessKeySecret:your_access_key_secret}")
    private String accessKeySecret;
    
    @Value("${aliyun.sms.signName:停车管理系统}")
    private String signName;
    
    @Value("${aliyun.sms.templateCode:SMS_000000}")
    private String templateCode;
    
    @Value("${aliyun.sms.endpoint:dysmsapi.aliyuncs.com}")
    private String endpoint;
    
    @Autowired
    private ViolationReminderService violationReminderService;
    
    /**
     * 创建阿里云短信客户端
     */
    private AsyncClient createClient() {
        // Configure Credentials authentication information, including ak, secret, token
        StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .build());

        // Configure the Client
        return AsyncClient.builder()
                .credentialsProvider(provider)
                .overrideConfiguration(
                        ClientOverrideConfiguration.create()
                                .setEndpointOverride(endpoint)
                )
                .build();
    }
    
    /**
     * 发送短信验证码
     */
    @ApiOperation("发送短信验证码")
    @PostMapping("/sendVerificationCode")
    public ResponseEntity<Result> sendVerificationCode(
            @ApiParam(value = "手机号", required = true) @RequestParam String phoneNumber) {
        
        logger.info("📱 开始发送短信验证码，手机号: [{}]", phoneNumber);
        
        AsyncClient client = null;
        try {
            // 参数验证
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return ResponseEntity.ok(Result.error("400", "手机号不能为空"));
            }
            
            // 手机号格式验证
            if (!phoneNumber.matches("^1[3-9]\\d{9}$")) {
                return ResponseEntity.ok(Result.error("400", "手机号格式不正确"));
            }
            
            // 生成6位随机验证码
            String verificationCode = generateVerificationCode();
            
            // 创建阿里云客户端
            client = createClient();
            
            // 构建短信发送请求
            SendSmsRequest sendSmsRequest = SendSmsRequest.builder()
                    .phoneNumbers(phoneNumber)
                    .signName(signName)
                    .templateCode(templateCode)
                    .templateParam("{\"code\":\"" + verificationCode + "\"}")
                    .build();
            
            // 同步发送短信
            CompletableFuture<SendSmsResponse> futureResponse = client.sendSms(sendSmsRequest);
            SendSmsResponse response = futureResponse.get();
            
            Map<String, Object> result = new HashMap<>();
            result.put("phoneNumber", phoneNumber);
            result.put("verificationCode", verificationCode); // 测试环境返回验证码，生产环境应该移除
            result.put("bizId", response.getBody().getBizId());
            result.put("requestId", response.getBody().getRequestId());
            result.put("code", response.getBody().getCode());
            result.put("message", response.getBody().getMessage());
            result.put("sendTime", new java.util.Date());
            
            if ("OK".equals(response.getBody().getCode())) {
                logger.info("✅ 短信验证码发送成功，手机号: [{}], 验证码: [{}]", phoneNumber, verificationCode);
                return ResponseEntity.ok(Result.success(result));
            } else {
                logger.error("❌ 短信验证码发送失败，手机号: [{}], 错误码: [{}], 错误信息: [{}]", 
                    phoneNumber, response.getBody().getCode(), response.getBody().getMessage());
                return ResponseEntity.ok(Result.error(response.getBody().getCode(), 
                    "短信发送失败: " + response.getBody().getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("❌ 短信验证码发送异常，手机号: [{}]", phoneNumber, e);
            return ResponseEntity.ok(Result.error("500", "短信发送异常: " + e.getMessage()));
        } finally {
            // 关闭客户端
            if (client != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    logger.warn("关闭阿里云客户端异常", e);
                }
            }
        }
    }
    
    /**
     * 发送自定义短信
     */
    @ApiOperation("发送自定义短信")
    @PostMapping("/sendCustomMessage")
    public ResponseEntity<Result> sendCustomMessage(
            @ApiParam(value = "手机号", required = true) @RequestParam String phoneNumber,
            @ApiParam(value = "短信签名", required = false) @RequestParam(required = false) String customSignName,
            @ApiParam(value = "模板代码", required = true) @RequestParam String customTemplateCode,
            @ApiParam(value = "模板参数JSON格式", required = false) @RequestParam(required = false) String templateParam) {
        
        logger.info("📱 开始发送自定义短信，手机号: [{}], 模板代码: [{}]", phoneNumber, customTemplateCode);
        
        AsyncClient client = null;
        try {
            // 参数验证
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return ResponseEntity.ok(Result.error("400", "手机号不能为空"));
            }
            
            if (customTemplateCode == null || customTemplateCode.trim().isEmpty()) {
                return ResponseEntity.ok(Result.error("400", "模板代码不能为空"));
            }
            
            // 手机号格式验证
            if (!phoneNumber.matches("^1[3-9]\\d{9}$")) {
                return ResponseEntity.ok(Result.error("400", "手机号格式不正确"));
            }
            
            // 创建阿里云客户端
            client = createClient();
            
            // 构建短信发送请求
            SendSmsRequest.Builder requestBuilder = SendSmsRequest.builder()
                    .phoneNumbers(phoneNumber)
                    .signName(customSignName != null ? customSignName : signName)
                    .templateCode(customTemplateCode);
            
            if (templateParam != null && !templateParam.trim().isEmpty()) {
                requestBuilder.templateParam(templateParam);
            }
            
            SendSmsRequest sendSmsRequest = requestBuilder.build();
            
            // 同步发送短信
            CompletableFuture<SendSmsResponse> futureResponse = client.sendSms(sendSmsRequest);
            SendSmsResponse response = futureResponse.get();
            
            Map<String, Object> result = new HashMap<>();
            result.put("phoneNumber", phoneNumber);
            result.put("signName", customSignName != null ? customSignName : signName);
            result.put("templateCode", customTemplateCode);
            result.put("templateParam", templateParam);
            result.put("bizId", response.getBody().getBizId());
            result.put("requestId", response.getBody().getRequestId());
            result.put("code", response.getBody().getCode());
            result.put("message", response.getBody().getMessage());
            result.put("sendTime", new java.util.Date());
            
            if ("OK".equals(response.getBody().getCode())) {
                logger.info("✅ 自定义短信发送成功，手机号: [{}], 模板代码: [{}]", phoneNumber, customTemplateCode);
                return ResponseEntity.ok(Result.success(result));
            } else {
                logger.error("❌ 自定义短信发送失败，手机号: [{}], 错误码: [{}], 错误信息: [{}]", 
                    phoneNumber, response.getBody().getCode(), response.getBody().getMessage());
                return ResponseEntity.ok(Result.error(response.getBody().getCode(), 
                    "短信发送失败: " + response.getBody().getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("❌ 自定义短信发送异常，手机号: [{}]", phoneNumber, e);
            return ResponseEntity.ok(Result.error("500", "短信发送异常: " + e.getMessage()));
        } finally {
            // 关闭客户端
            if (client != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    logger.warn("关闭阿里云客户端异常", e);
                }
            }
        }
    }
    
    /**
     * 批量发送短信
     */
    @ApiOperation("批量发送短信")
    @PostMapping("/sendBatchMessage")
    public ResponseEntity<Result> sendBatchMessage(
            @ApiParam(value = "手机号列表，逗号分隔", required = true) @RequestParam String phoneNumbers,
            @ApiParam(value = "短信签名", required = false) @RequestParam(required = false) String customSignName,
            @ApiParam(value = "模板代码", required = true) @RequestParam String customTemplateCode,
            @ApiParam(value = "模板参数JSON格式", required = false) @RequestParam(required = false) String templateParam) {
        
        logger.info("📱 开始批量发送短信，手机号: [{}], 模板代码: [{}]", phoneNumbers, customTemplateCode);
        
        AsyncClient client = null;
        try {
            // 参数验证
            if (phoneNumbers == null || phoneNumbers.trim().isEmpty()) {
                return ResponseEntity.ok(Result.error("400", "手机号列表不能为空"));
            }
            
            if (customTemplateCode == null || customTemplateCode.trim().isEmpty()) {
                return ResponseEntity.ok(Result.error("400", "模板代码不能为空"));
            }
            
            // 验证手机号格式
            String[] phoneArray = phoneNumbers.split(",");
            for (String phone : phoneArray) {
                if (!phone.trim().matches("^1[3-9]\\d{9}$")) {
                    return ResponseEntity.ok(Result.error("400", "手机号格式不正确: " + phone.trim()));
                }
            }
            
            // 创建阿里云客户端
            client = createClient();
            
            // 构建短信发送请求
            SendSmsRequest.Builder requestBuilder = SendSmsRequest.builder()
                    .phoneNumbers(phoneNumbers)
                    .signName(customSignName != null ? customSignName : signName)
                    .templateCode(customTemplateCode);
            
            if (templateParam != null && !templateParam.trim().isEmpty()) {
                requestBuilder.templateParam(templateParam);
            }
            
            SendSmsRequest sendSmsRequest = requestBuilder.build();
            
            // 同步发送短信
            CompletableFuture<SendSmsResponse> futureResponse = client.sendSms(sendSmsRequest);
            SendSmsResponse response = futureResponse.get();
            
            Map<String, Object> result = new HashMap<>();
            result.put("phoneNumbers", phoneNumbers);
            result.put("phoneCount", phoneArray.length);
            result.put("signName", customSignName != null ? customSignName : signName);
            result.put("templateCode", customTemplateCode);
            result.put("templateParam", templateParam);
            result.put("bizId", response.getBody().getBizId());
            result.put("requestId", response.getBody().getRequestId());
            result.put("code", response.getBody().getCode());
            result.put("message", response.getBody().getMessage());
            result.put("sendTime", new java.util.Date());
            
            if ("OK".equals(response.getBody().getCode())) {
                logger.info("✅ 批量短信发送成功，手机号数量: [{}], 模板代码: [{}]", phoneArray.length, customTemplateCode);
                return ResponseEntity.ok(Result.success(result));
            } else {
                logger.error("❌ 批量短信发送失败，错误码: [{}], 错误信息: [{}]", 
                    response.getBody().getCode(), response.getBody().getMessage());
                return ResponseEntity.ok(Result.error(response.getBody().getCode(), 
                    "短信发送失败: " + response.getBody().getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("❌ 批量短信发送异常，手机号: [{}]", phoneNumbers, e);
            return ResponseEntity.ok(Result.error("500", "短信发送异常: " + e.getMessage()));
        } finally {
            // 关闭客户端
            if (client != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    logger.warn("关闭阿里云客户端异常", e);
                }
            }
        }
    }
    
    /**
     * 获取短信配置信息
     */
    @ApiOperation("获取短信配置信息")
    @GetMapping("/config")
    public ResponseEntity<Result> getConfig() {
        
        logger.info("🔧 获取短信配置信息");
        
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("endpoint", endpoint);
            config.put("signName", signName);
            config.put("templateCode", templateCode);
            config.put("accessKeyId", accessKeyId.substring(0, Math.min(8, accessKeyId.length())) + "****"); // 脱敏显示
            config.put("configTime", new java.util.Date());
            
            return ResponseEntity.ok(Result.success(config));
            
        } catch (Exception e) {
            logger.error("❌ 获取短信配置信息异常", e);
            return ResponseEntity.ok(Result.error("500", "获取配置信息异常: " + e.getMessage()));
        }
    }
    
    /**
     * 生成6位随机验证码
     */
    private String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
    
    /**
     * 拉黑通知短信（固定模板：SMS_496055951）
     * 模板变量：
     *  ${license_plate_number} 车牌号
     *  ${year} 年
     *  ${month} 月
     *  ${day} 日
     *  ${time} 时间
     *  ${address} 地址
     */
    @ApiOperation("发送拉黑通知短信")
    @PostMapping("/sendBlacklistMessage")
    public ResponseEntity<Result> sendBlacklistMessage(     @ApiParam(value = "手机号", required = true) @RequestParam String phoneNumber,
                                                            @ApiParam(value = "短信签名", required = false) @RequestParam(required = false) String customSignName,
                                                            @ApiParam(value = "模板代码", required = true) @RequestParam String customTemplateCode,
                                                            @ApiParam(value = "模板参数JSON格式", required = false) @RequestParam(required = false) String templateParam) {

        logger.info("📱 开始发送拉黑短信，手机号: [{}], 模板代码: [{}]", phoneNumber, customTemplateCode);

        AsyncClient client = null;
        try {
            // 参数验证
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return ResponseEntity.ok(Result.error("400", "手机号不能为空"));
            }

            if (customTemplateCode == null || customTemplateCode.trim().isEmpty()) {
                return ResponseEntity.ok(Result.error("400", "模板代码不能为空"));
            }

            // 手机号格式验证
            if (!phoneNumber.matches("^1[3-9]\\d{9}$")) {
                return ResponseEntity.ok(Result.error("400", "手机号格式不正确"));
            }

            // 创建阿里云客户端
            client = createClient();

            // 构建短信发送请求
            SendSmsRequest.Builder requestBuilder = SendSmsRequest.builder()
                    .phoneNumbers(phoneNumber)
                    .signName(customSignName != null ? customSignName : signName)
                    .templateCode(customTemplateCode);

            if (templateParam != null && !templateParam.trim().isEmpty()) {
                requestBuilder.templateParam(templateParam);
            }

            SendSmsRequest sendSmsRequest = requestBuilder.build();

            // 同步发送短信
            CompletableFuture<SendSmsResponse> futureResponse = client.sendSms(sendSmsRequest);
            SendSmsResponse response = futureResponse.get();

            Map<String, Object> result = new HashMap<>();
            result.put("phoneNumber", phoneNumber);
            result.put("signName", customSignName != null ? customSignName : signName);
            result.put("templateCode", customTemplateCode);
            result.put("templateParam", templateParam);
            result.put("bizId", response.getBody().getBizId());
            result.put("requestId", response.getBody().getRequestId());
            result.put("code", response.getBody().getCode());
            result.put("message", response.getBody().getMessage());
            result.put("sendTime", new java.util.Date());

            if ("OK".equals(response.getBody().getCode())) {
                logger.info("✅ 拉黑短信发送成功，手机号: [{}], 模板代码: [{}]", phoneNumber, customTemplateCode);
                return ResponseEntity.ok(Result.success(result));
            } else {
                logger.error("❌ 拉黑短信发送失败，手机号: [{}], 错误码: [{}], 错误信息: [{}]",
                        phoneNumber, response.getBody().getCode(), response.getBody().getMessage());
                return ResponseEntity.ok(Result.error(response.getBody().getCode(),
                        "短信发送失败: " + response.getBody().getMessage()));
            }

        } catch (Exception e) {
            logger.error("❌ 拉黑短信发送异常，手机号: [{}]", phoneNumber, e);
            return ResponseEntity.ok(Result.error("500", "短信发送异常: " + e.getMessage()));
        } finally {
            // 关闭客户端
            if (client != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    logger.warn("关闭阿里云客户端异常", e);
                }
            }
        }
    }
    
    /**
     * 发送违规提醒短信
     * 根据违规次数决定发送提醒短信还是违规短信
     */
    @ApiOperation("发送违规提醒短信")
    @PostMapping("/sendViolationReminder")
    public ResponseEntity<Result> sendViolationReminder(
            @ApiParam(value = "车牌号", required = true) @RequestParam String plateNumber,
            @ApiParam(value = "车主姓名", required = true) @RequestParam String ownerName,
            @ApiParam(value = "车主电话", required = true) @RequestParam String ownerPhone,
            @ApiParam(value = "违规类型", required = true) @RequestParam String violationType,
            @ApiParam(value = "违规地点", required = true) @RequestParam String violationLocation,
            @ApiParam(value = "违规时间", required = true) @RequestParam String violationTime,
            @ApiParam(value = "车场编码", required = false) @RequestParam(required = false) String parkCode,
            @ApiParam(value = "车场名称", required = false) @RequestParam(required = false) String parkName) {
        
        logger.info("🚨 开始发送违规提醒短信，车牌号: [{}], 车主电话: [{}]", plateNumber, ownerPhone);
        
        AsyncClient client = null;
        try {
            // 参数验证
            if (plateNumber == null || plateNumber.trim().isEmpty()) {
                return ResponseEntity.ok(Result.error("400", "车牌号不能为空"));
            }
            
            if (ownerPhone == null || ownerPhone.trim().isEmpty()) {
                return ResponseEntity.ok(Result.error("400", "车主电话不能为空"));
            }
            
            // 手机号格式验证
            if (!ownerPhone.matches("^1[3-9]\\d{9}$")) {
                return ResponseEntity.ok(Result.error("400", "车主电话格式不正确"));
            }
            
            // 检查是否需要发送违规提醒（第一次违规）
            boolean isFirstViolation = violationReminderService.shouldSendReminder(plateNumber);
            
            String templateCode;
            String templateParam;
            String reminderContent;
            
            if (isFirstViolation) {
                // 第一次违规，发送提醒短信
                templateCode = "SMS_496055951"; // 提醒短信模板
                reminderContent = String.format("【停车提醒】您的车辆%s在%s发生%s违规，请及时处理。", 
                    plateNumber, violationLocation, violationType);
                
                // 构建模板参数
                LocalDateTime violationDateTime = LocalDateTime.parse(violationTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                templateParam = String.format("{\"license_plate_number\":\"%s\",\"year\":\"%d\",\"month\":\"%d\",\"day\":\"%d\",\"time\":\"%s\",\"address\":\"%s\"}",
                    plateNumber,
                    violationDateTime.getYear(),
                    violationDateTime.getMonthValue(),
                    violationDateTime.getDayOfMonth(),
                    violationDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    violationLocation);
            } else {
                // 第二次及以后违规，发送违规短信
                templateCode = "SMS_496055951"; // 违规短信模板（可以根据需要配置不同的模板）
                reminderContent = String.format("【违规通知】您的车辆%s在%s再次发生%s违规，请立即处理，否则将影响您的停车权益。", 
                    plateNumber, violationLocation, violationType);
                
                // 构建模板参数
                LocalDateTime violationDateTime = LocalDateTime.parse(violationTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                templateParam = String.format("{\"license_plate_number\":\"%s\",\"year\":\"%d\",\"month\":\"%d\",\"day\":\"%d\",\"time\":\"%s\",\"address\":\"%s\"}",
                    plateNumber,
                    violationDateTime.getYear(),
                    violationDateTime.getMonthValue(),
                    violationDateTime.getDayOfMonth(),
                    violationDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    violationLocation);
            }
            
            // 创建阿里云客户端
            client = createClient();
            
            // 构建短信发送请求
            SendSmsRequest sendSmsRequest = SendSmsRequest.builder()
                    .phoneNumbers(ownerPhone)
                    .signName(signName)
                    .templateCode(templateCode)
                    .templateParam(templateParam)
                    .build();
            
            // 同步发送短信
            CompletableFuture<SendSmsResponse> futureResponse = client.sendSms(sendSmsRequest);
            SendSmsResponse response = futureResponse.get();
            
            // 创建违规提醒记录
            ViolationReminder reminder = new ViolationReminder();
            reminder.setPlateNumber(plateNumber);
            reminder.setOwnerName(ownerName);
            reminder.setOwnerPhone(ownerPhone);
            reminder.setViolationType(violationType);
            reminder.setViolationLocation(violationLocation);
            reminder.setViolationTime(LocalDateTime.parse(violationTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            reminder.setReminderTime(LocalDateTime.now());
            reminder.setReminderTemplateCode(templateCode);
            reminder.setReminderContent(reminderContent);
            reminder.setParkCode(parkCode);
            reminder.setParkName(parkName);
            
            Map<String, Object> result = new HashMap<>();
            result.put("plateNumber", plateNumber);
            result.put("ownerPhone", ownerPhone);
            result.put("violationType", violationType);
            result.put("violationLocation", violationLocation);
            result.put("violationTime", violationTime);
            result.put("isFirstViolation", isFirstViolation);
            result.put("templateCode", templateCode);
            result.put("reminderContent", reminderContent);
            result.put("bizId", response.getBody().getBizId());
            result.put("requestId", response.getBody().getRequestId());
            result.put("code", response.getBody().getCode());
            result.put("message", response.getBody().getMessage());
            result.put("sendTime", new java.util.Date());
            
            if ("OK".equals(response.getBody().getCode())) {
                // 短信发送成功，保存违规提醒记录
                violationReminderService.createViolationReminder(reminder);
                
                logger.info("✅ 违规提醒短信发送成功，车牌号: [{}], 车主电话: [{}], 是否首次违规: [{}]", 
                    plateNumber, ownerPhone, isFirstViolation);
                return ResponseEntity.ok(Result.success(result));
            } else {
                logger.error("❌ 违规提醒短信发送失败，车牌号: [{}], 车主电话: [{}], 错误码: [{}], 错误信息: [{}]", 
                    plateNumber, ownerPhone, response.getBody().getCode(), response.getBody().getMessage());
                return ResponseEntity.ok(Result.error(response.getBody().getCode(), 
                    "短信发送失败: " + response.getBody().getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("❌ 违规提醒短信发送异常，车牌号: [{}], 车主电话: [{}]", plateNumber, ownerPhone, e);
            return ResponseEntity.ok(Result.error("500", "短信发送异常: " + e.getMessage()));
        } finally {
            // 关闭客户端
            if (client != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    logger.warn("关闭阿里云客户端异常", e);
                }
            }
        }
    }
}