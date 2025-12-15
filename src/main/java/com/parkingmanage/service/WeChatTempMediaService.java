package com.parkingmanage.service;

import com.parkingmanage.entity.WeChatTempMedia;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;

/**
 * 微信临时素材服务接口
 * 
 * @author System
 * @since 2024-01-01
 */
public interface WeChatTempMediaService {
    
    /**
     * 上传临时素材到微信服务器
     * 
     * @param file 要上传的文件
     * @param mediaType 素材类型：image、voice、video、thumb
     * @param description 素材用途说明
     * @return 上传结果，包含media_id等信息
     */
    Map<String, Object> uploadTempMedia(MultipartFile file, String mediaType, String description);
    
    /**
     * 上传本地文件到微信服务器
     * 
     * @param localFile 本地文件
     * @param mediaType 素材类型
     * @param description 素材用途说明
     * @return 上传结果
     */
    Map<String, Object> uploadTempMediaFromLocal(File localFile, String mediaType, String description);
    
    /**
     * 获取有效的临时素材media_id
     * 如果已过期则重新上传
     * 
     * @param description 素材用途说明
     * @return media_id
     */
    String getValidMediaId(String description);
    
    /**
     * 刷新指定用途的临时素材
     * 
     * @param description 素材用途说明
     * @return 是否刷新成功
     */
    boolean refreshMediaId(String description);
    
    /**
     * 批量刷新所有临时素材
     * 
     * @return 刷新成功的数量
     */
    int refreshAllMediaIds();
    
    /**
     * 根据用途查询临时素材
     * 
     * @param description 素材用途说明
     * @return 临时素材信息
     */
    WeChatTempMedia getByDescription(String description);
}
