package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.ViolationTypes;
import com.parkingmanage.mapper.ViolationTypesMapper;
import com.parkingmanage.service.ViolationTypesService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 违规类型配置表 服务实现类
 * </p>
 *
 * @author MLH
 * @since 2025-01-31
 */
@Service
public class ViolationTypesServiceImpl extends ServiceImpl<ViolationTypesMapper, ViolationTypes> implements ViolationTypesService {

    @Resource
    private ViolationTypesMapper violationTypesMapper;

    @Override
    public Map<String, List<ViolationTypes>> getViolationTypesByCategory() {
        LambdaQueryWrapper<ViolationTypes> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ViolationTypes::getIsActive, true);
        wrapper.orderByAsc(ViolationTypes::getSortOrder);
        
        List<ViolationTypes> allTypes = this.list(wrapper);
        
        Map<String, List<ViolationTypes>> result = new HashMap<>();
        result.put("common", allTypes.stream()
                .filter(type -> "common".equals(type.getCategory()))
                .collect(Collectors.toList()));
        result.put("others", allTypes.stream()
                .filter(type -> "others".equals(type.getCategory()))
                .collect(Collectors.toList()));
        
        return result;
    }

    @Override
    public boolean createViolationType(ViolationTypes violationType) {
        // 检查值是否已存在
        if (checkValueExists(violationType.getValue(), null)) {
            return false;
        }
        
        // 设置默认值
        if (violationType.getUsageCount() == null) {
            violationType.setUsageCount(0);
        }
        if (violationType.getIsActive() == null) {
            violationType.setIsActive(true);
        }
        if (violationType.getSortOrder() == null) {
            violationType.setSortOrder(0);
        }
        if (!StringUtils.hasText(violationType.getCategory())) {
            violationType.setCategory("others");
        }
        
        violationType.setCreatedAt(LocalDateTime.now());
        violationType.setUpdatedAt(LocalDateTime.now());
        
        return this.save(violationType);
    }

    @Override
    public boolean updateViolationType(ViolationTypes violationType) {
        // 检查值是否已存在（排除当前记录）
        if (StringUtils.hasText(violationType.getValue()) && 
            checkValueExists(violationType.getValue(), violationType.getId())) {
            return false;
        }
        
        violationType.setUpdatedAt(LocalDateTime.now());
        return this.updateById(violationType);
    }

    @Override
    public boolean deleteViolationType(Long id) {
        // 软删除：设置为不启用
        ViolationTypes violationType = new ViolationTypes();
        violationType.setId(id);
        violationType.setIsActive(false);
        violationType.setUpdatedAt(LocalDateTime.now());
        
        return this.updateById(violationType);
    }

    @Override
    public boolean updateUsageCount(String value) {
        return violationTypesMapper.updateUsageCount(value) > 0;
    }

    @Override
    public boolean checkValueExists(String value, Long excludeId) {
        return violationTypesMapper.checkValueExists(value, excludeId) > 0;
    }

    @Override
    public List<ViolationTypes> getActiveTypes() {
        return violationTypesMapper.selectActiveTypes();
    }
}
