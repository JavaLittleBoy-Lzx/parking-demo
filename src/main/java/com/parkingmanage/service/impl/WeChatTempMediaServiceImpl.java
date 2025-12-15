package com.parkingmanage.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkingmanage.entity.WeChatTempMedia;
import com.parkingmanage.mapper.WeChatTempMediaMapper;
import com.parkingmanage.service.WeChatTempMediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信临时素材服务实现类
 * 
 * @author System
 * @since 2024-01-01
 */
@Slf4j
@Service
public class WeChatTempMediaServiceImpl implements WeChatTempMediaService {
    
    @Value("${wechat.public.appid}")
    private String appId;
    
    @Value("${wechat.public.secret}")
    private String secret;
    
    @Value("${wechat.temp.media.storage.path:d:/temp/wechat/media/}")
    private String mediaStoragePath;
    
    @Resource
    private WeChatTempMediaMapper weChatTempMediaMapper;
    
    @Override
    public Map<String, Object> uploadTempMedia(MultipartFile file, String mediaType, String description) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("📤 开始上传临时素材 - 类型: {}, 用途: {}, 文件名: {}", 
                mediaType, description, file.getOriginalFilename());
            
            // 1. 保存文件到本地
            String localFilePath = saveFileToLocal(file, description);
            
            // 2. 获取access_token
            String accessToken = getAccessToken();
            if (accessToken == null) {
                result.put("success", false);
                result.put("message", "获取access_token失败");
                return result;
            }
            
            // 3. 上传到微信服务器
            String uploadUrl = String.format(
                "https://api.weixin.qq.com/cgi-bin/media/upload?access_token=%s&type=%s",
                accessToken, mediaType
            );
            
            JSONObject uploadResult = uploadFileToWeChat(uploadUrl, new File(localFilePath));
            
