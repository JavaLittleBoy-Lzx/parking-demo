package com.parkingmanage.entity;

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
 * 访客临时Token表（5分钟有效期）
 * @author System
 * @since 2025-11-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("visitor_token")
@ApiModel(value="VisitorToken对象", description="访客临时Token表（5分钟有效期，一次性使用）")
public class VisitorToken implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId
    @ApiModelProperty(value = "UUID token")
    private String token;
    
    @ApiModelProperty(value = "车场二维码ID")
    private String qrId;
    
    @ApiModelProperty(value = "访客手机号")
    private String phone;
    
    @ApiModelProperty(value = "GPS纬度")
    private Double latitude;
    
    @ApiModelProperty(value = "GPS经度")
    private Double longitude;
    
    @ApiModelProperty(value = "距离车场的距离（米）")
    private Double distance;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "过期时间（创建时间+5分钟）")
    private LocalDateTime expireTime;
    
    @ApiModelProperty(value = "是否已使用（0-未使用，1-已使用，一次性token）")
    private Integer isUsed;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "使用时间")
    private LocalDateTime usedTime;
}
