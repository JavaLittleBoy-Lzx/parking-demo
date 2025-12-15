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
 * 违规自动拉黑配置表
 * 
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("violation_config")
@ApiModel(value="ViolationConfig", description="违规自动拉黑配置")
public class ViolationConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "车场名称")
    private String parkName;

    @ApiModelProperty(value = "车场编码")
    private String parkCode;

    @ApiModelProperty(value = "配置类型：NEBU_AUTO_BLACKLIST-东北林大自动拉黑")
    private String configType;

    @ApiModelProperty(value = "最大违规次数，达到此次数后自动拉黑")
    private Integer maxViolationCount;

    @ApiModelProperty(value = "黑名单类型：黑名单/白名单/免费通行")
    private String blacklistType;

    @ApiModelProperty(value = "是否永久拉黑：true-永久，false-临时")
    private Boolean isPermanent;

    @ApiModelProperty(value = "临时拉黑开始时间（格式：yyyy-MM-dd HH:mm:ss）")
    private String blacklistStartTime;

    @ApiModelProperty(value = "临时拉黑结束时间（格式：yyyy-MM-dd HH:mm:ss）")
    private String blacklistEndTime;

    @ApiModelProperty(value = "临时拉黑有效天数（从最后一次违规时间开始计算）")
    private Integer blacklistValidDays;

    @ApiModelProperty(value = "违规提醒最小发送间隔（分钟）")
    private Integer reminderIntervalMinutes;

    @ApiModelProperty(value = "是否启用：1-启用，0-禁用")
    private Boolean isActive;

    @ApiModelProperty(value = "配置说明")
    private String description;

    @ApiModelProperty(value = "创建人")
    private String createdBy;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新人")
    private String updatedBy;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

