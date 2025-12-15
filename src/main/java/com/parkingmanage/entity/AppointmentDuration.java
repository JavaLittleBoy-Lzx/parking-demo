package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 <p>

 </p>

 @author MLH
 @since 2022-07-13
*/
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Appointment对象", description="")
public class AppointmentDuration implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String province;
    private String city;
    private String district;
    private String community;
    private String visitdate;
    private String recorddate;
    private String visitorphone;
    @TableField("plateNumber")
    private String platenumber;
    private String cartype;
    private String status;
    private String openid;
    private String building;
    private String units;
    private String floor;
    private String room;
    private String owneropenid;
    private String ownername;
    private String ownerphone;
    private String visitreason;
    private String appointtype;
    private String auditstatus;
    private String refusereason;
    private String venuestatus;
    private String arrivedate;
    private String leavedate;
    private String parking;
    private String auditopenid;
    private String auditusername;
    private String auditdate;
    @TableField(exist = false)
    private String parkingDuration;
}