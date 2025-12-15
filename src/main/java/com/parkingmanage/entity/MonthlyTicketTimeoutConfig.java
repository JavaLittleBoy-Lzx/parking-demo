package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 月票车超时配置表
 * 
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("monthly_ticket_timeout_config")
@ApiModel(value="MonthlyTicketTimeoutConfig", description="月票车超时配置")
public class MonthlyTicketTimeoutConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "车场编码")
    private String parkCode;

    @ApiModelProperty(value = "车场名称")
    private String parkName;

    @ApiModelProperty(value = "超时时间（分钟）")
    private Integer timeoutMinutes;

    @ApiModelProperty(value = "最大违规次数")
    private Integer maxViolationCount;

    @ApiModelProperty(value = "是否启用：1-启用，0-禁用")
    private Boolean isActive;

    @ApiModelProperty(value = "配置说明")
    private String description;

    @ApiModelProperty(value = "夜间开始时间（如：22:00）")
    private String nightStartTime;

    @ApiModelProperty(value = "夜间结束时间（如：06:00）")
    private String nightEndTime;

    @ApiModelProperty(value = "夜间时段停车超过X小时算违规")
    private Integer nightTimeHours;

    @ApiModelProperty(value = "是否启用过夜停车检查：1-启用，0-禁用")
    private Integer enableOvernightCheck;

    @ApiModelProperty(value = "创建人")
    private String createdBy;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
} 