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
import java.util.*;
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
        try {
            int num;
            
            // 生成所有可能的组合
            List<String> buildingList = generateRange(community.getBuildingBegin(), community.getBuildingEnd());
            List<String> unitsList = generateRange(community.getUnitsBegin(), community.getUnitsEnd());
            List<String> floorList = generateRange(community.getFloorBegin(), community.getFloorEnd());
            List<String> roomnumberList = generateRange(community.getRoomnumberBegin(), community.getRoomnumberEnd());
            
            System.out.println("Building range: " + buildingList);
            System.out.println("Units range: " + unitsList);
            System.out.println("Floor range: " + floorList);
            System.out.println("Roomnumber range: " + roomnumberList);
            
            int totalGenerated = 0;
            int totalSaved = 0;
            int totalDuplicate = 0;
            
            // 遍历所有组合
            for (String building : buildingList) {
                for (String unit : unitsList) {
                    for (String floor : floorList) {
                        for (String roomnumber : roomnumberList) {
                            totalGenerated++;
                            
                            Community newCommunity = new Community();
                            // 复制基本信息
                            newCommunity.setProvince(community.getProvince());
                            newCommunity.setCity(community.getCity());
                            newCommunity.setDistrict(community.getDistrict());
                            newCommunity.setCommunity(community.getCommunity());
                            newCommunity.setIsAudit(community.getIsAudit());
                            
                            if ("否".equals(community.getIsAudit())) {
                                newCommunity.setAuditStartTime("暂无审核时间");
                                newCommunity.setAuditEndTime("暂无审核时间");
                            } else {
                                newCommunity.setAuditStartTime(community.getAuditStartTime());
                                newCommunity.setAuditEndTime(community.getAuditEndTime());
                            }
                            
                            // 设置生成的值
                            newCommunity.setBuilding(building);
                            newCommunity.setUnits(unit);
                            newCommunity.setFloor(floor);
                            newCommunity.setRoomnumber(roomnumber);
                            
                            // 检查是否重复并保存
                            num = communityService.duplicate(newCommunity);
                            
                            if (num == 0) {
                                // 不重复，保存数据
                                boolean saved = communityService.save(newCommunity);
                                if (saved) {
                                    totalSaved++;
                                    System.out.println("✅ 保存成功: " + building + "-" + unit + "-" + floor + "-" + roomnumber);
                                } else {
                                    System.err.println("❌ 保存失败: " + building + "-" + unit + "-" + floor + "-" + roomnumber);
                                }
                            } else {
                                totalDuplicate++;
                                System.out.println("⚠️ 数据重复，跳过: " + building + "-" + unit + "-" + floor + "-" + roomnumber + " (重复数量: " + num + ")");
                            }
                        }
                    }
                }
            }
            
            System.out.println("========================================");
            System.out.println("批量添加完成统计:");
            System.out.println("生成总数: " + totalGenerated);
            System.out.println("保存成功: " + totalSaved);
            System.out.println("重复跳过: " + totalDuplicate);
            System.out.println("========================================");
            
            // 构建返回信息
            String message;
            if (totalSaved == 0 && totalDuplicate > 0) {
                message = "批量添加完成！所有" + totalGenerated + "条记录均已存在，未添加新数据。";
            } else if (totalSaved == totalGenerated) {
                message = "批量添加成功！共添加" + totalSaved + "条新记录。";
            } else {
                message = "批量添加完成！共生成" + totalGenerated + "条记录，成功添加" + totalSaved + "条，重复跳过" + totalDuplicate + "条。";
            }
            
            // 构建详细数据返回给前端
            Map<String, Object> data = new HashMap<>();
            data.put("totalGenerated", totalGenerated);
            data.put("totalSaved", totalSaved);
            data.put("totalDuplicate", totalDuplicate);
            data.put("message", message);
            
            return ResponseEntity.ok(Result.success(data));
        } catch (Exception e) {
            System.err.println("批量添加小区失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Result.error("批量添加失败: " + e.getMessage()));
        }
    }

    /**
     * 生成字符串范围列表，支持纯数字、纯字母、混合格式
     * @param start 开始值
     * @param end 结束值
     * @return 生成的范围列表
     */
    private List<String> generateRange(String start, String end) {
        List<String> result = new ArrayList<>();
        
        if (start == null || end == null || start.trim().isEmpty() || end.trim().isEmpty()) {
            return result;
        }
        
        start = start.trim();
        end = end.trim();
        
        // 如果开始和结束相同，直接返回
        if (start.equals(end)) {
            result.add(start);
            return result;
        }
        
        // 尝试纯数字处理
        if (isNumeric(start) && isNumeric(end)) {
            return generateNumericRange(start, end);
        }
        
        // 尝试纯字母处理 (A-Z)
        if (isAlphabetic(start) && isAlphabetic(end) && start.length() == 1 && end.length() == 1) {
            return generateAlphabeticRange(start, end);
        }
        
        // 对于其他情况，如果只有一个值或者无法确定范围，返回两个端点
        result.add(start);
        if (!start.equals(end)) {
            result.add(end);
        }
        
        return result;
    }
    
    /**
     * 生成纯数字范围
     */
    private List<String> generateNumericRange(String start, String end) {
        List<String> result = new ArrayList<>();
        try {
            int startNum = Integer.parseInt(start);
            int endNum = Integer.parseInt(end);
            
            // 保证从小到大
            if (startNum > endNum) {
                int temp = startNum;
                startNum = endNum;
                endNum = temp;
            }
            
            for (int i = startNum; i <= endNum; i++) {
                result.add(String.valueOf(i));
            }
        } catch (NumberFormatException e) {
            result.add(start);
            result.add(end);
        }
        return result;
    }
    
    /**
     * 生成纯字母范围 (A-Z)
     */
    private List<String> generateAlphabeticRange(String start, String end) {
        List<String> result = new ArrayList<>();
        
        char startChar = start.toUpperCase().charAt(0);
        char endChar = end.toUpperCase().charAt(0);
        
        // 保证从小到大
        if (startChar > endChar) {
            char temp = startChar;
            startChar = endChar;
            endChar = temp;
        }
        
        for (char c = startChar; c <= endChar; c++) {
            result.add(String.valueOf(c));
        }
        
        return result;
    }
    
    /**
     * 检查字符串是否为纯数字
     */
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 检查字符串是否为纯字母(单个字符)
     */
    private boolean isAlphabetic(String str) {
        return str != null && str.length() == 1 && Character.isLetter(str.charAt(0));
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

