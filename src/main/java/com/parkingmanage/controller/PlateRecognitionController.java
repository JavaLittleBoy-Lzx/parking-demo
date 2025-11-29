package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.service.PlateRecognitionService;
import com.parkingmanage.vo.PlateRecognitionRequest;
import com.parkingmanage.vo.PlateRecognitionResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 车牌识别控制器
 * @author AI Assistant
 */
@Slf4j
@RestController
@RequestMapping("/api/plate")
@Api(tags = "车牌识别接口")
public class PlateRecognitionController {

    @Resource
    private PlateRecognitionService plateRecognitionService;

    @ApiOperation("车牌识别 - Base64图片")
    @PostMapping("/recognize")
    public ResponseEntity<Result<PlateRecognitionResult>> recognizePlate(@RequestBody PlateRecognitionRequest request) {
        try {
            log.info("开始车牌识别, image size: {}, multiDetect: {}, detectComplete: {}, detectRisk: {}", 
                request.getImage().length(), request.isMultiDetect(), request.isDetectComplete(), request.isDetectRisk());
            
            PlateRecognitionResult result = plateRecognitionService.recognizePlateFromBase64(request.getImage(), request);
            
            if (result != null && result.isSuccess()) {
                log.info("车牌识别成功: {}", result.getPlateNumber());
                return ResponseEntity.ok(Result.success(result));
            } else {
                log.warn("车牌识别失败: {}", result != null ? result.getErrorMessage() : "未知错误");
                return ResponseEntity.ok(Result.error(result != null ? result.getErrorMessage() : "未检测到车牌"));
            }
        } catch (Exception e) {
            log.error("车牌识别异常", e);
            return ResponseEntity.ok(Result.error("车牌识别失败: " + e.getMessage()));
        }
    }

