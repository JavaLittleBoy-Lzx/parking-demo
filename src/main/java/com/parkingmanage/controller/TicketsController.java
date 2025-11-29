package com.parkingmanage.controller;


import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Butler;
import com.parkingmanage.entity.Community;
import com.parkingmanage.entity.Tickets;
import com.parkingmanage.service.ButlerService;
import com.parkingmanage.service.CommunityService;
import com.parkingmanage.service.TicketsService;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author MLH
 * @since 2023-03-03
 */
@RestController
@RequestMapping("/parking/tickets")
public class TicketsController {
    @Resource
    private TicketsService ticketsService;

    @ApiOperation("添加")
    @PostMapping("/insertTickets")
    public ResponseEntity<Result> insertTickets(@RequestParam(required = false) Integer gateid,
                                                @RequestParam(required = false) String createman,
                                                @RequestParam(required = false) String ticketcode,
                                                @RequestParam(required = false) String ticketname,
                                                @RequestParam("arrayId[]") List<Integer> arrayId) {
        System.out.println(arrayId);
        ticketsService.deleteByGateId(gateid);
        ticketsService.insertTickets(gateid, createman, ticketcode, ticketname, arrayId);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("分页查询")
    @GetMapping("/getManageBuilding")
    public ResponseEntity<Result> getManageBuilding(@RequestParam(required = false) Integer gateid) {
        List<Tickets> buildingList = ticketsService.getManageBuilding(gateid);
        Result result = new Result();
        result.setData(buildingList);
        return ResponseEntity.ok(result);
    }

}

