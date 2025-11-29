package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
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
 * @author lzx
 * @since 2024-04-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ReportCarIn对象", description = "")
public class ReportCarIn implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "进场通道编号")
    private String enterChannelCode;

    @ApiModelProperty(value = "进场类型（参考附录）")
    private String enterType;

    @ApiModelProperty(value = "进场Vip类型")
    private String enterVipType;

    @ApiModelProperty(value = "最终车牌号")
    private String carLicenseNumber;


    @ApiModelProperty(value = "进场说明（参考附录）")
    private String enterNoVipCode;


    @ApiModelProperty(value = "车牌号")
    private String enterCarLicenseNumber;

    @ApiModelProperty(value = "进场说明（参考附录）")
    private String enterNoVipCodeName;

    @ApiModelProperty(value = "停车记录")
    private String parkingCode;

    @ApiModelProperty(value = "进场通道名称")
    private String enterChannelName;

    @ApiModelProperty(value = "校正类型（0未校正，1手动校正，2自动校正）")
    private String correctType;

    @ApiModelProperty(value = "入场时间")
    private String enterTime;

    @ApiModelProperty(value = "车场名称")
    private String yardName;

    @ApiModelProperty(value = "入场车牌颜色")
    private String enterCarLicenseColor;

    @ApiModelProperty(value = "进场通道id")
    private String enterChannelId;

    @ApiModelProperty(value = "车位id")
    private String areaId;

    @ApiModelProperty(value = "进场放行操作员")
    private String inOperatorName;

    @ApiModelProperty(value = "进场放行时间")
    private String inOperatorTime;

    @TableLogic(value = "0", delval = "1")
    @ApiModelProperty(value = "逻辑删除标识：0：未删除，1：已删除")
    private Integer deleted;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;


}
