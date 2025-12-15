package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.*;
import com.parkingmanage.query.OwnerQuery;
import com.parkingmanage.service.CommunityService;
import com.parkingmanage.service.OwnerinfoService;
import com.parkingmanage.service.OwnerRoleVerificationService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author MLH
 * @since 2022-08-20
 */
@RestController
@RequestMapping("/parking/ownerinfo")
public class OwnerinfoController {
    @Resource
    private OwnerinfoService ownerinfoService;
    @Resource
    private CommunityService communityService;
    @Resource
    private OwnerRoleVerificationService ownerRoleVerificationService;
    @Resource
    private com.parkingmanage.service.ActivityLogService activityLogService;
    @Resource
    private com.parkingmanage.service.UserService userService;

    @ApiOperation("批量添加")
    @PostMapping("/batInsert")
    public ResponseEntity<Result> batInsertOwnerInfo(@RequestBody Ownerinfo[] ownerinfos) {
        String promt = new String();
        promt = "";
        String promtSuc = new String();
        promtSuc = "";
        Integer suc = 0;
        Integer fail = 0;
        Ownerinfo ownerinfo;
        Community community = new Community();
        Result result = new Result();
        int num;
        for (int i = 0; i < ownerinfos.length; i++) {
            ownerinfo = ownerinfos[i];
            if (ownerinfo.getProvince() != null && ownerinfo.getCity() != null && ownerinfo.getDistrict() != null
                    && ownerinfo.getCommunity() != null && ownerinfo.getBuilding() != null && ownerinfo.getUnits() != null
                    && ownerinfo.getFloor() != null && ownerinfo.getRoomnumber() != null
                    && ownerinfo.getOwnername() != null && ownerinfo.getOwnerphone() != null) {
                if (!ownerinfo.getProvince().equals("") && !ownerinfo.getCity().equals("") && !ownerinfo.getDistrict().equals("")
                        && !ownerinfo.getCommunity().equals("") && !ownerinfo.getBuilding().equals("") && !ownerinfo.getUnits().equals("")
                        && !ownerinfo.getFloor().equals("") && !ownerinfo.getRoomnumber().equals("")
                        && !ownerinfo.getOwnername().equals("") && !ownerinfo.getOwnerphone().equals("")
                ) {
                    community.setProvince(ownerinfo.getProvince());
                    community.setCity(ownerinfo.getCity());
                    community.setDistrict(ownerinfo.getDistrict());
                    community.setCommunity(ownerinfo.getCommunity());
                    community.setBuilding(ownerinfo.getBuilding());
                    community.setUnits(ownerinfo.getUnits());
                    community.setFloor(ownerinfo.getFloor());
                    num = communityService.duplicate(community);
                    if (num == 0) {
                        communityService.save(community);
                    }
                    num = ownerinfoService.duplicate(ownerinfo);
                    if (num == 0) {
                        ownerinfoService.save(ownerinfo);
                        suc++;
                    }
                } else {
                    if (promt.equals("")) {
                        promt = "第" + Integer.toString(i + 1);
                    } else {
                        promt = promt + "," + Integer.toString(i + 1);
                    }
                }
            } else {
                if (promt.equals("")) {
                    promt = "第" + Integer.toString(i + 1);
                } else {
                    promt = promt + "," + Integer.toString(i + 1);
                }
            }
        }

        if (!promt.equals("")) {
            promt = promt + "行存在问题！";
        }

        result.setMsg(promt + "\r\n成功导入:" + Integer.toString(suc) + "条。");
        return ResponseEntity.ok(result);
    }

