package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户微信信息实体类
 * 用于存储用户在小程序和公众号中的身份信息
 */
@Data
@TableName("user_wechat_info")
public class UserWechatInfo {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;
    
    /**
     * UnionID（关键字段）
     */
    @TableField("unionid")
    private String unionid;
    
    /**
     * 小程序OpenID
     */
    @TableField("miniapp_openid")
    private String miniappOpenid;
    
    /**
     * 公众号OpenID
     */
    @TableField("public_openid")
    private String publicOpenid;
    
    /**
     * 微信昵称
     */
    @TableField("nickname")
    private String nickname;
    
    /**
     * 头像URL
     */
    @TableField("avatar_url")
    private String avatarUrl;
    
    /**
     * 性别：0-未知，1-男，2-女
     */
    @TableField("gender")
    private Integer gender;
    
    /**
     * 国家
     */
    @TableField("country")
    private String country;
    
    /**
     * 省份
     */
    @TableField("province")
    private String province;
    
    /**
     * 城市
     */
    @TableField("city")
    private String city;
    
    /**
     * 是否关注公众号
     */
    @TableField("is_subscribed")
    private Integer isSubscribed;
    
    /**
     * 关注时间
     */
    @TableField("subscribe_time")
    private Date subscribeTime;
    
    /**
     * 关注场景
     */
    @TableField("subscribe_scene")
    private String subscribeScene;
    
    /**
     * 创建时间
     */
    @TableField("created_time")
    private Date createdTime;
    
    /**
     * 更新时间
     */
    @TableField("updated_time")
    private Date updatedTime;
} 