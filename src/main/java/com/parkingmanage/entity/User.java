package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.parkingmanage.handle.EncryptHandler;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *
 * </p>
 *
 * @author yuli
 * @since 2022-02-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "user", autoResultMap = true)
@ApiModel(value = "User对象", description = "用户")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "user_id", type = IdType.AUTO)
    private Integer userId;

    @ApiModelProperty(value = "登录账号")
    private String loginName;

    @ApiModelProperty(value = "用户名")
    private String userName;

    @ApiModelProperty(value = "'密码")
    @TableField(typeHandler = EncryptHandler.class)
    private String password;

    @ApiModelProperty(value = "手机号")
    private String telephone;

    @ApiModelProperty(value = "部门id")
    private Integer departmentId;

    @ApiModelProperty(value = "角色id")
    private Integer roleId;

    @ApiModelProperty(value = "邮箱")
    private String email;

    @ApiModelProperty(value = "头像URL")
    private String avatar;

    @ApiModelProperty(value = "性别：0-未知，1-男，2-女")
    private Integer gender;

    @ApiModelProperty(value = "生日")
    private java.time.LocalDate birthday;

    @ApiModelProperty(value = "地址")
    private String address;

    @ApiModelProperty(value = "最后登录时间")
    private java.time.LocalDateTime lastLoginTime;

    @ApiModelProperty(value = "最后登录IP")
    private String lastLoginIp;

    @ApiModelProperty(value = "登录次数")
    private Integer loginCount;

    @ApiModelProperty(value = "登录失败次数")
    private Integer failedLoginCount;

    @ApiModelProperty(value = "账户锁定时间")
    private java.time.LocalDateTime lockTime;

    @ApiModelProperty(value = "状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty(value = "管理的车场列表（逗号分隔）")
    private String managedParks;

    @ApiModelProperty(value = "创建时间")
    private java.time.LocalDateTime createdTime;

    @ApiModelProperty(value = "更新时间")
    private java.time.LocalDateTime updatedTime;

    private Integer deleted;

    @TableField(exist = false)
    private String departmentName;

    @TableField(exist = false)
    private String roleName;

    @TableField(exist = false)
    private String token;

    @TableField(exist = false)
    @ApiModelProperty(value = "角色列表（包含权限信息）")
    private java.util.List<Role> roles;

    /**
     * 🔄 前端发送数组时，自动转换为逗号分隔的字符串存储到数据库
     * 用于接收前端发送的车场数组：["车场A", "车场B", "车场C"]
     * 
     * @param parksList 车场名称列表
     */
    @JsonSetter("managedParks")
    public void setManagedParksFromList(List<String> parksList) {
        if (parksList == null || parksList.isEmpty()) {
            this.managedParks = null;
        } else {
            // 过滤空值，然后用逗号连接
            this.managedParks = parksList.stream()
                    .filter(park -> park != null && !park.trim().isEmpty())
                    .map(String::trim)
                    .collect(Collectors.joining(","));
        }
    }

    /**
     * 🔄 返回给前端时，自动将逗号分隔的字符串转换为数组
     * 返回格式：["车场A", "车场B", "车场C"]
     * 
     * @return 车场名称列表
     */
    @JsonGetter("managedParks")
    public List<String> getManagedParksAsList() {
        if (this.managedParks == null || this.managedParks.trim().isEmpty()) {
            return null;
        }
        // 分隔字符串，过滤空值
        return Arrays.stream(this.managedParks.split(","))
                .map(String::trim)
                .filter(park -> !park.isEmpty())
                .collect(Collectors.toList());
    }

}
