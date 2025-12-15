package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.Member;
import com.parkingmanage.entity.MonthTick;

import java.util.ArrayList;
import java.util.List;

/**
 <p>
  服务类
 </p>

 @author MLH
 @since 2022-07-13
*/
public interface MonthTicketService extends IService<MonthTick> {

    List<MonthTick> queryInfoOnly(String parkName, String carNo, String ticketName, String userName, Integer timeDays, String timePeriodList, String userPhone, String remark1, String remark2, String remark3,Integer isFrozen,Integer isValid);

    List<MonthTick> findOne(MonthTick monthTick);
}
