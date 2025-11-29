package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 用户映射表
 * </p>
 *
 * @author system
 * @since 2025-01-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "user_mapping", autoResultMap = true)
@ApiModel(value = "UserMapping对象", description = "用户映射")
public class UserMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "微信昵称")
    private String nickname;

    @ApiModelProperty(value = "微信OpenID")
    private String openid;

    @ApiModelProperty(value = "手机号")
    private String phone;

    @ApiModelProperty(value = "创建时间")
    @TableField("created_at")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField("updated_at")
    private Date updateTime;

    @ApiModelProperty(value = "是否删除 0-未删除 1-已删除")
    private Integer deleted;

    @ApiModelProperty(value = "头像URL")
    private String avatarUrl;

    @ApiModelProperty(value = "性别 0-未知 1-男 2-女")
    private Integer gender;

    @ApiModelProperty(value = "是否关注公众号 0-未关注 1-已关注")
    private Integer isFollowed;

    @ApiModelProperty(value = "关注时间")
    private Date followTime;

    @ApiModelProperty(value = "取消关注时间")
    private Date unfollowTime;

} 