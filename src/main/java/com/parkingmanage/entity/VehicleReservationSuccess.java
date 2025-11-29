package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author 李子雄
 *
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="VehicleReservationSuccess对象", description="")
public class VehicleReservationSuccess implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "ID号")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "车场编码")
    private String yardCode;

    @ApiModelProperty(value = "车场名称")
    private String yardName;

    @ApiModelProperty(value = "入场通道")
    private String channelName;

    @ApiModelProperty(value = "车牌号码")
    private String plateNumber;

    @ApiModelProperty(value = "车辆分类")
    private String vehicleClassification;

    @ApiModelProperty(value = "商户名称")
    private String merchantName;

    @ApiModelProperty(value = "放行原因")
    private String releaseReason;

    @ApiModelProperty(value = "通知人姓名")
    private String notifierName;

    @ApiModelProperty(value = "预约时间")
    private String appointmentTime;

    @ApiModelProperty(value = "放行时间")
    private String successTime;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "逻辑删除标识：0：未删除，1：已删除")
    private Integer deleted;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date gmtCreate;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date gmtModified;


}
