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
 * 测试用访客预约记录实体
 * 用于模拟外部接口返回的数据
 * 
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("test_visitor_reservation")
@ApiModel(value = "TestVisitorReservation对象", description = "测试用访客预约记录")
public class TestVisitorReservation implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "用户ID")
    @TableField("user_id")
    private Long userId;

    @ApiModelProperty(value = "网关通行开始时间")
    @TableField("gateway_transit_begin_time")
    private String gatewayTransitBeginTime;

    @ApiModelProperty(value = "网关通行结束时间")
    @TableField("gateway_transit_end_time")
    private String gatewayTransitEndTime;

    @ApiModelProperty(value = "预约开始时间")
    @TableField("begin_time")
    private String beginTime;

    @ApiModelProperty(value = "预约结束时间")
    @TableField("end_time")
    private String endTime;

    @ApiModelProperty(value = "申请来源名称")
    @TableField("apply_from_name")
    private String applyFromName;

    @ApiModelProperty(value = "申请来源")
    @TableField("apply_from")
    private Integer applyFrom;

    @ApiModelProperty(value = "表单ID")
    @TableField("form_id")
    private Long formId;

    @ApiModelProperty(value = "表单名称")
    @TableField("form_name")
    private String formName;

    @ApiModelProperty(value = "访客身份证号码")
    @TableField("visitor_id_card")
    private String visitorIdCard;

    @ApiModelProperty(value = "访客姓名")
    @TableField("visitor_user_name")
    private String visitorUserName;

    @ApiModelProperty(value = "访客手机号码")
    @TableField("visitor_phone_no")
    private String visitorPhoneNo;

    @ApiModelProperty(value = "通行证名称")
    @TableField("pass_name")
    private String passName;

    @ApiModelProperty(value = "申请状态")
    @TableField("apply_state")
    private Integer applyState;

    @ApiModelProperty(value = "申请状态名称")
    @TableField("apply_state_name")
    private String applyStateName;

    @ApiModelProperty(value = "使用状态ID")
    @TableField("use_status_id")
    private Integer useStatusId;

    @ApiModelProperty(value = "手机号码")
    @TableField("phone_no")
    private String phoneNo;

    @ApiModelProperty(value = "通行证编号")
    @TableField("pass_no")
    private String passNo;

    @ApiModelProperty(value = "通行证部门")
    @TableField("pass_dep")
    private String passDep;

    @ApiModelProperty(value = "同行人数")
    @TableField("companions_num")
    private Integer companionsNum;

    @ApiModelProperty(value = "编码字符串")
    @TableField("code_str")
    private String codeStr;

    @ApiModelProperty(value = "外部用户编号")
    @TableField("foreign_user_no")
    private String foreignUserNo;

    @ApiModelProperty(value = "授权状态")
    @TableField("auth_state")
    private Integer authState;

    @ApiModelProperty(value = "授权状态字符串")
    @TableField("auth_state_str")
    private String authStateStr;

    @ApiModelProperty(value = "审批流程ID")
    @TableField("approval_flow_id")
    private String approvalFlowId;

    @ApiModelProperty(value = "提交ID")
    @TableField("submit_id")
    private Long submitId;

    @ApiModelProperty(value = "任务ID")
    @TableField("task_id")
    private String taskId;

    @ApiModelProperty(value = "访客车牌号")
    @TableField("visitor_plate_number")
    private String visitorPlateNumber;

    @ApiModelProperty(value = "自定义VIP名称")
    @TableField("custom_vip_name")
    private String customVipName;

    @ApiModelProperty(value = "备注1")
    @TableField("bz")
    private String bz;

    @ApiModelProperty(value = "备注2")
    @TableField("bz2")
    private String bz2;

    @ApiModelProperty(value = "备注3")
    @TableField("bz3")
    private String bz3;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("ct_date")
    private String ctDate;

    @ApiModelProperty(value = "创建时间（Date类型）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "create_time", fill = FieldFill.INSERT, exist = false)
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE, exist = false)
    private Date updateTime;

    @ApiModelProperty(value = "排序字段映射")
    @TableField(exist = false)
    private Object orderByFieldMap;
}

