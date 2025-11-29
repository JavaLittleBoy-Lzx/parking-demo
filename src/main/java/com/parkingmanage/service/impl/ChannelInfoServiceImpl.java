package com.parkingmanage.service.impl;

import com.parkingmanage.entity.ChannelInfo;
import com.parkingmanage.entity.Sys;
import com.parkingmanage.mapper.ChannelInfoMapper;
import com.parkingmanage.service.ChannelInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lzx
 * @since 2023-11-08
 */
@Service
public class ChannelInfoServiceImpl extends ServiceImpl<ChannelInfoMapper, ChannelInfo> implements ChannelInfoService {

    @Override
    public List<ChannelInfo> getChannelNameList(String parkCode) {
        return baseMapper.getChannelNameList(parkCode);
    }

    @Override
    public List<ChannelInfo> getChannelNameById(String parkCode, Integer channelId) {
        return baseMapper.getChannelNameById(parkCode,channelId);
    }

    @Override
    public ChannelInfo channelByName(String entranceName) {
        return baseMapper.channelByName(entranceName);
    }

    @Override
    public String channelNameByParkCode(String enterChannelCustomCode) {
        return baseMapper.channelNameByParkCode(enterChannelCustomCode);
    }

    @Override
    public String getByChnnelId(int enterChannelId) {
        return baseMapper.getByChnnelId(enterChannelId);
    }
}
