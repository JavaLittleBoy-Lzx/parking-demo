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
 * 报废管理
 * </p>
 *
 * @author yuli
 * @since 2022-03-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Scrap对象", description="报废管理")
public class Scrap implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "scrap_id", type = IdType.AUTO)
    private Integer scrapId;

    @ApiModelProperty(value = "部门id")
    private Integer departmentId;

    @ApiModelProperty(value = "设备id")
    private Integer deviceId;

    @ApiModelProperty(value = "设备编码")
        private String deviceCode;

    @ApiModelProperty(value = "设备名称")
    private String deviceName;

    @ApiModelProperty(value = "报废日期")
    private LocalDateTime scrapDate;

    @ApiModelProperty(value = "报废原因")
    private String scrapReason;

    @ApiModelProperty(value = "申请/登记人")
    private Integer registrationUserId;

    @ApiModelProperty(value = "审批人")
    private Integer audiusUserId;

    @ApiModelProperty(value = "实际报废审批时间")
    private LocalDateTime approvalTime;

    @ApiModelProperty(value = "审批状态1待审批2通过3未通过")
    private Integer deviceStatus;

    @ApiModelProperty(value = "备注")
    private String remarks;

    @ApiModelProperty(value = "审批意见")
    private String audiusReason;

    @ApiModelProperty(value = "是否有效，1 有效，0 失效")
    private Integer deleted;

    @ApiModelProperty(value = "审批人")
    @TableField(exist = false)
    private String audiusUserName;
    @TableField(exist = false)

    private String departmentName;
    @TableField(exist = false)
    private String registrationUserName;

}
