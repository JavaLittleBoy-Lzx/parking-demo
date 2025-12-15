package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * 采申请管理
 * </p>
 *
 * @author yuli
 * @since 2022-02-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Purchase对象", description = "采申请管理")
public class Purchase implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "purchase_id", type = IdType.AUTO)
    private Integer purchaseId;

    @ApiModelProperty(value = "部门id")
    private Integer departmentId;

    @ApiModelProperty(value = "设备名称")
    private String deviceName;

    @ApiModelProperty(value = "设备价格")
    private BigDecimal devicePrice;

    @ApiModelProperty(value = "预购数量")
    private Integer orderQuantity;

    @ApiModelProperty(value = "申请时间")
    private LocalDateTime applicationTime;

    @ApiModelProperty(value = "申请人id")
    private Integer applicantUserId;

    @ApiModelProperty(value = "申请原因")
    private String applicationReason;

    @ApiModelProperty(value = "审批人")
    private Integer audiusUserId;

    @ApiModelProperty(value = "审批状态 0 审批中 1 通过 2 驳回 3购买凭证")
    private Integer auditStatus;

    @ApiModelProperty(value = "审批时间")
    private LocalDateTime audiusTime;

    @ApiModelProperty(value = "是否有效，1 有效，0 失效")
    private Integer deleted;

    @ApiModelProperty(value = "供应商")
    private Integer supplierId;

    @ApiModelProperty(value = "审批意见")
    private String fileReason;

    @ApiModelProperty(value = "购买凭证")
    private String purchaseVoucher;

    @ApiModelProperty(value = "规格型号")
    private String model;

    @ApiModelProperty(value = "设备类型")
    private Integer deviceType;

        @ApiModelProperty(value = "采购时间")
    private LocalDateTime purchaseTime;
}
