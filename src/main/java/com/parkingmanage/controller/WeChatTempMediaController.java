package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.entity.WeChatTempMedia;
import com.parkingmanage.service.WeChatTempMediaService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 微信临时素材管理控制器
 * 
 * @author System
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/parking/wechat/media")
@CrossOrigin(origins = "*")
@Api(tags = "微信临时素材管理")
public class WeChatTempMediaController {
    
    @Resource
    private WeChatTempMediaService weChatTempMediaService;
    
    /**
     * 上传临时素材
     */
    @ApiOperation("上传临时素材")
    @PostMapping("/upload")
    public ResponseEntity<Result> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mediaType", defaultValue = "image") String mediaType,
            @RequestParam("description") String description) {
        
        Result result = new Result();
        
        try {
            log.info("📥 接收到临时素材上传请求 - 类型: {}, 用途: {}, 文件: {}", 
                mediaType, description, file.getOriginalFilename());
            
            // 验证文件
            if (file.isEmpty()) {
                result.setCode("1");
                result.setMsg("文件不能为空");
                return ResponseEntity.ok(result);
            }
            
            // 验证文件类型
            if ("image".equals(mediaType)) {
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    result.setCode("1");
                    result.setMsg("请上传图片文件");
                    return ResponseEntity.ok(result);
                }
                
                // 图片大小限制：2MB
                if (file.getSize() > 2 * 1024 * 1024) {
                    result.setCode("1");
                    result.setMsg("图片大小不能超过2MB");
                    return ResponseEntity.ok(result);
                }
            }
            
            // 上传到微信服务器
            Map<String, Object> uploadResult = weChatTempMediaService.uploadTempMedia(file, mediaType, description);
            
            if ((Boolean) uploadResult.get("success")) {
                result.setCode("0");
                result.setMsg("上传成功");
                result.setData(uploadResult);
                log.info("✅ 临时素材上传成功 - media_id: {}", uploadResult.get("mediaId"));
            } else {
                result.setCode("1");
                result.setMsg((String) uploadResult.get("message"));
                log.error("❌ 临时素材上传失败: {}", uploadResult.get("message"));
            }
            
        } catch (Exception e) {
            log.error("❌ 上传临时素材异常", e);
            result.setCode("1");
            result.setMsg("上传失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取有效的media_id
     */
    @ApiOperation("获取有效的media_id")
    @GetMapping("/getMediaId")
    public ResponseEntity<Result> getMediaId(@RequestParam("description") String description) {
        Result result = new Result();
        
        try {
            log.info("📥 查询有效的media_id - 用途: {}", description);
            
            String mediaId = weChatTempMediaService.getValidMediaId(description);
            
            if (mediaId != null) {
                result.setCode("0");
                result.setMsg("查询成功");
                result.setData(mediaId);
                log.info("✅ 找到有效的media_id: {}", mediaId);
            } else {
                result.setCode("1");
                result.setMsg("未找到该用途的临时素材或已过期");
                log.warn("⚠️ 未找到有效的media_id - 用途: {}", description);
            }
            
        } catch (Exception e) {
            log.error("❌ 查询media_id异常", e);
            result.setCode("1");
            result.setMsg("查询失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 查询临时素材信息
     */
    @ApiOperation("查询临时素材信息")
    @GetMapping("/getInfo")
    public ResponseEntity<Result> getMediaInfo(@RequestParam("description") String description) {
        Result result = new Result();
        
        try {
            log.info("📥 查询临时素材信息 - 用途: {}", description);
            
            WeChatTempMedia media = weChatTempMediaService.getByDescription(description);
            
            if (media != null) {
                result.setCode("0");
                result.setMsg("查询成功");
                result.setData(media);
            } else {
                result.setCode("1");
                result.setMsg("未找到该临时素材");
            }
            
        } catch (Exception e) {
            log.error("❌ 查询临时素材信息异常", e);
            result.setCode("1");
            result.setMsg("查询失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 手动刷新指定临时素材
     */
    @ApiOperation("手动刷新指定临时素材")
    @PostMapping("/refresh")
    public ResponseEntity<Result> refreshMedia(@RequestParam("description") String description) {
        Result result = new Result();
        
        try {
            log.info("🔄 手动刷新临时素材 - 用途: {}", description);
            
            boolean success = weChatTempMediaService.refreshMediaId(description);
            
            if (success) {
                result.setCode("0");
                result.setMsg("刷新成功");
                log.info("✅ 临时素材刷新成功 - 用途: {}", description);
            } else {
                result.setCode("1");
                result.setMsg("刷新失败，请检查本地文件是否存在");
                log.error("❌ 临时素材刷新失败 - 用途: {}", description);
            }
            
        } catch (Exception e) {
            log.error("❌ 刷新临时素材异常", e);
            result.setCode("1");
            result.setMsg("刷新失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 批量刷新所有临时素材
     */
    @ApiOperation("批量刷新所有临时素材")
    @PostMapping("/refreshAll")
    public ResponseEntity<Result> refreshAllMedia() {
        Result result = new Result();
        
        try {
            log.info("🔄 批量刷新所有临时素材");
            
            int successCount = weChatTempMediaService.refreshAllMediaIds();
            
            result.setCode("0");
            result.setMsg("批量刷新完成");
            result.setData(successCount);
            log.info("✅ 批量刷新完成 - 成功数量: {}", successCount);
            
        } catch (Exception e) {
            log.error("❌ 批量刷新临时素材异常", e);
            result.setCode("1");
            result.setMsg("批量刷新失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
}
