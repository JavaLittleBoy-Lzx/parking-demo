package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 微信临时素材实体类
 * 用于存储临时素材的media_id，支持复用
 * 
 * @author System
 * @since 2024-01-01
 */
@Data
@TableName("wechat_temp_media")
public class WeChatTempMedia implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 素材类型：image-图片、voice-语音、video-视频、thumb-缩略图
     */
    private String mediaType;
    
    /**
     * 微信返回的media_id（临时素材标识，3天有效）
     */
    private String mediaId;
    
    /**
     * 素材用途说明（如：欢迎语小程序码）
     */
    private String description;
    
    /**
     * 原始文件名
     */
    private String fileName;
    
    /**
     * 文件路径（本地存储路径，用于重新上传）
     */
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * media_id创建时间
     */
    private Date createdAt;
    
    /**
     * media_id过期时间（创建时间+3天）
     */
    private Date expiredAt;
    
    /**
     * 最后更新时间
     */
    private Date updatedAt;
    
    /**
     * 状态：1-有效、0-已过期
     */
    private Integer status;
    
    /**
     * 备注
     */
    private String remark;
}