            if (uploadResult.containsKey("media_id")) {
                String mediaId = uploadResult.getString("media_id");
                Long createdAt = uploadResult.getLong("created_at");
                
                // 4. 保存或更新数据库记录
                saveOrUpdateMediaRecord(mediaId, mediaType, description, 
                    file.getOriginalFilename(), localFilePath, file.getSize(), createdAt);
                
                result.put("success", true);
                result.put("mediaId", mediaId);
                result.put("createdAt", createdAt);
                result.put("message", "上传成功");
                
                log.info("✅ 临时素材上传成功 - media_id: {}", mediaId);
            } else {
                Integer errcode = uploadResult.getInteger("errcode");
                String errmsg = uploadResult.getString("errmsg");
                result.put("success", false);
                result.put("message", "上传失败: " + errmsg);
                log.error("❌ 临时素材上传失败 - errcode: {}, errmsg: {}", errcode, errmsg);
            }
            
        } catch (Exception e) {
            log.error("❌ 上传临时素材异常", e);
            result.put("success", false);
            result.put("message", "上传异常: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> uploadTempMediaFromLocal(File localFile, String mediaType, String description) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("📤 从本地文件上传临时素材 - 类型: {}, 用途: {}, 文件: {}", 
                mediaType, description, localFile.getName());
            
            // 1. 获取access_token
            String accessToken = getAccessToken();
            if (accessToken == null) {
                result.put("success", false);
                result.put("message", "获取access_token失败");
                return result;
            }
            
            // 2. 上传到微信服务器
            String uploadUrl = String.format(
                "https://api.weixin.qq.com/cgi-bin/media/upload?access_token=%s&type=%s",
                accessToken, mediaType
            );
            
            JSONObject uploadResult = uploadFileToWeChat(uploadUrl, localFile);
            
            if (uploadResult.containsKey("media_id")) {
                String mediaId = uploadResult.getString("media_id");
                Long createdAt = uploadResult.getLong("created_at");
                
                // 3. 更新数据库记录
                saveOrUpdateMediaRecord(mediaId, mediaType, description, 
                    localFile.getName(), localFile.getAbsolutePath(), localFile.length(), createdAt);
                
                result.put("success", true);
                result.put("mediaId", mediaId);
                result.put("createdAt", createdAt);
                result.put("message", "上传成功");
                
                log.info("✅ 临时素材上传成功 - media_id: {}", mediaId);
            } else {
                Integer errcode = uploadResult.getInteger("errcode");
                String errmsg = uploadResult.getString("errmsg");
                result.put("success", false);
                result.put("message", "上传失败: " + errmsg);
                log.error("❌ 临时素材上传失败 - errcode: {}, errmsg: {}", errcode, errmsg);
            }
            
        } catch (Exception e) {
            log.error("❌ 上传临时素材异常", e);
            result.put("success", false);
            result.put("message", "上传异常: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public String getValidMediaId(String description) {
        try {
            WeChatTempMedia media = getByDescription(description);
            
            if (media == null) {
                log.warn("⚠️ 未找到用途为 [{}] 的临时素材", description);
                return null;
            }
            
            // 检查是否过期
            Date now = new Date();
            if (media.getExpiredAt() != null && now.after(media.getExpiredAt())) {
                log.info("🔄 临时素材已过期，重新上传 - 用途: {}", description);
                refreshMediaId(description);
                media = getByDescription(description);
            }
            
            return media != null ? media.getMediaId() : null;
            
        } catch (Exception e) {
            log.error("❌ 获取有效media_id异常 - 用途: {}", description, e);
            return null;
        }
    }
    
    @Override
    public boolean refreshMediaId(String description) {
        try {
            WeChatTempMedia media = getByDescription(description);
            
            if (media == null || media.getFilePath() == null) {
                log.warn("⚠️ 无法刷新，未找到素材或文件路径 - 用途: {}", description);
                return false;
            }
            
            File localFile = new File(media.getFilePath());
            if (!localFile.exists()) {
                log.warn("⚠️ 本地文件不存在 - 路径: {}", media.getFilePath());
                return false;
            }
            
            Map<String, Object> result = uploadTempMediaFromLocal(localFile, media.getMediaType(), description);
            return (Boolean) result.getOrDefault("success", false);
            
        } catch (Exception e) {
            log.error("❌ 刷新media_id异常 - 用途: {}", description, e);
            return false;
        }
    }
    
    @Override
    public int refreshAllMediaIds() {
        try {
            log.info("🔄 开始批量刷新所有临时素材");
            
            List<WeChatTempMedia> mediaList = weChatTempMediaMapper.selectList(
                new LambdaQueryWrapper<WeChatTempMedia>()
                    .eq(WeChatTempMedia::getStatus, 1)
            );
            
            int successCount = 0;
            for (WeChatTempMedia media : mediaList) {
                if (refreshMediaId(media.getDescription())) {
                    successCount++;
                }
            }
            
            log.info("✅ 批量刷新完成 - 成功: {}/{}", successCount, mediaList.size());
            return successCount;
            
        } catch (Exception e) {
            log.error("❌ 批量刷新临时素材异常", e);
            return 0;
        }
    }
    
    @Override
    public WeChatTempMedia getByDescription(String description) {
        return weChatTempMediaMapper.selectOne(
            new LambdaQueryWrapper<WeChatTempMedia>()
                .eq(WeChatTempMedia::getDescription, description)
                .eq(WeChatTempMedia::getStatus, 1)
                .orderByDesc(WeChatTempMedia::getCreatedAt)
                .last("LIMIT 1")
        );
    }
    
    /**
     * 保存上传的文件到本地
     */
    private String saveFileToLocal(MultipartFile file, String description) throws Exception {
        // 确保存储目录存在
        File storageDir = new File(mediaStoragePath);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        
        // 生成文件名：用途_时间戳_原始文件名
        String timestamp = String.valueOf(System.currentTimeMillis());
        String fileName = description.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_") 
            + "_" + timestamp + "_" + file.getOriginalFilename();
        String filePath = mediaStoragePath + fileName;
        
        // 保存文件
        Files.copy(file.getInputStream(), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
        
        log.info("💾 文件已保存到本地 - 路径: {}", filePath);
        return filePath;
    }
    
    /**
     * 上传文件到微信服务器
     */
    private JSONObject uploadFileToWeChat(String uploadUrl, File file) throws Exception {
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        
        HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        
        OutputStream out = conn.getOutputStream();
        
        // 写入文件数据
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"media\"; filename=\"")
            .append(file.getName()).append("\"\r\n");
        sb.append("Content-Type: application/octet-stream\r\n\r\n");
        out.write(sb.toString().getBytes("UTF-8"));
        
        // 读取文件内容
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        fis.close();
        
        // 写入结束标记
        out.write(("\r\n--" + boundary + "--\r\n").getBytes("UTF-8"));
        out.flush();
        out.close();
        
        // 读取响应
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder responseStr = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseStr.append(line);
            }
            reader.close();
            log.info("📥 微信服务器响应: {}", responseStr.toString());
            return JSONObject.parseObject(responseStr.toString());
        } else {
            throw new Exception("HTTP响应码: " + responseCode);
        }
    }
    
    /**
     * 保存或更新临时素材记录
     */
    private void saveOrUpdateMediaRecord(String mediaId, String mediaType, String description,
                                        String fileName, String filePath, Long fileSize, Long createdAt) {
        WeChatTempMedia media = getByDescription(description);
        
        Date createDate = new Date(createdAt * 1000); // 微信返回的是秒级时间戳
        Date expireDate = new Date(createdAt * 1000 + 3 * 24 * 60 * 60 * 1000L); // 3天后过期
        
        if (media == null) {
            // 新建记录
            media = new WeChatTempMedia();
            media.setMediaType(mediaType);
            media.setMediaId(mediaId);
            media.setDescription(description);
            media.setFileName(fileName);
            media.setFilePath(filePath);
            media.setFileSize(fileSize);
            media.setCreatedAt(createDate);
            media.setExpiredAt(expireDate);
            media.setUpdatedAt(new Date());
            media.setStatus(1);
            
            weChatTempMediaMapper.insert(media);
            log.info("💾 新建临时素材记录 - ID: {}", media.getId());
        } else {
            // 更新记录
            media.setMediaId(mediaId);
            media.setFileName(fileName);
            media.setFilePath(filePath);
            media.setFileSize(fileSize);
            media.setCreatedAt(createDate);
            media.setExpiredAt(expireDate);
            media.setUpdatedAt(new Date());
            media.setStatus(1);
            
            weChatTempMediaMapper.updateById(media);
            log.info("🔄 更新临时素材记录 - ID: {}", media.getId());
        }
    }
    
    /**
     * 获取微信access_token
     */
    private String getAccessToken() {
        try {
            String url = String.format(
                "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                appId, secret
            );
            
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder responseStr = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseStr.append(line);
            }
            reader.close();
            JSONObject jsonResponse = JSONObject.parseObject(responseStr.toString());
            
            if (jsonResponse.containsKey("access_token")) {
                return jsonResponse.getString("access_token");
            } else {
                log.error("❌ 获取access_token失败: {}", responseStr);
                return null;
            }
            
        } catch (Exception e) {
            log.error("❌ 获取access_token异常", e);
            return null;
        }
    }
}
