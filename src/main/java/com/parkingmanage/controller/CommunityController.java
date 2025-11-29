package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.*;
import com.parkingmanage.query.CommunityQuery;
import com.parkingmanage.query.CommunityVisitorQuery;
import com.parkingmanage.service.CommunityService;
import com.parkingmanage.service.DepartmentService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author yuli
 * @since 2022-07-11
 */
@RestController
@RequestMapping("/parking/community")
@CrossOrigin(origins = "*")
public class CommunityController {
    @Resource
    private CommunityService communityService;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertCommunity(@RequestBody Community community) {
        Result result = new Result();
        if (community.getIsAudit().equals('否')) {
            // 设置审核时间为'暂无审核时间'
            community.setAuditStartTime("暂无审核时间");
            community.setAuditStartTime("暂无审核时间");
            int num = communityService.duplicate(community);
            if (num == 0) {
                communityService.save(community);
            } else {
                result.setCode("1");
                result.setMsg("数据重复，增加失败！");
            }
        } else {
            int num = communityService.duplicate(community);
            if (num == 0) {
                communityService.save(community);
            } else {
                result.setCode("1");
                result.setMsg("数据重复，增加失败！");
            }
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("批量添加")
    @PostMapping("/insertBatCommunity")
    public ResponseEntity<Result> insertBatCommunity(@RequestBody Community community) {
        int num;
        int buildbingBegin = Integer.parseInt(community.getBuildingBegin());
        int buildbingEnd = Integer.parseInt(community.getBuildingEnd());
        int unitsBegin = Integer.parseInt(community.getUnitsBegin());
        int unitsEnd = Integer.parseInt(community.getUnitsEnd());
        int floorBegin = Integer.parseInt(community.getFloorBegin());
        int floorEnd = Integer.parseInt(community.getFloorEnd());
        int roomnumberBegin = Integer.parseInt(community.getRoomnumberBegin());
        int roomnumberEnd = Integer.parseInt(community.getRoomnumberEnd());
        System.out.println("roomnumberBegin = " + roomnumberBegin);
        System.out.println("roomnumberEnd = " + roomnumberEnd);
        for (int i = buildbingBegin; i <= buildbingEnd; i++) {
            for (int j = unitsBegin; j <= unitsEnd; j++) {
                for (int k = floorBegin; k <= floorEnd; k++) {
                    for (int i1 = roomnumberBegin; i1 <= roomnumberEnd; i1++) {
                        if (community.getIsAudit().equals('否')) {
                            community.setAuditStartTime("暂无审核时间");
                            community.setAuditStartTime("暂无审核时间");
                            community.setBuilding(Integer.toString(i));
                            community.setUnits(Integer.toString(j));
                            community.setFloor(Integer.toString(k));
                            community.setRoomnumber(Integer.toString(i1));
                            num = communityService.duplicate(community);
                            if (num == 0) {
                                communityService.save(community);
                            }
                        } else {
                            community.setBuilding(Integer.toString(i));
                            community.setUnits(Integer.toString(j));
                            community.setFloor(Integer.toString(k));
                            community.setRoomnumber(Integer.toString(i1));
                            num = communityService.duplicate(community);
                            if (num == 0) {
                                communityService.save(community);
                            }
                        }
                    }
                }
            }
        }
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Community community) {
        int num = communityService.duplicate(community);
        Result result = new Result();
        if (num == 0) {
            communityService.updateById(community);
        } else {
            result.setCode("1");
            result.setMsg("数据重复，修改失败！");
        }
        return ResponseEntity.ok(result);
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
        wrapper.eq("a.province", query.getProvince());
        wrapper.eq("a.city", query.getCity());
        wrapper.eq("a.district", query.getDistrict());
        wrapper.eq("a.community", query.getCommunity());
        List<Community> myquery = communityService.queryCommunity(wrapper);
        System.out.println("正在查询省市区");
        return myquery;
    }

    @ApiOperation("查询所有")
    @GetMapping("/queryVisitorCommunity")
    public List<Community> queryVisitorCommunity(CommunityVisitorQuery query) {
        QueryWrapper<Community> wrapper = Wrappers.<Community>query();
        wrapper.eq("a.province", query.getProvince());
        wrapper.eq("a.city", query.getCity());
        wrapper.eq("a.district", query.getDistrict());
        wrapper.eq("a.community", query.getCommunity());
        wrapper.eq("a.building", query.getBuilding());
        wrapper.eq("a.units", query.getUnits());
        wrapper.eq("a.floor", query.getFloor());
        wrapper.eq("a.roomnumber", query.getRoomnumber());
        List<Community> myquery = communityService.queryCommunity(wrapper);
        System.out.println("正在查询省市区");
        return myquery;
    }

    @ApiOperation("查询指定小区Community")
    @GetMapping("/butlerCommunityAuditTime")
    public Community butlerCommunityAuditTime(@RequestParam(required = false) String butlerCommunity) {
        Community butlerCommunityAuditTime = communityService.butlerCommunityAuditTime(butlerCommunity);
        return butlerCommunityAuditTime;
    }

    @ApiOperation("根据小区名称和地址查询省市区信息")
    @GetMapping("/findProvinceByCommunityName")
    public Community findProvinceByCommunityName(
            @RequestParam(required = false) String community,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String units,
            @RequestParam(required = false) String floor,
            @RequestParam(required = false) String room) {

        System.out.println("🔍 [省市区查询] 接收参数: community=" + community +
                          ", building=" + building + ", units=" + units +
                          ", floor=" + floor + ", room=" + room);

        Community communityInfo = communityService.findProvinceByCommunityName(community, building, floor, units, room);

        if (communityInfo != null) {
            System.out.println("✅ [省市区查询] 查询成功: " + communityInfo.getProvince() +
                              communityInfo.getCity() + communityInfo.getDistrict());
        } else {
            System.out.println("⚠️ [省市区查询] 未找到匹配的小区信息");
        }

        return communityInfo;
    }

    @ApiOperation("查询所有添加的小区名称")
    @GetMapping("/communityName")
    public List<Community> communityName() {
        List<Community> communityNames = communityService.getCommunityName();
        return communityNames;
    }

    //@ApiOperation("查询所有")
//@GetMapping("/queryCommunity")
//public List<Community> queryCommunity(){
////    QueryWrapper<Community> wrapper= Wrappers.<Community>query();
////    wrapper.eq("a.province",query.getProvince());
////    wrapper.eq("a.city",query.getCity());
////    wrapper.eq("a.district",query.getDistrict());
////    wrapper.eq("a.community",query.getCommunity());
//    List<Community> myquery = communityService.list();
//    System.out.println("正在查询省市区");
//    return myquery;
//}
    @ApiOperation("查询所有Community数据，支持按小区名称筛选")
    @GetMapping("/getAllCommunity")
    public Result<List<Community>> getAllCommunity(@RequestParam(required = false) String community) {
        System.out.println("🔍 收到getAllCommunity请求，小区名称: " + community);
        
        try {
            List<Community> myquery;
            if (StringUtils.isNotBlank(community)) {
                // 根据小区名称筛选
                QueryWrapper<Community> wrapper = Wrappers.<Community>query();
                wrapper.eq("community", community);
                myquery = communityService.list(wrapper);
                System.out.println("📊 根据小区名称筛选结果数量: " + (myquery != null ? myquery.size() : 0));
            } else {
                // 如果没有传递小区名称，返回所有数据
                myquery = communityService.list();
                System.out.println("📊 返回所有数据数量: " + (myquery != null ? myquery.size() : 0));
            }
            
            if (myquery != null && !myquery.isEmpty()) {
                System.out.println("📋 第一条数据示例: building=" + myquery.get(0).getBuilding() + 
                                 ", units=" + myquery.get(0).getUnits() + 
                                 ", floor=" + myquery.get(0).getFloor() + 
                                 ", room=" + myquery.get(0).getRoomnumber());
            }
            
            Result<List<Community>> result = new Result<>();
            result.setCode("0");
            result.setMsg("成功");
            result.setData(myquery);
            return result;
        } catch (Exception e) {
            System.err.println("❌ 查询地址数据失败: " + e.getMessage());
            e.printStackTrace();
            Result<List<Community>> result = new Result<>();
            result.setCode("-1");
            result.setMsg("查询失败: " + e.getMessage());
            return result;
        }
    }

    @ApiOperation("查询仅小区")
    @GetMapping("/getOnlyCommunity")
    public List<Community> getOnlyCommunity() {
        List<Community> myquery = communityService.getOnlyCommunity();
        return myquery;
    }

    @ApiOperation("查询仅楼栋")
    @GetMapping("/getOnlyBuilding")
    public List<Community> getOnlyBuilding(@RequestParam(required = false) String province,
                                           @RequestParam(required = false) String city,
                                           @RequestParam(required = false) String district,
                                           @RequestParam(required = false) String community
    ) {
        System.out.println("🏢 收到楼栋查询请求:");
        System.out.println("province: " + province);
        System.out.println("city: " + city);
        System.out.println("district: " + district);
        System.out.println("community: " + community);
        
        List<Community> myquery = communityService.getOnlyBuilding(province, city, district, community);
        System.out.println("📊 查询结果数量: " + (myquery != null ? myquery.size() : 0));
        
        if (myquery != null && !myquery.isEmpty()) {
            System.out.println("📋 楼栋列表:");
            for (Community c : myquery) {
                System.out.println("  - " + c.getBuilding());
            }
        } else {
            System.out.println("⚠️ 没有找到楼栋数据，可能原因:");
            System.out.println("  1. 数据库中没有该小区的数据");
            System.out.println("  2. 参数不匹配");
        }
        
        return myquery;
    }

    @ApiOperation("查询仅单元")
    @GetMapping("/getOnlyUnits")
    public List<Community> getOnlyUnits(@RequestParam(required = false) String province,
                                        @RequestParam(required = false) String city,
                                        @RequestParam(required = false) String district,
                                        @RequestParam(required = false) String community,
                                        @RequestParam(required = false) String building
    ) {
        List<Community> myquery = communityService.getOnlyUnits(province, city, district, community, building);
        return myquery;
    }

    @ApiOperation("查询仅楼层")
    @GetMapping("/getOnlyFloor")
    public List<Community> getOnlyFloor(@RequestParam(required = false) String province,
                                        @RequestParam(required = false) String city,
                                        @RequestParam(required = false) String district,
                                        @RequestParam(required = false) String community,
                                        @RequestParam(required = false) String building,
                                        @RequestParam(required = false) String units
    ) {
        List<Community> myquery = communityService.getOnlyFloor(province, city, district, community, building, units);
        return myquery;
    }

    @ApiOperation("查询仅房号")
    @GetMapping("/getOnlyRoomNumber")
    public List<Community> getOnlyRoomNumber(@RequestParam(required = false) String province,
                                             @RequestParam(required = false) String city,
                                             @RequestParam(required = false) String district,
                                             @RequestParam(required = false) String community,
                                             @RequestParam(required = false) String building,
                                             @RequestParam(required = false) String units,
                                             @RequestParam(required = false) String floor
    ) {
        List<Community> myquery = communityService.getOnlyRoomNumber(province, city, district, community, building, units, floor);
        return myquery;
    }

    @ApiOperation("查询房号列表（前端管理系统专用）")
    @GetMapping("/roomnumber")
    public List<Community> getRoomNumberList(@RequestParam(required = false) String province,
                                             @RequestParam(required = false) String city,
                                             @RequestParam(required = false) String district,
                                             @RequestParam(required = false) String community,
                                             @RequestParam(required = false) String building,
                                             @RequestParam(required = false) String units,
                                             @RequestParam(required = false) String floor
    ) {
        System.out.println("🏠 [房号查询] 接收参数:");
        System.out.println("  province: " + province);
        System.out.println("  city: " + city);
        System.out.println("  district: " + district);
        System.out.println("  community: " + community);
        System.out.println("  building: " + building);
        System.out.println("  units: " + units);
        System.out.println("  floor: " + floor);

        List<Community> myquery = communityService.getOnlyRoomNumber(province, city, district, community, building, units, floor);

        System.out.println("🏠 [房号查询] 查询结果数量: " + (myquery != null ? myquery.size() : 0));
        if (myquery != null && !myquery.isEmpty()) {
            for (Community room : myquery) {
                System.out.println("  - 房号: " + room.getRoomnumber());
            }
        }

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

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    @ApiOperation("分页查询")
    @GetMapping("/duplicatePage")
    public IPage<Community> duplicatePage(
            @RequestParam(required = false) String community,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Community> communityList = communityService.duplicatePage(community);
        //按照设备名和申请日期排序
        List<Community> asServices = communityList.stream()
                .filter(distinctByKey(Community::getCommunity))
                .collect(Collectors.toList());
        System.out.println("asServices = " + asServices);
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

    @ApiOperation("分页查询")
    @GetMapping("/getBuilding")
    public ResponseEntity<Result> getBuilding(@RequestParam(required = false) String province,
                                              @RequestParam(required = false) String city,
                                              @RequestParam(required = false) String district,
                                              @RequestParam(required = false) String community
    ) {

        List<Community> buildingList = communityService.getBuilding(province, city, district, community);
        for (int i = 0; i < buildingList.size(); i++) {
            buildingList.get(i).setBuilding(buildingList.get(i).getBuilding() + "栋");
        }

        Result result = new Result();
        result.setData(buildingList);
        return ResponseEntity.ok(result);
    }
}

