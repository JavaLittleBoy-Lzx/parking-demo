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
 * 系统配置实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("system_config")
@ApiModel(value = "SystemConfig对象", description = "系统配置")
public class SystemConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "配置ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "配置键")
    private String configKey;

    @ApiModelProperty(value = "配置值")
    private String configValue;

    @ApiModelProperty(value = "配置名称")
    private String configName;

    @ApiModelProperty(value = "配置描述")
    private String configDesc;

    @ApiModelProperty(value = "配置类型：system-系统配置，business-业务配置，security-安全配置")
    private String configType;

    @ApiModelProperty(value = "是否可编辑：0-不可编辑，1-可编辑")
    private Integer editable;

    @ApiModelProperty(value = "排序")
    private Integer sortOrder;

    @ApiModelProperty(value = "状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

    @ApiModelProperty(value = "创建人")
    private String createdBy;

    @ApiModelProperty(value = "更新人")
    private String updatedBy;
}
