package com.parkingmanage.service;

import com.parkingmanage.vo.PlateRecognitionRequest;
import com.parkingmanage.vo.PlateRecognitionResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 车牌识别服务接口
 * @author AI Assistant
 */
public interface PlateRecognitionService {

    /**
     * 从Base64图片识别车牌
     * @param base64Image Base64编码的图片
     * @return 识别结果
     */
    PlateRecognitionResult recognizePlateFromBase64(String base64Image);

    /**
     * 从Base64图片识别车牌（带配置参数）
     * @param base64Image Base64编码的图片
     * @param request 识别配置参数
     * @return 识别结果
     */
    PlateRecognitionResult recognizePlateFromBase64(String base64Image, PlateRecognitionRequest request);

    /**
     * 从文件识别车牌
     * @param file 图片文件
     * @return 识别结果
     * @throws IOException 文件读取异常
     */
    PlateRecognitionResult recognizePlateFromFile(MultipartFile file) throws IOException;

    /**
     * 批量识别车牌
     * @param files 图片文件数组
     * @return 识别结果列表
     * @throws IOException 文件读取异常
     */
    List<PlateRecognitionResult> recognizePlatesBatch(MultipartFile[] files) throws IOException;

    /**
     * 清理base64图片数据
     * 移除可能的数据头和多余字符
     * @param base64Image 原始base64数据
     * @return 清理后的base64数据
     */
    String cleanBase64Image(String base64Image);

    /**
     * 获取百度AI访问令牌（用于调试）
     * @return 访问令牌
     */
    String getAccessToken();
} 