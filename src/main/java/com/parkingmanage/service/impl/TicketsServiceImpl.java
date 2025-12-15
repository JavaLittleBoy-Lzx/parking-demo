package com.parkingmanage.service.impl;

import com.parkingmanage.entity.Tickets;
import com.parkingmanage.mapper.TicketsMapper;
import com.parkingmanage.service.TicketsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 <p>
  服务实现类
 </p>

 @author MLH
 @since 2023-03-03
*/
@Service
public class TicketsServiceImpl extends ServiceImpl<TicketsMapper, Tickets> implements TicketsService {
    @Override
    public List<Tickets> getManageBuilding(Integer gateid){
        return baseMapper.getManageBuilding(gateid);
    }
    @Override
    public void deleteByGateId(Integer gateid){
        baseMapper.deleteByGateId(gateid);
    }
    @Override
    public void insertTickets(Integer gateid,String createman,String ticketcode,String ticketname,List<Integer> arrayId){
        baseMapper.insertTickets(gateid,createman,ticketcode,ticketname,arrayId);
    }
}
