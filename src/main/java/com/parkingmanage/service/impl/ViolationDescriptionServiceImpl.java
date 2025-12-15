package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.ViolationDescription;
import com.parkingmanage.mapper.ViolationDescriptionMapper;
import com.parkingmanage.service.ViolationDescriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 违规描述模板Service实现类
 * @author system
 * @date 2025-01-31
 */
@Slf4j
@Service
public class ViolationDescriptionServiceImpl extends ServiceImpl<ViolationDescriptionMapper, ViolationDescription> 
        implements ViolationDescriptionService {
    
    @Override
    public Page<ViolationDescription> getDescriptionPage(Integer page, Integer size, 
                                                         String descriptionText, String violationTypeCode, 
                                                         String parkName, Boolean isEnabled) {
        Page<ViolationDescription> pageParam = new Page<>(page, size);
        return baseMapper.selectDescriptionPage(pageParam, descriptionText, violationTypeCode, parkName, isEnabled);
    }
    
    @Override
    public List<ViolationDescription> getEnabledDescriptions(String violationTypeCode, String parkName) {
        return baseMapper.selectEnabledDescriptions(violationTypeCode, parkName);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addDescription(ViolationDescription description) {
        // 初始化使用次数为0
        if (description.getUsageCount() == null) {
            description.setUsageCount(0);
        }
        return this.save(description);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDescription(ViolationDescription description) {
        return this.updateById(description);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDescription(Long id) {
        return this.removeById(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleEnabled(Long id, Boolean isEnabled) {
        ViolationDescription description = this.getById(id);
        if (description == null) {
            log.warn("违规描述不存在: id={}", id);
            throw new RuntimeException("违规描述不存在");
        }
        
        description.setIsEnabled(isEnabled);
        return this.updateById(description);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementUsageCount(Long id) {
        baseMapper.incrementUsageCount(id);
    }
}

