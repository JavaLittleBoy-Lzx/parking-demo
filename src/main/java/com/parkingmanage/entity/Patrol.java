package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 巡逻员表
 * </p>
 *
 * @author MLH
 * @since 2025-01-31
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("patrol")
public class Patrol implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 巡逻员编码
     */
    private String usercode;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 区县
     */
    private String district;

    /**
     * 负责小区
     */
    private String community;

    /**
     * 巡逻员手机号
     */
    private String phone;

    /**
     * 巡逻员用户名
     */
    private String username;

    /**
     * 创建日期
     */
    private LocalDateTime createdate;

    /**
     * 创建人
     */
    private String createman;

    /**
     * 状态
     */
    private String status;

    /**
     * 微信openid
     */
    private String openid;

    /**
     * 确认日期
     */
    private LocalDateTime confirmdate;

    /**
     * 消息通知开关：1=接收（值班中），0=不接收（离岗）
     */
    private Integer notificationEnabled;

    /**
     * 最后一次值班状态变更时间
     */
    private LocalDateTime lastStatusChangeTime;

}