    @ApiOperation("我的社区")
    @GetMapping("/myCommunity/{userphone}")
    public ResponseEntity<Result> myCommunity(@PathVariable String userphone) {
        System.out.println(" 00000000000000000000000000000000000000000000000");
        System.out.println(userphone);
        List<String> myquery = ownerinfoService.myCommunity(userphone);
        Result result = new Result();
        result.setData(myquery);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("我的社区")
    @GetMapping("/myRooms")
    public ResponseEntity<Result> myRooms(@RequestParam(required = false) String community,
                                          @RequestParam(required = false) String building,
                                          @RequestParam(required = false) String units,
                                          @RequestParam(required = false) String floor,
                                          @RequestParam(required = false) String roomnumber,
                                          @RequestParam(required = false) String userphone
    ) {
        System.out.println("🔍 [业主查询] myRooms接口调用参数:");
        System.out.println("  community: " + community);
        System.out.println("  building: " + building);
        System.out.println("  units: " + units);
        System.out.println("  floor: " + floor);
        System.out.println("  roomnumber: " + roomnumber);
        System.out.println("  userphone: '" + userphone + "'");

        List<Ownerinfo> myquery = ownerinfoService.myRooms( community, building, units, floor,
                roomnumber, userphone);

        System.out.println("🔍 [业主查询] 查询结果数量: " + (myquery != null ? myquery.size() : 0));
        if (myquery != null && !myquery.isEmpty()) {
            for (Ownerinfo owner : myquery) {
                System.out.println("  - 业主: " + owner.getOwnername() + ", 电话: " + owner.getOwnerphone());
            }
        }

        Result result = new Result();
        result.setData(myquery);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("我的社区")
    @GetMapping("/listByPhone")
    public ResponseEntity<Result> listByPhone(@RequestParam(required = false) String userphone
    ) {
        System.out.println(" 00000000000000000000000000000000000000000000000");
        System.out.println(userphone);
        List<Ownerinfo> myquery = ownerinfoService.listByPhone(userphone);
        Result result = new Result();
        result.setData(myquery);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("业主")
    @GetMapping("/getById")
    public ResponseEntity<Result> getById(@RequestParam(required = false) String id
    ) {
        Ownerinfo myquery = ownerinfoService.getById(id);
        Result result = new Result();
        result.setData(myquery);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertOwnerInfo(@RequestBody Ownerinfo ownerinfo, javax.servlet.http.HttpServletRequest request) {
        int num = ownerinfoService.duplicate(ownerinfo);
        List<Ownerinfo> res = ownerinfoService.phoneNumberOwnerInfo(ownerinfo.getOwnerphone());
        Result result = new Result();
        if (num == 0) {
            System.out.println(res);
            if (res.isEmpty()) {
                Boolean insertNum = ownerinfoService.save(ownerinfo);
                result.setCode("0");
                result.setMsg("添加成功！");
                
                // 📝 记录操作日志
                User currentUser = getCurrentUser(request);
                String username = currentUser != null && currentUser.getLoginName() != null 
                                ? currentUser.getLoginName() 
                                : (currentUser != null && currentUser.getUserName() != null 
                                    ? currentUser.getUserName() 
                                    : "未知用户");
                String description = String.format("用户 %s 添加了业主信息：姓名 %s，电话 %s，车牌号 %s，小区 %s", 
                                                  username,
                                                  ownerinfo.getOwnername() != null ? ownerinfo.getOwnername() : "未填写",
                                                  ownerinfo.getOwnerphone() != null ? ownerinfo.getOwnerphone() : "未填写",
                                                  ownerinfo.getPlates() != null ? ownerinfo.getPlates() : "未填写",
                                                  ownerinfo.getCommunity() != null ? ownerinfo.getCommunity() : "未填写");
                recordOperation(request, "业主管理", "新增业主", description);
            } else {
                result.setCode("1");
                result.setMsg("业主号码已存在，增加失败！");
            }
        } else {
            result.setCode("1");
            result.setMsg("数据重复，增加失败！");
        }
        return ResponseEntity.ok(result);
    }
//    @ApiOperation("修改")
//    @PutMapping
//    public ResponseEntity<Result> update(@RequestBody Ownerinfo ownerinfo) {
//        System.out.println("ownerinfo = " + ownerinfo);
//        int num = ownerinfoService.duplicate(ownerinfo);
//        Result result=new Result();
//        System.out.println("num = " + num);
//        if (num == 1) {
//             ownerinfoService.updateById(ownerinfo);
//        }else{
//            result.setCode("1");
//            result.setMsg("修改失败！");
//        }
//        return ResponseEntity.ok(result);
//    }

    @ApiOperation("修改")
    @PutMapping("update")
    public ResponseEntity<Result> update(@RequestBody Ownerinfo ownerinfo, javax.servlet.http.HttpServletRequest request) {
        // 获取修改前的数据
        Ownerinfo oldOwnerinfo = ownerinfoService.getById(ownerinfo.getId());
        
        int num = ownerinfoService.duplicate(ownerinfo);
        System.out.println("OwnerInfo：" + ownerinfo);
        Result result = new Result();
        if (num == 0) {
            int res = ownerinfoService.updateByIdNew(ownerinfo);
            if (res != 0) {
                result.setCode("0");
                result.setMsg("修改成功！");
                
                // 📝 记录操作日志（详细记录变更内容）
                User currentUser = getCurrentUser(request);
                String username = currentUser != null && currentUser.getLoginName() != null 
                                ? currentUser.getLoginName() 
                                : (currentUser != null && currentUser.getUserName() != null 
                                    ? currentUser.getUserName() 
                                    : "未知用户");
                
                StringBuilder changeDetails = new StringBuilder();
                if (oldOwnerinfo != null) {
                    if (ownerinfo.getOwnername() != null && !ownerinfo.getOwnername().equals(oldOwnerinfo.getOwnername())) {
                        changeDetails.append("姓名从\"").append(oldOwnerinfo.getOwnername())
                                   .append("\"改为\"").append(ownerinfo.getOwnername()).append("\"；");
                    }
                    if (ownerinfo.getOwnerphone() != null && !ownerinfo.getOwnerphone().equals(oldOwnerinfo.getOwnerphone())) {
                        changeDetails.append("电话从\"").append(oldOwnerinfo.getOwnerphone())
                                   .append("\"改为\"").append(ownerinfo.getOwnerphone()).append("\"；");
                    }
                    if (ownerinfo.getPlates() != null && !ownerinfo.getPlates().equals(oldOwnerinfo.getPlates())) {
                        changeDetails.append("车牌号从\"").append(oldOwnerinfo.getPlates())
                                   .append("\"改为\"").append(ownerinfo.getPlates()).append("\"；");
                    }
                    if (ownerinfo.getCommunity() != null && !ownerinfo.getCommunity().equals(oldOwnerinfo.getCommunity())) {
                        changeDetails.append("小区从\"").append(oldOwnerinfo.getCommunity())
                                   .append("\"改为\"").append(ownerinfo.getCommunity()).append("\"；");
                    }
                }
                
                String description = changeDetails.length() > 0 
                    ? String.format("用户 %s 修改了业主信息（%s）：%s", username, 
                                  ownerinfo.getOwnername() != null ? ownerinfo.getOwnername() : "ID:" + ownerinfo.getId(),
                                  changeDetails.toString())
                    : String.format("用户 %s 修改了业主信息：%s", username,
                                  ownerinfo.getOwnername() != null ? ownerinfo.getOwnername() : "ID:" + ownerinfo.getId());
                recordOperation(request, "业主管理", "修改业主", description);
            } else {
                result.setCode("1");
                result.setMsg("修改失败！");
            }
        } else {
//            System.out.println("数据重复，修改错误")
            result.setCode("1");
            result.setMsg("数据重复，修改失败！");
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id, javax.servlet.http.HttpServletRequest request) {
        // 在删除前获取业主信息（用于日志）
        Ownerinfo ownerinfo = ownerinfoService.getById(id);
        boolean result = ownerinfoService.removeById(id);
        
        if (result && ownerinfo != null) {
            // 📝 记录操作日志
            User currentUser = getCurrentUser(request);
            String username = currentUser != null && currentUser.getLoginName() != null 
                            ? currentUser.getLoginName() 
                            : (currentUser != null && currentUser.getUserName() != null 
                                ? currentUser.getUserName() 
                                : "未知用户");
            String description = String.format("用户 %s 删除了业主信息：姓名 %s，电话 %s，车牌号 %s", 
                                              username,
                                              ownerinfo.getOwnername() != null ? ownerinfo.getOwnername() : "未知",
                                              ownerinfo.getOwnerphone() != null ? ownerinfo.getOwnerphone() : "未知",
                                              ownerinfo.getPlates() != null ? ownerinfo.getPlates() : "未知");
            recordOperation(request, "业主管理", "删除业主", description);
        }
        
        return result;
    }

    @ApiOperation("查询所有")
    @GetMapping("/queryOwner")

    public List<Ownerinfo> queryOwner(OwnerQuery query) {
        QueryWrapper<Ownerinfo> wrapper = Wrappers.<Ownerinfo>query();
        wrapper.eq("a.province", query.getProvince());
        wrapper.eq("a.city", query.getCity());
        wrapper.eq("a.district", query.getDistrict());
        wrapper.eq("a.community", query.getCommunity());
        wrapper.eq("a.building", query.getBuilding());
        wrapper.eq("a.units", query.getUnits());
        wrapper.eq("a.floor", query.getFloor());
        wrapper.eq("a.roomnumber", query.getRoomnumber());
        List<Ownerinfo> myquery = ownerinfoService.queryOwner(wrapper);
        System.out.println("myquery = " + query);
        return myquery;
    }

    @ApiOperation("分页查询")
    @GetMapping("/querypage")
    public IPage<Ownerinfo> queryPage(
            @RequestParam(required = false) String ownername,
            @RequestParam(required = false, value = "community") String community,
            @RequestParam(required = false) String ownerphone,
            @RequestParam(required = false) String plates,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Ownerinfo> ownerList = ownerinfoService.queryListOwner(ownername, community, ownerphone, plates);

        //按照设备名和申请日期排序
        List<Ownerinfo> asServices = ownerList.stream().sorted(Comparator.comparing(Ownerinfo::getOwnername).thenComparing(Ownerinfo::getCommunity)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }


    @ApiOperation("查询所有")
    @GetMapping("/phoneNumberOwnerInfo")
    public List<Ownerinfo> phoneNumberOwnerInfo(String phoneNumber) {
        List<Ownerinfo> myquery = ownerinfoService.phoneNumberOwnerInfo(phoneNumber);
        return myquery;
    }

    @ApiOperation("业主姓名搜索建议")
    @GetMapping("/owner-name-suggestions")
    public ResponseEntity<Result> getOwnerNameSuggestions(@RequestParam String keyword) {
        Result result = new Result();
        
        try {
            System.out.println("🔍 [业主姓名搜索] 开始查询，关键词: " + keyword);
            
            if (keyword == null || keyword.trim().isEmpty()) {
                result.setCode("0");
                result.setData(new ArrayList<>());
                return ResponseEntity.ok(result);
            }
            
            // 使用模糊查询业主姓名
            QueryWrapper<Ownerinfo> wrapper = Wrappers.<Ownerinfo>query();
            wrapper.like("ownername", keyword.trim())
                   .orderBy(true, true, "ownername"); // 按姓名排序
            
            List<Ownerinfo> ownerList = ownerinfoService.list(wrapper);
            
            // 构建返回结果（去重并格式化）
            List<Map<String, Object>> suggestions = new ArrayList<>();
            Map<String, Map<String, Object>> uniqueOwners = new HashMap<>();
            
            for (Ownerinfo owner : ownerList) {
                // 使用姓名和电话作为唯一键，避免重复
                String key = owner.getOwnername() + "_" + (owner.getOwnerphone() != null ? owner.getOwnerphone() : "");
                
                if (!uniqueOwners.containsKey(key)) {
                    Map<String, Object> suggestion = new HashMap<>();
                    suggestion.put("ownerName", owner.getOwnername());
                    suggestion.put("ownerPhone", owner.getOwnerphone());
                    suggestion.put("plateNumber", owner.getPlates()); // 车牌号
                    suggestion.put("community", owner.getCommunity()); // 所属小区
                    suggestion.put("building", owner.getBuilding()); // 楼栋
                    suggestion.put("roomnumber", owner.getRoomnumber()); // 房号
                    
                    uniqueOwners.put(key, suggestion);
                }
            }
            
            // 转换为列表，限制返回数量（最多20条）
            suggestions = uniqueOwners.values().stream()
                    .limit(20)
                    .collect(Collectors.toList());
            
            System.out.println("✅ [业主姓名搜索] 查询完成，找到 " + suggestions.size() + " 条结果");
            
            result.setCode("0");
            result.setData(suggestions);
            
        } catch (Exception e) {
            System.err.println("❌ [业主姓名搜索] 查询失败: " + e.getMessage());
            e.printStackTrace();
            result.setCode("1");
            result.setMsg("查询失败: " + e.getMessage());
            result.setData(new ArrayList<>());
        }
        
        return ResponseEntity.ok(result);
    }

    @ApiOperation("清除业主验证缓存")
    @PostMapping("/clearCache")
    public ResponseEntity<Result> clearOwnerCache(@RequestParam(required = false) String phone) {
        Result result = new Result();
        
        try {
            if (phone != null && !phone.trim().isEmpty()) {
                // 清除指定手机号的缓存
                ownerRoleVerificationService.clearCache(phone.trim());
                result.setCode("0");
                result.setMsg("已清除手机号 " + phone + " 的缓存");
                System.out.println("🗑️ 手动清除缓存 - 手机号: " + phone);
            } else {
                // 清除所有缓存
                ownerRoleVerificationService.clearAllCache();
                result.setCode("0");
                result.setMsg("已清除所有业主验证缓存");
                System.out.println("🗑️ 手动清除所有缓存");
            }
            
        } catch (Exception e) {
            result.setCode("1");
            result.setMsg("清除缓存失败: " + e.getMessage());
            System.err.println("❌ 清除缓存失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    @ApiOperation("验证业主月票信息")
    @GetMapping("/checkMonthlyTicket")
    public ResponseEntity<Result> checkMonthlyTicket(
            @RequestParam String phone,
            @RequestParam(required = false, defaultValue = "四季上东") String community
    ) {
        Result result = new Result();
        long startTime = System.currentTimeMillis();

        try {
            // 🎯 第一步：先查询本地业主表（和登录验证逻辑保持一致）
            List<Ownerinfo> localOwnerList = ownerinfoService.phoneNumberOwnerInfo(phone);
            
            if (!localOwnerList.isEmpty()) {
                // 本地找到业主信息
                Ownerinfo localOwner = localOwnerList.get(0);
                
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                result.setCode("0");
                result.setMsg("验证成功 (本地数据，耗时: " + duration + "ms)");
                
                // 构建本地业主信息响应
                Map<String, Object> localOwnerInfo = new java.util.HashMap<>();
                localOwnerInfo.put("phone", phone);
                localOwnerInfo.put("community", community);
                localOwnerInfo.put("isValidOwner", true);
                localOwnerInfo.put("ownername", localOwner.getOwnername());
                localOwnerInfo.put("userName", localOwner.getOwnername());
                localOwnerInfo.put("ownerphone", localOwner.getOwnerphone());
                localOwnerInfo.put("province", localOwner.getProvince());
                localOwnerInfo.put("city", localOwner.getCity());
                localOwnerInfo.put("district", localOwner.getDistrict());
                localOwnerInfo.put("building", localOwner.getBuilding());
                localOwnerInfo.put("units", localOwner.getUnits());
                localOwnerInfo.put("floor", localOwner.getFloor());
                localOwnerInfo.put("roomnumber", localOwner.getRoomnumber());
                localOwnerInfo.put("source", "local_database");
                localOwnerInfo.put("verification", "phone_only");
                localOwnerInfo.put("duration", duration);
                
                // 🎯 从本地业主信息中提取车牌数据
                List<Map<String, Object>> monthlyTickets = new ArrayList<>();
                
                // 处理本地业主的车牌信息
                String plates = localOwner.getPlates();
                String parkingSpaces = localOwner.getParkingspaces();
                
                System.out.println("🚗 本地业主车牌信息 - plates: " + plates + ", parkingSpaces: " + parkingSpaces);
                
                if (plates != null && !plates.trim().isEmpty()) {
                    // 解析车牌信息，可能是多个车牌用逗号分隔
                    String[] plateArray = plates.split("[,，;；|]"); // 支持多种分隔符
                    
                    for (String plate : plateArray) {
                        plate = plate.trim();
                        if (!plate.isEmpty()) {
                            Map<String, Object> plateInfo = new HashMap<>();
                            plateInfo.put("plateNumber", plate);
                            plateInfo.put("platenumber", plate);
                            plateInfo.put("source", "local_database");
                            plateInfo.put("status", "有效");
                            
                            // 如果有停车位信息，也加上
                            if (parkingSpaces != null && !parkingSpaces.trim().isEmpty()) {
                                plateInfo.put("parkingSpace", parkingSpaces);
                            }
                            
                            monthlyTickets.add(plateInfo);
                        }
                    }
                    
                    System.out.println("✅ 从本地数据解析到 " + monthlyTickets.size() + " 个车牌信息");
                } else {
                    System.out.println("ℹ️ 本地业主数据中无车牌信息");
                }
                
                System.out.println("✅ 本地业主信息查询成功，提取车牌完成");
                
                localOwnerInfo.put("data", monthlyTickets);
                localOwnerInfo.put("monthlyTickets", monthlyTickets);
                localOwnerInfo.put("plateCount", monthlyTickets.size());
                
                result.setData(localOwnerInfo);
                
                System.out.println("✅ 返回本地业主信息成功: " + result);
                return ResponseEntity.ok(result);
            }
            
            System.out.println("❌ 本地业主表无记录，继续外部API查询...");
            
            // 🎯 第二步：本地没有数据，查询外部API（原有逻辑）
            System.out.println("🌐 第二步：查询外部API...");
            String parkCode = "四季上东".equals(community) ? "2KUG6XLU" : "2KST9MNP";
            System.out.println("🏢 停车场代码: " + parkCode);
            
            // 优先获取详细业主信息（包含userName等字段）
            System.out.println("🚀 调用业主详细信息查询方法...");
            Map<String, Object> ownerDetails = ownerRoleVerificationService.getOwnerDetailsByPark(phone, community);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.println("⏱️ 总验证耗时: " + duration + "ms");
            
            if (ownerDetails != null) {
                // 外部API验证成功，构建详细的业主信息响应
                result.setCode("0");
                result.setMsg("验证成功 (外部API，耗时: " + duration + "ms)");
                
                // 提取真实的业主信息，优先使用userName字段
                Map<String, Object> enhancedInfo = new java.util.HashMap<>(ownerDetails);
                
                // 从原始数据中提取userName作为业主姓名
                Object originalData = ownerDetails.get("originalData");
                String ownerName = "业主"; // 默认值
                
                if (originalData instanceof Map) {
                    Map<String, Object> originalMap = (Map<String, Object>) originalData;
                    
                    // 优先使用userName字段
                    if (originalMap.get("userName") != null) {
                        ownerName = String.valueOf(originalMap.get("userName"));
                        System.out.println("✅ 从userName字段获取业主姓名: " + ownerName);
                    } else if (originalMap.get("ownername") != null) {
                        ownerName = String.valueOf(originalMap.get("ownername"));
                        System.out.println("✅ 从ownername字段获取业主姓名: " + ownerName);
                    }
                }
                
                // 更新业主信息
                enhancedInfo.put("phone", phone);
                enhancedInfo.put("community", community);
                enhancedInfo.put("parkCode", parkCode);
                enhancedInfo.put("isValidOwner", true);
                enhancedInfo.put("ownername", ownerName);  // 使用提取的真实姓名
                enhancedInfo.put("userName", ownerName);    // 同时提供userName字段
                enhancedInfo.put("source", "external_api_with_details");
                enhancedInfo.put("verification", "phone_only");
                enhancedInfo.put("duration", duration);
                
                result.setData(enhancedInfo);
                
                System.out.println("✅ 返回外部API详细业主信息成功: " + result);
                
            } else {
                // 详细信息获取失败，退回到简单验证
                System.out.println("⚠️ 详细信息获取失败，使用简单验证方法...");
                boolean isOwner = ownerRoleVerificationService.isOwnerByPhoneNumberInParkCode(phone, parkCode);
                System.out.println("📊 简单验证结果: " + (isOwner ? "✅业主" : "❌非业主"));
                
                if (isOwner) {
                    // 即使是简单验证，也要尝试获取真实的userName
                    System.out.println("🔍 尝试获取简单验证的业主详细信息...");
                    Map<String, Object> simpleOwnerInfo = ownerRoleVerificationService.getOwnerDetailsByPhoneOptimized(phone);
                    
                    String realOwnerName = "业主"; // 默认值
                    if (simpleOwnerInfo != null) {
                        // 尝试从详细信息中提取userName
                        Object originalData = simpleOwnerInfo.get("originalData");
                        if (originalData instanceof Map) {
                            Map<String, Object> originalMap = (Map<String, Object>) originalData;
                            if (originalMap.get("userName") != null) {
                                realOwnerName = String.valueOf(originalMap.get("userName"));
                                System.out.println("✅ 简单验证也获取到真实姓名: " + realOwnerName);
                            } else if (originalMap.get("ownername") != null) {
                                realOwnerName = String.valueOf(originalMap.get("ownername"));
                                System.out.println("✅ 简单验证获取到ownername: " + realOwnerName);
                            }
                        }
                        // 直接从顶层尝试获取
                        else if (simpleOwnerInfo.get("userName") != null) {
                            realOwnerName = String.valueOf(simpleOwnerInfo.get("userName"));
                            System.out.println("✅ 从顶层获取到userName: " + realOwnerName);
                        } else if (simpleOwnerInfo.get("ownername") != null) {
                            realOwnerName = String.valueOf(simpleOwnerInfo.get("ownername"));
                            System.out.println("✅ 从顶层获取到ownername: " + realOwnerName);
                        }
                    }
                    
                    result.setCode("0");
                    result.setMsg("验证成功 (外部API简单验证，耗时: " + duration + "ms)");
                    
                    // 构建基本业主信息
                    Map<String, Object> basicInfo = new java.util.HashMap<>();
                    basicInfo.put("phone", phone);
                    basicInfo.put("community", community);
                    basicInfo.put("parkCode", parkCode);
                    basicInfo.put("isValidOwner", true);
                    basicInfo.put("ownername", realOwnerName);    // 使用真实姓名而不是"业主"
                    basicInfo.put("userName", realOwnerName);      // 使用真实姓名而不是"业主"
                    basicInfo.put("source", "external_api_basic_verification");
                    basicInfo.put("verification", "phone_only");
                    basicInfo.put("duration", duration);
                    result.setData(basicInfo);
                    
                    System.out.println("✅ 返回外部API基本验证成功: " + result);
                    
                } else {
                    result.setCode("1");
                    result.setMsg("该手机号非本小区月票用户 (耗时: " + duration + "ms)");
                    
                    System.out.println("❌ 返回失败结果: " + result);
                }
            }
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            result.setCode("1");
            result.setMsg("验证失败: " + e.getMessage() + " (耗时: " + duration + "ms)");
            
            // 记录详细错误日志
            System.err.println("❌ 业主月票验证异常 - 手机号: " + phone + ", 停车场: " + community + ", 耗时: " + duration + "ms");
            System.err.println("❌ 错误详情: " + e.getMessage());
            e.printStackTrace();
            
            // 根据错误类型提供不同的错误信息
            if (e.getMessage().contains("timeout") || e.getMessage().contains("超时")) {
                result.setMsg("外部月票系统查询超时，请稍后重试 (耗时: " + duration + "ms)");
            } else if (e.getMessage().contains("网络") || e.getMessage().contains("连接")) {
                result.setMsg("网络连接异常，请检查网络后重试 (耗时: " + duration + "ms)");
            } else if (duration > 60000) { // 超过60秒
                result.setMsg("查询时间过长，外部系统响应缓慢，请稍后重试 (耗时: " + duration + "ms)");
            }
            
            System.out.println("💥 返回异常结果: " + result);
        }
        
        System.out.println("🏁 业主月票验证完成，最终结果: " + result);
        return ResponseEntity.ok(result);
    }

    // ==================== 📝 操作日志记录方法 ====================

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser(javax.servlet.http.HttpServletRequest request) {
        try {
            return com.parkingmanage.utils.TokenUtils.getCurrentUser();
        } catch (Exception e) {
            System.err.println("获取当前用户失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 记录操作日志
     */
    private void recordOperation(javax.servlet.http.HttpServletRequest request, String module, String action, String description) {
        try {
            User currentUser = getCurrentUser(request);
            String username = currentUser != null && currentUser.getLoginName() != null 
                            ? currentUser.getLoginName() 
                            : (currentUser != null && currentUser.getUserName() != null 
                                ? currentUser.getUserName() 
                                : "未知用户");

            ActivityLog activityLog = new ActivityLog();
            activityLog.setUserId(currentUser != null ? currentUser.getUserId().toString() : "unknown");
            activityLog.setUsername(username);
            activityLog.setModule(module);
            activityLog.setAction(action);
            activityLog.setDescription(description);
            activityLog.setStatus("success");
            activityLog.setCreatedAt(LocalDateTime.now());
            activityLog.setIpAddress(getClientIpAddress(request));
            activityLog.setUserAgent(request.getHeader("User-Agent"));

            activityLogService.save(activityLog);

            System.out.println("📝 [操作日志] 用户：" + username + "，模块：" + module + "，操作：" + action + "，描述：" + description);
        } catch (Exception e) {
            System.err.println("记录操作日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(javax.servlet.http.HttpServletRequest request) {
        String[] headerNames = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0];
            }
        }
        return request.getRemoteAddr();
    }
}

