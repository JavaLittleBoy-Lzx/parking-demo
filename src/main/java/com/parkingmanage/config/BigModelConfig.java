package com.parkingmanage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * BigModel配置类
 */
@Configuration
@ConfigurationProperties(prefix = "bigmodel")
@Data
public class BigModelConfig {

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API基础URL
     */
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";

    /**
     * 请求超时时间（毫秒）
     */
    private int timeout = 60000;

    /**
     * 重试次数
     */
    private int retryTimes = 3;

    /**
     * 功能开关
     */
    private Features features = new Features();

    /**
     * 缓存配置
     */
    private Cache cache = new Cache();

    /**
     * 限流配置
     */
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class Features {
        private boolean customerService = true;
        private boolean violationDescription = true;
        private boolean dataReport = true;
        private boolean notificationText = true;
        private boolean behaviorAnalysis = true;
    }

    @Data
    public static class Cache {
        private int ttl = 1800; // 30分钟
        private int maxSize = 1000;
    }

    @Data
    public static class RateLimit {
        private int perMinute = 60;
        private int perHour = 1000;
    }

    /**
     * 配置BigModel专用的RestTemplate
     */
    @Bean(name = "bigModelRestTemplate")
    public RestTemplate bigModelRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(clientHttpRequestFactory());
        return restTemplate;
    }

    /**
     * 配置请求工厂
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return factory;
    }

    /**
     * 检查功能是否启用
     */
    public boolean isCustomerServiceEnabled() {
        return features.isCustomerService();
    }

    public boolean isViolationDescriptionEnabled() {
        return features.isViolationDescription();
    }

    public boolean isDataReportEnabled() {
        return features.isDataReport();
    }

    public boolean isNotificationTextEnabled() {
        return features.isNotificationText();
    }

    public boolean isBehaviorAnalysisEnabled() {
        return features.isBehaviorAnalysis();
    }
}