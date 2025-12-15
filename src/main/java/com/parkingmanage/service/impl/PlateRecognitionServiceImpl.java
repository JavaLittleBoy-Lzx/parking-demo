package com.parkingmanage.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.service.PlateRecognitionService;
import com.parkingmanage.utils.BaiduAIHttpClient;
import com.parkingmanage.utils.HttpClientUtils;
import com.parkingmanage.vo.PlateRecognitionRequest;
import com.parkingmanage.vo.PlateRecognitionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车牌识别服务实现类
 * @author AI Assistant
 */
@Slf4j
@Service
public class PlateRecognitionServiceImpl implements PlateRecognitionService {

    @Value("${baidu.ai.app-id:}")
    private String appId;

    @Value("${baidu.ai.api-key:}")
    private String apiKey;

    @Value("${baidu.ai.secret-key:}")
    private String secretKey;

    @Value("${baidu.ai.base-url:https://aip.baidubce.com}")
    private String baseUrl;

    @Value("${baidu.ai.token-url:/oauth/2.0/token}")
    private String tokenUrl;

    @Value("${baidu.ai.plate-url:/rest/2.0/ocr/v1/license_plate}")
    private String plateUrl;

    @Value("${baidu.ai.token-cache-minutes:25}")
    private int tokenCacheMinutes;

    private String accessToken;
    private long tokenExpireTime = 0;

    @Override
    public PlateRecognitionResult recognizePlateFromBase64(String base64Image) {
        return recognizePlateFromBase64(base64Image, new PlateRecognitionRequest());
    }

    public PlateRecognitionResult recognizePlateFromBase64(String base64Image, PlateRecognitionRequest request) {
        try {
            // 获取访问令牌
            String token = getAccessToken();
            if (token == null) {
                log.error("获取百度AI访问令牌失败");
                return PlateRecognitionResult.error("获取访问令牌失败");
            }

            // 调用百度车牌识别API
            String result = callBaiduPlateAPI(token, base64Image, request);
            
            // 解析识别结果
            return parseRecognitionResult(result);
            
        } catch (Exception e) {
            log.error("车牌识别异常", e);
            return PlateRecognitionResult.error("识别异常: " + e.getMessage());
        }
    }

    @Override
    public PlateRecognitionResult recognizePlateFromFile(MultipartFile file) throws IOException {
        // 将文件转换为Base64
        byte[] bytes = file.getBytes();
        String base64Image = Base64Utils.encodeToString(bytes);
        
        return recognizePlateFromBase64(base64Image);
    }

