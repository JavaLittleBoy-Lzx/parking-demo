package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息发送记录实体类
 * 
 * 功能说明：
 * - 记录所有微信模板消息的发送情况
 * - 用于防止重复发送、追踪发送历史、问题排查、统计分析
 * 
 * @author System
 * @since 2024-12-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("notification_log")
@ApiModel(value = "NotificationLog对象", description = "消息发送记录表")
public class NotificationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Integer id;

    // ==================== 关联信息 ====================
    
    @ApiModelProperty(value = "预约ID（关联appointment表）")
    private Integer appointmentId;

    @ApiModelProperty(value = "车牌号")
    private String plateNumber;

    @ApiModelProperty(value = "接收者openid", required = true)
    private String openid;

    // ==================== 通知类型 ====================
    
    @ApiModelProperty(value = "通知类型：timeout_15min, timeout_5min, timeout_1min, violation", required = true)
    private String notificationType;

    @ApiModelProperty(value = "微信模板ID")
    private String templateId;

    // ==================== 通知内容 ====================
    
    @ApiModelProperty(value = "通知时间点（分钟）：15/5/1")
    private Integer notifyTimePoint;

    @ApiModelProperty(value = "剩余时间（分钟）")
    private Integer remainingMinutes;

    @ApiModelProperty(value = "停车时长（分钟）")
    private Integer parkingMinutes;

    @ApiModelProperty(value = "停车场名称")
    private String parkName;

    @ApiModelProperty(value = "违规地点")
    private String violationLocation;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "进场时间")
    private LocalDateTime entryTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "出场时间")
    private LocalDateTime exitTime;

    @ApiModelProperty(value = "停车时长文本（格式化后），如\"2小时30分\"")
    private String parkingDuration;

    @ApiModelProperty(value = "加入黑名单原因")
    private String blacklistReason;

    @ApiModelProperty(value = "黑名单有效天数")
    private Integer blacklistDays;

    @ApiModelProperty(value = "审核状态：通过/拒绝")
    private String auditStatus;

    @ApiModelProperty(value = "审核原因/拒绝原因")
    private String auditReason;

    @ApiModelProperty(value = "预约访问日期")
    private String visitDate;

    // ==================== 接收者信息 ====================
    
    @ApiModelProperty(value = "接收者类型：visitor(访客), owner(业主), housekeeper(管家)")
    private String receiverType;

    @ApiModelProperty(value = "预约类型：邀请/代人/自助/业主")
    private String appointType;

    // ==================== 发送结果 ====================
    
    @ApiModelProperty(value = "发送状态：0=失败, 1=成功, 2=跳过（重复）", required = true)
    private Integer sendStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "发送时间", required = true)
    private LocalDateTime sendTime;

    @ApiModelProperty(value = "是否成功：0=否, 1=是")
    private Integer success;

    @ApiModelProperty(value = "错误代码")
    private String errorCode;

    @ApiModelProperty(value = "错误信息")
    private String errorMessage;

    // ==================== 微信返回 ====================
    
    @ApiModelProperty(value = "微信消息ID")
    private String wechatMsgId;

    @ApiModelProperty(value = "微信返回的完整响应（JSON格式）")
    private String wechatResponse;

    // ==================== 元数据 ====================
    
    @ApiModelProperty(value = "发送来源：scheduled_task(定时任务), api(API调用), manual(手动)")
    private String sendBy;

    @ApiModelProperty(value = "重试次数")
    private Integer retryCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    // ==================== 常量定义 ====================
    
    /** 通知类型常量 */
    public static class NotificationType {
        // 超时通知
        public static final String TIMEOUT_15MIN = "timeout_15min";  // 超时15分钟提醒
        public static final String TIMEOUT_5MIN = "timeout_5min";    // 超时5分钟提醒
        public static final String TIMEOUT_1MIN = "timeout_1min";    // 超时1分钟提醒
        
        // 违规通知
        public static final String VIOLATION = "violation";          // 车辆违规停车告警通知
        
        // 进出场通知
        public static final String ENTRY = "entry";                  // 车辆进场通知
        public static final String EXIT = "exit";                    // 车辆出场通知
        
        // 黑名单通知
        public static final String BLACKLIST_ADD = "blacklist_add";  // 车辆加入黑名单成功通知
        
        // 预约通知
        public static final String APPOINTMENT_SUCCESS = "appointment_success";  // 预约成功提醒
        public static final String APPOINTMENT_PENDING = "appointment_pending";  // 预约车辆待审核提醒
        public static final String APPOINTMENT_AUDIT = "appointment_audit";      // 停车场预约审核结果通知
    }

    /** 发送状态常量 */
    public static class SendStatus {
        public static final int FAILED = 0;  // 失败
        public static final int SUCCESS = 1; // 成功
        public static final int SKIPPED = 2; // 跳过（重复）
    }

    /** 接收者类型常量 */
    public static class ReceiverType {
        public static final String VISITOR = "visitor";      // 访客
        public static final String OWNER = "owner";          // 业主
        public static final String HOUSEKEEPER = "housekeeper"; // 管家
    }

    /** 发送来源常量 */
    public static class SendBy {
        public static final String SCHEDULED_TASK = "scheduled_task"; // 定时任务
        public static final String API = "api";                       // API调用
        public static final String MANUAL = "manual";                 // 手动
    }
}
