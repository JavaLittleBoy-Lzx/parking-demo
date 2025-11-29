package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parkingmanage.entity.Parking;
import com.parkingmanage.entity.QrVisitorUsage;
import com.parkingmanage.entity.VisitorToken;
import com.parkingmanage.service.ParkingService;
import com.parkingmanage.service.QrVisitorUsageService;
import com.parkingmanage.service.VisitorTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 外来访客验证控制器
 * @author System
 * @since 2025-11-23
 */
@RestController
@RequestMapping("/visitor")
public class ExternalVisitorController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalVisitorController.class);
    
    @Resource
    private ParkingService parkingService;
    
    @Resource
    private QrVisitorUsageService qrVisitorUsageService;
    
    @Resource
    private VisitorTokenService visitorTokenService;
    
    // 配置参数
    private static final int MAX_DISTANCE = 500;           // 最大允许距离（米）
    private static final int TOKEN_EXPIRE_MINUTES = 5;     // Token有效期（分钟）
    private static final int IDENTITY_EXPIRE_HOURS = 24;   // 身份有效期（小时）
    private static final int MAX_USES_PER_DAY = 3;         // 每天最多使用次数
    
    /**
     * 验证并获取Token（GPS位置验证 + Token生成）
     * 
     * 验证流程：
     * 1. GPS位置验证（500米范围内）
     * 2. 使用次数检查（每天最多3次）
     * 3. 生成5分钟有效Token
     * 4. 记录使用信息（24小时有效期）
     */
    @PostMapping("/verifyAndGetToken")
    public Map<String, Object> verifyAndGetToken(
            @RequestParam String qrId,
            @RequestParam String phone,
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        
        logger.info("📍 开始验证外来访客: qrId={}, phone={}, 位置=({}, {})", 
            qrId, phone, latitude, longitude);
        
        try {
            // 第一步：验证位置
            // 注意：这里假设parking表有qr_id字段，如果没有需要添加
            Parking parking = parkingService.getOne(
                new QueryWrapper<Parking>().eq("qr_id", qrId)
            );
            
            if (parking == null) {
                logger.warn("❌ 车场不存在: qrId={}", qrId);
                return error("车场不存在");
            }
            
            // 计算距离
            // 注意：Parking表需要有latitude和longitude字段
            // 如果没有，需要先添加这些字段或使用其他表
            double distance = calculateDistance(
                latitude, longitude,
                45.7568, 126.6425 // 临时硬编码，实际应从parking表获取
            );
            
            logger.info("📏 计算距离: {}米, 车场={}", distance, parking.getCommunity());
            
            // 验证距离
            int maxRadius = MAX_DISTANCE;
                
            if (distance > maxRadius) {
                logger.warn("❌ 位置验证失败: 距离{}米 > 最大允许{}米", distance, maxRadius);
                return error(String.format(
                    "请在车场现场扫码，当前距离%.1f公里", distance / 1000));
            }
            
            logger.info("✅ 位置验证通过: 距离{}米 < 最大允许{}米", distance, maxRadius);
            
            // 第二步：检查使用记录
            QrVisitorUsage usage = qrVisitorUsageService.getOne(
                new QueryWrapper<QrVisitorUsage>()
                    .eq("qr_id", qrId)
                    .eq("phone", phone)
            );
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusHours(IDENTITY_EXPIRE_HOURS);
            
            if (usage == null) {
                // 首次使用，创建记录
                logger.info("🆕 首次使用，创建记录");
                usage = new QrVisitorUsage();
                usage.setQrId(qrId);
                usage.setPhone(phone);
                usage.setFirstScanTime(now);
                usage.setLastScanTime(now);
                usage.setExpiresAt(expiresAt);
                usage.setScanCount(1);
                usage.setTotalCount(1);
                usage.setLastLatitude(latitude);
                usage.setLastLongitude(longitude);
                usage.setLastDistance(distance);
                usage.setStatus("active");
                qrVisitorUsageService.save(usage);
                
            } else {
                // 已有记录，检查是否过期
                if (now.isAfter(usage.getExpiresAt())) {
                    // 已过期，重新创建
                    logger.info("⏰ 记录已过期，重新创建");
                    usage.setFirstScanTime(now);
                    usage.setExpiresAt(expiresAt);
                    usage.setScanCount(1);
                } else {
                    // 检查今日使用次数
                    LocalDate today = LocalDate.now();
                    LocalDate lastScanDate = usage.getLastScanTime().toLocalDate();
                    
                    if (lastScanDate.equals(today)) {
                        // 今天的记录
                        if (usage.getScanCount() >= MAX_USES_PER_DAY) {
                            logger.warn("❌ 今日使用次数已达上限: {}/{}", 
                                usage.getScanCount(), MAX_USES_PER_DAY);
                            return error(String.format(
                                "今日使用次数已达上限(%d次)，请明天再试", MAX_USES_PER_DAY));
                        }
                        usage.setScanCount(usage.getScanCount() + 1);
                    } else {
                        // 新的一天，重置计数
                        usage.setScanCount(1);
                    }
                }
                
                // 更新记录
                usage.setLastScanTime(now);
                usage.setTotalCount(usage.getTotalCount() + 1);
                usage.setLastLatitude(latitude);
                usage.setLastLongitude(longitude);
                usage.setLastDistance(distance);
                qrVisitorUsageService.updateById(usage);
            }
            
            logger.info("📝 使用记录更新完成: 今日第{}次, 累计第{}次", 
                usage.getScanCount(), usage.getTotalCount());
            
            // 第三步：生成Token并存入数据库
            String token = UUID.randomUUID().toString();
            
            VisitorToken visitorToken = new VisitorToken();
            visitorToken.setToken(token);
            visitorToken.setQrId(qrId);
            visitorToken.setPhone(phone);
            visitorToken.setLatitude(latitude);
            visitorToken.setLongitude(longitude);
            visitorToken.setDistance(distance);
            visitorToken.setCreateTime(now);
            visitorToken.setExpireTime(now.plusMinutes(TOKEN_EXPIRE_MINUTES));
            visitorToken.setIsUsed(0);
            
            visitorTokenService.save(visitorToken);
            
            logger.info("✅ Token生成成功: {}, 有效期{}分钟", token, TOKEN_EXPIRE_MINUTES);
            
            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("expiresAt", System.currentTimeMillis() + 
                (IDENTITY_EXPIRE_HOURS * 60 * 60 * 1000));
            result.put("distance", Math.round(distance * 10) / 10.0); // 保留1位小数
            result.put("parkingName", parking.getCommunity());
            result.put("remainingUses", MAX_USES_PER_DAY - usage.getScanCount());
            
            return success(result);
            
        } catch (Exception e) {
            logger.error("❌ 验证失败:", e);
            return error("验证失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证Token是否有效（预约提交时调用）
     * 
     * 验证内容：
     * 1. Token是否存在
     * 2. 是否已过期（5分钟）
     * 3. 是否已使用（一次性）
     */
    @PostMapping("/validateToken")
    public Map<String, Object> validateToken(@RequestParam String token) {
        logger.info("🔍 验证Token: {}", token);
        
        VisitorToken visitorToken = visitorTokenService.getById(token);
        
        if (visitorToken == null) {
            logger.warn("❌ Token不存在");
            return error("Token不存在");
        }
        
        // 检查是否已过期
        if (LocalDateTime.now().isAfter(visitorToken.getExpireTime())) {
            logger.warn("❌ Token已过期");
            return error("Token已过期");
        }
        
        // 检查是否已使用
        if (visitorToken.getIsUsed() == 1) {
            logger.warn("❌ Token已使用");
            return error("Token已使用");
        }
        
        // 构建返回数据
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("qrId", visitorToken.getQrId());
        tokenData.put("phone", visitorToken.getPhone());
        tokenData.put("latitude", visitorToken.getLatitude());
        tokenData.put("longitude", visitorToken.getLongitude());
        tokenData.put("distance", visitorToken.getDistance());
        
        // 标记为已使用（一次性）
        visitorToken.setIsUsed(1);
        visitorToken.setUsedTime(LocalDateTime.now());
        visitorTokenService.updateById(visitorToken);
        
        logger.info("✅ Token验证通过并标记为已使用");
        
        return success(tokenData);
    }
    
    /**
     * 计算两点之间的距离（Haversine公式）
     * 
     * @param lat1 纬度1
     * @param lon1 经度1
     * @param lat2 纬度2
     * @param lon2 经度2
     * @return 距离（米）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // 地球半径（米）
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * 成功响应
     */
    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", "0");
        result.put("msg", "success");
        result.put("data", data);
        return result;
    }
    
    /**
     * 失败响应
     */
    private Map<String, Object> error(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", "-1");
        result.put("msg", message);
        return result;
    }
}
