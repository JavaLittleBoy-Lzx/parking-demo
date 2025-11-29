package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.TempCarPreEntry;

/**
 * 临时车预进场数据服务接口
 * 
 * @author lzx
 */
public interface TempCarPreEntryService extends IService<TempCarPreEntry> {
    
    /**
     * 保存临时车预进场数据
     * @param plateNumber 车牌号
     * @param parkCode 车场编码
     * @param parkName 车场名称
     * @param enterChannelCode 进场通道编码
     * @param enterChannelId 进场通道ID
     * @param enterVipType 进场VIP类型
     * @param preEnterTime 预进场时间
     * @return 是否保存成功
     */
    boolean saveTempCarPreEntry(String plateNumber, String parkCode, String parkName, 
                               String enterChannelCode, Integer enterChannelId, 
                               Integer enterVipType, String preEnterTime);
    
    /**
     * 根据车牌号和车场编码查询最近的预进场时间
     * @param plateNumber 车牌号
     * @param parkCode 车场编码
     * @return 预进场时间，如果未找到返回null
     */
    String getLatestPreEnterTime(String plateNumber, String parkCode);
    
    /**
     * 标记记录为已使用
     * @param plateNumber 车牌号
     * @param parkCode 车场编码
     * @param preEnterTime 预进场时间
     * @return 是否更新成功
     */
    boolean markAsUsed(String plateNumber, String parkCode, String preEnterTime);
} 