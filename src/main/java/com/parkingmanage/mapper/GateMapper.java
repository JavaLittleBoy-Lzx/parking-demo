package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.parkingmanage.entity.Gate;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 <p>
  Mapper 接口
 </p>

 @author MLH
 @since 2023-02-18
*/
public interface GateMapper extends BaseMapper<Gate> {
    int duplicate(Gate gate);
    List<Gate> queryGate(@Param(Constants.WRAPPER) Wrapper<Gate> wrapper);
}
