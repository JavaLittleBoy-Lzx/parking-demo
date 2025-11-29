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
 * 调拨管理
 * </p>
 *
 * @author yuli
 * @since 2022-03-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Allocation对象", description = "调拨管理")
public class Allocation implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "allocation_id", type = IdType.AUTO)
    private Integer allocationId;

    @ApiModelProperty(value = "原部门")
    private Integer departmentId;

    @ApiModelProperty(value = "调往部门")
    private Integer afterDepartmentId;

    @ApiModelProperty(value = "设备id")
    private Integer deviceId;

    @ApiModelProperty(value = "设备编码")
    private String deviceCode;

    @ApiModelProperty(value = "设备名称")
    private String deviceName;

    @ApiModelProperty(value = "申请人")
    private String applicantUserId;

    @ApiModelProperty(value = "申请时间")
    private LocalDateTime applicationTime;

    @ApiModelProperty(value = "申请原因")
    private String applicationReason;

    @ApiModelProperty(value = "审批人")
    private Integer auditUserId;

    @ApiModelProperty(value = "审批时间")
    private LocalDateTime auditusTime;

    @ApiModelProperty(value = "审批状态1 2 ")
    private Integer auditStatus;

    @ApiModelProperty(value = "备注")
    private String remarks;

    @ApiModelProperty(value = "是否有效，1 有效，0 失效")
    private Integer deleted;

    @ApiModelProperty(value = "审批意见")
    private String fileReason;

    @TableField(exist = false)
    private String departmentName;

    @TableField(exist = false)
    private String afterDepartmentName;

    @TableField(exist = false)
    private String applicantUserName;

    @TableField(exist = false)
    private String auditUserName;
}
