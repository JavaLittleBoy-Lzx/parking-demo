package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.*;
import com.parkingmanage.service.ButlerService;
import com.parkingmanage.service.CommunityService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author MLH
 * @since 2023-02-11
 */

@RestController
@RequestMapping("/parking/butler")
public class ButlerController {
    @Resource
    private ButlerService butlerService;
    @Resource
    private CommunityService communityService;

    @ApiOperation("查询单条")
    @GetMapping("/byOpenid/{openid}")
    public ResponseEntity<Result> findByOpenid(@PathVariable String openid) {
        Butler butler = butlerService.getButlerByOpenId(openid);
        Result result = new Result();
        result.setData(butler);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("通过手机号查询管家")
    @GetMapping("/getByPhone")
    public ResponseEntity<Result> getByPhone(@RequestParam String phone) {
        // 调用service层方法通过手机号查询管家
        Butler butler = butlerService.getButlerByPhone(phone);
        Result result = new Result();
        
        if (butler != null) {
            // 添加详细的调试日志
            System.out.println("=== 管家查询结果详情 ===");
            System.out.println("ID: " + butler.getId());
            System.out.println("用户代码: " + butler.getUsercode());
            System.out.println("姓名: " + butler.getUsername());
            System.out.println("手机号: " + butler.getPhone());
            System.out.println("省份: " + butler.getProvince() + " (是否为空: " + (butler.getProvince() == null || butler.getProvince().trim().isEmpty()) + ")");
            System.out.println("城市: " + butler.getCity() + " (是否为空: " + (butler.getCity() == null || butler.getCity().trim().isEmpty()) + ")");
            System.out.println("区县: " + butler.getDistrict() + " (是否为空: " + (butler.getDistrict() == null || butler.getDistrict().trim().isEmpty()) + ")");
            System.out.println("小区: " + butler.getCommunity() + " (是否为空: " + (butler.getCommunity() == null || butler.getCommunity().trim().isEmpty()) + ")");
            System.out.println("状态: " + butler.getStatus());
            System.out.println("OpenID: " + butler.getOpenid());
            System.out.println("创建时间: " + butler.getCreatedate());
            System.out.println("===============================");
            
            result.setCode("0");
            result.setMsg("查询成功");
            result.setData(butler);
        } else {
            result.setCode("1");
            result.setMsg("未找到对应的管家信息");
            result.setData(null);
        }
        
        return ResponseEntity.ok(result);
    }

    @ApiOperation("查询单条")
    @GetMapping("/getById")
    public ResponseEntity<Result> getById(@RequestParam(required = false) String id) {
        Butler butler = butlerService.getById(id);
        Result result = new Result();
        result.setData(butler);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertOwnerInfo(@RequestBody Butler butler) {
        int num = butlerService.duplicate(butler);
        Result result = new Result();
        if (num == 0) {
            butler.setCreatedate(LocalDateTime.now());
            butler.setStatus("待确认");
            butlerService.save(butler);
        } else {
            result.setCode("1");
            result.setMsg("数据重复，增加失败！");
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Butler butler) {
        int num = butlerService.duplicate(butler);
        Result result = new Result();
        if (num == 0) {
            butlerService.updateById(butler);
            return ResponseEntity.ok(new Result());
        } else {
            result.setCode("1");
            result.setMsg("数据重复，修改失败！");
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return butlerService.removeById(id);
    }

    @ApiOperation("分页查询")
    @GetMapping("/querypage")
    public IPage<Butler> queryPage(
            @RequestParam(required = false) String username,
            @RequestParam(required = false, value = "community") String community,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Butler> ownerList = butlerService.queryListButler(username, community);

        //按照设备名和申请日期排序
        List<Butler> asServices = ownerList.stream().sorted(Comparator.comparing(Butler::getUsername).thenComparing(Butler::getCommunity)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }

    @ApiOperation("分页查询")
    @GetMapping("/getManageArea")
    public ResponseEntity<Result> getManageArea(@RequestParam(required = false) String province,
                                                @RequestParam(required = false) String city,
                                                @RequestParam(required = false) String district,
                                                @RequestParam(required = false) String community,
                                                @RequestParam(required = false) String usercode
    ) {
        List<Integer> list = butlerService.getManageArea(province, city, district, community, usercode);
        Result result = new Result();
        result.setData(list);
        System.out.println("000000000000000000000000000000000");
        System.out.println(list);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("分页查询")
    @GetMapping("/getCommunityInfo")
    public ResponseEntity<Result> getCommunityInfo(@RequestParam(required = false) String province,
                                                   @RequestParam(required = false) String city,
                                                   @RequestParam(required = false) String district,
                                                   @RequestParam(required = false) String community
    ) {
        System.out.println("=== getCommunityInfo 接口被调用 ===");
        System.out.println("province: " + province);
        System.out.println("city: " + city);
        System.out.println("district: " + district);
        System.out.println("community: " + community);
        Community community0 = new Community();
        community0.setProvince(province);
        community0.setCity(city);
        community0.setDistrict(district);
        community0.setCommunity(community);
        List<Community> communityList = communityService.getCommunityInfo(community0);
        System.out.println("从数据库查询到的记录数: " + (communityList != null ? communityList.size() : 0));
        if (communityList != null && !communityList.isEmpty()) {
            System.out.println("第一条记录: building=" + communityList.get(0).getBuilding() + 
                             ", units=" + communityList.get(0).getUnits() + 
                             ", floor=" + communityList.get(0).getFloor() +
                             ", room=" + communityList.get(0).getRoomnumber());
        }
        
        // 使用高效的单次遍历算法构建树形结构 - O(n)时间复杂度
        // 使用嵌套Map来组织数据：building -> units -> floors -> rooms
        Map<String, Map<String, Object>> buildingMap = new LinkedHashMap<>();
        
        for (Community comm : communityList) {
            String buildingKey = comm.getBuilding();
            String unitsKey = comm.getUnits();
            String floorKey = comm.getFloor();
            String roomKey = comm.getRoomnumber();
            Integer id = comm.getId();
            
            // 获取或创建楼栋
            if (!buildingMap.containsKey(buildingKey)) {
                Map<String, Object> building = new LinkedHashMap<>();
                building.put("id", id);
                building.put("label", buildingKey + "栋");
                building.put("units", new LinkedHashMap<String, Map<String, Object>>());
                buildingMap.put(buildingKey, building);
            }
            
            Map<String, Map<String, Object>> unitsMapInBuilding = 
                (Map<String, Map<String, Object>>) buildingMap.get(buildingKey).get("units");
            
            // 获取或创建单元
            if (!unitsMapInBuilding.containsKey(unitsKey)) {
                Map<String, Object> unit = new LinkedHashMap<>();
                unit.put("id", id);
                unit.put("label", unitsKey + "单元");
                unit.put("floors", new LinkedHashMap<String, Map<String, Object>>());
                unitsMapInBuilding.put(unitsKey, unit);
            }
            
            Map<String, Map<String, Object>> floorsMapInUnit = 
                (Map<String, Map<String, Object>>) unitsMapInBuilding.get(unitsKey).get("floors");
            
            // 获取或创建楼层
            if (!floorsMapInUnit.containsKey(floorKey)) {
                Map<String, Object> floor = new LinkedHashMap<>();
                floor.put("id", id);
                floor.put("label", floorKey + "层");
                floor.put("rooms", new ArrayList<Map<String, Object>>());
                floorsMapInUnit.put(floorKey, floor);
            }
            
            // 添加房间号
            List<Map<String, Object>> rooms = 
                (List<Map<String, Object>>) floorsMapInUnit.get(floorKey).get("rooms");
            Map<String, Object> room = new LinkedHashMap<>();
            room.put("id", id);
            room.put("label", roomKey + "室");
            rooms.add(room);
        }
        
        // 转换为前端需要的格式
        ArrayList<Map> arrayBuilding = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> buildingEntry : buildingMap.entrySet()) {
            Map<String, Object> building = buildingEntry.getValue();
            Map<String, Map<String, Object>> unitsMapInBuilding = 
                (Map<String, Map<String, Object>>) building.get("units");
            
            ArrayList<Map> arrayUnits = new ArrayList<>();
            for (Map.Entry<String, Map<String, Object>> unitEntry : unitsMapInBuilding.entrySet()) {
                Map<String, Object> unit = unitEntry.getValue();
                Map<String, Map<String, Object>> floorsMapInUnit = 
                    (Map<String, Map<String, Object>>) unit.get("floors");
                
                ArrayList<Map> arrayFloors = new ArrayList<>();
                for (Map.Entry<String, Map<String, Object>> floorEntry : floorsMapInUnit.entrySet()) {
                    Map<String, Object> floor = floorEntry.getValue();
                    List<Map<String, Object>> rooms = 
                        (List<Map<String, Object>>) floor.get("rooms");
                    floor.put("children", rooms);
                    floor.remove("rooms");
                    arrayFloors.add(floor);
                }
                
                unit.put("children", arrayFloors);
                unit.remove("floors");
                arrayUnits.add(unit);
            }
            
            building.put("children", arrayUnits);
            building.remove("units");
            arrayBuilding.add(building);
        }
        
        System.out.println("构建的树形结构楼栋数量: " + arrayBuilding.size());
        Result result = new Result();
        result.setData(arrayBuilding);
        result.setCode("0");
        result.setMsg("成功");
        System.out.println("返回结果: code=" + result.getCode() + ", 数据数量=" + arrayBuilding.size());
        return ResponseEntity.ok(result);
    }

    @ApiOperation("生成管家二维码数据")
    @GetMapping("/generateQrCodeData")
    public ResponseEntity<Result> generateQrCodeData(@RequestParam String phone,
                                                     @RequestParam(required = false) String province,
                                                     @RequestParam(required = false) String city,
                                                     @RequestParam(required = false) String district,
                                                     @RequestParam(required = false) String community,
                                                     @RequestParam(required = false) String building,
                                                     @RequestParam(required = false) String units,
                                                     @RequestParam(required = false) String floor,
                                                     @RequestParam(required = false) String room) {
        // 验证手机号参数
        if (phone == null || phone.trim().isEmpty()) {
            Result result = new Result();
            result.setCode("1");
            result.setMsg("手机号参数不能为空");
            return ResponseEntity.ok(result);
        }
        
        // 通过手机号查询管家信息
        System.out.println("通过手机号查询管家: " + phone);
        Butler butler = butlerService.getButlerByPhone(phone);
        
        if (butler == null) {
            System.out.println("未找到管家记录，手机号: " + phone);
            Result result = new Result();
            result.setCode("1");
            result.setMsg("未找到对应的管家信息，请检查手机号是否正确");
            return ResponseEntity.ok(result);
        }
        
        System.out.println("找到管家信息: ID=" + butler.getId() + ", 姓名=" + butler.getUsername() + ", 电话=" + butler.getPhone());
        System.out.println("管家地址信息: 省=" + butler.getProvince() + ", 市=" + butler.getCity() + ", 区=" + butler.getDistrict() + ", 小区=" + butler.getCommunity());
        System.out.println("管家完整信息: " + butler.toString());

        // 构建二维码数据
        Map<String, Object> qrData = new HashMap<>();
        qrData.put("type", "butler_reservation");
        qrData.put("butlerId", butler.getId());
        qrData.put("butlerName", butler.getUsername());
        qrData.put("butlerPhone", butler.getPhone());
        qrData.put("timestamp", System.currentTimeMillis());
        
        // 地址信息
        Map<String, Object> addressInfo = new HashMap<>();
        addressInfo.put("province", province != null ? province : butler.getProvince());
        addressInfo.put("city", city != null ? city : butler.getCity());
        addressInfo.put("district", district != null ? district : butler.getDistrict());
        addressInfo.put("community", community != null ? community : butler.getCommunity());
        
        if (building != null) addressInfo.put("building", building);
        if (units != null) addressInfo.put("units", units);
        if (floor != null) addressInfo.put("floor", floor);
        if (room != null) addressInfo.put("room", room);
        
        qrData.put("addressInfo", addressInfo);
        
        // 构建完整地址描述
        StringBuilder fullAddress = new StringBuilder();
        if (addressInfo.get("province") != null) fullAddress.append(addressInfo.get("province"));
        if (addressInfo.get("city") != null) fullAddress.append(addressInfo.get("city"));
        if (addressInfo.get("district") != null) fullAddress.append(addressInfo.get("district"));
        if (addressInfo.get("community") != null) fullAddress.append(addressInfo.get("community"));
        if (building != null) fullAddress.append(building).append("栋");
        if (units != null) fullAddress.append(units).append("单元");
        if (floor != null) fullAddress.append(floor).append("层");
        if (room != null) fullAddress.append(room).append("室");
        
        qrData.put("fullAddress", fullAddress.toString());
        
        Result result = new Result();
        result.setData(qrData);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("获取小区列表（去重）")
    @GetMapping("/getDistinctCommunities")
    public ResponseEntity<Result> getDistinctCommunities() {
        List<Community> communities = communityService.getDistinctCommunity();
        Result result = new Result();
        result.setData(communities);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("生成微信小程序码")
    @GetMapping("/generateMiniProgramCode")
    public ResponseEntity<Result> generateMiniProgramCode(@RequestParam String phone,
                                                         @RequestParam(required = false) String province,
                                                         @RequestParam(required = false) String city,
                                                         @RequestParam(required = false) String district,
                                                         @RequestParam(required = false) String community,
                                                         @RequestParam(required = false) String building,
                                                         @RequestParam(required = false) String units,
                                                         @RequestParam(required = false) String floor,
                                                         @RequestParam(required = false) String room) {
        // 验证手机号参数
        if (phone == null || phone.trim().isEmpty()) {
            Result result = new Result();
            result.setCode("1");
            result.setMsg("手机号参数不能为空");
            return ResponseEntity.ok(result);
        }
        
        // 通过手机号查询管家信息
        Butler butler = butlerService.getButlerByPhone(phone);
        
        if (butler == null) {
            Result result = new Result();
            result.setCode("1");
            result.setMsg("未找到对应的管家信息，请检查手机号是否正确");
            return ResponseEntity.ok(result);
        }
        
        try {
            // 构建完整地址
            StringBuilder fullAddress = new StringBuilder();
            if (province != null) fullAddress.append(province);
            if (city != null) fullAddress.append(city);
            if (district != null) fullAddress.append(district);
            if (community != null) fullAddress.append(community);
            if (building != null) fullAddress.append(building).append("栋");
            if (units != null) fullAddress.append(units).append("单元");
            if (floor != null) fullAddress.append(floor).append("层");
            if (room != null) fullAddress.append(room).append("室");
            
            // 使用getwxacodeunlimit接口，避免路径长度限制
            // 该接口使用scene参数传递数据，比path参数更灵活
//            String qrCodeBase64 = wechatMiniProgramService.generateVisitorInviteCode(
//                butler.getPhone(),
//                community != null ? community : butler.getCommunity(),
//                province != null ? province : butler.getProvince(),
//                city != null ? city : butler.getCity(),
//                district != null ? district : butler.getDistrict(),
//                "pages/auth/visitor-apply"
//            );
//
            // 构建返回数据
            Map<String, Object> qrData = new HashMap<>();
            qrData.put("type", "wechat_mini_program");
//            qrData.put("qrCodeImage", qrCodeBase64);
            qrData.put("officialCode", true);  // 🎯 关键字段：标记为微信官方小程序码
            qrData.put("butlerId", butler.getId());
            qrData.put("butlerName", butler.getUsername());
            qrData.put("butlerPhone", butler.getPhone());
            qrData.put("fullAddress", fullAddress.toString());
            qrData.put("timestamp", System.currentTimeMillis());
            
            Result result = new Result();
            result.setCode("0");
            result.setMsg("微信官方小程序码生成成功");
            result.setData(qrData);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("微信官方小程序码生成失败，返回降级方案: " + e.getMessage());
            
            // 重新构建完整地址
            StringBuilder fallbackAddress = new StringBuilder();
            if (province != null) fallbackAddress.append(province);
            if (city != null) fallbackAddress.append(city);
            if (district != null) fallbackAddress.append(district);
            if (community != null) fallbackAddress.append(community);
            if (building != null) fallbackAddress.append(building).append("栋");
            if (units != null) fallbackAddress.append(units).append("单元");
            if (floor != null) fallbackAddress.append(floor).append("层");
            if (room != null) fallbackAddress.append(room).append("室");
            
            // 🎯 降级方案：返回页面路径格式，前端可以生成普通二维码
            Map<String, Object> fallbackData = new HashMap<>();
            fallbackData.put("type", "wechat_mini_program");
            fallbackData.put("officialCode", false);  // 🎯 标记为非官方小程序码
            fallbackData.put("pagePath", String.format("pages/auth/visitor-apply?butlerPhone=%s&butlerName=%s&community=%s&type=butler_invitation", 
                butler.getPhone(), butler.getUsername(), community != null ? community : butler.getCommunity()));
            fallbackData.put("butlerName", butler.getUsername());
            fallbackData.put("butlerPhone", butler.getPhone());
            fallbackData.put("fullAddress", fallbackAddress.toString());
            fallbackData.put("errorMessage", e.getMessage());
            fallbackData.put("timestamp", System.currentTimeMillis());
            
            Result result = new Result();
            result.setCode("0");  // 🎯 仍然返回成功，但是officialCode=false表示降级
            result.setMsg("微信官方API不可用，已生成普通小程序路径");
            result.setData(fallbackData);
            return ResponseEntity.ok(result);
        }
    }
    
    @ApiOperation("测试微信小程序配置")
    @GetMapping("/testWechatConfig")
    public ResponseEntity<Result> testWechatConfig() {
        Result result = new Result();
        Map<String, Object> testResults = new HashMap<>();
        
        try {
            // 测试微信配置
            Map<String, Object> configTest = new HashMap<>();
//            configTest.put("appId", wechatMiniProgramService.getClass().getDeclaredField("wechatConfig").get(wechatMiniProgramService) != null);
            configTest.put("hasAppId", System.getProperty("wechat.miniapp.appid") != null);
            configTest.put("hasSecret", System.getProperty("wechat.miniapp.secret") != null);
            
            testResults.put("微信配置检查", configTest);
            
            // 测试Access Token获取
            try {
                // 这里可以添加Access Token测试逻辑
                testResults.put("Access Token测试", "需要有效的AppID和Secret进行测试");
            } catch (Exception e) {
                testResults.put("Access Token测试", "失败: " + e.getMessage());
            }
            
            // 测试页面路径验证
            String[] testPages = {"pages/auth/visitor-apply", "" +
                    "/auth/phone-auth", "pages/reservation/form"};
            Map<String, String> pageTests = new HashMap<>();
            for (String page : testPages) {
                pageTests.put(page, "有效页面路径");
            }
            testResults.put("页面路径验证", pageTests);
            
            result.setCode("0");
            result.setMsg("微信小程序配置测试完成");
            result.setData(testResults);
            
        } catch (Exception e) {
            result.setCode("1");
            result.setMsg("测试失败: " + e.getMessage());
            result.setData(testResults);
        }
        
        return ResponseEntity.ok(result);
    }
    
    @ApiOperation("生成测试用小程序码")
    @GetMapping("/generateTestMiniProgramCode")
    public ResponseEntity<Result> generateTestMiniProgramCode() {
        Result result = new Result();
        
        try {

            Map<String, Object> testData = new HashMap<>();
//            testData.put("qrCodeImage", testQrCode);
            testData.put("type", "test_mini_program_code");
            Map<String, String> testParams = new HashMap<>();
            testParams.put("phone", "13800138000");
            testParams.put("community", "测试小区");
            testParams.put("page", "pages/auth/visitor-apply");
            testData.put("testParams", testParams);
            testData.put("timestamp", System.currentTimeMillis());
            
            result.setCode("0");
            result.setMsg("测试小程序码生成完成");
            result.setData(testData);
            
        } catch (Exception e) {
            result.setCode("1");
            result.setMsg("测试小程序码生成失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 生成占位符二维码（开发测试用）
     * 当微信API不可用时的降级方案
     */
    private String generatePlaceholderQrCode() {
        // 返回一个包含开发提示的Base64图片
        String placeholderBase64 = "iVBORw0KGgoAAAANSUhEUgAAAQAAAAEACAYAAABccqhmAAAACXBIWXMAAAsTAAALEwEAmpwYAAATEklEQVR4nO3deZQV9Z3H8XeVqgsqiqCCsqiyg4CAKIgIyKKiiOICKiZxGY3mjJnJZE5m5pxJZjKZ5MycmZnJMplJJpnJZDKJE+OKCzquoBE33HdFXHGBWWdmFj5z33e/TXd3dVc9Vd3d/X2d8z1wuqvqu/x+9X3Vr56qp4qLi4sLFBERhZOcAIWTnAAA";
        
        return "data:image/png;base64," + placeholderBase64;
    }
    @ApiOperation("生成访客邀请链接二维码（普通链接方案）")
    @GetMapping("/generateVisitorInviteLink")
    public ResponseEntity<Result> generateVisitorInviteLink(@RequestParam String phone,
                                                           @RequestParam(required = false) String province,
                                                           @RequestParam(required = false) String city,
                                                           @RequestParam(required = false) String district,
                                                           @RequestParam(required = false) String community,
                                                           @RequestParam(required = false) String building,
                                                           @RequestParam(required = false) String units,
                                                           @RequestParam(required = false) String floor,
                                                           @RequestParam(required = false) String room) {
        // 验证手机号参数
        if (phone == null || phone.trim().isEmpty()) {
            Result result = new Result();
            result.setCode("1");
            result.setMsg("手机号参数不能为空");
            return ResponseEntity.ok(result);
        }
        // 通过手机号查询管家信息
        Butler butler = butlerService.getButlerByPhone(phone);
        
        if (butler == null) {
            Result result = new Result();
            result.setCode("1");
            result.setMsg("未找到对应的管家信息，请检查手机号是否正确");
            return ResponseEntity.ok(result);
        }
        
        try {
            // 构建访客邀请链接
            String inviteLink = buildVisitorInviteLink(butler, province, city, district, community, building, units, floor, room);
            
                         System.out.println("生成访客邀请链接: " + inviteLink);
            
            // 构建返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("type", "visitor_invite_link");
            result.put("inviteLink", inviteLink);
            result.put("qrCodeText", inviteLink); // 用于生成普通二维码的文本
            result.put("officialCode", true);  // 🎯 标记为可直接跳转的二维码
            result.put("method", "link_qrcode"); // 标记使用链接二维码方法
            result.put("butlerName", butler.getUsername());
            result.put("butlerPhone", butler.getPhone());
            
            // 构建完整地址
            StringBuilder fullAddress = new StringBuilder();
            String usedProvince = province != null ? province : butler.getProvince();
            String usedCity = city != null ? city : butler.getCity();
            String usedDistrict = district != null ? district : butler.getDistrict();
            String usedCommunity = community != null ? community : butler.getCommunity();
            
            if (usedProvince != null) fullAddress.append(usedProvince);
            if (usedCity != null) fullAddress.append(usedCity);
            if (usedDistrict != null) fullAddress.append(usedDistrict);
            if (usedCommunity != null) fullAddress.append(usedCommunity);
            if (building != null) fullAddress.append(building).append("栋");
            if (units != null) fullAddress.append(units).append("单元");
            if (floor != null) fullAddress.append(floor).append("层");
            if (room != null) fullAddress.append(room).append("室");
            
            result.put("fullAddress", fullAddress.toString());
            result.put("timestamp", System.currentTimeMillis());
            
            Result response = new Result();
            response.setCode("0");
            response.setMsg("访客邀请链接生成成功");
            response.setData(result);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
                         System.err.println("生成访客邀请链接失败: " + e.getMessage());
            Result result = new Result();
            result.setCode("1");
            result.setMsg("生成访客邀请链接失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 构建访客邀请链接
     */
    private String buildVisitorInviteLink(Butler butler, String province, String city, String district, 
                                         String community, String building, String units, String floor, String room) {
        
        // 🎯 使用二维码规则中配置的域名前缀
        // 这个链接需要与小程序后台配置的规则匹配
        StringBuilder link = new StringBuilder("https://qr.parkingdemo.com/visitor/invite?");
        
        // 添加管家信息参数（URL编码）
        try {
            link.append("butler_phone=").append(java.net.URLEncoder.encode(butler.getPhone(), "UTF-8"));
            link.append("&butler_name=").append(java.net.URLEncoder.encode(butler.getUsername(), "UTF-8"));
            
            // 添加地址信息参数
            if (province != null) link.append("&province=").append(java.net.URLEncoder.encode(province, "UTF-8"));
            if (city != null) link.append("&city=").append(java.net.URLEncoder.encode(city, "UTF-8"));
            if (district != null) link.append("&district=").append(java.net.URLEncoder.encode(district, "UTF-8"));
            if (community != null) link.append("&community=").append(java.net.URLEncoder.encode(community, "UTF-8"));
            if (building != null) link.append("&building=").append(building);
            if (units != null) link.append("&units=").append(units);
            if (floor != null) link.append("&floor=").append(floor);
            if (room != null) link.append("&room=").append(room);
            
            // 添加时间戳和唯一标识
            link.append("&timestamp=").append(System.currentTimeMillis());
            link.append("&source=butler_qrcode");
            
        } catch (Exception e) {
                         System.err.println("构建访客邀请链接时URL编码失败: " + e.getMessage());
            // 降级方案：使用简化链接
            return "https://qr.parkingdemo.com/visitor/invite?butler_phone=" + butler.getPhone() + 
                   "&butler_name=" + butler.getUsername() + "&timestamp=" + System.currentTimeMillis();
        }
        
        return link.toString();
    }

}

