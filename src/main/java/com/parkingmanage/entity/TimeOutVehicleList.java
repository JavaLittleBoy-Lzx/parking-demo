package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 <p>

 </p>

 @author 李子雄
*/
@Data
@EqualsAndHashCode(callSuper = false)
public class TimeOutVehicleList implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "ID号")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "车场名称")
    private String yardName;

    @ApiModelProperty(value = "车牌号码")
    private String plateNumber;

    @ApiModelProperty(value = "入场时间")
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private String enterTime;

    @ApiModelProperty(value = "预约时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date appointmentTime;

    @ApiModelProperty(value = "车牌号码")
    private String timeOutInterval;

    @TableField(select = false)
    @TableLogic(value="0",delval="1")
    @ApiModelProperty(value = "逻辑删除标识：0：未删除，1：已删除")
    private Integer deleted;

}
