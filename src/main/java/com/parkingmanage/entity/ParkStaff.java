package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 车场人员实体类
 * 对应数据库表：park_staff
 * 
 * @author parking-system
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("park_staff")
@ApiModel(value = "ParkStaff对象", description = "车场人员表")
public class ParkStaff implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "用户名（登录用）")
    @TableField("username")
    private String username;

    @JsonIgnore  // 防止密码被序列化返回给前端
    @ApiModelProperty(value = "密码（加密存储）")
    @TableField("password")
    private String password;

    @ApiModelProperty(value = "车场名称")
    @TableField("park_name")
    private String parkName;

    @ApiModelProperty(value = "真实姓名")
    @TableField("real_name")
    private String realName;

    @ApiModelProperty(value = "手机号码")
    @TableField("phone")
    private String phone;

    @ApiModelProperty(value = "邮箱地址")
    @TableField("email")
    private String email;

    @ApiModelProperty(value = "职位（管理员、巡逻员、收费员等）")
    @TableField("position")
    private String position;

    @ApiModelProperty(value = "状态：1=正常，0=禁用")
    @TableField("status")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最后登录时间")
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @ApiModelProperty(value = "最后登录IP")
    @TableField("last_login_ip")
    private String lastLoginIp;

    @ApiModelProperty(value = "登录失败次数")
    @TableField("failed_login_count")
    private Integer failedLoginCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "账户锁定时间")
    @TableField("lock_time")
    private LocalDateTime lockTime;

    @ApiModelProperty(value = "账户锁定次数（累计）")
    @TableField("lock_count")
    private Integer lockCount;

    @ApiModelProperty(value = "禁用原因")
    @TableField("disable_reason")
    private String disableReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "禁用时间")
    @TableField("disable_time")
    private LocalDateTime disableTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    @TableField("created_time")
    private LocalDateTime createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    @TableField("updated_time")
    private LocalDateTime updatedTime;

    @ApiModelProperty(value = "创建人ID")
    @TableField("created_by")
    private Integer createdBy;

    @ApiModelProperty(value = "备注信息")
    @TableField("remark")
    private String remark;

    // 无参构造函数（MyBatis Plus需要）
    public ParkStaff() {}

    // 有参构造函数
    public ParkStaff(String username, String password, String parkName, String realName) {
        this.username = username;
        this.password = password;
        this.parkName = parkName;
        this.realName = realName;
        this.status = 1; // 默认正常状态
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
    }
} 