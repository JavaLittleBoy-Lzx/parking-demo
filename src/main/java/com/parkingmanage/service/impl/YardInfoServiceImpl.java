package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parkingmanage.entity.VehicleReservation;
import com.parkingmanage.entity.YardInfo;
import com.parkingmanage.mapper.YardInfoMapper;
import com.parkingmanage.service.YardInfoService;
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
public class YardInfoServiceImpl extends ServiceImpl<YardInfoMapper, YardInfo> implements YardInfoService {

    @Resource
    private YardInfoService yardInfoService;
    @Override
    public int duplicate(YardInfo yardInfo) {
        return baseMapper.duplicate(yardInfo);
    }

    @Override
    public List<YardInfo> queryYardInfo(QueryWrapper<YardInfo> wrapper) {
        return baseMapper.queryYardInfo(wrapper);
    }

    @Override
    public List<String> yardCode(String yardName) {
        return baseMapper.yardCode(yardName);
    }


    @Override
    public List<YardInfo> yardNameList() {
        return baseMapper.yardNameList();
    }

    @Override
    public List<YardInfo> queryListYardInfo(String yardName) {
        LambdaQueryWrapper<YardInfo> queryWrapper = new LambdaQueryWrapper();
        if (StringUtils.hasLength(yardName)) {
            queryWrapper.like(YardInfo::getYardName, yardName);
        }
        List<YardInfo> yardInfoList = yardInfoService.list(queryWrapper);
        return yardInfoList;
    }

    @Override
    public List<YardInfo> expYardNameList() {
        return baseMapper.expYardNameList();
    }

    @Override
    public YardInfo yardByName(String parkName) {
        return baseMapper.yardByName(parkName);

    }

    @Override
    public String selectParkCode(String parkName) {
        return baseMapper.selectParkCode(parkName);
    }

    @Override
    public List<String>  selectByParkCode(String parkCode) {
        return baseMapper.selectByParkCode(parkCode);
    }
}
