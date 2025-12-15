package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author yuli
 * @since 2022-07-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Community对象", description="")
public class Community implements Serializable {
    private static final long serialVersionUID = 1L;
    private String province;
    private String city;
    private String district;
    private String community;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String building;
    private String units;
    private String floor;
    private String roomnumber;
    //是否需要审核
    private String isAudit;
    //审核开始时间
    private String auditStartTime;
    //审核开始时间
    private String auditEndTime;
    @TableField(exist = false)
    private String buildingBegin;
    @TableField(exist = false)
    private String unitsBegin;
    @TableField(exist = false)
    private String floorBegin;
    @TableField(exist = false)
    private String roomnumberBegin;
    @TableField(exist = false)
    private String buildingEnd;
    @TableField(exist = false)
    private String unitsEnd;
    @TableField(exist = false)
    private String floorEnd;
    @TableField(exist = false)
    private String roomnumberEnd;
    @TableField(exist = false)
    private String openid;
    @TableField(exist = false)
    private String flag;
    @TableField(exist = false)
    private String username;
}
