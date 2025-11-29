package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkingmanage.entity.NotifierInfo;
import com.parkingmanage.entity.VehicleClassification;
import com.parkingmanage.mapper.NotifierInfoMapper;
import com.parkingmanage.service.NotifierInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 李子雄
 *
 */
@Service
public class NotifierInfoServiceImpl extends ServiceImpl<NotifierInfoMapper, NotifierInfo> implements NotifierInfoService {
    @Resource
    private NotifierInfoService notifierInfoService;
    @Override
    public List<NotifierInfo> notifierNameList(String merchantName) {
        return baseMapper.notifierNameList(merchantName);
    }

    @Override
    public List<NotifierInfo> merchantNameList() {
        return baseMapper.merchantNameList();
    }

    @Override
    public int duplicate(NotifierInfo notifierInfo) {
        return baseMapper.duplicate(notifierInfo);
    }

    @Override
    public List<NotifierInfo> queryListNotifierInfo(String merchantName) {
        LambdaQueryWrapper<NotifierInfo> queryWrapper = new LambdaQueryWrapper();

        if (StringUtils.hasLength(merchantName)) {
            queryWrapper.like(NotifierInfo::getMerchantName, merchantName);
        }
        List<NotifierInfo> notifierInfoList = notifierInfoService.list(queryWrapper);
        return notifierInfoList;
    }
}
