package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.Whitelist;
import com.parkingmanage.exception.BusinessException;
import com.parkingmanage.mapper.WhitelistMapper;
import com.parkingmanage.service.IWhitelistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 白名单管理 服务实现类
 * </p>
 *
 * @author MLH
 * @since 2025-10-07
 */
@Service
public class WhitelistServiceImpl extends ServiceImpl<WhitelistMapper, Whitelist> implements IWhitelistService {

    @Override
    public IPage<Whitelist> getWhitelistPage(Integer page, Integer size, String plateNumber, 
                                              String ownerName, String ownerPhone, String parkName) {
        Page<Whitelist> pageParam = new Page<>(page, size);
        QueryWrapper<Whitelist> wrapper = new QueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.hasText(plateNumber)) {
            wrapper.like("plate_number", plateNumber);
        }
        if (StringUtils.hasText(ownerName)) {
            wrapper.like("owner_name", ownerName);
        }
        if (StringUtils.hasText(ownerPhone)) {
            wrapper.like("owner_phone", ownerPhone);
        }
        if (StringUtils.hasText(parkName)) {
            wrapper.eq("park_name", parkName);
        }
        
        // 按创建时间倒序排列
        wrapper.orderByDesc("created_at");
        
        return this.page(pageParam, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createWhitelist(Whitelist whitelist) {
        // 检查是否已存在相同车牌和车场的记录
        QueryWrapper<Whitelist> wrapper = new QueryWrapper<>();
        wrapper.eq("plate_number", whitelist.getPlateNumber())
               .eq("park_name", whitelist.getParkName());
        
        Integer count = this.baseMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("该车牌已在白名单中，请勿重复添加");
        }
        
        whitelist.setCreatedAt(LocalDateTime.now());
        whitelist.setUpdatedAt(LocalDateTime.now());
        return this.save(whitelist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWhitelist(Whitelist whitelist) {
        // 检查是否存在其他相同车牌和车场的记录
        QueryWrapper<Whitelist> wrapper = new QueryWrapper<>();
        wrapper.eq("plate_number", whitelist.getPlateNumber())
               .eq("park_name", whitelist.getParkName())
               .ne("id", whitelist.getId());
        
        Integer count = this.baseMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("该车牌已在白名单中，请勿重复添加");
        }
        
        whitelist.setUpdatedAt(LocalDateTime.now());
        return this.updateById(whitelist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteWhitelist(Long id) {
        return this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteWhitelist(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public Whitelist getWhitelistByPlate(String plateNumber, String parkName) {
        QueryWrapper<Whitelist> wrapper = new QueryWrapper<>();
        wrapper.eq("plate_number", plateNumber)
               .eq("park_name", parkName);
        return this.getOne(wrapper);
    }

    @Override
    public boolean checkWhitelist(String plateNumber, String parkName) {
        int count = this.baseMapper.checkWhitelist(plateNumber, parkName);
        return count > 0;
    }
}

