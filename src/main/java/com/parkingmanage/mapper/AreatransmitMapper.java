package com.parkingmanage.mapper;

import com.parkingmanage.entity.Areatransmit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 <p>
  Mapper 接口
 </p>

 @author MLH
 @since 2022-09-08
*/
public interface AreatransmitMapper extends BaseMapper<Areatransmit> {
    void saveAreatransmit(String openid, String sourceopenid, LocalDateTime begindate, LocalDateTime enddate );
}
