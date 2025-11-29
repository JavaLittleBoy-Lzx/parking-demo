package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 二维码使用记录实体类
 * 用于记录管家生成的二维码使用情况，确保每个二维码只能使用一次
 */
@Data
@TableName("qr_code_usage")
@ApiModel(value = "QrCodeUsage对象", description = "二维码使用记录表")
public class QrCodeUsage {

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "二维码唯一标识")
    private String qrId;

    @ApiModelProperty(value = "管家手机号")
    private String butlerPhone;

    @ApiModelProperty(value = "管家姓名")
    private String butlerName;

    @ApiModelProperty(value = "小区名称")
    private String community;

    @ApiModelProperty(value = "楼栋")
    private String building;

    @ApiModelProperty(value = "单元")
    private String unit;

    @ApiModelProperty(value = "楼层")
    private String floor;

    @ApiModelProperty(value = "房间号")
    private String room;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdTime;

    @ApiModelProperty(value = "使用时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date usedTime;

    @ApiModelProperty(value = "是否已使用 0-未使用 1-已使用")
    private Integer isUsed;

    @ApiModelProperty(value = "访客openid")
    private String visitorOpenid;

    @ApiModelProperty(value = "访客手机号")
    private String visitorPhone;

    @ApiModelProperty(value = "二维码类型")
    private String qrType;

    @ApiModelProperty(value = "有效期至")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;

    @ApiModelProperty(value = "备注")
    private String remark;
}
