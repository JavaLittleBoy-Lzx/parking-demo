package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.ViolationType;
import com.parkingmanage.mapper.ViolationTypeMapper;
import com.parkingmanage.service.ViolationTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 违规类型配置Service实现类
 * @author system
 * @date 2025-01-31
 */
@Slf4j
@Service
public class ViolationTypeServiceImpl extends ServiceImpl<ViolationTypeMapper, ViolationType> 
        implements ViolationTypeService {
    
    @Override
    public Page<ViolationType> getTypePage(Integer page, Integer size, 
                                          String typeName, String parkName, String severityLevel, Boolean isEnabled) {
        Page<ViolationType> pageParam = new Page<>(page, size);
        return baseMapper.selectTypePage(pageParam, typeName, parkName, severityLevel, isEnabled);
    }
    
    @Override
    public List<ViolationType> getEnabledTypes(String parkName) {
        return baseMapper.selectEnabledTypes(parkName);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addType(ViolationType type) {
        // 检查是否已存在相同代码的类型
        ViolationType existing = baseMapper.selectByCodeAndPark(
            type.getTypeCode(), type.getParkName());
        if (existing != null) {
            log.warn("违规类型已存在: typeCode={}, parkName={}", 
                type.getTypeCode(), type.getParkName());
            throw new RuntimeException("违规类型代码已存在");
        }
        
        return this.save(type);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateType(ViolationType type) {
        return this.updateById(type);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteType(Long id) {
        return this.removeById(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleEnabled(Long id, Boolean isEnabled) {
        ViolationType type = this.getById(id);
        if (type == null) {
            log.warn("违规类型不存在: id={}", id);
            throw new RuntimeException("违规类型不存在");
        }
        
        type.setIsEnabled(isEnabled);
        return this.updateById(type);
    }
}

