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
 * 访客申请实体类
 * </p>
 *
 * @author System
 * @since 2024-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="VisitorApplication对象", description="访客申请")
public class VisitorApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "申请编号")
    @TableField("application_no")
    private String applicationNo;

    @ApiModelProperty(value = "访客姓名")
    private String nickname;

    @ApiModelProperty(value = "访客手机号")
    private String phone;

    @ApiModelProperty(value = "业主姓名")
    private String ownerName;

    @ApiModelProperty(value = "业主手机号")
    private String ownerPhone;

    @ApiModelProperty(value = "性别")
    private String gender;

    @ApiModelProperty(value = "身份证号")
    @TableField("id_card")
    private String idCard;

    @ApiModelProperty(value = "申请原因")
    private String reason;

    @ApiModelProperty(value = "省份")
    private String province;

    @ApiModelProperty(value = "城市")
    private String city;

    @ApiModelProperty(value = "区县")
    private String district;

    @ApiModelProperty(value = "小区")
    private String community;

    @ApiModelProperty(value = "栋")
    private String building;

    @ApiModelProperty(value = "单元")
    private String units;

    @ApiModelProperty(value = "楼层")
    private String floor;

    @ApiModelProperty(value = "房间号")
    private String roomnumber;

    @ApiModelProperty(value = "完整地址")
    @TableField("full_address")
    private String fullAddress;

    @ApiModelProperty(value = "用户类型")
    private String userkind;

    @ApiModelProperty(value = "审批状态：待审批、已通过、未通过")
    private String auditstatus;

    @ApiModelProperty(value = "申请时间")
    private LocalDateTime applydate;

    @ApiModelProperty(value = "审批人")
    private String auditusername;

    @ApiModelProperty(value = "审批时间")
    private LocalDateTime auditdate;

    @ApiModelProperty(value = "审批意见/拒绝原因")
    private String refusereason;

    @ApiModelProperty(value = "微信OpenID")
    private String openid;

    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;
} 