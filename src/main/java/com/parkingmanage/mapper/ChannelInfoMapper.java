package com.parkingmanage.mapper;

import com.parkingmanage.entity.ChannelInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author lzx
 * @since 2023-11-08
 */
public interface ChannelInfoMapper extends BaseMapper<ChannelInfo> {

    List<ChannelInfo> getChannelNameList(String parkCode);

    List<ChannelInfo> getChannelNameById(String parkCode, Integer channelId);

    ChannelInfo channelByName(String entranceName);

    String channelNameByParkCode(String enterChannelCustomCode);

    String getByChnnelId(int enterChannelId);
}
