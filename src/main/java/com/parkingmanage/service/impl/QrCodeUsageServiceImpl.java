package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.QrCodeUsage;
import com.parkingmanage.mapper.QrCodeUsageMapper;
import com.parkingmanage.service.QrCodeUsageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;

/**
 * 二维码使用记录服务实现类
 */
@Service
public class QrCodeUsageServiceImpl extends ServiceImpl<QrCodeUsageMapper, QrCodeUsage> implements QrCodeUsageService {

    private static final Logger logger = LoggerFactory.getLogger(QrCodeUsageServiceImpl.class);
    
    // 访问令牌密钥
    private static final String ACCESS_TOKEN_SECRET = "QR_CODE_ACCESS_TOKEN_SECRET_2024";
    
    // 令牌有效期（24小时）
    private static final long TOKEN_VALIDITY_PERIOD = 24 * 60 * 60 * 1000;

    @Override
    public boolean recordQrCodeGeneration(QrCodeUsage qrCodeUsage) {
        try {
            // 设置创建时间
            qrCodeUsage.setCreatedTime(new Date());
            
            // 设置默认值
            if (qrCodeUsage.getIsUsed() == null) {
                qrCodeUsage.setIsUsed(0);
            }
            
            // 设置过期时间（24小时后）
            if (qrCodeUsage.getExpireTime() == null) {
                qrCodeUsage.setExpireTime(new Date(System.currentTimeMillis() + TOKEN_VALIDITY_PERIOD));
            }
            
            // 保存到数据库
            boolean result = this.save(qrCodeUsage);
            
            if (result) {
                logger.info("🎯 二维码生成记录保存成功: qrId={}, butlerPhone={}", 
                    qrCodeUsage.getQrId(), qrCodeUsage.getButlerPhone());
            } else {
                logger.error("❌ 二维码生成记录保存失败: qrId={}", qrCodeUsage.getQrId());
            }
            
            return result;
        } catch (Exception e) {
            logger.error("❌ 记录二维码生成失败", e);
            return false;
        }
    }

    @Override
    public QrCodeValidationResult validateQrCodeOnly(String qrId, String visitorOpenid, String visitorPhone) {
        try {
            logger.info("🔍 开始验证二维码（不标记为已使用）: qrId={}, visitorOpenid={}", qrId, visitorOpenid);

            // 查询二维码记录
            QrCodeUsage qrUsage = this.baseMapper.findByQrId(qrId);

            if (qrUsage == null) {
                logger.warn("⚠️ 二维码不存在: qrId={}", qrId);
                return new QrCodeValidationResult(false, "二维码不存在或已失效");
            }

            // 检查是否已使用
            if (qrUsage.getIsUsed() != null && qrUsage.getIsUsed() == 1) {
                logger.warn("⚠️ 二维码已使用: qrId={}, usedTime={}", qrId, qrUsage.getUsedTime());
                return new QrCodeValidationResult(false, "二维码已使用，请联系管家重新生成");
            }

            // 检查有效期
            if (qrUsage.getExpireTime() != null && qrUsage.getExpireTime().before(new Date())) {
                logger.warn("⚠️ 二维码已过期: qrId={}, expireTime={}", qrId, qrUsage.getExpireTime());
                return new QrCodeValidationResult(false, "二维码已过期，请联系管家重新生成");
            }

            // 🔒 不标记为已使用，只生成访问令牌
            String accessToken = generateAccessToken(visitorOpenid, qrId);

            logger.info("✅ 二维码验证成功（未标记为已使用）: qrId={}", qrId);
            return new QrCodeValidationResult(true, "验证成功（未标记为已使用）", qrUsage, accessToken);

        } catch (Exception e) {
            logger.error("❌ 验证二维码时发生异常: qrId=" + qrId, e);
            return new QrCodeValidationResult(false, "验证失败，系统异常");
        }
    }

