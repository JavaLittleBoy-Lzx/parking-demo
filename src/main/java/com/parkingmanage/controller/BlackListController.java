package com.parkingmanage.controller;


import cn.hutool.core.io.resource.MultiFileResource;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.HttpClientUtil;
import com.parkingmanage.common.Result;
import com.parkingmanage.common.config.AIKEConfig;
import com.parkingmanage.entity.*;
import com.parkingmanage.service.BlackListService;
import com.parkingmanage.service.MonthTicketService;
import com.parkingmanage.service.ViolationsService;
import com.parkingmanage.service.YardInfoService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author lzx
 * @since 2023-12-21
 */
@RestController
@RequestMapping("/parking/blackList")
public class BlackListController {

    @Autowired
    private BlackListService blackListService;

    @Autowired
    public AIKEConfig aikeConfig;

    @Resource
    private YardInfoService yardInfoService;

    @Autowired
    private ViolationsService violationsService;

    @Autowired
    private MonthTicketService monthTicketService;

    @Resource
    private com.parkingmanage.service.ActivityLogService activityLogService;

    @Resource
    private com.parkingmanage.service.UserService userService;

    /**
     * 查询黑名单
     *
     * @param parkCodeList
     * @return
     */
    @ApiOperation("查询黑名单车辆列表")
    @RequestMapping("/getParkBlackList")
    public ResponseEntity getParkBlackList(String parkCodeList) {
        HashMap<String, Object> params = new HashMap<>();
        //enterTime格式必须是yyyy-MM-dd HH:mm:ss
        params.put("parkCodeList", Arrays.asList(parkCodeList));
        params.put("pageNum", 1);
        params.put("pageSize", 1000);
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "getParkBlackList", params);
        return ResponseEntity.ok(data);
    }

    @ApiOperation("查询黑名单车辆列表")
    @RequestMapping ("/getParkBlack")
    public ResponseEntity getParkBlack(String parkCodeList, String carCode) {
        HashMap<String, Object> params = new HashMap<>();
        //enterTime格式必须是yyyy-MM-dd HH:mm:ss
        params.put("parkCodeList", Arrays.asList(parkCodeList));
        params.put("pageNum", 1);
        params.put("carCode", carCode);
        System.out.println("params = " + params);
        params.put("pageSize", 1000);
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "getParkBlackList", params);
        return ResponseEntity.ok(data);
    }

    @ApiOperation("查询黑名单类型")
    @GetMapping("/getSpecialCarTypeList")
    public ResponseEntity getSpecialCarTypeList(String parkCodeList) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("parkCodeList", Arrays.asList(parkCodeList));
        params.put("pageNum", 1);
        params.put("pageSize", 1000);
        params.put("vipGroupType", 2);
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "getSpecialCarTypeList", params);
        return ResponseEntity.ok(data);
    }

    /**
     * 添加黑名单
     *
     * @param parkCode
     * @param carCode
     * @param carOwner
     * @param isPermament
     * @param timePeriod
     * @param reason
     * @param specialCarTypeId
     * @return
     */
    @ApiOperation("添加黑名单")
    @GetMapping("/addBlackListCar")
    public ResponseEntity addBlackListCar(String parkCode, String carCode, String carOwner, String isPermament, String timePeriod, String reason, String remark1, String remark2, String specialCarTypeId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("parkCode", parkCode);
        params.put("carCode", carCode);
        params.put("carOwner", carOwner);
        params.put("reason", reason);
        params.put("isPermament", isPermament);
        params.put("specialCarTypeId", specialCarTypeId);
        if (!remark1.isEmpty()) {
            params.put("remark1", remark1);
        } else if (!remark2.isEmpty()) {
            params.put("remark2", remark2);
        }
        params.put("timePeriod", timePeriod);
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "addBlackListCar", params);
        return ResponseEntity.ok(data);
    }

    /**
     * 文件批量搭导入改写后添加黑名单
     *
     * @param blackVue
     * @return
     */
    @ApiOperation("文件批量导入改写后添加黑名单")
    @PostMapping("/addBlackListCarVue")
    public ResponseEntity addBlackListCarVue(@RequestBody BlackVue blackVue) {
        ArrayList<String> strings = new ArrayList<>();
        HashMap<String, String> params = new HashMap<>();
        //enterTime格式必须是yyyy-MM-dd HH:mm:ss
//        System.out.println("blackFileVue = " + blackVue);
        params.put("parkCode", blackVue.getParkCode());
        params.put("carCode", blackVue.getCarCode());
        params.put("carOwner", blackVue.getCarOwner());
        params.put("reason", blackVue.getReason());
        if (blackVue.getIsPermament().equals("永久")) {
            params.put("isPermament", "1");
            params.put("timePeriod", "");
        } else if (blackVue.getIsPermament().equals("自定义")) {
            params.put("isPermament", "0");
            params.put("timePeriod", blackVue.getTimePeriod());
        } else {
            params.put("isPermament", blackVue.getIsPermament());
        }
        params.put("specialCarTypeId", blackVue.getSpecialCarTypeId());
        params.put("remark1", blackVue.getRemark1());
        params.put("remark2", blackVue.getRemark2());
//        String get = HttpClientUtil.doGet("http://www.xuerparking.cn:8543/parking/blackList/addBlackListCar", params);
        String get = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/blackList/addBlackListCar", params);
        JSONObject jsonObject = JSONObject.parseObject(get);
//        System.out.println("jsonObject = " + jsonObject);
        JSONObject jsonObjectData = (JSONObject) jsonObject.get("data");
//        System.out.println("调用参数：" + jsonObjectData.getString("message"));
        strings.add(blackVue.getCarCode() + "--" + jsonObjectData.getString("message") + "--" + jsonObjectData.getString("status"));
        if (jsonObjectData.getString("message").equals("业务成功")) {
            BlackList blackList = new BlackList();
            blackList.setCarCode(blackVue.getCarCode());
            blackList.setOwner(blackVue.getCarOwner());
            blackList.setParkName(blackVue.getParkName());
            blackList.setRemark1(blackVue.getRemark1());
            blackList.setRemark2(blackVue.getRemark2());
            blackList.setReason(blackVue.getReason());
            if (blackVue.getIsPermament().equals("永久")) {
                blackList.setBlackListForeverFlag("永久");
            } else if (blackVue.getIsPermament().equals("自定义")) {
                blackList.setBlackListForeverFlag(blackVue.getTimePeriod());
            }
            // 根据blackVue.getSpecialCarTypeName()调用查询接口
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("parkCodeList", blackVue.getParkCode());
//            String getSpecialCarType = HttpClientUtil.doGet("http://www.xuerparking.cn:8543/parking/blackList/getSpecialCarTypeList", hashMap);
            String getSpecialCarType = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/blackList/getSpecialCarTypeList", hashMap);
            JSONObject jsonObjectSpecialCarType = JSONObject.parseObject(getSpecialCarType);
            // 获取嵌套的JSON对象
            JSONObject dataObject = jsonObjectSpecialCarType.getJSONObject("data").getJSONObject("data");
//            System.out.println("dataObject = " + dataObject);
            // 获取recordList数组
            JSONArray recordList = dataObject.getJSONArray("recordList");
//            System.out.println("recordList = " + recordList.toString());
            // 遍历recordList数组，查找id为502071的对象
            for (int i = 0; i < recordList.size(); i++) {
                JSONObject record = recordList.getJSONObject(i);
                if (record.getString("id").equals(blackVue.getSpecialCarTypeId())) {
                    // 找到匹配的对象，提取name值
                    String name = record.getString("name");
                    blackList.setSpecialCarTypeConfigName(name);
                    break;
                }
            }
            if (blackListService.findOne(blackList).isEmpty()) {
                blackListService.save(blackList);
            }
        }
        Result result = new Result();
        result.setMsg("添加黑名单成功！");
        result.setCode("0");
        result.setData(strings);
        return ResponseEntity.ok(result);
    }


    @ApiOperation("接收前端数据")
    @PostMapping("/addBlackCar")
    public ResponseEntity addBlackCar(@RequestBody BlackVue blackVue, javax.servlet.http.HttpServletRequest request) throws ParseException {
//        System.out.println("blackVue = " + blackVue);
        ArrayList<String> strings = new ArrayList<>();
        HashMap<String, String> hashMap = new HashMap<>();
        // 处理数据,将接收到的字符串按照","进行拆分
        String[] split = blackVue.getCarCode().split(",");
        int successCount = 0;
        for (String carNo : split) {
            BlackList blackList = new BlackList();
            blackList.setParkName(blackVue.getParkName());
            blackList.setOwner(blackVue.getCarOwner());
            blackList.setCarCode(carNo);
            blackList.setReason(blackVue.getReason());
            blackList.setRemark1(blackVue.getRemark1());
            blackList.setRemark2(blackVue.getRemark2());
            blackList.setSpecialCarTypeConfigName(blackVue.getSpecialCarTypeName());
            hashMap.put("parkCode", blackVue.getParkCode());
            hashMap.put("carCode", carNo);
            hashMap.put("carOwner", blackVue.getCarOwner());
            hashMap.put("reason", blackVue.getReason());
            hashMap.put("remark1", blackVue.getRemark1());
            hashMap.put("remark2", blackVue.getRemark2());
            if (blackVue.getIsPermament().equals("永久")) {
                hashMap.put("isPermament", "1");
                blackList.setBlackListForeverFlag("永久");
            } else if (blackVue.getIsPermament().equals("自定义")) {
                hashMap.put("isPermament", "0");
                hashMap.put("timePeriod", formatDateRange(blackVue.getTimePeriod()));
                blackList.setBlackListForeverFlag(blackVue.getTimePeriod());
            }
            hashMap.put("specialCarTypeId", blackVue.getSpecialCarTypeId());
            String get = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/blackList/addBlackListCar", hashMap);
//            String get = HttpClientUtil.doGet("http://www.xuerparking.cn:8543/parking/blackList/addBlackListCar", hashMap);
            JSONObject jsonObject = JSONObject.parseObject(get);
            JSONObject jsonObjectData = (JSONObject) jsonObject.get("data");
            strings.add(carNo + "--" + jsonObjectData.getString("message"));
            if (jsonObjectData.getString("message").equals("业务成功")) {
                if (blackListService.findOne(blackList).isEmpty()) {
                    blackListService.save(blackList);
                    successCount++;
                }
            }
        }
        
        // 📝 记录操作日志
        if (successCount > 0) {
            User currentUser = getCurrentUser(request);
            String username = currentUser != null && currentUser.getLoginName() != null 
                            ? currentUser.getLoginName() 
                            : (currentUser != null && currentUser.getUserName() != null 
                                ? currentUser.getUserName() 
                                : "未知用户");
            String description = split.length > 1 
                ? String.format("用户 %s 批量添加了 %d 个车牌到黑名单：车牌号 %s 等，原因 %s，停车场 %s", 
                              username, successCount, split[0], 
                              blackVue.getReason() != null ? blackVue.getReason() : "未填写",
                              blackVue.getParkName() != null ? blackVue.getParkName() : "未填写")
                : String.format("用户 %s 添加了车牌到黑名单：车牌号 %s，原因 %s，停车场 %s", 
                              username, split[0], 
                              blackVue.getReason() != null ? blackVue.getReason() : "未填写",
                              blackVue.getParkName() != null ? blackVue.getParkName() : "未填写");
            recordOperation(request, "黑名单管理", "添加黑名单", description);
        }
        
        Result result = new Result();
        result.setMsg("添加黑名单成功！");
        result.setCode("0");
        result.setData(strings);
        return ResponseEntity.ok(result);
    }

    /**
     * 批量导入黑名单
     *
     * @param file
     * @return
     * @throws IOException
     */
    @ApiOperation("批量导入黑名单数据")
    @PostMapping("/import")
    public ResponseEntity importParkBlackList(MultipartFile file) throws IOException, ParseException {
        Result res = new Result();
        ExcelReader reader = ExcelUtil.getReader(file.getInputStream());
        // 调用接口查询车场名称
        HashMap<String, String> hashMap = new HashMap<>();
        ArrayList<BlackList> listArrayList = new ArrayList<>();
        List<BlackList> blackLists = reader.readAll(BlackList.class);
        ArrayList<BlackList> listArraySingleList = new ArrayList<>();
        // 将表格中的车辆字符串进行拆分，接着将所有的其余属性添加到新的BlackList中
        for (BlackList blackList : blackLists) {
            // 将车牌那一行是否包含","包含的话则进行拆分否则的话执行之前的操作
            if (blackList.getCarCode().contains(",")) {
                // 将车牌号码进行拆分
                String[] carNoList = blackList.getCarCode().split(",");
                for (String carNo : carNoList) {
                    BlackList blackListSplit = new BlackList();
                    blackListSplit.setParkName(blackList.getParkName());
                    blackListSplit.setCarCode(carNo);
                    blackListSplit.setReason(blackList.getReason());
                    blackListSplit.setRemark1(blackList.getRemark1());
                    blackListSplit.setRemark2(blackList.getRemark2());
                    blackListSplit.setBlackListForeverFlag(blackList.getBlackListForeverFlag());
                    blackListSplit.setOwner(blackList.getOwner());
                    blackListSplit.setSpecialCarTypeConfigName(blackList.getSpecialCarTypeConfigName());
                    listArrayList.add(blackListSplit);
                }
            } else {
                listArraySingleList.add(blackList);
            }
        }
        // 将listArrayList的值全部添加到blackLists之中
        listArraySingleList.addAll(listArrayList);
        ArrayList<String> strings = new ArrayList<>();
        for (BlackList blackList : listArraySingleList) {
//            System.out.println("blackList = " + blackList);
            // 判断车场名称，寻找到车场名称下的黑名单类型ID
            if (blackList.getParkName().equals("万象上东")) {
                hashMap.put("parkCode", "2KST9MNP");
                hashMap.put("carCode", blackList.getCarCode());
                hashMap.put("carOwner", blackList.getOwner());
                hashMap.put("reason", blackList.getReason());
                hashMap.put("remark1", blackList.getRemark1());
                hashMap.put("remark2", blackList.getRemark2());
                if (blackList.getBlackListForeverFlag().equals("永久")) {
                    hashMap.put("isPermament", "1");
                } else {
                    hashMap.put("isPermament", "0");
                    // 将字符串格式化去除中间的空格，只保留中间的"-"
                    try {
                        String result = formatDateRange(blackList.getBlackListForeverFlag());
//                        System.out.println("格式化后的日期范围: " + result);
                        hashMap.put("timePeriod", result);
                    } catch (IllegalArgumentException | ParseException e) {
                        System.out.println("错误: " + e.getMessage());
                    }
                }
                hashMap.put("specialCarTypeId", "502071");
//                System.out.println("hashMap = " + hashMap);
                String get = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/blackList/addBlackListCar", hashMap);
                JSONObject jsonObject = JSONObject.parseObject(get);
                JSONObject jsonObjectData = (JSONObject) jsonObject.get("data");
                strings.add(blackList.getCarCode() + "--" + blackList.getParkName() + "--" + formatDateRange(blackList.getBlackListForeverFlag()) + "--" + blackList.getOwner() + "--" + "您已欠费" + "--" + "502071" + "--" + jsonObjectData.getString("message"));
                if (jsonObjectData.getString("message").equals("业务成功")) {
                    if (blackListService.findOne(blackList).isEmpty()) {
                        blackList.setSpecialCarTypeConfigName("您已欠费");
                        blackListService.save(blackList);
                    }
                }
            } else if (blackList.getParkName().equals("四季上东")) {
                hashMap.put("parkCode", "2KUG6XLU");
                hashMap.put("carCode", blackList.getCarCode());
                hashMap.put("carOwner", blackList.getOwner());
                hashMap.put("reason", blackList.getReason());
                hashMap.put("remark1", blackList.getRemark1());
                hashMap.put("remark2", blackList.getRemark2());
                if (blackList.getBlackListForeverFlag().equals("永久")) {
                    hashMap.put("isPermament", "1");
                    hashMap.put("timePeriod", null);
                } else {
                    hashMap.put("isPermament", "0");
                    // 将字符串格式化去除中间的空格，只保留中间的"-"
                    hashMap.put("timePeriod", formatDateRange(blackList.getBlackListForeverFlag()));
                }
                hashMap.put("specialCarTypeId", "1526");
                String get = HttpClientUtil.doPost("https://www.xuerparking.cn:8543/parking/blackList/addBlackListCar", hashMap);
                JSONObject jsonObject = JSONObject.parseObject(get);
                JSONObject jsonObjectData = (JSONObject) jsonObject.get("data");
//                System.out.println("jsonObjectData = " + jsonObjectData);
                strings.add(blackList.getCarCode() + "--" + blackList.getParkName() + "--" + formatDateRange(blackList.getBlackListForeverFlag()) + "--" + blackList.getOwner() + "--" + "您已欠费请及时缴费" + "--" + "1526" + "--" + jsonObjectData.getString("message"));
                if (jsonObjectData.getString("message").equals("业务成功")) {
                    if (blackListService.findOne(blackList).isEmpty()) {
                        blackList.setSpecialCarTypeConfigName("您已欠费请及时缴费");
                        blackListService.save(blackList);
                    }
                }
            }
        }
        // 返回数据
        res.setMsg("导入成功！");
        res.setCode("0");
        res.setData(strings);
        return ResponseEntity.ok(res);
    }

    /**
     * 更新同步黑名单信息
     *
     * @param parkName
     * @return
     */
    @ApiOperation("更新同步黑名单信息")
    @GetMapping("/synchroBlack")
    public ResponseEntity synchroBlack(String parkName) {
        // 调用接口查询车场名称
        HashMap<String, String> hashMap = new HashMap<>();
        if (parkName.equals("万象上东")) {
            hashMap.put("parkCodeList", "2KST9MNP");
        } else if (parkName.equals("四季上东")) {
            hashMap.put("parkCodeList", "2KUG6XLU");
        }else if (parkName.equals("欧洲新城")) {
            hashMap.put("parkCodeList", "2KPL6XFF");
        }
//        System.out.println("hashMap = " + hashMap);
//        String get = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/vehicleReservation/getParkInfo", hashMap);
        String get = HttpClientUtil.doPost("https://www.xuerparking.cn:8543/parking/blackList/getParkBlackList", hashMap);
        JSONObject jsonObject = JSONObject.parseObject(get);
        JSONObject jsonObjectData = (JSONObject) jsonObject.get("data");
//        System.out.println("jsonObjectData = " + jsonObjectData);
        JSONObject jsonObjectDataData = (JSONObject) jsonObjectData.get("data");
        JSONArray recordList = (JSONArray) jsonObjectDataData.get("recordList");
        List<BlackList> blackLists = new ArrayList<>();
        // System.out.println("ArrayList")
        for (int i1 = 0; i1 < recordList.size(); i1++) {
            BlackList blackList = new BlackList();
            JSONObject record = (JSONObject) recordList.get(i1);
            if (record.getString("blacklistForeverFlag").equals("1")) {
                blackList.setBlackListForeverFlag("永久");
            } else {
                String validFrom = record.getString("validFrom");
                String validTo = record.getString("validTo");
                // 将数据格式化成:yyyy-mm-dd hh:mm:ss
                convertDateFormat(validFrom);
                convertDateFormat(validTo);
                blackList.setBlackListForeverFlag(validFrom + "-" + validTo);
            }
            blackList.setOwner(record.getString("owner"));
            blackList.setCarCode(record.getString("carCode"));
            blackList.setReason(record.getString("reason"));
            blackList.setRemark1(record.getString("remark1"));
            blackList.setRemark2(record.getString("remark2"));
            if (parkName.equals("万象上东")) {
                blackList.setParkName("万象上东");
            } else if (parkName.equals("四季上东")) {
                blackList.setParkName("四季上东");
            } else if (parkName.equals("欧洲新城")) {
                blackList.setParkName("欧洲新城");
            }
            blackList.setSpecialCarTypeConfigName(record.getString("specialCarTypeConfigName"));
            blackLists.add(blackList);
        }
        int n = 0;
        for (BlackList blackList : blackLists) {
            System.out.println("blackList = " + blackListService.findOne(blackList));
            // 针对欧洲新城：仅按 车场+车牌 去重
            if ("欧洲新城".equals(blackList.getParkName())) {
                if (!blackListService.existsByParkAndCar(blackList.getParkName(), blackList.getCarCode())) {
                    boolean save = blackListService.save(blackList);
                    if (save) {
                        n++;
                    }
                }
                continue;
            }
            if (blackListService.findOne(blackList).isEmpty()) {
                boolean save = blackListService.save(blackList);
                if (save) {
                    n++;
                }
            }
        }
        Result result = new Result();
        result.setMsg("更新成功！");
        result.setCode("0");
        result.setData(n);
        return ResponseEntity.ok(result);
    }

    /**
     * 分页查询
     *
     * @param parkName
     * @param carCode
     * @param pageNum
     * @param pageSize
     * @return
     */
    @ApiOperation("分页查询")
    @GetMapping("/page")
