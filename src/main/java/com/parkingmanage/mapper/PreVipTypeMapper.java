package com.parkingmanage.mapper;

import com.parkingmanage.entity.PreVipType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author lzx
 * @since 2024-05-11
 */
public interface PreVipTypeMapper extends BaseMapper<PreVipType> {

    String selectByCarNumber(String enterCarLicenseNumber,String enterTime);

    PreVipType checkByLicenseNumber(String enterCarLicenseNumber,String enterTime);
}