    @ApiOperation("车牌识别 - 文件上传")
    @PostMapping("/recognize/upload")
    public ResponseEntity<Result<PlateRecognitionResult>> recognizePlateFromFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.ok(Result.error("文件不能为空"));
            }

            log.info("开始车牌识别, 文件名: {}, 大小: {}", file.getOriginalFilename(), file.getSize());
            
            PlateRecognitionResult result = plateRecognitionService.recognizePlateFromFile(file);
            
            if (result != null && result.isSuccess()) {
                log.info("车牌识别成功: {}", result.getPlateNumber());
                return ResponseEntity.ok(Result.success(result));
            } else {
                log.warn("车牌识别失败: 未检测到车牌");
                return ResponseEntity.ok(Result.error("未检测到车牌"));
            }
        } catch (IOException e) {
            log.error("文件读取异常", e);
            return ResponseEntity.ok(Result.error("文件读取失败"));
        } catch (Exception e) {
            log.error("车牌识别异常", e);
            return ResponseEntity.ok(Result.error("车牌识别失败: " + e.getMessage()));
        }
    }

    @ApiOperation("批量车牌识别")
    @PostMapping("/recognize/batch")
    public ResponseEntity<Result<java.util.List<PlateRecognitionResult>>> recognizePlatesBatch(@RequestParam("files") MultipartFile[] files) {
        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.ok(Result.error("文件不能为空"));
            }

            log.info("开始批量车牌识别, 文件数量: {}", files.length);
            
            java.util.List<PlateRecognitionResult> results = plateRecognitionService.recognizePlatesBatch(files);
            
            return ResponseEntity.ok(Result.success(results));
        } catch (Exception e) {
            log.error("批量车牌识别异常", e);
            return ResponseEntity.ok(Result.error("批量车牌识别失败: " + e.getMessage()));
        }
    }

    @ApiOperation("调试图片格式")
    @PostMapping("/debug/image-format")
    public ResponseEntity<Result<Map<String, Object>>> debugImageFormat(@RequestBody PlateRecognitionRequest request) {
        try {
            String base64Image = request.getImage();
            Map<String, Object> debugInfo = new HashMap<>();
            
            // 分析base64数据
            debugInfo.put("originalLength", base64Image.length());
            debugInfo.put("hasDataHeader", base64Image.contains("data:image"));
            debugInfo.put("firstChars", base64Image.length() > 50 ? base64Image.substring(0, 50) : base64Image);
            
            // 清理base64数据
            String cleaned = plateRecognitionService.cleanBase64Image(base64Image);
            debugInfo.put("cleanedLength", cleaned.length());
            debugInfo.put("cleanedFirstChars", cleaned.length() > 50 ? cleaned.substring(0, 50) : cleaned);
            
            // 尝试解码验证
            try {
                byte[] decoded = java.util.Base64.getDecoder().decode(cleaned);
                debugInfo.put("decodedSize", decoded.length);
                
                // 检查文件头
                if (decoded.length >= 4) {
                    String header = String.format("%02X%02X%02X%02X", 
                        decoded[0] & 0xFF, decoded[1] & 0xFF, decoded[2] & 0xFF, decoded[3] & 0xFF);
                    debugInfo.put("fileHeader", header);
                    
                    // 判断图片格式
                    String format = "unknown";
                    if (header.startsWith("FFD8")) {
                        format = "JPEG";
                    } else if (header.startsWith("8950")) {
                        format = "PNG";
                    } else if (header.startsWith("424D")) {
                        format = "BMP";
                    }
                    debugInfo.put("detectedFormat", format);
                }
                
                debugInfo.put("valid", true);
            } catch (Exception e) {
                debugInfo.put("valid", false);
                debugInfo.put("error", e.getMessage());
            }
            
            return ResponseEntity.ok(Result.success(debugInfo));
            
        } catch (Exception e) {
            log.error("调试图片格式异常", e);
            return ResponseEntity.ok(Result.error("调试失败: " + e.getMessage()));
        }
    }

    @ApiOperation("调试车牌识别API原始响应")
    @PostMapping("/debug/raw-response")
    public ResponseEntity<Result<Map<String, Object>>> debugRawResponse(@RequestBody PlateRecognitionRequest request) {
        try {
            log.info("调试车牌识别API原始响应");
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("requestTime", new java.util.Date().toString());
            debugInfo.put("imageLength", request.getImage().length());
            
            // 获取访问令牌
            String token = plateRecognitionService.getAccessToken();
            if (token == null) {
                debugInfo.put("error", "获取访问令牌失败");
                return ResponseEntity.ok(Result.success(debugInfo));
            }
            
            // 调用百度API获取原始响应
            String cleanBase64 = plateRecognitionService.cleanBase64Image(request.getImage());
            String rawResponse = com.parkingmanage.utils.BaiduAIHttpClient.sendLicensePlateRequest(
                token, cleanBase64, request.isMultiDetect()
            );
            
            debugInfo.put("rawResponse", rawResponse);
            debugInfo.put("tokenObtained", true);
            debugInfo.put("cleanedImageLength", cleanBase64.length());
            
            // 尝试解析JSON结构
            try {
                com.alibaba.fastjson.JSONObject jsonResponse = com.alibaba.fastjson.JSON.parseObject(rawResponse);
                debugInfo.put("responseStructure", analyzeJsonStructure(jsonResponse));
            } catch (Exception e) {
                debugInfo.put("jsonParseError", e.getMessage());
            }
            
            return ResponseEntity.ok(Result.success(debugInfo));
            
        } catch (Exception e) {
            log.error("调试车牌识别API异常", e);
            return ResponseEntity.ok(Result.error("调试失败: " + e.getMessage()));
        }
    }
    
    /**
     * 分析JSON结构
     */
    private Map<String, Object> analyzeJsonStructure(com.alibaba.fastjson.JSONObject json) {
        Map<String, Object> structure = new HashMap<>();
        
        for (String key : json.keySet()) {
            Object value = json.get(key);
            if (value != null) {
                structure.put(key, value.getClass().getSimpleName() + ": " + value.toString());
            } else {
                structure.put(key, "null");
            }
        }
        
        // 特别分析words_result结构
        if (json.containsKey("words_result")) {
            Object wordsResult = json.get("words_result");
            if (wordsResult instanceof com.alibaba.fastjson.JSONArray) {
                com.alibaba.fastjson.JSONArray array = (com.alibaba.fastjson.JSONArray) wordsResult;
                if (!array.isEmpty()) {
                    Object firstItem = array.get(0);
                    if (firstItem instanceof com.alibaba.fastjson.JSONObject) {
                        Map<String, Object> plateStructure = new HashMap<>();
                        com.alibaba.fastjson.JSONObject plateInfo = (com.alibaba.fastjson.JSONObject) firstItem;
                        for (String plateKey : plateInfo.keySet()) {
                            Object plateValue = plateInfo.get(plateKey);
                            if (plateValue != null) {
                                plateStructure.put(plateKey, plateValue.getClass().getSimpleName() + ": " + plateValue.toString());
                            }
                        }
                        structure.put("words_result_first_item_structure", plateStructure);
                    }
                }
            }
        }
        
        return structure;
    }
} 