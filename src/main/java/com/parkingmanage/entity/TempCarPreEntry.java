package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 临时车预进场数据记录
 * 用于存储enterVipType=1且enterChannelCode=520243的临时车预进场数据
 * 
 * @author lzx
 */
@Data
@TableName("temp_car_pre_entry")
public class TempCarPreEntry {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    /**
     * 车牌号
     */
    private String plateNumber;
    
    /**
     * 车场编码
     */
    private String parkCode;
    
    /**
     * 车场名称
     */
    private String parkName;
    
    /**
     * 进场通道编码
     */
    private String enterChannelCode;
    
    /**
     * 进场通道ID
     */
    private Integer enterChannelId;
    
    /**
     * 进场VIP类型
     */
    private Integer enterVipType;
    
    /**
     * 预进场时间
     */
    private String preEnterTime;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 是否已使用 (0-未使用, 1-已使用)
     */
    private Integer used;
    
    /**
     * 备注
     */
    private String remark;
} 