package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.ViolationLocation;
import com.parkingmanage.mapper.ViolationLocationMapper;
import com.parkingmanage.service.ViolationLocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 违规位置配置Service实现类
 * @author system
 * @date 2025-01-31
 */
@Slf4j
@Service
public class ViolationLocationServiceImpl extends ServiceImpl<ViolationLocationMapper, ViolationLocation> 
        implements ViolationLocationService {
    
    @Override
    public Page<ViolationLocation> getLocationPage(Integer page, Integer size, 
                                                   String locationName, String parkName, Boolean isEnabled) {
        Page<ViolationLocation> pageParam = new Page<>(page, size);
        return baseMapper.selectLocationPage(pageParam, locationName, parkName, isEnabled);
    }
    
    @Override
    public List<ViolationLocation> getEnabledLocations(String parkName) {
        return baseMapper.selectEnabledLocations(parkName);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addLocation(ViolationLocation location) {
        // 检查是否已存在相同名称的位置
        ViolationLocation existing = baseMapper.selectByNameAndPark(
            location.getLocationName(), location.getParkName());
        if (existing != null) {
            log.warn("违规位置已存在: locationName={}, parkName={}", 
                location.getLocationName(), location.getParkName());
            throw new RuntimeException("违规位置已存在");
        }
        
        return this.save(location);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLocation(ViolationLocation location) {
        return this.updateById(location);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLocation(Long id) {
        return this.removeById(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleEnabled(Long id, Boolean isEnabled) {
        ViolationLocation location = this.getById(id);
        if (location == null) {
            log.warn("违规位置不存在: id={}", id);
            throw new RuntimeException("违规位置不存在");
        }
        
        location.setIsEnabled(isEnabled);
        return this.updateById(location);
    }
}

