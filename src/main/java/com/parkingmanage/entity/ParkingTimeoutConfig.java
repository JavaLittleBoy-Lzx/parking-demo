package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 停车超时时间配置实体
 * 
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("parking_timeout_config")
@ApiModel(value="ParkingTimeoutConfig", description="停车超时时间配置")
public class ParkingTimeoutConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "车场编码")
    private String yardCode;

    @ApiModelProperty(value = "车场名称")
    private String yardName;

    @ApiModelProperty(value = "车辆类型（临时、访客、业主等）")
    private String vehicleType;

    @ApiModelProperty(value = "超时时间（分钟）")
    private Integer timeoutMinutes;

    @ApiModelProperty(value = "是否启用：1-启用，0-禁用")
    private Boolean isActive;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
} 