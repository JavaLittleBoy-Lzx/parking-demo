package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.QrCodeUsage;

/**
 * 二维码使用记录服务接口
 */
public interface QrCodeUsageService extends IService<QrCodeUsage> {

    /**
     * 记录二维码生成
     * @param qrCodeUsage 二维码使用记录
     * @return 是否成功
     */
    boolean recordQrCodeGeneration(QrCodeUsage qrCodeUsage);

    /**
     * 验证二维码（不标记为已使用）
     * @param qrId 二维码ID
     * @param visitorOpenid 访客openid
     * @param visitorPhone 访客手机号
     * @return 验证结果
     */
    QrCodeValidationResult validateQrCodeOnly(String qrId, String visitorOpenid, String visitorPhone);

    /**
     * 验证二维码并标记为已使用
     * @param qrId 二维码ID
     * @param visitorOpenid 访客openid
     * @param visitorPhone 访客手机号
     * @return 验证结果
     */
    QrCodeValidationResult validateAndUseQrCode(String qrId, String visitorOpenid, String visitorPhone);

    /**
     * 根据二维码ID查询记录
     * @param qrId 二维码ID
     * @return 二维码使用记录
     */
    QrCodeUsage findByQrId(String qrId);

    /**
     * 根据访客openid查询二维码使用记录
     * @param visitorOpenid 访客openid
     * @return 二维码使用记录
     */
    QrCodeUsage findByVisitorOpenid(String visitorOpenid);

    /**
     * 清理过期的二维码记录
     * @return 清理的记录数
     */
    int cleanExpiredQrCodes();

    /**
     * 生成访问令牌
     * @param openid 用户openid
     * @param qrId 二维码ID
     * @return 访问令牌
     */
    String generateAccessToken(String openid, String qrId);

    /**
     * 验证访问令牌
     * @param token 访问令牌
     * @return 是否有效
     */
    boolean validateAccessToken(String token);

    /**
     * 二维码验证结果类
     */
    class QrCodeValidationResult {
        private boolean valid;
        private String message;
        private QrCodeUsage qrCodeUsage;
        private String accessToken;

        public QrCodeValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public QrCodeValidationResult(boolean valid, String message, QrCodeUsage qrCodeUsage, String accessToken) {
            this.valid = valid;
            this.message = message;
            this.qrCodeUsage = qrCodeUsage;
            this.accessToken = accessToken;
        }

        // Getters and Setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public QrCodeUsage getQrCodeUsage() { return qrCodeUsage; }
        public void setQrCodeUsage(QrCodeUsage qrCodeUsage) { this.qrCodeUsage = qrCodeUsage; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    }
}
