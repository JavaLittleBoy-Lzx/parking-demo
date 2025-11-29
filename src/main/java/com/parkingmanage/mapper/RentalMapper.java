package com.parkingmanage.mapper;

import com.parkingmanage.entity.Rental;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 <p>
 设备租赁 Mapper 接口
 </p>
 @author yuli
 @since 2022-03-02
*/
public interface RentalMapper extends BaseMapper<Rental> {
    List<Rental> getByTypeRent();
}
