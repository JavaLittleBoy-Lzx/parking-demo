package com.parkingmanage.mapper;

import com.parkingmanage.entity.Tickets;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author MLH
 * @since 2023-03-03
 */
public interface TicketsMapper extends BaseMapper<Tickets> {
    List<Tickets> getManageBuilding(Integer gateid);
    void deleteByGateId(Integer gateid);
    void insertTickets(Integer gateid,String createman,String ticketcode,String ticketname,List<Integer> arrayId);
}