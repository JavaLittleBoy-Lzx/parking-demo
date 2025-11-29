package com.parkingmanage.controller;


import com.parkingmanage.common.Result;
import com.parkingmanage.vo.AreaSimple;
import com.parkingmanage.service.AreatransmitService;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 <p>
  前端控制器
 </p>

 @author MLH
 @since 2022-09-08
*/
@RestController
@RequestMapping("/parking/areatransmit")
public class AreatransmitController {
    @Resource
    private AreatransmitService areatransmitService;

    @ApiOperation("添加")
    @PostMapping("/insertAreaTransmit")
    public ResponseEntity<Result> insertAreatransmit(@RequestBody AreaSimple areaSimple) {
        System.out.println(areaSimple.getEnddate());
        areatransmitService.saveAreatransmit(areaSimple);
        return ResponseEntity.ok(new Result());
    }
}

