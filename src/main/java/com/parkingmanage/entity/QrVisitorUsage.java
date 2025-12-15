package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 车场二维码访客使用记录表
 * @author System
 * @since 2025-11-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("qr_visitor_usage")
@ApiModel(value="QrVisitorUsage对象", description="车场二维码访客使用记录表（24小时有效期，每天3次限制）")
public class QrVisitorUsage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;
    
    @ApiModelProperty(value = "车场二维码ID")
    private String qrId;
    
    @ApiModelProperty(value = "访客手机号")
    private String phone;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "首次扫码时间")
    private LocalDateTime firstScanTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最后扫码时间")
    private LocalDateTime lastScanTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "过期时间（首次扫码+24小时）")
    private LocalDateTime expiresAt;
    
    @ApiModelProperty(value = "今日扫码次数")
    private Integer scanCount;
    
    @ApiModelProperty(value = "累计使用次数")
    private Integer totalCount;
    
    @ApiModelProperty(value = "最后扫码纬度")
    private Double lastLatitude;
    
    @ApiModelProperty(value = "最后扫码经度")
    private Double lastLongitude;
    
    @ApiModelProperty(value = "最后扫码距离（米）")
    private Double lastDistance;
    
    @ApiModelProperty(value = "状态: active-活跃, expired-已过期, blocked-已封禁")
    private String status;
    
    @ApiModelProperty(value = "封禁原因")
    private String blockReason;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}
