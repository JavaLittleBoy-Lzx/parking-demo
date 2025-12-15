package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 维修管理
 * </p>
 *
 * @author yuli
 * @since 2022-03-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Maintenance对象", description = "维修管理")
public class Maintenance implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "maintenance_id", type = IdType.AUTO)
    private Integer maintenanceId;

    @ApiModelProperty(value = "设备信息id")
    private Integer deviceId;

    @ApiModelProperty(value = "设备编码")
    private String deviceCode;

    @ApiModelProperty(value = "设备名称")
    private String deviceName;

    @ApiModelProperty(value = "部门id")
    private Integer departmentId;

    @ApiModelProperty(value = "地址")
    private String departmentAddress;

    @ApiModelProperty(value = "维修人")
    private Integer maintenanceUserId;

    @ApiModelProperty(value = "报修时间")
    private LocalDateTime repairTime;

    @ApiModelProperty(value = "报修人")
    private String repairmanUserId;

    @ApiModelProperty(value = "报修原因 //todo 目前不用")
    private String repairReason;

    @ApiModelProperty(value = "障描述")
    private String faultDescription;

    @ApiModelProperty(value = "1待维修，2已维修3 申请报废")
    private Integer auditStatus;

    @ApiModelProperty(value = "维修时间")
    private LocalDateTime maintainTime;

    @ApiModelProperty(value = "备注")
    private String remarks;

    @ApiModelProperty(value = "是否有效，1 有效，0 失效")
    private Integer deleted;
    @ApiModelProperty(value = "维修意见")
    private String maintOpinions;

    @ApiModelProperty(value = "设备类型")
    private Integer deviceType;

    @TableField(exist = false)
    private String repairmanUserName;

    @TableField(exist = false)
    private String maintenanceUserName;

    @TableField(exist = false)
    private String auditStatusName;


}
