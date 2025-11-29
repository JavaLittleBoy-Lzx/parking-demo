package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 违规提醒记录表
 * </p>
 *
 * @author parking-system
 * @since 2024-01-XX
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("violation_reminders")
@ApiModel(value="ViolationReminder对象", description="违规提醒记录表")
public class ViolationReminder implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "车牌号")
    @TableField("plate_number")
    private String plateNumber;

    @ApiModelProperty(value = "车主姓名")
    @TableField("owner_name")
    private String ownerName;

    @ApiModelProperty(value = "车主电话")
    @TableField("owner_phone")
    private String ownerPhone;

    @ApiModelProperty(value = "违规类型")
    @TableField("violation_type")
    private String violationType;

    @ApiModelProperty(value = "违规地点")
    @TableField("violation_location")
    private String violationLocation;

    @ApiModelProperty(value = "违规时间")
    @TableField("violation_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime violationTime;

    @ApiModelProperty(value = "提醒发送时间")
    @TableField("reminder_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reminderTime;

    @ApiModelProperty(value = "提醒模板代码")
    @TableField("reminder_template_code")
    private String reminderTemplateCode;

    @ApiModelProperty(value = "提醒内容")
    @TableField("reminder_content")
    private String reminderContent;

    @ApiModelProperty(value = "是否已处理(0:未处理,1:已处理)")
    @TableField("is_processed")
    private Integer isProcessed;

    @ApiModelProperty(value = "处理时间")
    @TableField("processed_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processedTime;

    @ApiModelProperty(value = "处理人")
    @TableField("processed_by")
    private String processedBy;

    @ApiModelProperty(value = "车场编码")
    @TableField("park_code")
    private String parkCode;

    @ApiModelProperty(value = "车场名称")
    @TableField("park_name")
    private String parkName;

    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
