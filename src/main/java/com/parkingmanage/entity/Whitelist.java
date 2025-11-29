package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * <p>
 * 白名单管理表
 * </p>
 *
 * @author MLH
 * @since 2025-10-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("whitelist")
@ApiModel(value="Whitelist对象", description="白名单管理表")
public class Whitelist implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "车牌号")
    @TableField("plate_number")
    private String plateNumber;

    @ApiModelProperty(value = "停车场名称")
    @TableField("park_name")
    private String parkName;

    @ApiModelProperty(value = "车主姓名")
    @TableField("owner_name")
    private String ownerName;

    @ApiModelProperty(value = "车主电话")
    @TableField("owner_phone")
    private String ownerPhone;

    @ApiModelProperty(value = "车主地址")
    @TableField("owner_address")
    private String ownerAddress;

    @ApiModelProperty(value = "备注")
    @TableField("remark")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    @TableField("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    @TableField("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @ApiModelProperty(value = "创建人")
    @TableField("created_by")
    private String createdBy;
}

