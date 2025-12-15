package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.parkingmanage.entity.BlackList;
import com.parkingmanage.entity.MonthTick;
import com.parkingmanage.mapper.BlackListMapper;
import com.parkingmanage.service.BlackListService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lzx
 * @since 2023-12-21
 */
@Service
public class BlackListServiceImpl extends ServiceImpl<BlackListMapper, BlackList> implements BlackListService {

    @Override
    public List<BlackList> queryInfoOnly(String parkName, String carCode, String specialCarTypeConfigName, String userName, String blackReason, String remark1, String remark2) {
        QueryWrapper<BlackList> queryWrapper = new QueryWrapper<>();
        // 只查询未删除的记录
        queryWrapper.eq("deleted", 0);
        // 模糊搜索部分
        queryWrapper.like(StringUtils.isNotBlank(carCode), "car_code", carCode);
        queryWrapper.like(StringUtils.isNotBlank(userName), "owner", userName);
        queryWrapper.like(StringUtils.isNotBlank(blackReason), "reason", blackReason);
        queryWrapper.like(StringUtils.isNotBlank(remark1), "remark1", remark1);
        queryWrapper.like(StringUtils.isNotBlank(remark2), "remark2", remark2);
        queryWrapper.eq(StringUtils.isNotBlank(parkName), "park_name", parkName);
        queryWrapper.eq(StringUtils.isNotBlank(specialCarTypeConfigName), "special_car_type_config_name", specialCarTypeConfigName);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<BlackList> findOne(BlackList blackList) {
//        System.out.println("blackList = " + blackList);
        LambdaQueryWrapper<BlackList> queryWrapper = new LambdaQueryWrapper<>();
        // 只查询未删除的记录
        queryWrapper.eq(BlackList::getDeleted, 0);
        queryWrapper.eq(BlackList::getCarCode, blackList.getCarCode()).eq(BlackList::getReason, blackList.getReason())
                .eq(BlackList::getParkName, blackList.getParkName()).eq(BlackList::getRemark1, blackList.getRemark1())
                .eq(BlackList::getRemark2, blackList.getRemark2()).eq(BlackList::getBlackListForeverFlag, blackList.getBlackListForeverFlag()).
                eq(BlackList::getOwner, blackList.getOwner()).eq(BlackList::getSpecialCarTypeConfigName, blackList.getSpecialCarTypeConfigName());
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public boolean logicDeleteByIds(Collection<Integer> ids, String deleteBy) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        
        LambdaUpdateWrapper<BlackList> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(BlackList::getId, ids);
        updateWrapper.set(BlackList::getDeleted, 1);
        updateWrapper.set(BlackList::getDeleteTime, LocalDateTime.now());
        updateWrapper.set(BlackList::getDeleteBy, deleteBy);
        
        int updateCount = baseMapper.update(null, updateWrapper);
        return updateCount > 0;
    }

    @Override
    public boolean logicDeleteById(Integer id, String deleteBy) {
        if (id == null) {
            return false;
        }
        
        LambdaUpdateWrapper<BlackList> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BlackList::getId, id);
        updateWrapper.set(BlackList::getDeleted, 1);
        updateWrapper.set(BlackList::getDeleteTime, LocalDateTime.now());
        updateWrapper.set(BlackList::getDeleteBy, deleteBy);
        
        int updateCount = baseMapper.update(null, updateWrapper);
        return updateCount > 0;
    }

    @Override
    public List<BlackList> queryDeletedInfoOnly(String parkName, String carCode, String specialCarTypeConfigName, String userName, String blackReason, String remark1, String remark2) {
        QueryWrapper<BlackList> queryWrapper = new QueryWrapper<>();
        // 只查询已删除的记录
        queryWrapper.eq("deleted", 1);
        // 模糊搜索部分
        queryWrapper.like(StringUtils.isNotBlank(carCode), "car_code", carCode);
        queryWrapper.like(StringUtils.isNotBlank(userName), "owner", userName);
        queryWrapper.like(StringUtils.isNotBlank(blackReason), "reason", blackReason);
        queryWrapper.like(StringUtils.isNotBlank(remark1), "remark1", remark1);
        queryWrapper.like(StringUtils.isNotBlank(remark2), "remark2", remark2);
        queryWrapper.eq(StringUtils.isNotBlank(parkName), "park_name", parkName);
        queryWrapper.eq(StringUtils.isNotBlank(specialCarTypeConfigName), "special_car_type_config_name", specialCarTypeConfigName);
        // 按删除时间倒序排列
        queryWrapper.orderByDesc("delete_time");
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public boolean restoreById(Integer id) {
        if (id == null) {
            return false;
        }
        
        LambdaUpdateWrapper<BlackList> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BlackList::getId, id);
        updateWrapper.eq(BlackList::getDeleted, 1); // 只恢复已删除的记录
        updateWrapper.set(BlackList::getDeleted, 0);
        updateWrapper.set(BlackList::getDeleteTime, null);
        updateWrapper.set(BlackList::getDeleteBy, null);
        
        int updateCount = baseMapper.update(null, updateWrapper);
        return updateCount > 0;
    }

    @Override
    public boolean existsByParkAndCar(String parkName, String carCode) {
        LambdaQueryWrapper<BlackList> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlackList::getDeleted, 0)
                .eq(BlackList::getParkName, parkName)
                .eq(BlackList::getCarCode, carCode);
        return baseMapper.selectCount(wrapper) > 0;
    }
}
