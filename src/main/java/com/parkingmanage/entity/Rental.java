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
 * 设备租赁
 * </p>
 *
 * @author yuli
 * @since 2022-03-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Rental对象", description = "设备租赁")
public class Rental implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "order_id", type = IdType.AUTO)
    private Integer orderId;
    @ApiModelProperty(value = "客户id/租赁公司")
    private Integer customerId;

    @ApiModelProperty(value = "客户名称")
    private String customerName;

    @ApiModelProperty(value = "客户编码")
    private String customerCode;

    @ApiModelProperty(value = "客户地址")
    private String customerAddress;

    @ApiModelProperty(value = "设备id")
    private Integer deviceId;

    @ApiModelProperty(value = "设备编码")
    private String deviceCode;

    @ApiModelProperty(value = "设备名称")
    private String deviceName;

    @ApiModelProperty(value = "部门id")
    private Integer departmentId;

    @ApiModelProperty(value = "电话")
    private String telephone;

    @ApiModelProperty(value = "租金")
    private BigDecimal rent;
    
    @ApiModelProperty(value = "实际租金")
    private BigDecimal actualRent;

    @ApiModelProperty(value = "租赁时间")
    private LocalDateTime leaseTime;

    @ApiModelProperty(value = "审批人")
    private String audiusUserId;

    @ApiModelProperty(value = "审批状态1待审批2通过3未通过")
    private Integer audiusStatus;

    @ApiModelProperty(value = "预计归还时间")
    private LocalDateTime expectedReturn;

    @ApiModelProperty(value = "备注")
    private String remarks;

    @ApiModelProperty(value = "实际归还时间")
    private LocalDateTime actualReturn;

    @ApiModelProperty(value = "是否有效，1 有效，0 失效")
    private Integer deleted;

    @ApiModelProperty(value = "申请人id")
    private Integer applicantUserId;
    /**
     * vo
     */
    @ApiModelProperty(value = "审批人")
    @TableField(exist = false)
    private String audiusUserName;
    @TableField(exist = false)

    private String departmentName;
    @TableField(exist = false)
    private String applicantUserName;

    @ApiModelProperty(value = "日租赁价格")
    private BigDecimal dayPrice;

    @ApiModelProperty(value = "设备类型")
    private Integer deviceType;
}
