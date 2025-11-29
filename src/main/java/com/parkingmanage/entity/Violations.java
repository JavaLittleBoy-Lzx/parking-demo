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
 * 违规记录表
 * </p>
 *
 * @author MLH
 * @since 2025-01-31
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("violations")
@ApiModel(value="Violations对象", description="违规记录表")
public class Violations implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "车牌号")
    private String plateNumber;

    @ApiModelProperty(value = "车主ID")
    private Integer ownerId;

    @ApiModelProperty(value = "预约记录ID，关联appointment表")
    private Integer appointmentId;

    @ApiModelProperty(value = "月票ID，关联month_tick表")
    private Integer monthTicketId;

    @ApiModelProperty(value = "是否月票车")
    private Boolean isMonthlyTicket;

    @ApiModelProperty(value = "违规类型")
    private String violationType;

    @ApiModelProperty(value = "自定义违规类型")
    private String customType;

    @ApiModelProperty(value = "违规位置")
    private String location;

    @ApiModelProperty(value = "违规描述")
    private String description;

    @ApiModelProperty(value = "停车场编码")
    private String parkCode;

    @ApiModelProperty(value = "停车场名称")
    private String parkName;

    @ApiModelProperty(value = "预约时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appointmentTime;

    @ApiModelProperty(value = "进场时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime enterTime;

    @ApiModelProperty(value = "离场时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime leaveTime;

    @ApiModelProperty(value = "处理状态")
    private String status;

    @ApiModelProperty(value = "严重程度")
    private String severity;

    @ApiModelProperty(value = "举报人ID")
    private Integer reporterId;

    @ApiModelProperty(value = "处理人ID")
    private Integer handlerId;

    @ApiModelProperty(value = "创建者ID")
    private String createdBy;

    @ApiModelProperty(value = "现场照片")
    private String photos;

    @ApiModelProperty(value = "语音备注文件路径")
    private String voiceMemo;

    @ApiModelProperty(value = "处理备注")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @ApiModelProperty(value = "是否拉黑")
    @TableField("should_blacklist")
    private Integer shouldBlacklist;

    @ApiModelProperty(value = "拉黑原因")
    @TableField("blacklist_reason")
    private String blacklistReason;

    @ApiModelProperty(value = "黑名单类型编码")
    @TableField("blacklist_type_code")
    private String blacklistTypeCode;

    @ApiModelProperty(value = "黑名单类型名称")
    @TableField("blacklist_type_name")
    private String blacklistTypeName;

    @ApiModelProperty(value = "拉黑时长类型：permanent(永久)/temporary(临时)")
    @TableField("blacklist_duration_type")
    private String blacklistDurationType;

    @ApiModelProperty(value = "拉黑开始时间")
    @TableField("blacklist_start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime blacklistStartTime;

    @ApiModelProperty(value = "拉黑结束时间")
    @TableField("blacklist_end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime blacklistEndTime;

    // 以下字段仅用于东北林业大学车场
    @ApiModelProperty(value = "VIP类型名称/月票类型名称(仅东北林业大学)")
    @TableField("vip_type_name")
    private String vipTypeName;

    @ApiModelProperty(value = "车主姓名(仅东北林业大学)")
    @TableField("owner_name")
    private String ownerName;

    @ApiModelProperty(value = "车主手机号(仅东北林业大学)")
    @TableField("owner_phone")
    private String ownerPhone;

    @ApiModelProperty(value = "车主单位地址/部门(仅东北林业大学)")
    @TableField("owner_address")
    private String ownerAddress;

    @ApiModelProperty(value = "人员类别(仅东北林业大学)")
    @TableField("owner_category")
    private String ownerCategory;

    @ApiModelProperty(value = "单位/公司(仅东北林业大学)")
    @TableField("customer_company")
    private String customerCompany;

    @ApiModelProperty(value = "车位号(仅东北林业大学)")
    @TableField("customer_room_number")
    private String customerRoomNumber;

    // ==================== 🆕 违规记录处理状态字段 ====================
    
    @ApiModelProperty(value = "处理状态: pending-未处理, processed-已处理")
    @TableField("process_status")
    private String processStatus;

    @ApiModelProperty(value = "处理方式: auto_blacklist-系统自动拉黑, manual-手动处理")
    @TableField("process_type")
    private String processType;

    @ApiModelProperty(value = "处理时间")
    @TableField("processed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processedAt;

    @ApiModelProperty(value = "处理人（用户名或SYSTEM）")
    @TableField("processed_by")
    private String processedBy;

    @ApiModelProperty(value = "处理备注")
    @TableField("process_remark")
    private String processRemark;
}