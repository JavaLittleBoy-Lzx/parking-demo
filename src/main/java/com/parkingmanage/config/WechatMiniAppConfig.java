package com.parkingmanage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WechatMiniAppConfig {
    
    @Value("${wechat.miniapp.appid}")
    private String appId;
    
    @Value("${wechat.miniapp.secret}")
    private String secret;
    
    public String getAppId() {
        return appId;
    }
    
    public String getSecret() {
        return secret;
    }
    
    @Bean(name = "wechatRestTemplate")
    public RestTemplate wechatRestTemplate() {
        return new RestTemplate();
    }
} 