package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 设备信息
 * </p>
 *
 * @author yuli
 * @since 2022-03-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Device对象", description = "设备信息")
public class Device implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "device_id", type = IdType.AUTO)
    private Integer deviceId;

    @ApiModelProperty(value = "申请id")
    private Integer purchaseId;

    @ApiModelProperty(value = "设备名称")
    private String deviceName;

    @ApiModelProperty(value = "设备价格")
    private BigDecimal devicePrice;

    @ApiModelProperty(value = "设备编码")
    private String deviceCode;

    @ApiModelProperty(value = "规格型号")
    private String model;

    @ApiModelProperty(value = "设备类型")
    private Integer deviceType;

    @ApiModelProperty(value = "技术文档")
    private String technicalDocumentation;

    @ApiModelProperty(value = "是否有效，1 有效，0 失效")
    private Integer deleted;

    @ApiModelProperty(value = "使用部门")
    private Integer departmentId;

    @ApiModelProperty(value = "登记日期")
    private LocalDateTime purchaseTime;

    @ApiModelProperty(value = "使用情况 1空闲 2使用中 3维修中 4租赁 5待调拨 6已报废 7维修失败 8租赁申请中 9 报废中")
    private Integer deviceStatus;

    @ApiModelProperty(value = "原始状态")
    private Integer originalState;

    @ApiModelProperty(value = "部门名称")
    @TableField(exist = false)
    private String departmentName;

    @TableField(exist = false)
    private String deviceTypeName;

    @TableField(exist = false)
    private String deviceStatusName;
}