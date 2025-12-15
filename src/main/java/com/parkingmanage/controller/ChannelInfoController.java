package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.ChannelInfo;
import com.parkingmanage.entity.YardInfo;
import com.parkingmanage.service.ChannelInfoService;
import com.parkingmanage.service.YardInfoService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lzx
 * @since 2023-11-08
 */
@RestController
@RequestMapping("/parking/channelInfo")
public class ChannelInfoController {
    @Autowired
    private ChannelInfoService channelInfoService;

    @ApiOperation(value = "获取停车场列表", notes = "停车场列表")
    @GetMapping("/channelName")
    public List<ChannelInfo> getChannelNameList(String parkName) {
        return channelInfoService.getChannelNameList(parkName);
    }
}