//    public IPage<BlackList> findPage(@RequestParam(required = false) String parkName,
    public IPage<BlackList> findPage(@RequestParam(required = false) String parkName,
                                     @RequestParam(required = false) String carCode,
                                     @RequestParam(required = false) String userName,
                                     @RequestParam(required = false) String specialCarTypeConfigName,
                                     @RequestParam(required = false) String blackReason,
                                     @RequestParam(required = false) String remark1,
                                     @RequestParam(required = false) String remark2,
                                     @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                     @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        // TODO 编写查询逻辑
        List<BlackList> blackListList = blackListService.queryInfoOnly(parkName, carCode, specialCarTypeConfigName, userName, blackReason, remark1, remark2);
        // 或者使用 Java 8 的 lambda 表达式
        blackListList.sort((o1, o2) -> Integer.compare(o2.getId(), o1.getId()));
        return PageUtils.getPage(blackListList, pageNum, pageSize);
    }

    /**
     * 字符串日期格式化
     * @param input
     * @return
     */
    public static String convertDateFormat(String input) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = inputFormat.parse(input);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return outputFormat.format(date);
    }

    /**
     * 格式化日日期
     *
     * @param input
     * @return
     * @throws ParseException
     * @throws IllegalArgumentException
     */
    public static String formatDateRange(String input) throws ParseException, IllegalArgumentException {
        // 去除输入字符串中的不必要空格
        if (input.equals("永久")) {
            return "永久";
        } else {
            input = input.replaceAll("[\\s:-]+", "");
//            System.out.println("input = " + input);
            // 在第15到16字符中间添加"-"
            String result = input.substring(0, 14) + "-" + input.substring(14);
            return result;
        }
    }

    /**
     * 移除黑名单
     *
     * @param parkCode
     * @param carNo
     * @param id 黑名单记录ID
     * @return
     */
    @ApiOperation("移除黑名单")
    @GetMapping("/removeBlackListCar")
    public ResponseEntity removeBlackListCar(String parkCode, String carNo, Integer id) {
        // 先清除相关的违规记录
        try {
            int deletedCount = violationsService.deleteViolationsByPlateAndPark(carNo, parkCode);
            System.out.println("已清除车牌号 " + carNo + " 在停车场 " + parkCode + " 的 " + deletedCount + " 条违规记录");
        } catch (Exception e) {
            System.err.println("清除车牌号 " + carNo + " 的违规记录时出错: " + e.getMessage());
        }
        
        HashMap<String, Object> params = new HashMap<>();
        //enterTime格式必须是yyyy-MM-dd HH:mm:ss
        params.put("parkCode", parkCode);
        params.put("carNo", carNo);
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "removeBlackListCar", params);
        
        // 如果提供了ID，则进行逻辑删除
        if (id != null) {
            String deleteBy = getCurrentUser();
            boolean success = blackListService.logicDeleteById(id, deleteBy);
            if (!success) {
                Result result = new Result();
                result.setMsg("本地删除失败！");
                result.setCode("1");
                result.setData(null);
                return ResponseEntity.ok(result);
            }
        }
        
        return ResponseEntity.ok(data);
    }

    @ApiOperation("批量移除黑名单")
    @PostMapping("/batchDelete")
    public ResponseEntity batchDelete(@RequestBody List<String> removeInfo, javax.servlet.http.HttpServletRequest request) {
        ArrayList<RemoveInfo> removeInfos = new ArrayList<>();
        ArrayList<Integer> ids = new ArrayList<>();
        StringBuilder carNos = new StringBuilder();
        for (String s : removeInfo) {
            RemoveInfo info = new RemoveInfo();
            // 将字符串利用"_"进行拆分
            String[] s1 = s.split("_");
            info.setCarNo(processCarNo(s1[0]));
            info.setParkCode(s1[1]);
            Integer id = Integer.parseInt(s1[2]);
            ids.add(id);
            removeInfos.add(info);
            if (carNos.length() > 0) carNos.append("、");
            carNos.append(info.getCarNo());
        }
        HashMap<String, String> hashMap = new HashMap<>();
        // 遍历removeInfos
        ArrayList<String> strings = new ArrayList<>();
        for (RemoveInfo info : removeInfos) {
            // 查询yardInfo数据库
            String parkCode = yardInfoService.selectParkCode(info.getParkCode());
            //enterTime格式必须是yyyy-MM-dd HH:mm:ss
            hashMap.put("parkCode", parkCode);
            hashMap.put("carNo", info.getCarNo());
//            String get = HttpClientUtil.doGet("http://www.xuerparking.cn:8543/parking/blackList/removeBlackListCar", hashMap);
            String get = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/blackList/removeBlackListCar", hashMap);
            JSONObject jsonObject = JSONObject.parseObject(get);
            JSONObject jsonObjectData = (JSONObject) jsonObject.get("data");
//            System.out.println("jsonObjectData = " + jsonObjectData);
            strings.add(info.getCarNo() + "--" + jsonObjectData.getString("message"));
        }
        
        // 在删除黑名单记录之前，先清除相关的违规记录
        for (RemoveInfo info : removeInfos) {
            try {
                int deletedCount = violationsService.deleteViolationsByPlateAndPark(info.getCarNo(), info.getParkCode());
                System.out.println("已清除车牌号 " + info.getCarNo() + " 在停车场 " + info.getParkCode() + " 的 " + deletedCount + " 条违规记录");
            } catch (Exception e) {
                System.err.println("清除车牌号 " + info.getCarNo() + " 的违规记录时出错: " + e.getMessage());
            }
        }
        
        // 改为逻辑删除
        String deleteBy = getCurrentUser();
        boolean success = blackListService.logicDeleteByIds(ids, deleteBy);
        
        Result result = new Result();
        if (success) {
            result.setMsg("批量删除成功！");
            result.setCode("0");
            result.setData(strings);
            
            // 📝 记录操作日志
            User currentUser = getCurrentUser(request);
            String username = currentUser != null && currentUser.getLoginName() != null 
                            ? currentUser.getLoginName() 
                            : (currentUser != null && currentUser.getUserName() != null 
                                ? currentUser.getUserName() 
                                : "未知用户");
            String description = removeInfos.size() > 1 
                ? String.format("用户 %s 批量移除了 %d 个车牌的黑名单：%s", 
                              username, removeInfos.size(), 
                              carNos.length() > 100 ? carNos.substring(0, 97) + "..." : carNos.toString())
                : String.format("用户 %s 移除了车牌的黑名单：%s", 
                              username, carNos.toString());
            recordOperation(request, "黑名单管理", "移除黑名单", description);
        } else {
            result.setMsg("批量删除失败！");
            result.setCode("1");
            result.setData(null);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 处理车牌号码，移除特殊字符
     * @param carNo 原始车牌号码
     * @return 处理后的车牌号码
     */
    /**
     * 分页查询已删除的黑名单记录
     *
     * @param parkName
     * @param carCode
     * @param pageNum
     * @param pageSize
     * @return
     */
    @ApiOperation("分页查询已删除的黑名单记录")
    @GetMapping("/deletedPage")
    public IPage<BlackList> findDeletedPage(@RequestParam(required = false) String parkName,
                                           @RequestParam(required = false) String carCode,
                                           @RequestParam(required = false) String userName,
                                           @RequestParam(required = false) String specialCarTypeConfigName,
                                           @RequestParam(required = false) String blackReason,
                                           @RequestParam(required = false) String remark1,
                                           @RequestParam(required = false) String remark2,
                                           @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                           @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        // 查询已删除的记录
        List<BlackList> blackListList = blackListService.queryDeletedInfoOnly(parkName, carCode, specialCarTypeConfigName, userName, blackReason, remark1, remark2);
        // 按ID倒序排列
        blackListList.sort((o1, o2) -> Integer.compare(o2.getId(), o1.getId()));
        return PageUtils.getPage(blackListList, pageNum, pageSize);
    }

    /**
     * 恢复已删除的黑名单记录
     *
     * @param id
     * @return
     */
    @ApiOperation("恢复已删除的黑名单记录")
    @PostMapping("/restore/{id}")
    public ResponseEntity restoreBlackList(@PathVariable Integer id) {
        boolean success = blackListService.restoreById(id);
        Result result = new Result();
        if (success) {
            result.setMsg("恢复成功！");
            result.setCode("0");
        } else {
            result.setMsg("恢复失败！");
            result.setCode("1");
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 获取当前操作用户
     * @return
     */
    private String getCurrentUser() {
        // 这里可以从session、JWT token或其他方式获取当前用户信息
        // 示例：从localStorage中获取用户名（实际应用中应该从后端session或token中获取）
        String username = "管理员"; // 默认值
        try {
            // 可以从请求头中获取用户信息
            // String token = request.getHeader("Authorization");
            // 解析token获取用户信息...
        } catch (Exception e) {
            // 如果获取失败，使用默认值
        }
        return username;
    }

    private String processCarNo(String carNo) {
        if (carNo == null || carNo.trim().isEmpty()) {
            return carNo;
        }
        // 移除车牌号码中的特殊字符，如 ●
        return carNo.replace("●", "").trim();
    }

    /**
     * 根据车牌号查询车主信息
     * @param carCode 车牌号
     * @param parkName 车场名称
     * @return 车主信息
     */
    @ApiOperation("根据车牌号查询车主信息")
    @GetMapping("/getCarOwnerByPlate")
    public ResponseEntity getCarOwnerByPlate(@RequestParam String carCode, @RequestParam(required = false) String parkName) {
        try {
            System.out.println("🔍 [查询车主信息] 车牌号: " + carCode + ", 车场: " + parkName);
            
            // 查询month_tick表获取车主信息
            QueryWrapper<MonthTick> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("car_no", carCode);
            if (parkName != null && !parkName.trim().isEmpty()) {
                queryWrapper.eq("park_name", parkName);
            }
            
            MonthTick monthTick = monthTicketService.getOne(queryWrapper);
            
            if (monthTick != null) {
                Map<String, Object> ownerInfo = new HashMap<>();
                ownerInfo.put("carCode", monthTick.getCarNo());
                ownerInfo.put("carOwner", monthTick.getUserName());
                ownerInfo.put("userPhone", monthTick.getUserPhone());
                ownerInfo.put("parkName", monthTick.getParkName());
                ownerInfo.put("ticketName", monthTick.getTicketName());
                ownerInfo.put("validStatus", monthTick.getValidStatus());
                ownerInfo.put("isFrozen", monthTick.getIsFrozen());
                
                System.out.println("✅ [查询车主信息] 找到车主: " + monthTick.getUserName());
                return ResponseEntity.ok(Result.success(ownerInfo));
            } else {
                System.out.println("⚠️ [查询车主信息] 未找到车牌号对应的车主信息");
                return ResponseEntity.ok(Result.error("未找到车牌号对应的车主信息"));
            }
            
        } catch (Exception e) {
            System.err.println("❌ [查询车主信息] 失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Result.error("查询车主信息失败: " + e.getMessage()));
        }
    }

    /**
     * 批量查询车牌号对应的车主信息
     * @return 车主信息列表
     */
    @ApiOperation("批量查询车牌号对应的车主信息")
    @PostMapping("/batchGetCarOwnerByPlate")
    public ResponseEntity batchGetCarOwnerByPlate(@RequestBody Map<String, Object> requestData) {
        try {
            @SuppressWarnings("unchecked")
            List<String> carCodes = (List<String>) requestData.get("carCodes");
            String parkName = (String) requestData.get("parkName");
            
            System.out.println("🔍 [批量查询车主信息] 车牌号数量: " + carCodes.size() + ", 车场: " + parkName);
            
            List<Map<String, Object>> ownerInfoList = new ArrayList<>();
            
            for (String carCode : carCodes) {
                QueryWrapper<MonthTick> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("car_no", carCode);
                if (parkName != null && !parkName.trim().isEmpty()) {
                    queryWrapper.eq("park_name", parkName);
                }
                
                MonthTick monthTick = monthTicketService.getOne(queryWrapper);
                
                Map<String, Object> ownerInfo = new HashMap<>();
                ownerInfo.put("carCode", carCode);
                
                if (monthTick != null) {
                    ownerInfo.put("carOwner", monthTick.getUserName());
                    ownerInfo.put("userPhone", monthTick.getUserPhone());
                    ownerInfo.put("parkName", monthTick.getParkName());
                    ownerInfo.put("ticketName", monthTick.getTicketName());
                    ownerInfo.put("validStatus", monthTick.getValidStatus());
                    ownerInfo.put("isFrozen", monthTick.getIsFrozen());
                    ownerInfo.put("found", true);
                } else {
                    ownerInfo.put("carOwner", "未知车主");
                    ownerInfo.put("userPhone", "");
                    ownerInfo.put("parkName", parkName);
                    ownerInfo.put("ticketName", "");
                    ownerInfo.put("validStatus", 0);
                    ownerInfo.put("isFrozen", 0);
                    ownerInfo.put("found", false);
                }
                
                ownerInfoList.add(ownerInfo);
            }
            
            System.out.println("✅ [批量查询车主信息] 完成，返回" + ownerInfoList.size() + "条记录");
            return ResponseEntity.ok(Result.success(ownerInfoList));
            
        } catch (Exception e) {
            System.err.println("❌ [批量查询车主信息] 失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Result.error("批量查询车主信息失败: " + e.getMessage()));
        }
    }

    /**
     * 基于外部API查询结果清理多余的黑名单记录
     * 对比本地与外部API数据，删除本地存在但外部API中不存在的多余记录
     * 
     * @param parkName 车场名称
     * @return 清理结果统计
     */
    @ApiOperation("基于外部API查询结果清理多余的黑名单记录")
    @GetMapping("/autoDeleteByExternalApi")
    public ResponseEntity autoDeleteByExternalApi(@RequestParam String parkName) {
        try {
            System.out.println("🔍 [数据清理检查] 开始检查车场: " + parkName + " 的黑名单记录");
            
            // 获取车场代码
            String parkCode = getParkCodeByName(parkName);
            if (parkCode == null) {
                return ResponseEntity.ok(Result.error("未找到车场代码: " + parkName));
            }
            
            // 1. 查询本地黑名单记录
            List<BlackList> localBlackList = blackListService.queryInfoOnly(parkName, null, null, null, null, null, null);
            System.out.println("📋 [数据清理检查] 本地黑名单记录数量: " + localBlackList.size());
            
            if (localBlackList.isEmpty()) {
                return ResponseEntity.ok(Result.success("本地无黑名单记录，无需检查"));
            }
            
            // 2. 查询外部API黑名单记录
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("parkCodeList", parkCode);
            String get = HttpClientUtil.doPost("https://www.xuerparking.cn:8543/parking/blackList/getParkBlackList", hashMap);
            JSONObject jsonObject = JSONObject.parseObject(get);
            JSONObject jsonObjectData = (JSONObject) jsonObject.get("data");
            
            if (jsonObjectData == null || !jsonObjectData.containsKey("data")) {
                System.err.println("❌ [数据清理检查] 外部API返回数据格式异常");
                return ResponseEntity.ok(Result.error("外部API返回数据格式异常"));
            }
            
            JSONObject jsonObjectDataData = (JSONObject) jsonObjectData.get("data");
            JSONArray externalRecordList = (JSONArray) jsonObjectDataData.get("recordList");
            
            // 3. 构建外部API车牌号集合
            Set<String> externalCarCodes = new HashSet<>();
            if (externalRecordList != null) {
                for (int i = 0; i < externalRecordList.size(); i++) {
                    JSONObject record = (JSONObject) externalRecordList.get(i);
                    String carCode = record.getString("carCode");
                    if (carCode != null && !carCode.trim().isEmpty()) {
                        externalCarCodes.add(carCode.trim());
                    }
                }
            }
            
            System.out.println("🌐 [数据清理检查] 外部API黑名单记录数量: " + externalCarCodes.size());
            
            // 4. 找出需要清理的多余记录（本地有但外部API没有）
            List<BlackList> toDeleteList = new ArrayList<>();
            for (BlackList localRecord : localBlackList) {
                if (!externalCarCodes.contains(localRecord.getCarCode())) {
                    toDeleteList.add(localRecord);
                }
            }
            
            System.out.println("🗑️ [数据清理检查] 需要清理的多余记录数量: " + toDeleteList.size());
            
            if (toDeleteList.isEmpty()) {
                return ResponseEntity.ok(Result.success("所有本地记录在外部API中都存在，无需清理"));
            }
            
            // 5. 执行清理操作
            int deletedCount = 0;
            int violationDeletedCount = 0;
            List<String> deletedCars = new ArrayList<>();
            
            for (BlackList record : toDeleteList) {
                try {
                    // 清理违规记录
                    int violationCount = violationsService.deleteViolationsByPlateAndPark(record.getCarCode(), parkName);
                    violationDeletedCount += violationCount;
                    System.out.println("🧹 [数据清理] 清理车牌 " + record.getCarCode() + " 的 " + violationCount + " 条违规记录");
                    
                    // 清理黑名单记录（逻辑删除）
                    String deleteBy = getCurrentUser();
                    boolean success = blackListService.logicDeleteById(record.getId(), deleteBy);
                    
                    if (success) {
                        deletedCount++;
                        deletedCars.add(record.getCarCode());
                        System.out.println("✅ [数据清理] 成功清理黑名单记录: " + record.getCarCode());
                    } else {
                        System.err.println("❌ [数据清理] 清理黑名单记录失败: " + record.getCarCode());
                    }
                    
                } catch (Exception e) {
                    System.err.println("❌ [数据清理] 清理记录时出错: " + record.getCarCode() + ", 错误: " + e.getMessage());
                }
            }
            
            // 6. 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("parkName", parkName);
            result.put("localRecordCount", localBlackList.size());
            result.put("externalRecordCount", externalCarCodes.size());
            result.put("deletedBlackListCount", deletedCount);
            result.put("deletedViolationCount", violationDeletedCount);
            result.put("deletedCars", deletedCars);
            
            String message = String.format("数据清理完成！清理了 %d 条多余的黑名单记录和 %d 条违规记录", 
                deletedCount, violationDeletedCount);
            
            System.out.println("✅ [数据清理检查] " + message);
            Result<Map<String, Object>> resultObj = new Result<>();
            resultObj.setCode("0");
            resultObj.setMsg(message);
            resultObj.setData(result);
            return ResponseEntity.ok(resultObj);
            
        } catch (Exception e) {
            System.err.println("❌ [数据清理检查] 执行失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Result.error("数据清理检查失败: " + e.getMessage()));
        }
    }

    /**
     * 根据车场名称获取车场代码
     * @param parkName 车场名称
     * @return 车场代码
     */
    private String getParkCodeByName(String parkName) {
        if ("万象上东".equals(parkName)) {
            return "2KST9MNP";
        } else if ("四季上东".equals(parkName)) {
            return "2KUG6XLU";
        } else if ("欧洲新城".equals(parkName)) {
            return "2KPL6XFF";
        }
        return null;
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
            activityLog.setCreatedAt(java.time.LocalDateTime.now());
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

