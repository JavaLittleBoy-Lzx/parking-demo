package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 <p>
 调用Appointment对象object
 </p>
 @author MLH
 @since 2022-07-13
*/
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Appointment对象", description="")
public class Appointment implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String province;
    private String city;
    private String district;
    private String community;
    private String visitdate;
    private LocalDateTime recorddate;
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
    private String ordernumber;
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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime auditdate;
    
    // 🆕 新增微信用户姓名字段
    @ApiModelProperty(value = "访客微信姓名")
    private String visitorname;
    
    // 🆕 管家代人预约和访客邀请预约的管家信息传递字段（仅用于接收前端参数，不存储到数据库）
    @ApiModelProperty(value = "管家昵称（用于代人预约）")
    @TableField(exist = false)
    private String managerNickname;
    
    @ApiModelProperty(value = "管家openid（用于代人预约）")
    @TableField(exist = false)
    private String managerOpenid;
    
    @ApiModelProperty(value = "管家姓名（用于邀请预约）")
    @TableField(exist = false)
    private String butlerName;
    
    @TableField(exist = false)
    private String parkingDuration;
    
    // 新增字段用于停车超时提醒功能 - 标记为不存在于数据库表中
    @ApiModelProperty(value = "预计离场时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(exist = false)
    private LocalDateTime expectedLeaveTime;

    @ApiModelProperty(value = "是否已发送超时提醒：0-未发送，1-已发送")
    @TableField(exist = false)
    private Integer timeoutNotified;

    @ApiModelProperty(value = "车场编码")
    @TableField(exist = false)
    private String yardCode;

    @ApiModelProperty(value = "车场名称")
    @TableField(exist = false)
    private String yardName;

    @ApiModelProperty(value = "车辆类型")
    @TableField(exist = false)
    private String vehicleType;

    @ApiModelProperty(value = "进场时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(exist = false)
    private LocalDateTime enterTime;

    @ApiModelProperty(value = "离场时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(exist = false)
    private LocalDateTime leaveTime;
    
    /**
     * 获取预约开始时间
     * 从 visitdate 字符串中解析出开始时间
     * @return 开始时间字符串，格式：YYYY-MM-DD HH:mm:ss
     */
    public String getStartTime() {
        if (visitdate != null && visitdate.contains(" - ")) {
            return visitdate.split(" - ")[0];
        }
        return visitdate;
    }
    
    /**
     * 获取预约结束时间
     * 从 visitdate 字符串中解析出结束时间
     * @return 结束时间字符串，格式：YYYY-MM-DD HH:mm:ss
     */
    public String getEndTime() {
        if (visitdate != null && visitdate.contains(" - ")) {
            String[] times = visitdate.split(" - ");
            if (times.length > 1) {
                return times[1];
            }
        }
        return visitdate;
    }
    
    /**
     * 获取预约时间段标签
     * 从 visitdate 中提取时间段，格式：HH:mm-HH:mm
     * @return 时间段标签，如：08:00-10:00
     */
    public String getTimeSlotLabel() {
        if (visitdate != null && visitdate.contains(" - ")) {
            String[] times = visitdate.split(" - ");
            if (times.length == 2) {
                String startTime = times[0].substring(11); // 提取 HH:mm:ss 部分
                String endTime = times[1].substring(11);   // 提取 HH:mm:ss 部分
                return startTime.substring(0, 5) + "-" + endTime.substring(0, 5); // 只保留 HH:mm 部分
            }
        }
        return "";
    }
    
    /**
     * 检查预约时间是否有效
     * @return true 如果时间格式正确
     */
    public boolean isValidTimeRange() {
        return visitdate != null && visitdate.contains(" - ") && visitdate.split(" - ").length == 2;
    }
}