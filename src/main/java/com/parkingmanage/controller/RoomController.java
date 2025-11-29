package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Community;
import com.parkingmanage.query.CommunityQuery;
import com.parkingmanage.service.CommunityService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author MLH
 * @since 2023-02-06
 */
@RestController
@RequestMapping("/parking/room")
public class RoomController {
    @Resource
    private CommunityService communityService;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertCommunity(@RequestBody Community community) {
        int num = communityService.duplicate(community);
        Result result = new Result();
        if (num == 0) {
            communityService.save(community);
        } else {
            result.setCode("1");
            result.setMsg("数据重复！");
        }

        return ResponseEntity.ok(result);
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Community community) {
        communityService.updateById(community);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return communityService.removeById(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/queryCommunity")
    public List<Community> queryCommunity(CommunityQuery query) {
        QueryWrapper<Community> wrapper = Wrappers.<Community>query();
//        wrapper.eq("a.province",StringUtils.isNotBlank(query.getProvince()));
//        wrapper.eq("a.city",StringUtils.isNotBlank(query.getCity()));
//        wrapper.eq("a.district",StringUtils.isNotBlank(query.getDistrict()));
        wrapper.eq("a.province", query.getProvince());
        wrapper.eq("a.city", query.getCity());
        wrapper.eq("a.district", query.getDistrict());
        wrapper.eq("a.community", query.getCommunity());
        List<Community> myquery = communityService.queryCommunity(wrapper);
        return myquery;
    }

    @ApiOperation("查询所有")
    @GetMapping("/getAllCommunity")
    public List<Community> getAllCommunity() {
        List<Community> myquery = communityService.list();
        return myquery;
    }

    @ApiOperation("查询所有")
    @GetMapping("/getDistinctCommunity")
    public List<Community> getDistinctCommunity() {
        List<Community> myquery = communityService.getDistinctCommunity();
        return myquery;
    }

    @ApiOperation("查询所有")
    @GetMapping("/province")
    public List<Community> provinceList() {
        return communityService.provinceList();
    }

    @ApiOperation("查询所有")
    @GetMapping("/city")
    public List<Community> cityList(@RequestParam(required = false) String province) {
        return communityService.cityList(province);
    }

    @ApiOperation("查询所有")
    @GetMapping("/district")
    public List<Community> districtList(@RequestParam(required = false) String province,
                                        @RequestParam(required = false) String city) {
        return communityService.districtList(province, city);
    }

    @ApiOperation("查询所有")
    @GetMapping("/community")
    public List<Community> communityList(@RequestParam(required = false) String province,
                                         @RequestParam(required = false) String city,
                                         @RequestParam(required = false) String district) {
        return communityService.communityList(province, city, district);
    }

    @ApiOperation("查询所有")
    @GetMapping("/building")
    public List<Community> buildingList(@RequestParam(required = false) String province,
                                        @RequestParam(required = false) String city,
                                        @RequestParam(required = false) String district,
                                        @RequestParam(required = false) String community) {
        return communityService.buildingList(province, city, district, community);
    }

    @ApiOperation("查询所有")
    @GetMapping("/units")
    public List<Community> unitsList(@RequestParam(required = false) String province,
                                     @RequestParam(required = false) String city,
                                     @RequestParam(required = false) String district,
                                     @RequestParam(required = false) String community,
                                     @RequestParam(required = false) String building) {
        return communityService.unitsList(province, city, district, community, building);
    }

    @ApiOperation("查询所有")
    @GetMapping("/floor")
    public List<Community> floorList(@RequestParam(required = false) String province,
                                     @RequestParam(required = false) String city,
                                     @RequestParam(required = false) String district,
                                     @RequestParam(required = false) String community,
                                     @RequestParam(required = false) String building,
                                     @RequestParam(required = false) String units) {
        return communityService.floorList(province, city, district, community, building, units);
    }

    @ApiOperation("分页查询")
    @GetMapping("/mypage")
    public IPage<Community> managePage(
            @RequestParam(required = false) String openid,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String community,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Community> communityList = communityService.queryManage(openid, province, city, district, community);

        //按照设备名和申请日期排序
        List<Community> asServices = communityList.stream().sorted(Comparator.comparing(Community::getBuilding).thenComparing(Community::getUnits)
                .thenComparing(Community::getFloor)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }

    @ApiOperation("分页查询")
    @GetMapping("/allpage")
    public IPage<Community> allPage(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String community,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Community> communityList = communityService.allManage(province, city, district, community);

        //按照设备名和申请日期排序
        List<Community> asServices = communityList.stream().sorted(Comparator.comparing(Community::getProvince).
                thenComparing(Community::getCity).thenComparing(Community::getDistrict).thenComparing(Community::getCommunity).
                thenComparing(Community::getBuilding).thenComparing(Community::getUnits)
                .thenComparing(Community::getFloor)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }

    @ApiOperation("分页查询")
    @GetMapping("/distinctpage")
    public IPage<Community> distinctPage(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String community,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Community> communityList = communityService.distinctPage(province, city, district, community);

        //按照设备名和申请日期排序
        List<Community> asServices = communityList.stream().sorted(Comparator.comparing(Community::getProvince).
                thenComparing(Community::getCity).thenComparing(Community::getDistrict).thenComparing(Community::getCommunity)
        ).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }

}

