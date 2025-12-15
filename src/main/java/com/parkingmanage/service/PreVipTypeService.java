package com.parkingmanage.service;

import com.parkingmanage.entity.PreVipType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lzx
 * @since 2024-05-11
 */
public interface PreVipTypeService extends IService<PreVipType> {

    String selectByCarNumber(String enterCarLicenseNumber,String enterTime);

    PreVipType checkByLicenseNumber(String enterCarLicenseNumber,String enterTime);
}
