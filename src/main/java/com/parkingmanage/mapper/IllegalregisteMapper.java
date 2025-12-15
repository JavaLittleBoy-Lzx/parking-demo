package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.Illegalregiste;

import java.util.List;

/**
 <p>
  Mapper 接口
 </p>

 @author MLH
 @since 2022-09-18
*/
public interface IllegalregisteMapper extends BaseMapper<Illegalregiste> {
    List<Illegalregiste> allManage(String  community, String plateNumber, String operatordate);
}
