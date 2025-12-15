package com.parkingmanage.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.MultipartConfigElement;

/**
 * @PACKAGE_NAME:  com.parkingmanage.commom.config
 * @NAME: WebMvcConfig
 * @author: yuli
 * @Version: 1.0
 * @DATE: 2021/12/8 13:03
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 在配置文件中配置的文件保存路径
     */
    @Value("${file.upload-path}")
    private String uploadPath;

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        //文件最大KB,MB
        factory.setMaxFileSize(DataSize.parse("1024MB"));
        //设置总上传数据总大小
        factory.setMaxRequestSize(DataSize.parse("1024MB"));
        return factory.createMultipartConfig();
    }

    /**
     * 这里是映射文件路径的方法
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置模板资源路径
//        registry.addResourceHandler("/uploadfile/**").addResourceLocations(ResourceUtils.FILE_URL_PREFIX + uploadPath);
        registry.addResourceHandler("/uploadfile/**").addResourceLocations("file:C:/Users/Administrator/Desktop/static/images");
        
        // 🆕 配置头像上传路径映射（项目根目录下的 uploads 文件夹）
        String projectRoot = System.getProperty("user.dir");
        String uploadsPath = projectRoot + java.io.File.separator + "uploads" + java.io.File.separator;
        
        // 确保使用正确的文件URL格式
        String fileUrl = "file:///" + uploadsPath.replace("\\", "/");
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(fileUrl);
        
        System.out.println("📁 [静态资源映射] 项目根目录: " + projectRoot);
        System.out.println("📁 [静态资源映射] uploads目录: " + uploadsPath);
        System.out.println("📁 [静态资源映射] 文件URL: " + fileUrl);
        System.out.println("📁 [静态资源映射] 配置完成: /uploads/** -> " + fileUrl);
        
        // 验证目录是否存在
        java.io.File uploadsDir = new java.io.File(uploadsPath);
        if (!uploadsDir.exists()) {
            boolean created = uploadsDir.mkdirs();
            System.out.println("📁 [静态资源映射] uploads目录不存在，创建结果: " + created);
        } else {
            System.out.println("✅ [静态资源映射] uploads目录已存在");
        }
        
        // registry.addResourceHandler("/avatar/").addResourceLocations(ResourceUtils.FILE_URL_PREFIX+"/avatar/");
        // registry.addResourceHandler("/files/").addResourceLocations(ResourceUtils.FILE_URL_PREFIX + System.getProperty("user.dir") + "/files/");
    }
}
