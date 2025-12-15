package com.parkingmanage.vo;

import com.parkingmanage.entity.Purchase;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @PROJECT_NAME: parkingmanage
 * @PACKAGE_NAME: com.parkingmanage.vo
 * @NAME: PurchaseVo
 * @author:yuli
 * @DATE: 2022/2/28 16:22
 * @description: 采购申请
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PurchaseVo extends Purchase {
    /**
     * 部门
     */
    private String departmentName;
    /**
     * 厂商
     */
    private String supplierName;
    /**
     * 申请人
     */
    private String applicantName;
    /**
     * 申请人
     */
    private String audiustName;
    /**
     *
     */
    private String auditStatusType;
}
