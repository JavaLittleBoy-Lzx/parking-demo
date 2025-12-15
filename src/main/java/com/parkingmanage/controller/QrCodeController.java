package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.entity.QrCodeUsage;
import com.parkingmanage.service.QrCodeUsageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码管理控制器
 */
@Api(tags = "二维码管理")
@RestController
@RequestMapping("/parking/qrcode")
public class QrCodeController {

    private static final Logger logger = LoggerFactory.getLogger(QrCodeController.class);

    // 类型别名，用于兼容现有代码
    private static class r extends Result<Object> {}

    @Autowired
    private QrCodeUsageService qrCodeUsageService;

    @ApiOperation("记录二维码生成")
    @PostMapping("/record")
    public ResponseEntity<Result> recordQrCodeGeneration(@RequestBody QrCodeUsage qrCodeUsage) {
        r result = new r();
        
        try {
            logger.info("🎯 接收二维码生成记录请求: qrId={}, butlerPhone={}", 
                qrCodeUsage.getQrId(), qrCodeUsage.getButlerPhone());
            
            // 验证必要参数
            if (qrCodeUsage.getQrId() == null || qrCodeUsage.getQrId().trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("二维码ID不能为空");
                return ResponseEntity.ok(result);
            }
            
            if (qrCodeUsage.getButlerPhone() == null || qrCodeUsage.getButlerPhone().trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("管家手机号不能为空");
                return ResponseEntity.ok(result);
            }
            
            // 检查二维码ID是否已存在
            QrCodeUsage existingQrCode = qrCodeUsageService.findByQrId(qrCodeUsage.getQrId());
            if (existingQrCode != null) {
                result.setCode("1");
                result.setMsg("二维码ID已存在");
                return ResponseEntity.ok(result);
            }
            
            // 记录二维码生成
            boolean success = qrCodeUsageService.recordQrCodeGeneration(qrCodeUsage);
            
            if (success) {
                result.setCode("0");
                result.setMsg("记录成功");
                result.setData(qrCodeUsage);
                logger.info("✅ 二维码生成记录成功: qrId={}", qrCodeUsage.getQrId());
            } else {
                result.setCode("1");
                result.setMsg("记录失败");
                logger.error("❌ 二维码生成记录失败: qrId={}", qrCodeUsage.getQrId());
            }
            
        } catch (Exception e) {
            logger.error("❌ 记录二维码生成时发生异常", e);
            result.setCode("1");
            result.setMsg("记录失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    @ApiOperation("检查二维码使用状态")
    @GetMapping("/checkUsed")
    public ResponseEntity<Result> checkQrCodeUsed(
            @ApiParam(value = "二维码ID", required = true) @RequestParam String qrId,
            @ApiParam(value = "访客openid", required = true) @RequestParam String openid) {

        r result = new r();

        try {
            logger.info("🔍 检查二维码使用状态: qrId={}, openid={}", qrId, openid);

            // 验证参数
            if (qrId == null || qrId.trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("二维码ID不能为空");
                return ResponseEntity.ok(result);
            }

            if (openid == null || openid.trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("用户openid不能为空");
                return ResponseEntity.ok(result);
            }

            // 查询二维码记录
            QrCodeUsage qrUsage = qrCodeUsageService.findByQrId(qrId);

            Map<String, Object> data = new HashMap<>();
            
            if (qrUsage != null) {
                // 🆕 检查二维码是否已过期（24小时有效期）
                Date createdDate = qrUsage.getCreatedTime();
                LocalDateTime createdTime = createdDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime expiresAt = createdTime.plusHours(24);
                boolean isExpired = now.isAfter(expiresAt);
                
                if (isExpired) {
                    // 二维码已过期
                    data.put("expired", true);
                    data.put("used", false);
                    data.put("createdTime", createdTime);
                    data.put("expiresAt", expiresAt);
                    logger.warn("⏰ 二维码已过期: qrId={}, 创建时间={}, 过期时间={}, 当前时间={}", 
                        qrId, createdTime, expiresAt, now);
                } else if (qrUsage.getIsUsed() != null && qrUsage.getIsUsed() == 1) {
                    // 二维码已使用
                    data.put("expired", false);
                    data.put("used", true);
                    data.put("usedTime", qrUsage.getUsedTime());
                    data.put("usedBy", qrUsage.getVisitorOpenid());
                    logger.info("✅ 二维码已使用: qrId={}, usedTime={}", qrId, qrUsage.getUsedTime());
                } else {
                    // 二维码未使用且未过期
                    data.put("expired", false);
                    data.put("used", false);
                    data.put("createdTime", createdTime);
                    data.put("expiresAt", expiresAt);
                    logger.info("✅ 二维码未使用且未过期: qrId={}, 过期时间={}", qrId, expiresAt);
                }
            } else {
                // 二维码不存在
                data.put("expired", false);
                data.put("used", false);
                logger.warn("⚠️ 二维码不存在: qrId={}", qrId);
            }

            result.setCode("0");
            result.setMsg("查询成功");
            result.setData(data);

        } catch (Exception e) {
            logger.error("❌ 检查二维码使用状态时发生异常: qrId={}", qrId, e);
            result.setCode("1");
            result.setMsg("查询失败: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @ApiOperation("验证二维码（不标记为已使用）")
    @PostMapping("/validateOnly")
    public ResponseEntity<r> validateQrCodeOnly(
            @ApiParam(value = "二维码ID", required = true) @RequestParam String qrId,
            @ApiParam(value = "访客openid", required = true) @RequestParam String openid,
            @ApiParam(value = "访客手机号") @RequestParam(required = false) String visitorPhone) {

        r result = new r();

        try {
            logger.info("🔍 接收二维码验证请求（不标记为已使用）: qrId={}, openid={}", qrId, openid);

            // 验证参数
            if (qrId == null || qrId.trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("二维码ID不能为空");
                return ResponseEntity.ok(result);
            }

            if (openid == null || openid.trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("用户openid不能为空");
                return ResponseEntity.ok(result);
            }

            // 🔒 只验证二维码有效性，不标记为已使用
            QrCodeUsageService.QrCodeValidationResult validationResult =
                qrCodeUsageService.validateQrCodeOnly(qrId, openid, visitorPhone);

            if (validationResult.isValid()) {
                // 验证成功
                Map<String, Object> data = new HashMap<>();
                data.put("valid", true);
                data.put("accessToken", validationResult.getAccessToken());
                data.put("butlerInfo", validationResult.getQrCodeUsage());

                result.setCode("0");
                result.setMsg("验证成功（未标记为已使用）");
                result.setData(data);

                logger.info("✅ 二维码验证成功（未标记为已使用）: qrId={}", qrId);
            } else {
                // 验证失败
                result.setCode("1");
                result.setMsg(validationResult.getMessage());

                logger.warn("❌ 二维码验证失败: qrId={}, reason={}", qrId, validationResult.getMessage());
            }

        } catch (Exception e) {
            logger.error("❌ 验证二维码时发生异常: qrId={}", qrId, e);
            result.setCode("1");
            result.setMsg("验证失败: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @ApiOperation("验证二维码")
    @PostMapping("/validate")
    public ResponseEntity<Result> validateQrCode(
            @ApiParam(value = "二维码ID", required = true) @RequestParam String qrId,
            @ApiParam(value = "访客openid", required = true) @RequestParam String openid,
            @ApiParam(value = "访客手机号") @RequestParam(required = false) String visitorPhone) {
        
        r result = new r();
        
        try {
            logger.info("🔍 接收二维码验证请求: qrId={}, openid={}", qrId, openid);
            
            // 验证参数
            if (qrId == null || qrId.trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("二维码ID不能为空");
                return ResponseEntity.ok(result);
            }
            
            if (openid == null || openid.trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("用户openid不能为空");
                return ResponseEntity.ok(result);
            }
            
            // 验证二维码
            QrCodeUsageService.QrCodeValidationResult validationResult = 
                qrCodeUsageService.validateAndUseQrCode(qrId, openid, visitorPhone);
            
            if (validationResult.isValid()) {
                // 验证成功
                Map<String, Object> data = new HashMap<>();
                data.put("valid", true);
                data.put("accessToken", validationResult.getAccessToken());
                data.put("butlerInfo", validationResult.getQrCodeUsage());
                
                result.setCode("0");
                result.setMsg("验证成功");
                result.setData(data);
                
                logger.info("✅ 二维码验证成功: qrId={}", qrId);
            } else {
                // 验证失败
                result.setCode("1");
                result.setMsg(validationResult.getMessage());
                
                logger.warn("⚠️ 二维码验证失败: qrId={}, reason={}", qrId, validationResult.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("❌ 验证二维码时发生异常: qrId=" + qrId, e);
            result.setCode("1");
            result.setMsg("验证失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    @ApiOperation("验证访问令牌")
    @PostMapping("/validateToken")
    public ResponseEntity<Result> validateAccessToken(
            @ApiParam(value = "访问令牌", required = true) @RequestParam String token) {
        
        Result result = new Result();
        
        try {
            boolean isValid = qrCodeUsageService.validateAccessToken(token);
            
            Map<String, Object> data = new HashMap<>();
            data.put("valid", isValid);
            
            result.setCode("0");
            result.setMsg(isValid ? "令牌有效" : "令牌无效");
            result.setData(data);
            
        } catch (Exception e) {
            logger.error("❌ 验证访问令牌时发生异常", e);
            result.setCode("1");
            result.setMsg("验证失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    @ApiOperation("清理过期二维码")
    @PostMapping("/cleanExpired")
    public ResponseEntity<Result> cleanExpiredQrCodes() {
        Result result = new Result();
        
        try {
            int cleanedCount = qrCodeUsageService.cleanExpiredQrCodes();
            
            Map<String, Object> data = new HashMap<>();
            data.put("cleanedCount", cleanedCount);
            
            result.setCode("0");
            result.setMsg("清理完成");
            result.setData(data);
            
            logger.info("🧹 清理过期二维码完成: 清理数量={}", cleanedCount);
            
        } catch (Exception e) {
            logger.error("❌ 清理过期二维码时发生异常", e);
            result.setCode("1");
            result.setMsg("清理失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    @ApiOperation("查询二维码使用记录")
    @GetMapping("/query/{qrId}")
    public ResponseEntity<Result> queryQrCodeUsage(
            @ApiParam(value = "二维码ID", required = true) @PathVariable String qrId) {
        
        r result = new r();
        
        try {
            QrCodeUsage qrCodeUsage = qrCodeUsageService.findByQrId(qrId);
            
            if (qrCodeUsage != null) {
                result.setCode("0");
                result.setMsg("查询成功");
                result.setData(qrCodeUsage);
            } else {
                result.setCode("1");
                result.setMsg("二维码记录不存在");
            }
            
        } catch (Exception e) {
            logger.error("❌ 查询二维码使用记录时发生异常: qrId=" + qrId, e);
            result.setCode("1");
            result.setMsg("查询失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
}
