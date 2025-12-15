package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.MonthTick;

import java.util.ArrayList;
import java.util.List;

public interface MonthTicketMapper extends BaseMapper<MonthTick> {

    List<MonthTick> selectAll(String parkName);
}
