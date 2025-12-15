package com.parkingmanage.service.impl;

import com.parkingmanage.entity.PreVipType;
import com.parkingmanage.mapper.PreVipTypeMapper;
import com.parkingmanage.service.PreVipTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lzx
 * @since 2024-05-11
 */
@Service
public class PreVipTypeServiceImpl extends ServiceImpl<PreVipTypeMapper, PreVipType> implements PreVipTypeService {

    @Override
    public String selectByCarNumber(String enterCarLicenseNumber,String enterTime) {
        return baseMapper.selectByCarNumber(enterCarLicenseNumber,enterCarLicenseNumber);
    }

    @Override
    public PreVipType checkByLicenseNumber(String enterCarLicenseNumber,String enterTime) {
        return baseMapper.checkByLicenseNumber(enterCarLicenseNumber,enterTime);
    }

}
