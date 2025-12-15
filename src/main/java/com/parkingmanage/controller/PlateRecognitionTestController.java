package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.service.PlateRecognitionService;
import com.parkingmanage.vo.PlateRecognitionResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 车牌识别测试控制器
 * 用于测试百度API连接和调试
 * @author AI Assistant
 */
@Slf4j
@RestController
@RequestMapping("/api/plate/test")
@Api(tags = "车牌识别测试接口")
public class PlateRecognitionTestController {

    @Resource
    private PlateRecognitionService plateRecognitionService;

    @Value("${baidu.ai.api-key:}")
    private String apiKey;

    @Value("${baidu.ai.secret-key:}")
    private String secretKey;

    @Value("${baidu.ai.base-url:https://aip.baidubce.com}")
    private String baseUrl;

    @ApiOperation("检查百度AI配置")
    @GetMapping("/config")
    public ResponseEntity<Result<Map<String, Object>>> checkConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // 检查配置完整性
        boolean isConfigComplete = apiKey != null && secretKey != null 
                && !apiKey.isEmpty() && !secretKey.isEmpty()
                && !apiKey.contains("your_") && !secretKey.contains("your_");
        
        config.put("configComplete", isConfigComplete);
        config.put("baseUrl", baseUrl);
        config.put("apiKeyConfigured", apiKey != null && !apiKey.isEmpty() && !apiKey.contains("your_"));
        config.put("secretKeyConfigured", secretKey != null && !secretKey.isEmpty() && !secretKey.contains("your_"));
        
        if (!isConfigComplete) {
            config.put("message", "请在application.yml中配置正确的百度AI密钥");
            config.put("steps", new String[]{
                "1. 访问 https://ai.baidu.com/",
                "2. 注册并创建车牌识别应用",
                "3. 获取API Key和Secret Key",
                "4. 在application.yml中配置baidu.ai.api-key和baidu.ai.secret-key"
            });
        }
        
        return ResponseEntity.ok(Result.success(config));
    }

    @ApiOperation("测试百度AI连接")
    @PostMapping("/connection")
    public ResponseEntity<Result<Map<String, Object>>> testConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 使用一个简单的测试图片（base64编码的小图片）
            String testBase64 = getTestImageBase64();
            
            log.info("开始测试百度AI连接...");
            PlateRecognitionResult recognition = plateRecognitionService.recognizePlateFromBase64(testBase64);
            
            result.put("connectionSuccess", true);
            result.put("message", "百度AI连接测试成功");
            
            if (recognition.isSuccess()) {
                result.put("recognitionSuccess", true);
                result.put("plateResult", recognition);
            } else {
                result.put("recognitionSuccess", false);
                result.put("recognitionError", recognition.getErrorMessage());
                result.put("note", "连接成功但识别失败（可能是测试图片没有车牌，这是正常的）");
            }
            
        } catch (Exception e) {
            log.error("百度AI连接测试失败", e);
            result.put("connectionSuccess", false);
            result.put("error", e.getMessage());
            result.put("message", "百度AI连接测试失败，请检查网络和配置");
        }
        
        return ResponseEntity.ok(Result.success(result));
    }

    @ApiOperation("获取测试用车牌图片示例")
    @GetMapping("/sample-image")
    public ResponseEntity<Result<Map<String, String>>> getSampleImage() {
        Map<String, String> result = new HashMap<>();
        result.put("description", "这是一个包含车牌的测试图片base64编码");
        result.put("usage", "可以将此base64数据用于测试车牌识别功能");
        
        // 这里应该放一个真实的包含车牌的图片的base64编码
        // 由于篇幅限制，这里只是示例
        result.put("base64", "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
        result.put("note", "实际使用时请替换为真实的车牌图片base64编码");
        
        return ResponseEntity.ok(Result.success(result));
    }
    
    @ApiOperation("调试base64图片格式")
    @PostMapping("/debug-image")
    public ResponseEntity<Result<Map<String, Object>>> debugImage(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String base64Image = request.get("image");
            if (base64Image == null || base64Image.trim().isEmpty()) {
                result.put("error", "图片数据为空");
                return ResponseEntity.ok(Result.success(result));
            }
            
            // 分析base64图片数据
            result.put("原始长度", base64Image.length());
            result.put("是否包含数据头", base64Image.contains("data:image"));
            result.put("是否包含逗号", base64Image.contains(","));
            result.put("是否包含空格", base64Image.contains(" "));
            result.put("是否包含换行", base64Image.contains("\n") || base64Image.contains("\r"));
            
            // 显示前100个字符
            String preview = base64Image.length() > 100 ? base64Image.substring(0, 100) + "..." : base64Image;
            result.put("前100字符", preview);
            
            // 尝试识别图片格式（如果有数据头）
            if (base64Image.contains("data:image")) {
                int commaIndex = base64Image.indexOf(",");
                if (commaIndex > 0) {
                    String header = base64Image.substring(0, commaIndex);
                    result.put("数据头", header);
                    
                    String actualBase64 = base64Image.substring(commaIndex + 1);
                    result.put("清理后长度", actualBase64.length());
                }
            }
            
            // 检查base64字符是否有效
            try {
                String cleanBase64 = base64Image;
                if (cleanBase64.contains("data:image")) {
                    cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
                }
                cleanBase64 = cleanBase64.replaceAll("\\s", "");
                
                // 尝试解码
                byte[] decoded = java.util.Base64.getDecoder().decode(cleanBase64);
                result.put("解码成功", true);
                result.put("解码后字节长度", decoded.length);
                
                // 检查图片魔数
                if (decoded.length >= 4) {
                    String magicNumber = String.format("%02X%02X%02X%02X", 
                        decoded[0], decoded[1], decoded[2], decoded[3]);
                    result.put("魔数", magicNumber);
                    
                    // 识别图片格式
                    if (magicNumber.startsWith("FFD8")) {
                        result.put("图片格式", "JPEG");
                    } else if (magicNumber.startsWith("8950")) {
                        result.put("图片格式", "PNG");
                    } else if (magicNumber.startsWith("424D")) {
                        result.put("图片格式", "BMP");
                    } else {
                        result.put("图片格式", "未知格式");
                    }
                }
                
            } catch (Exception e) {
                result.put("解码成功", false);
                result.put("解码错误", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("调试图片失败", e);
            result.put("分析失败", e.getMessage());
        }
        
        return ResponseEntity.ok(Result.success(result));
    }

    /**
     * 获取测试用的小图片base64编码
     * 这是一个1x1像素的透明PNG图片
     */
    private String getTestImageBase64() {
        // 1x1像素的透明PNG图片
        return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
    }
} 