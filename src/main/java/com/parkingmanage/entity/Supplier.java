package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 供应商管理
 * </p>
 *
 * @author yuli
 * @since 2022-02-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Supplier对象", description="供应商管理")
public class Supplier implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "supplier_id", type = IdType.AUTO)
    private Integer supplierId;

    @ApiModelProperty(value = "供应商名称")
    private String supplierName;

    @ApiModelProperty(value = "供应商编码")
    private String supplierCode;

    @ApiModelProperty(value = "供应商地址")
    private String supplierAddress;

    @ApiModelProperty(value = "联系人")
    private String contactsPerson;

    @ApiModelProperty(value = "电话")
    private String telephone;

    @ApiModelProperty(value = "登记日期")
    private LocalDateTime registrationTime;

    @ApiModelProperty(value = "备注")
    private String remarks;

    @ApiModelProperty(value = "是否有效，1 有效，0 失效")
    private Integer deleted;


}
