package com.parkingmanage.service;

import com.parkingmanage.entity.ChannelInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lzx
 * @since 2023-11-08
 */
public interface ChannelInfoService extends IService<ChannelInfo> {
    //查询车场内所有的通道信息
    List<ChannelInfo> getChannelNameList(String parkCode);
    //根据通道ID查询通道名称
    List<ChannelInfo> getChannelNameById(String parkCode, Integer channelId);

    ChannelInfo channelByName(String entranceName);

    String channelNameByParkCode(String enterChannelCustomCode);

    String getByChnnelId(int enterChannelId);

}