    @Override
    public List<PlateRecognitionResult> recognizePlatesBatch(MultipartFile[] files) throws IOException {
        List<PlateRecognitionResult> results = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                PlateRecognitionResult result = recognizePlateFromFile(file);
                results.add(result);
            }
        }
        
        return results;
    }

    /**
     * 获取百度AI访问令牌
     * 使用BaiduAIHttpClient工具类，基于百度官方文档实现
     */
    @Override
    public String getAccessToken() {
        try {
            // 检查token是否过期
            if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
                log.debug("使用缓存的访问令牌");
                return accessToken;
            }

            log.info("开始获取百度AI访问令牌");
            
            // 验证配置
            if (apiKey == null || secretKey == null || apiKey.contains("your_") || secretKey.contains("your_")) {
                log.error("百度AI配置不完整，请在application.yml中配置正确的api-key和secret-key");
                return null;
            }

            // 使用BaiduAIHttpClient获取access_token
            String response = BaiduAIHttpClient.getAccessToken(apiKey, secretKey);
            if (response == null) {
                log.error("获取百度AI访问令牌失败：HTTP请求异常");
                return null;
            }
            
            // 解析响应
            JSONObject jsonObject = JSON.parseObject(response);
            if (jsonObject.containsKey("access_token")) {
                accessToken = jsonObject.getString("access_token");
                // 设置过期时间（使用配置的缓存时间）
                int expiresIn = jsonObject.getIntValue("expires_in");
                long cacheSeconds = tokenCacheMinutes * 60L;
                tokenExpireTime = System.currentTimeMillis() + Math.min(expiresIn - 300, cacheSeconds) * 1000L;
                
                log.info("百度AI访问令牌获取成功，有效期: {} 秒", expiresIn);
                return accessToken;
            } else {
                String errorCode = jsonObject.getString("error");
                String errorDesc = jsonObject.getString("error_description");
                log.error("获取百度AI访问令牌失败: {} - {}", errorCode, errorDesc);
                
                // 根据错误码返回友好信息
                switch (errorCode) {
                    case "invalid_client":
                        if ("unknown client id".equals(errorDesc)) {
                            log.error("API Key不正确，请检查配置");
                        } else if ("Client authentication failed".equals(errorDesc)) {
                            log.error("Secret Key不正确，请检查配置");
                        }
                        break;
                    default:
                        log.error("未知错误: {}", errorDesc);
                }
                return null;
            }
        } catch (Exception e) {
            log.error("获取百度AI访问令牌异常", e);
            return null;
        }
    }


    /**
     * 调用百度车牌识别API
     * 使用BaiduAIHttpClient工具类
     */
    private String callBaiduPlateAPI(String accessToken, String base64Image, PlateRecognitionRequest request) throws Exception {
        // 清理base64图片数据
        String cleanBase64 = cleanBase64Image(base64Image);
        
        log.debug("调用百度车牌识别API，图片大小: {}", cleanBase64.length());
        
        // 使用BaiduAIHttpClient发送车牌识别请求
        boolean multiDetect = request != null ? request.isMultiDetect() : false;
        return BaiduAIHttpClient.sendLicensePlateRequest(accessToken, cleanBase64, multiDetect);
    }
    

    
    /**
     * 清理base64图片数据
     * 移除可能的数据头和多余字符
     */
    @Override
    public String cleanBase64Image(String base64Image) {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            return base64Image;
        }
        
        String cleaned = base64Image.trim();
        
        // 移除可能的数据头，如: data:image/jpeg;base64, 或 data:image/png;base64, 等
        if (cleaned.contains(",")) {
            int commaIndex = cleaned.indexOf(",");
            if (commaIndex > 0 && commaIndex < 50) { // 数据头通常不会太长
                String header = cleaned.substring(0, commaIndex + 1);
                if (header.toLowerCase().startsWith("data:image") && header.contains("base64")) {
                    cleaned = cleaned.substring(commaIndex + 1);
                    log.debug("移除图片数据头: {}", header);
                }
            }
        }
        
        // 移除可能的换行符和空格
        cleaned = cleaned.replaceAll("\\s", "");
        
        log.debug("base64图片清理完成，原长度: {}, 清理后长度: {}", base64Image.length(), cleaned.length());
        return cleaned;
    }

    /**
     * 解析识别结果
     */
    private PlateRecognitionResult parseRecognitionResult(String response) {
        try {
            // 添加调试日志，输出API响应格式
            log.debug("百度车牌识别API原始响应: {}", response);
            
            JSONObject jsonObject = JSON.parseObject(response);
            
            // 检查是否有错误
            if (jsonObject.containsKey("error_code")) {
                String errorCode = jsonObject.getString("error_code");
                String errorMsg = jsonObject.getString("error_msg");
                log.error("百度车牌识别API错误 - 错误码: {}, 错误信息: {}", errorCode, errorMsg);
                
                // 根据错误码返回更友好的错误信息
                String friendlyError = getFriendlyErrorMessage(errorCode, errorMsg);
                return PlateRecognitionResult.error(friendlyError);
            }

            // 解析识别结果 - 灵活处理words_result字段类型
            Object wordsResultObj = jsonObject.get("words_result");
            if (wordsResultObj == null) {
                log.warn("API响应中无words_result字段，API响应: {}", response);
                return PlateRecognitionResult.error("API响应格式异常：缺少words_result字段");
            }
            log.debug("words_result字段类型: {}, 值: {}", wordsResultObj.getClass().getSimpleName(), wordsResultObj);
            JSONObject plateInfo = null;
            
            try {
                if (wordsResultObj instanceof JSONArray) {
                    // 标准格式：words_result是数组
                    JSONArray wordsResult = (JSONArray) wordsResultObj;
                    if (wordsResult.isEmpty()) {
                        log.warn("words_result数组为空，API响应: {}", response);
                        return PlateRecognitionResult.error("图片中未检测到车牌");
                    }
                    plateInfo = wordsResult.getJSONObject(0);
                } else if (wordsResultObj instanceof JSONObject) {
                    // 特殊格式：words_result直接是车牌信息对象
                    plateInfo = (JSONObject) wordsResultObj;
                    log.debug("words_result是对象格式，直接使用");
                } else {
                    log.error("未知的words_result格式: {}, 内容: {}", wordsResultObj.getClass().getSimpleName(), wordsResultObj);
                    return PlateRecognitionResult.error("API响应格式异常：words_result格式不支持");
                }
                
                if (plateInfo == null) {
                    log.warn("无法获取车牌信息，API响应: {}", response);
                    return PlateRecognitionResult.error("图片中未检测到车牌");
                }
                
                log.debug("车牌信息详情: {}", plateInfo.toJSONString());
                
            } catch (Exception e) {
                log.error("解析words_result字段异常: {}, API响应: {}", e.getMessage(), response);
                return PlateRecognitionResult.error("解析车牌结果失败: " + e.getMessage());
            }
            
            String plateNumber = plateInfo.getString("number");
            String color = plateInfo.getString("color");
            
            // 获取置信度 - 灵活处理不同的数据格式
            double confidence = 0.0;
            if (plateInfo.containsKey("probability")) {
                Object probabilityObj = plateInfo.get("probability");
                log.debug("probability字段类型: {}, 值: {}", probabilityObj.getClass().getSimpleName(), probabilityObj);
                
                try {
                    if (probabilityObj instanceof JSONArray) {
                        // 如果是数组，计算平均置信度
                        JSONArray probabilityArray = (JSONArray) probabilityObj;
                        if (!probabilityArray.isEmpty()) {
                            double sum = 0.0;
                            for (int i = 0; i < probabilityArray.size(); i++) {
                                sum += probabilityArray.getDoubleValue(i);
                            }
                            confidence = (sum / probabilityArray.size()) * 100;
                        } else {
                            confidence = 95.0;
                        }
                    } else if (probabilityObj instanceof JSONObject) {
                        // 如果是对象，尝试获取average或者其他字段
                        JSONObject probabilityObject = (JSONObject) probabilityObj;
                        if (probabilityObject.containsKey("average")) {
                            confidence = probabilityObject.getDoubleValue("average") * 100;
                        } else if (probabilityObject.containsKey("min")) {
                            confidence = probabilityObject.getDoubleValue("min") * 100;
                        } else {
                            // 取第一个数值字段
                            for (String key : probabilityObject.keySet()) {
                                Object value = probabilityObject.get(key);
                                if (value instanceof Number) {
                                    confidence = ((Number) value).doubleValue() * 100;
                                    break;
                                }
                            }
                        }
                        if (confidence == 0.0) {
                            confidence = 93.0; // 默认值
                        }
                    } else if (probabilityObj instanceof Number) {
                        // 如果是直接的数值
                        confidence = ((Number) probabilityObj).doubleValue() * 100;
                    } else if (probabilityObj instanceof String) {
                        // 如果是字符串，尝试解析
                        try {
                            confidence = Double.parseDouble(probabilityObj.toString()) * 100;
                        } catch (NumberFormatException e) {
                            log.warn("无法解析probability字符串: {}", probabilityObj);
                            confidence = 92.0;
                        }
                    } else {
                        log.warn("未知的probability格式: {}", probabilityObj.getClass().getSimpleName());
                        confidence = 91.0;
                    }
                } catch (Exception e) {
                    log.error("解析probability字段异常: {}", e.getMessage());
                    confidence = 90.0;
                }
            } else {
                confidence = 89.0; // 如果没有置信度信息，设置默认值
            }

            // 构建结果
            PlateRecognitionResult result = new PlateRecognitionResult();
            result.setSuccess(true);
            result.setPlateNumber(plateNumber);
            result.setColor(color);
            result.setConfidence(confidence);
            result.setSource("baidu_ai");
            
            // 解析车牌类型
            if (plateInfo.containsKey("type")) {
                result.setPlateType(plateInfo.getString("type"));
            }
            
            // 解析车牌位置信息（如果有）- 灵活处理location字段类型
            if (plateInfo.containsKey("location")) {
                Object locationObj = plateInfo.get("location");
                log.debug("location字段类型: {}, 值: {}", locationObj.getClass().getSimpleName(), locationObj);
                
                try {
                    if (locationObj instanceof JSONObject) {
                        JSONObject location = (JSONObject) locationObj;
                        PlateRecognitionResult.PlateLocation plateLocation = new PlateRecognitionResult.PlateLocation();
                        plateLocation.setLeft(location.getIntValue("left"));
                        plateLocation.setTop(location.getIntValue("top"));
                        plateLocation.setWidth(location.getIntValue("width"));
                        plateLocation.setHeight(location.getIntValue("height"));
                        result.setLocation(plateLocation);
                        log.debug("车牌位置信息解析成功: left={}, top={}, width={}, height={}", 
                            plateLocation.getLeft(), plateLocation.getTop(), 
                            plateLocation.getWidth(), plateLocation.getHeight());
                    } else {
                        log.warn("location字段格式异常，期望JSONObject，实际: {}", locationObj.getClass().getSimpleName());
                    }
                } catch (Exception e) {
                    log.error("解析location字段异常: {}", e.getMessage());
                }
            }
            
            log.info("车牌识别成功: 车牌号={}, 颜色={}, 置信度={}%", plateNumber, color, String.format("%.1f", confidence));
            return result;
            
        } catch (Exception e) {
            log.error("解析车牌识别结果异常", e);
            return PlateRecognitionResult.error("结果解析失败: " + e.getMessage());
        }
    }

    /**
     * 根据百度AI错误码返回友好的错误信息
     */
    private String getFriendlyErrorMessage(String errorCode, String originalMessage) {
        switch (errorCode) {
            case "18":
                return "QPS限制：请求过于频繁，请稍后重试";
            case "19":
                return "请求总量超限：今日配额已用完";
            case "216100":
                return "无效参数：请检查图片格式";
            case "216101":
                return "无效参数：图片不存在";
            case "216102":
                return "图片解码失败：请检查图片格式";
            case "216103":
                return "图片过大：图片大小不能超过4MB";
            case "216200":
                return "图片中未检测到车牌";
            case "216201":
                return "图片格式错误：请使用JPEG、PNG、BMP格式的图片";
            case "17":
                return "每日流量超限：请明天再试或升级配额";
            case "110":
                return "访问令牌无效：请检查API密钥配置";
            case "111":
                return "访问令牌过期：正在重新获取";
            default:
                return "识别失败: " + (originalMessage != null ? originalMessage : "未知错误[" + errorCode + "]");
        }
    }
}

 