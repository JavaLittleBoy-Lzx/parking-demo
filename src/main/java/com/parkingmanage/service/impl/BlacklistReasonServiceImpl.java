package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.BlacklistReason;
import com.parkingmanage.mapper.BlacklistReasonMapper;
import com.parkingmanage.service.BlacklistReasonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 拉黑原因模板Service实现类
 * @author system
 * @date 2025-01-31
 */
@Slf4j
@Service
public class BlacklistReasonServiceImpl extends ServiceImpl<BlacklistReasonMapper, BlacklistReason> 
        implements BlacklistReasonService {
    
    @Override
    public Page<BlacklistReason> getReasonPage(Integer page, Integer size, 
                                              String reasonText, String reasonCategory, 
                                              String parkName, Boolean isEnabled) {
        Page<BlacklistReason> pageParam = new Page<>(page, size);
        return baseMapper.selectReasonPage(pageParam, reasonText, reasonCategory, parkName, isEnabled);
    }
    
    @Override
    public List<BlacklistReason> getEnabledReasons(String reasonCategory, String parkName) {
        return baseMapper.selectEnabledReasons(reasonCategory, parkName);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addReason(BlacklistReason reason) {
        // 初始化使用次数为0
        if (reason.getUsageCount() == null) {
            reason.setUsageCount(0);
        }
        return this.save(reason);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateReason(BlacklistReason reason) {
        return this.updateById(reason);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteReason(Long id) {
        return this.removeById(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleEnabled(Long id, Boolean isEnabled) {
        BlacklistReason reason = this.getById(id);
        if (reason == null) {
            log.warn("拉黑原因不存在: id={}", id);
            throw new RuntimeException("拉黑原因不存在");
        }
        
        reason.setIsEnabled(isEnabled);
        return this.updateById(reason);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementUsageCount(Long id) {
        baseMapper.incrementUsageCount(id);
    }
}

