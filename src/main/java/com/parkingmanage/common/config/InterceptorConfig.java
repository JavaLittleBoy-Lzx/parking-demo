package com.parkingmanage.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import javax.annotation.Resource;

/**
 * @PROJECT_NAME: parkingmanage
 * @PACKAGE_NAME: com.parkingmanage.commom.config
 * @NAME: InterceptorConfig
 * @author:yuli
 * @DATE: 2022/1/18 17:57
 * @description: 拦截config
 */
// @Configuration
public class InterceptorConfig extends WebMvcConfigurationSupport {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Resource
    private  LoginInterceptor loginInterceptor;
    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        // 多个拦截器组成一个拦截器链
        // addPathPatterns 用于添加拦截规则，/**表示拦截所有请求
        // excludePathPatterns 用户排除拦截
        registry.addInterceptor(loginInterceptor).addPathPatterns("/**")
                .excludePathPatterns(
                        // "/avatar/**",
                        "/**/login",
                        "/**/upload"
                );
        super.addInterceptors(registry);
    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                //放行哪些原始域
                .allowedOrigins("*")
                .allowedMethods(new String[]{"GET", "POST", "PUT", "DELETE"})
                .allowedHeaders("*")
                .exposedHeaders("*");
    }
}