    @Override
    public QrCodeValidationResult validateAndUseQrCode(String qrId, String visitorOpenid, String visitorPhone) {
        try {
            logger.info("🔍 开始验证二维码: qrId={}, visitorOpenid={}", qrId, visitorOpenid);
            
            // 查询二维码记录
            QrCodeUsage qrUsage = this.baseMapper.findByQrId(qrId);
            
            if (qrUsage == null) {
                logger.warn("⚠️ 二维码不存在: qrId={}", qrId);
                return new QrCodeValidationResult(false, "二维码不存在或已失效");
            }
            
            // 检查是否已使用
            if (qrUsage.getIsUsed() != null && qrUsage.getIsUsed() == 1) {
                logger.warn("⚠️ 二维码已使用: qrId={}, usedTime={}", qrId, qrUsage.getUsedTime());
                return new QrCodeValidationResult(false, "二维码已使用，请联系管家重新生成");
            }
            
            // 检查有效期
            if (qrUsage.getExpireTime() != null && qrUsage.getExpireTime().before(new Date())) {
                logger.warn("⚠️ 二维码已过期: qrId={}, expireTime={}", qrId, qrUsage.getExpireTime());
                return new QrCodeValidationResult(false, "二维码已过期，请联系管家重新生成");
            }
            
            // 标记为已使用
            int updateResult = this.baseMapper.markAsUsed(qrId, visitorOpenid, visitorPhone);
            
            if (updateResult > 0) {
                // 生成访问令牌
                String accessToken = generateAccessToken(visitorOpenid, qrId);
                
                // 重新查询更新后的记录
                qrUsage = this.baseMapper.findByQrId(qrId);
                
                logger.info("✅ 二维码验证成功并标记为已使用: qrId={}", qrId);
                return new QrCodeValidationResult(true, "验证成功", qrUsage, accessToken);
            } else {
                logger.error("❌ 标记二维码为已使用失败: qrId={}", qrId);
                return new QrCodeValidationResult(false, "验证失败，请重试");
            }
            
        } catch (Exception e) {
            logger.error("❌ 验证二维码时发生异常: qrId=" + qrId, e);
            return new QrCodeValidationResult(false, "验证失败，系统异常");
        }
    }

    @Override
    public QrCodeUsage findByQrId(String qrId) {
        return this.baseMapper.findByQrId(qrId);
    }

    @Override
    public QrCodeUsage findByVisitorOpenid(String visitorOpenid) {
        try {
            // 查询最近使用的二维码记录（按使用时间倒序）
            return this.baseMapper.findByVisitorOpenid(visitorOpenid);
        } catch (Exception e) {
            logger.error("❌ 根据访客openid查询二维码记录失败: visitorOpenid=" + visitorOpenid, e);
            return null;
        }
    }

    @Override
    public int cleanExpiredQrCodes() {
        try {
            int cleanedCount = this.baseMapper.cleanExpiredQrCodes();
            logger.info("🧹 清理过期二维码记录: 清理数量={}", cleanedCount);
            return cleanedCount;
        } catch (Exception e) {
            logger.error("❌ 清理过期二维码记录失败", e);
            return 0;
        }
    }

    @Override
    public String generateAccessToken(String openid, String qrId) {
        try {
            // 构建令牌数据
            long timestamp = System.currentTimeMillis();
            String data = openid + "|" + qrId + "|" + timestamp;
            
            // 使用HMAC-SHA256生成签名
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(ACCESS_TOKEN_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // 组合最终令牌
            String token = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8)) + 
                          "." + 
                          Base64.getEncoder().encodeToString(signature);
            
            logger.debug("🔑 生成访问令牌成功: openid={}, qrId={}", openid, qrId);
            return token;
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("❌ 生成访问令牌失败", e);
            return null;
        }
    }

    @Override
    public boolean validateAccessToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }
            
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                return false;
            }
            
            // 解码数据部分
            String dataBase64 = parts[0];
            String signatureBase64 = parts[1];
            
            byte[] dataBytes = Base64.getDecoder().decode(dataBase64);
            String data = new String(dataBytes, StandardCharsets.UTF_8);
            
            // 验证签名
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(ACCESS_TOKEN_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] expectedSignature = mac.doFinal(dataBytes);
            byte[] actualSignature = Base64.getDecoder().decode(signatureBase64);
            
            if (!java.util.Arrays.equals(expectedSignature, actualSignature)) {
                logger.warn("⚠️ 访问令牌签名验证失败");
                return false;
            }
            
            // 检查时间戳
            String[] dataParts = data.split("\\|");
            if (dataParts.length != 3) {
                return false;
            }
            
            long timestamp = Long.parseLong(dataParts[2]);
            long currentTime = System.currentTimeMillis();
            
            // 检查令牌是否过期（24小时）
            if (currentTime - timestamp > TOKEN_VALIDITY_PERIOD) {
                logger.warn("⚠️ 访问令牌已过期: timestamp={}", timestamp);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("❌ 验证访问令牌失败", e);
            return false;
        }
    }
}
