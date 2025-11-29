package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 客户管理
 * </p>
 *
 * @author yuli
 * @since 2022-02-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Customer对象", description="客户管理")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键id")
    @TableId(value = "customer_id", type = IdType.AUTO)
    private Integer customerId;

    @ApiModelProperty(value = "客户公司编码")
    private String customerCode;

    @ApiModelProperty(value = "客户公司名称")
    private String customerName;

    @ApiModelProperty(value = "负责人")
    private String leader;

    @ApiModelProperty(value = "电话")
    private String telephone;

    @ApiModelProperty(value = "登记日期")
    private LocalDateTime registionDate;

    @ApiModelProperty(value = "备注")
    private String remarks;

    @ApiModelProperty(value = "是否有效，1 有效，0 失效")
    private Integer deleted;


}
