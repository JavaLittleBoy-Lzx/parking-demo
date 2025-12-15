package com.parkingmanage.service;

import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Community;
import com.parkingmanage.entity.Tickets;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 <p>
  服务类
 </p>

 @author MLH
 @since 2023-03-03
*/
public interface TicketsService extends IService<Tickets> {
    List<Tickets> getManageBuilding(Integer gateid);
    void deleteByGateId(Integer gateid);
    void insertTickets(Integer gateid,String createman,String ticketcode,String ticketname,List<Integer> arrayId);

}
