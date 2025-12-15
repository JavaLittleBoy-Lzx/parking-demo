package com.parkingmanage.controller;


import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.common.HttpClientUtil;
import com.parkingmanage.common.Result;
import com.parkingmanage.common.config.AIKEConfig;
import com.parkingmanage.entity.*;
import com.parkingmanage.service.BlackListService;
import com.parkingmanage.service.MonthTicketService;
import com.parkingmanage.service.YardInfoService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.ParseException;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;
import java.util.*;
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
@RequestMapping("/parking/monthTicket")
public class MonthTicketController {

    @Autowired
    public AIKEConfig aikeConfig;

    @Resource
    private MonthTicketService monthTicketService;
    ;

    /**
     * 查询月票列表
     *
     * @param parkCodeList
     * @return
     */
    @ApiOperation("查询月票列表")
    @RequestMapping("/getOnlineMonthTicketList")
    public ResponseEntity getOnlineMonthTicketList(String parkCodeList, String pageNum, String pageSize,String validStatus) {
        HashMap<String, Object> params = new HashMap<>();
        //enterTime格式必须是yyyy-MM-dd HH:mm:ss
//        System.out.println("parkCodeList = " + parkCodeList);
        
        // 修复parkCodeList参数处理
        if (StringUtils.isNotBlank(parkCodeList)) {
        params.put("parkCodeList", Arrays.asList(parkCodeList));
        } else {
            // 如果没有指定车场，可以传空列表或者不传这个参数
            params.put("parkCodeList", new ArrayList<>());
        }
        
        params.put("pageNum", pageNum);
        params.put("pageSize", pageSize);
        params.put("validStatus", Integer.parseInt(validStatus));
        
        System.out.println("🔍 调用外部API参数: " + params);
        
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "getOnlineMonthTicketList", params);
        return ResponseEntity.ok(data);
    }

    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<MonthTick> findPages(@RequestParam(required = false) String parkName,
                                      @RequestParam(required = false) String carNo,
                                      @RequestParam(required = false) String ticketName,
                                      @RequestParam(required = false) String userName,
                                      @RequestParam(required = false) String timePeriodList,
                                      @RequestParam(required = false) String userPhone,
                                      @RequestParam(required = false) Integer timeDays,
                                      @RequestParam(required = false) String remark1, @RequestParam(required = false) String remark2,
                                      @RequestParam(required = false) String remark3,
                                      @RequestParam(required = false) Integer isValid,
                                      @RequestParam(required = false) Integer isFrozen,
                                      @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                      @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        // TODO 编写查询逻辑
        List<MonthTick> monthTickPage = monthTicketService.queryInfoOnly(parkName, carNo, ticketName, userName, timeDays, timePeriodList, userPhone, remark1, remark2, remark3, isFrozen, isValid);
        List<MonthTick> asServices = monthTickPage.stream().sorted(Comparator.comparing(MonthTick::getCarNo)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }

    /**
     * 查询车场开通的月票名称
     *
     * @param parkCodeList
     * @return
     */
    @ApiOperation("查询月票列表")
    @GetMapping("/getMonthTicketConfigDetailList")
    public ResponseEntity getMonthTicketConfigDetailList(String parkCodeList) {
        HashMap<String, Object> params = new HashMap<>();
//        System.out.println("parkCodeList = " + parkCodeList);
        params.put("parkCodeList", Arrays.asList(parkCodeList));
        params.put("pageNum", 1);
        params.put("pageSize", 100);
//        System.out.println("params = " + params);
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "getMonthTicketConfigDetailList", params);
        return ResponseEntity.ok(data);
    }

    /**
     * 调用接口查询更新数据
     *
     * @param parkName
     * @return
     */
    @ApiOperation("分页查询")
    @GetMapping("/AKEPage")
    public ResponseEntity findPage(@RequestParam(required = false) String parkName,@RequestParam(required = false) String validStatus) {
        // validStatus：1(生效)、4(过期)
        // 调用接口查询车场名称
        HashMap<String, String> hashMap = new HashMap<>();
        ArrayList<String> parkCodeLists = new ArrayList<>();
        parkCodeLists.add("2KST9MNP");
        parkCodeLists.add("2KUG6XLU");
        parkCodeLists.add("2KPL6XFF");
        if (parkName.equals("万象上东")) {
            hashMap.put("parkCodeList", "2KST9MNP");
        } else if (parkName.equals("四季上东")) {
            hashMap.put("parkCodeList", "2KUG6XLU");
        }else if (parkName.equals("欧洲新城")) {
            hashMap.put("parkCodeList", "2KPL6XFF");
        }
        hashMap.put("pageSize", "100");
        hashMap.put("validStatus", validStatus);
        System.out.println("hashMap = " + hashMap);
//        String get = HttpClientUtil.doGet("http://www.xuerparking.cn:8543/parking/monthTicket/getOnlineMonthTicketList", hashMap);
        String get = HttpClientUtil.doPost("https://www.xuerparking.cn:8543/parking/monthTicket/getOnlineMonthTicketList", hashMap);
        JSONObject jsonObject = JSONObject.parseObject(get);
        System.out.println("jsonObject = " + jsonObject);
        // 将查询到的data中的total进行计算还需要轮询多少次
        JSONObject data1 = (JSONObject) jsonObject.get("data");
        JSONObject data2 = (JSONObject) data1.get("data");
        Integer total = data2.getInteger("total");
//        System.out.println("total = " + total);
        JSONArray recordList = data2.getJSONArray("recordList");
        ArrayList<MonthTick> monthTicks = new ArrayList<>();
        for (int i = 0; i < recordList.size(); i++) {
            JSONObject jsonObject1 = recordList.getJSONObject(i);
            // 存储到集合中
            MonthTick monthTick = new MonthTick();
            monthTick.setCarNo(processCarNo(jsonObject1.getString("carNo")));
            monthTick.setCreateTime(jsonObject1.getString("createTime"));
            monthTick.setCreateBy(jsonObject1.getString("createBy"));
            monthTick.setTicketName(jsonObject1.getString("ticketName"));
            if (jsonObject1.getInteger("carNoNum") == null) {
                monthTick.setCarNoNum(-1);
            } else {
                monthTick.setCarNoNum(jsonObject1.getInteger("carNoNum"));
            }
            if (jsonObject1.getString("userPhone") == null) {
                monthTick.setUserPhone("11111111111");
            } else {
                monthTick.setUserPhone(jsonObject1.getString("userPhone"));
            }
            monthTick.setRemark1(jsonObject1.getString("remark1"));
            monthTick.setRemark2(jsonObject1.getString("remark2"));
            monthTick.setRemark3(jsonObject1.getString("remark3"));
            monthTick.setIsFrozen(jsonObject1.getInteger(("isFrozen")));
            monthTick.setUserName(jsonObject1.getString("userName"));
            monthTick.setUserName(jsonObject1.getString("userName"));
            monthTick.setValidStatus(jsonObject1.getInteger("validStatus"));
            JSONArray timePeriodList1 = jsonObject1.getJSONArray("timePeriodList");
            ArrayList<TimePeriodList> timePeriodLists = new ArrayList<>();
            for (int i2 = 0; i2 < timePeriodList1.size(); i2++) {
                JSONObject jsonObjectTime = timePeriodList1.getJSONObject(i2);
                TimePeriodList timePeriodListParams = new TimePeriodList();
                timePeriodListParams.setStartTime(convertDateFormat(jsonObjectTime.getString("startTime")));
                timePeriodListParams.setEndTime(convertDateFormat(jsonObjectTime.getString("endTime")));
//                System.out.println("timePeriodListParams = " + timePeriodListParams);
                timePeriodLists.add(timePeriodListParams);
            }
            // 将timePeriodLists中的对象转为字符串
            StringBuilder str = new StringBuilder();
            for (int j = 0; j < timePeriodLists.size(); j++) {
                str.append(timePeriodLists.get(j).toString());
                if (j < timePeriodLists.size() - 1) {
                    str.append(",");
                }
            }
            monthTick.setTimePeriodList(str.toString());
            // 添加车场名称
            if (parkName.equals("万象上东")) {
                monthTick.setParkName("万象上东");
            } else if (parkName.equals("四季上东")) {
                monthTick.setParkName("四季上东");
            }else if (parkName.equals("欧洲新城")) {
                monthTick.setParkName("欧洲新城");
            }
            monthTicks.add(monthTick);
        }

        // 计算还需要轮询的次数
        int n = total / 100;
        int remainder = total % 100;
        if (remainder != 0) {
            // 还需要的轮询次数
            for (int i = 2; i <= (n + 1); i++) {
                HashMap<String, String> hashMapOut = new HashMap<>();
                if (parkName.equals("万象上东")) {
                    hashMapOut.put("parkCodeList", "2KST9MNP");
                } else if (parkName.equals("四季上东")) {
                    hashMapOut.put("parkCodeList", "2KUG6XLU");
                }else if (parkName.equals("欧洲新城")) {
                    hashMapOut.put("parkCodeList", "2KPL6XFF");
                }
                hashMapOut.put("pageNum", String.valueOf(i));
                hashMapOut.put("pageSize", "100");
                hashMapOut.put("validStatus", validStatus);
//                String getIn = HttpClientUtil.doPost("http://www.xuerparking.cn:8543/parking/monthTicket/getOnlineMonthTicketList", hashMapOut);
                String getIn = HttpClientUtil.doPost("https://www.xuerparking.cn:8543/parking/monthTicket/getOnlineMonthTicketList", hashMapOut);
                JSONObject jsonObjectIn = JSONObject.parseObject(getIn);
                JSONObject dataInfo = (JSONObject) jsonObjectIn.get("data");
                JSONObject dataInfo1 = (JSONObject) dataInfo.get("data");
                JSONArray recordOutList = (JSONArray) dataInfo1.get("recordList");
                for (int i1 = 0; i1 < recordOutList.size(); i1++) {
                    JSONObject jsonObject1 = recordOutList.getJSONObject(i1);
                    // 存储到集合中
                    MonthTick monthTick = new MonthTick();
                    monthTick.setCarNo(processCarNo(jsonObject1.getString("carNo")));
                    monthTick.setCreateTime(jsonObject1.getString("createTime"));
                    monthTick.setCreateBy(jsonObject1.getString("createBy"));
                    monthTick.setTicketName(jsonObject1.getString("ticketName"));
                    if (jsonObject1.getInteger("carNoNum") == null) {
                        monthTick.setCarNoNum(-1);
                    } else {
                        monthTick.setCarNoNum(jsonObject1.getInteger("carNoNum"));
                    }
                    monthTick.setUserPhone(jsonObject1.getString("userPhone"));
                    monthTick.setRemark1(jsonObject1.getString("remark1"));
                    monthTick.setRemark2(jsonObject1.getString("remark2"));
                    monthTick.setRemark3(jsonObject1.getString("remark3"));
                    monthTick.setIsFrozen(jsonObject1.getInteger(("isFrozen")));
                    monthTick.setUserName(jsonObject1.getString("userName"));
                    monthTick.setUserName(jsonObject1.getString("userName"));
                    monthTick.setValidStatus(jsonObject1.getInteger("validStatus"));
                    JSONArray timePeriodList1 = jsonObject1.getJSONArray("timePeriodList");
                    ArrayList<TimePeriodList> timePeriodListIn = new ArrayList<>();
                    for (int i2 = 0; i2 < timePeriodList1.size(); i2++) {
                        JSONObject jsonObjectInData = timePeriodList1.getJSONObject(i2);
                        TimePeriodList timePeriodListInData = new TimePeriodList();
                        timePeriodListInData.setStartTime(convertDateFormat(jsonObjectInData.getString("startTime")));
                        timePeriodListInData.setEndTime(convertDateFormat(jsonObjectInData.getString("endTime")));
                        timePeriodListIn.add(timePeriodListInData);
//                        System.out.println("timePeriodListParams = " + timePeriodListInData);
                    }
                    StringBuilder strIn = new StringBuilder();
                    for (int j = 0; j < timePeriodListIn.size(); j++) {
                        strIn.append(timePeriodListIn.get(j).toString());
                        if (j < timePeriodListIn.size() - 1) {
                            strIn.append(",");
                        }
                    }
//                    System.out.println("strIn = " + strIn);
                    monthTick.setTimePeriodList(strIn.toString());
                    // 添加车场名称
                    if (parkName.equals("万象上东")) {
                        monthTick.setParkName("万象上东");
                    } else if (parkName.equals("四季上东")) {
                        monthTick.setParkName("四季上东");
                    } else if (parkName.equals("欧洲新城")) {
                        monthTick.setParkName("欧洲新城");
                    }
                    monthTicks.add(monthTick);
                }
            }
        } else {
            for (int i = 2; i <= n; i++) {
                HashMap<String, String> hashMapElse = new HashMap<>();
                if (parkName.equals("万象上东")) {
                    hashMapElse.put("parkCodeList", "2KST9MNP");
                } else if (parkName.equals("四季上东")) {
                    hashMapElse.put("parkCodeList", "2KUG6XLU");
                } else if (parkName.equals("欧洲新城")) {
                    hashMapElse.put("parkCodeList", "2KPL6XFF");
                }
                hashMapElse.put("pageNum", String.valueOf(i));
                hashMapElse.put("pageSize", "100");
                hashMapElse.put("validStatus", validStatus);
//                String getElse = HttpClientUtil.doPost("http://www.xuerparking.cn:8543/parking/monthTicket/getOnlineMonthTicketList", hashMapElse);
                String getElse = HttpClientUtil.doPost("https://www.xuerparking.cn:8543/parking/monthTicket/getOnlineMonthTicketList", hashMapElse);
                JSONObject jsonObjectElse = JSONObject.parseObject(getElse);
                JSONObject dataInfoElse = (JSONObject) jsonObjectElse.get("data");
                JSONObject dataInfoElse1 = (JSONObject) dataInfoElse.get("data");
                JSONArray recordOutList = (JSONArray) dataInfoElse1.get("recordList");
                for (int i1 = 0; i1 < recordOutList.size(); i1++) {
                    JSONObject jsonObject1 = recordOutList.getJSONObject(i1);
                    // 存储到集合中
                    MonthTick monthTick = new MonthTick();
                    monthTick.setCarNo(processCarNo(jsonObject1.getString("carNo")));
                    monthTick.setCreateTime(jsonObject1.getString("createTime"));
                    monthTick.setCreateBy(jsonObject1.getString("createBy"));
                    monthTick.setTicketName(jsonObject1.getString("ticketName"));
                    monthTick.setCarNoNum(jsonObject1.getInteger("carNoNum"));
                    monthTick.setRemark1(jsonObject1.getString("remark1"));
                    monthTick.setRemark2(jsonObject1.getString("remark2"));
                    monthTick.setRemark3(jsonObject1.getString("remark3"));
                    monthTick.setUserPhone(jsonObject1.getString("userPhone"));
                    monthTick.setIsFrozen(jsonObject1.getInteger(("isFrozen")));
                    monthTick.setUserName(jsonObject1.getString("userName"));
                    monthTick.setUserName(jsonObject1.getString("userName"));
                    monthTick.setValidStatus(jsonObject1.getInteger("validStatus"));
                    JSONArray timePeriodList1 = jsonObject1.getJSONArray("timePeriodList");
                    ArrayList<TimePeriodList> timePeriodLists1 = new ArrayList<>();
                    for (int i2 = 0; i2 < timePeriodList1.size(); i2++) {
                        JSONObject jsonObjectELse = timePeriodList1.getJSONObject(i2);
                        TimePeriodList timePeriodListELse = new TimePeriodList();
                        timePeriodListELse.setStartTime(convertDateFormat(jsonObjectELse.getString("startTime")));
                        timePeriodListELse.setEndTime(convertDateFormat(jsonObjectELse.getString("endTime")));
                        timePeriodLists1.add(timePeriodListELse);
//                        System.out.println("timePeriodListELse = " + timePeriodListELse);
                    }
                    StringBuilder strData = new StringBuilder();
                    for (int j = 0; j < timePeriodLists1.size(); j++) {
                        strData.append(timePeriodLists1.get(j).toString());
                        if (j < timePeriodLists1.size() - 1) {
                            strData.append(",");
                        }
                    }
//                    System.out.println("strData = " + strData);
//                    System.out.println("timePeriodListsElse = " + timePeriodLists1);
                    monthTick.setTimePeriodList(strData.toString());
                    // 添加车场名称
                    if (parkName.equals("万象上东")) {
                        monthTick.setParkName("万象上东");
                    } else if (parkName.equals("四季上东")) {
                        monthTick.setParkName("四季上东");
                    }else if (parkName.equals("欧洲新城")) {
                        monthTick.setParkName("欧洲新城");
                    }
                    monthTicks.add(monthTick);
                }
            }
        }
        int updateNum = 0;
        int InsertNum = 0;
        // 将这个列表批量添加进数据库中
        for (MonthTick monthTick : monthTicks) {
            if (monthTicketService.findOne(monthTick).isEmpty()) {
                boolean save = monthTicketService.save(monthTick);
                if (save) {
                    InsertNum++;
                }
            } else {
                boolean b = monthTicketService.updateById(monthTick);
                if (b) {
                    updateNum++;
                }
            }
        }
        Result result = new Result();
        result.setMsg("数据调用成功！");
        result.setCode("0");
        result.setData(InsertNum);
        return ResponseEntity.ok(result);
    }

    /**
     * 日期转换
     *
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
        input = input.replaceAll("[\\s:-]+", "");
//        System.out.println("input = " + input);
        // 在第15到16字符中间添加"-"
        String result = input.substring(0, 14) + "-" + input.substring(14);
        return result;
    }

    /**
     * 移除黑名单
     * @param parkCode
     * @param carNo
     * @return
     */
    @ApiOperation("移除黑名单")
    @GetMapping("/removeBlackListCar")
    public ResponseEntity removeBlackListCar(String parkCode, String carNo) {
        HashMap<String, Object> params = new HashMap<>();
        //enterTime格式必须是yyyy-MM-dd HH:mm:ss
        params.put("parkCode", parkCode);
        params.put("carNo", carNo);
//        System.out.println("params = " + params);
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "removeBlackListCar", params);
        return ResponseEntity.ok(data);
    }

    /**
     * 本地数据搜索接口 - 直接查询month_tick表
     * @param keyword 车牌号关键词
     * @param parkName 车场名称
     * @param page 页码
     * @param size 每页数量
     * @return
     */
    @ApiOperation("本地数据搜索")
    @RequestMapping(value = "/searchLocalData", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity searchLocalData(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String parkName, 
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        try {

            System.out.println("🔍 [本地搜索] 参数: keyword=" + keyword + ", parkName=" + parkName + ", page=" + page + ", size=" + size);

            long startTime = System.currentTimeMillis();
            
            // 构建查询条件
            QueryWrapper<MonthTick> queryWrapper = new QueryWrapper<>();
            if (StringUtils.isNotBlank(keyword)) {
                queryWrapper.like("car_no", keyword);
            }
            if (StringUtils.isNotBlank(parkName)) {
                queryWrapper.eq("park_name", parkName);
            }
            
            // 分页查询
            Page<MonthTick> pageObj = new Page<>(page, size);
            IPage<MonthTick> resultPage = monthTicketService.page(pageObj, queryWrapper);
            
            long searchTime = System.currentTimeMillis() - startTime;
            
            // 转换数据格式，将MonthTick实体转换为前端期望的格式
            List<Map<String, Object>> convertedRecords = resultPage.getRecords().stream()
                .map(monthTick -> {
                    Map<String, Object> record = new HashMap<>();
                    record.put("plateNumber", monthTick.getCarNo()); // carNo -> plateNumber
                    record.put("ownerName", monthTick.getUserName()); // userName -> ownerName
                    record.put("ownerPhone", monthTick.getUserPhone()); // userPhone -> ownerPhone
                    record.put("ownerId", monthTick.getId()); // 使用月票ID作为ownerId
                    record.put("monthTicketId", monthTick.getId());
                    record.put("ticketName", monthTick.getTicketName());
                    record.put("parkingSpot", monthTick.getDynamicCarportNumber() > 0 ? "动态车位" + monthTick.getDynamicCarportNumber() : null);
                    record.put("validStatus", monthTick.getValidStatus());
                    record.put("isFrozen", monthTick.getIsFrozen());
                    record.put("isInPark", false); // 默认不在场，可以后续扩展
                    record.put("appointmentCount", 0); // 预约次数，可以后续查询
                    record.put("violationCount", 0); // 违规次数，可以后续查询
                    record.put("creditScore", 100); // 默认信用分，可以后续查询
                    record.put("createTime", monthTick.getCreateTime());
                    record.put("updateTime", monthTick.getUpdateTime());
                    record.put("remark", monthTick.getRemark1());
                    return record;
                })
                .collect(Collectors.toList());
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("records", convertedRecords); // 使用转换后的数据
            responseData.put("total", resultPage.getTotal());
            responseData.put("page", page);
            responseData.put("size", size);
            responseData.put("hasMore", page * size < resultPage.getTotal());
            responseData.put("searchTime", searchTime);
            
            System.out.println("✅ [本地搜索] 完成: 找到" + resultPage.getTotal() + "条记录，耗时" + searchTime + "ms");
            System.out.println("🔄 [本地搜索] 数据转换完成，返回" + convertedRecords.size() + "条格式化记录");
            
            Result result = Result.success(responseData);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.err.println("❌ [本地搜索] 失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Result.error("本地搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 检查车场数据是否存在
     * @param parkName 车场名称
     * @return
     */
    @ApiOperation("检查车场数据是否存在")
    @GetMapping("/checkParkDataExists")
    public ResponseEntity checkParkDataExists(@RequestParam String parkName) {
        try {
            System.out.println("🔍 [检查车场数据] 车场: " + parkName);
            
            QueryWrapper<MonthTick> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("park_name", parkName);
            
            long count = monthTicketService.count(queryWrapper);
            boolean exists = count > 0;
            
            // 获取最后更新时间（如果有数据）
            String lastUpdateTime = null;
            if (exists) {
                queryWrapper.orderByDesc("update_time").last("LIMIT 1");
                MonthTick latestRecord = monthTicketService.getOne(queryWrapper);
                if (latestRecord != null && latestRecord.getUpdateTime() != null) {
                    lastUpdateTime = latestRecord.getUpdateTime();
                }
            }
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("exists", exists);
            responseData.put("count", count);
            responseData.put("lastUpdateTime", lastUpdateTime);
            
            System.out.println("✅ [检查车场数据] 结果: exists=" + exists + ", count=" + count);
            
            return ResponseEntity.ok(Result.success(responseData));
            
        } catch (Exception e) {
            System.err.println("❌ [检查车场数据] 失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Result.error("检查车场数据失败: " + e.getMessage()));
        }
    }

    /**
     * 批量导入车场数据
     * @param requestParams 包含parkName和forceUpdate参数
     * @return
     */
    @ApiOperation("批量导入车场数据")
    @PostMapping("/batchImportParkData")
    public ResponseEntity batchImportParkData(@RequestBody Map<String, Object> requestParams) {
        try {
            String parkName = (String) requestParams.get("parkName");
            Boolean forceUpdate = requestParams.get("forceUpdate") != null ? (Boolean) requestParams.get("forceUpdate") : false;
            
            System.out.println("📥 [批量导入] 开始: parkName=" + parkName + ", forceUpdate=" + forceUpdate);
            
            long startTime = System.currentTimeMillis();
            
            // 检查是否已存在数据
            if (!forceUpdate) {
                QueryWrapper<MonthTick> existsWrapper = new QueryWrapper<>();
                existsWrapper.eq("park_name", parkName);
                long existsCount = monthTicketService.count(existsWrapper);
                if (existsCount > 0) {
                    System.out.println("⚠️ [批量导入] 车场数据已存在，跳过导入");
                    
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("totalImported", 0);
                    responseData.put("newRecords", 0);
                    responseData.put("updatedRecords", 0);
                    responseData.put("skippedRecords", (int) existsCount);
                    responseData.put("importTime", 0);
                    responseData.put("success", true);
                    responseData.put("message", "数据已存在，跳过导入");
                    
                    return ResponseEntity.ok(Result.success(responseData));
                }
            }
            
            // 调用现有的AKEPage方法逻辑来导入数据
            // 这里复用现有的导入逻辑，但是优化为通用方法
            int[] importResult = importParkDataFromExternalAPI(parkName);
            int newRecords = importResult[0];
            int updatedRecords = importResult[1];
            
            long importTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("totalImported", newRecords + updatedRecords);
            responseData.put("newRecords", newRecords);
            responseData.put("updatedRecords", updatedRecords);
            responseData.put("skippedRecords", 0);
            responseData.put("importTime", importTime);
            responseData.put("success", true);
            
            System.out.println("✅ [批量导入] 完成: 新增" + newRecords + "条，更新" + updatedRecords + "条，耗时" + importTime + "ms");
            
            return ResponseEntity.ok(Result.success(responseData));
            
        } catch (Exception e) {
            System.err.println("❌ [批量导入] 失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Result.error("批量导入失败: " + e.getMessage()));
        }
    }

    /**
     * 获取本地车牌建议列表
     * @param keyword 搜索关键词
     * @param parkName 车场名称（可选）
     * @param limit 限制数量
     * @return
     */
    @ApiOperation("获取本地车牌建议列表")
    @GetMapping("/getLocalPlateSuggestions")
    public ResponseEntity getLocalPlateSuggestions(
            @RequestParam String keyword,
            @RequestParam(required = false) String parkName,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            System.out.println("🔍 [车牌建议] 参数: keyword=" + keyword + ", parkName=" + parkName + ", limit=" + limit);
            
            long startTime = System.currentTimeMillis();
            
            QueryWrapper<MonthTick> queryWrapper = new QueryWrapper<>();
            queryWrapper.like("car_no", keyword);
            
            if (StringUtils.isNotBlank(parkName)) {
                queryWrapper.eq("park_name", parkName);
            }
            
            // 限制查询数量并按车牌号排序
            queryWrapper.orderByAsc("car_no").last("LIMIT " + limit);
            
            List<MonthTick> suggestions = monthTicketService.list(queryWrapper);
            
            // 转换为建议格式
            List<Map<String, Object>> suggestionList = new ArrayList<>();
            for (MonthTick monthTick : suggestions) {
                Map<String, Object> suggestion = new HashMap<>();
                suggestion.put("plateNumber", monthTick.getCarNo());
                suggestion.put("ownerName", monthTick.getUserName());
                suggestion.put("matchScore", calculateMatchScore(keyword, monthTick.getCarNo()));
                suggestionList.add(suggestion);
            }
            
            long searchTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("suggestions", suggestionList);
            responseData.put("total", suggestionList.size());
            responseData.put("searchTime", searchTime);
            
            System.out.println("✅ [车牌建议] 完成: 找到" + suggestionList.size() + "条建议，耗时" + searchTime + "ms");
            
            return ResponseEntity.ok(Result.success(responseData));
            
        } catch (Exception e) {
            System.err.println("❌ [车牌建议] 失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Result.error("获取车牌建议失败: " + e.getMessage()));
        }
    }

    /**
     * 从外部API导入车场数据的通用方法
     * @param parkName 车场名称
     * @return int数组，[0]为新增数量，[1]为更新数量
     */
    private int[] importParkDataFromExternalAPI(String parkName) {
        // 这里使用现有的外部API调用逻辑
        // 为了简化，我们调用现有的数据获取逻辑
        
        HashMap<String, String> hashMap = new HashMap<>();
        String parkCode = getParkCodeByName(parkName);
        
        if (parkCode == null) {
            throw new RuntimeException("未知的车场名称: " + parkName);
        }
        
        hashMap.put("parkCodeList", parkCode);
        hashMap.put("pageSize", "100");
        hashMap.put("validStatus", "1"); // 只导入有效的月票
        
        try {
            String response = HttpClientUtil.doPost("https://www.xuerparking.cn:8543/parking/monthTicket/getOnlineMonthTicketList", hashMap);
            JSONObject jsonObject = JSONObject.parseObject(response);
            
            JSONObject data1 = (JSONObject) jsonObject.get("data");
            JSONObject data2 = (JSONObject) data1.get("data");
            Integer total = data2.getInteger("total");
            JSONArray recordList = data2.getJSONArray("recordList");
            
            int newRecords = 0;
            int updatedRecords = 0;
            
            // 处理第一页数据
            int[] pageResult = processImportData(recordList, parkName);
            newRecords += pageResult[0];
            updatedRecords += pageResult[1];
            
            // 处理其他页数据
            int totalPages = (total + 99) / 100; // 向上取整
            for (int page = 2; page <= totalPages; page++) {
                hashMap.put("pageNum", String.valueOf(page));
                String pageResponse = HttpClientUtil.doPost("https://www.xuerparking.cn:8543/parking/monthTicket/getOnlineMonthTicketList", hashMap);
                JSONObject pageJsonObject = JSONObject.parseObject(pageResponse);
                
                JSONObject pageData1 = (JSONObject) pageJsonObject.get("data");
                JSONObject pageData2 = (JSONObject) pageData1.get("data");
                JSONArray pageRecordList = pageData2.getJSONArray("recordList");
                
                int[] pageResultNext = processImportData(pageRecordList, parkName);
                newRecords += pageResultNext[0];
                updatedRecords += pageResultNext[1];
            }
            
            return new int[]{newRecords, updatedRecords};
            
        } catch (Exception e) {
            throw new RuntimeException("导入数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理导入数据
     * @param recordList JSON数组
     * @param parkName 车场名称
     * @return int数组，[0]为新增数量，[1]为更新数量
     */
    private int[] processImportData(JSONArray recordList, String parkName) {
        int newRecords = 0;
        int updatedRecords = 0;
        
        for (int i = 0; i < recordList.size(); i++) {
            JSONObject jsonObject1 = recordList.getJSONObject(i);
            MonthTick monthTick = new MonthTick();
            
            // 设置基本信息
            monthTick.setCarNo(processCarNo(jsonObject1.getString("carNo")));
            monthTick.setCreateTime(jsonObject1.getString("createTime"));
            monthTick.setCreateBy(jsonObject1.getString("createBy"));
            monthTick.setTicketName(jsonObject1.getString("ticketName"));
            monthTick.setCarNoNum(jsonObject1.getInteger("carNoNum") != null ? jsonObject1.getInteger("carNoNum") : -1);
            monthTick.setUserPhone(jsonObject1.getString("userPhone") != null ? jsonObject1.getString("userPhone") : "11111111111");
            monthTick.setRemark1(jsonObject1.getString("remark1"));
            monthTick.setRemark2(jsonObject1.getString("remark2"));
            monthTick.setRemark3(jsonObject1.getString("remark3"));
            monthTick.setIsFrozen(jsonObject1.getInteger("isFrozen"));
            monthTick.setUserName(jsonObject1.getString("userName"));
            monthTick.setValidStatus(jsonObject1.getInteger("validStatus"));
            monthTick.setParkName(parkName);
            
            // 处理时间段信息
            JSONArray timePeriodList1 = jsonObject1.getJSONArray("timePeriodList");
            StringBuilder str = new StringBuilder();
            if (timePeriodList1 != null) {
                for (int i2 = 0; i2 < timePeriodList1.size(); i2++) {
                    JSONObject jsonObjectTime = timePeriodList1.getJSONObject(i2);
                    String startTime = convertDateFormat(jsonObjectTime.getString("startTime"));
                    String endTime = convertDateFormat(jsonObjectTime.getString("endTime"));
                    str.append("startTime:").append(startTime).append(",endTime:").append(endTime);
                    if (i2 < timePeriodList1.size() - 1) {
                        str.append(";");
                    }
                }
            }
            monthTick.setTimePeriodList(str.toString());
            
            // 检查是否已存在
            QueryWrapper<MonthTick> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("car_no", monthTick.getCarNo()).eq("park_name", parkName);
            MonthTick existingRecord = monthTicketService.getOne(queryWrapper);
            
            if (existingRecord == null) {
                // 新增记录
                if (monthTicketService.save(monthTick)) {
                    newRecords++;
                }
            } else {
                // 更新记录
                monthTick.setId(existingRecord.getId());
                if (monthTicketService.updateById(monthTick)) {
                    updatedRecords++;
                }
            }
        }
        
        return new int[]{newRecords, updatedRecords};
    }

    /**
     * 处理车牌号码，去除末尾的"绿"字
     * 支持处理多个车牌号码，用逗号分隔的情况
     * 
     * @param carNo 原始车牌号码
     * @return 处理后的车牌号码
     */
    private String processCarNo(String carNo) {
        if (carNo == null || carNo.trim().isEmpty()) {
            return carNo;
        }
        
        // 处理多个车牌号码的情况（用逗号分隔）
        if (carNo.contains(",")) {
            String[] carNos = carNo.split(",");
            StringBuilder result = new StringBuilder();
            
            for (int i = 0; i < carNos.length; i++) {
                String processedCarNo = processSingleCarNo(carNos[i].trim());
                result.append(processedCarNo);
                
                // 如果不是最后一个，添加逗号
                if (i < carNos.length - 1) {
                    result.append(",");
                }
            }
            
            return result.toString();
        } else {
            // 处理单个车牌号码的情况
            return processSingleCarNo(carNo.trim());
        }
    }
    
    /**
     * 处理单个车牌号码，去除末尾的"绿"字
     * 
     * @param carNo 单个车牌号码
     * @return 处理后的车牌号码
     */
    private String processSingleCarNo(String carNo) {
        if (carNo == null || carNo.trim().isEmpty()) {
            return carNo;
        }
        
        String trimmedCarNo = carNo.trim();
        
        // 如果车牌号末尾是"绿"字，则去除
        if (trimmedCarNo.endsWith("绿")) {
            return trimmedCarNo.substring(0, trimmedCarNo.length() - 1);
        }
        
        return trimmedCarNo;
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
        }
        // 可以根据需要添加更多车场映射
        return null;
    }

    /**
     * 🆕 通过车牌号查询外部月票信息（用于违规记录创建）
     * 查询当前时间在有效期内的月票信息
     * @param plateNumber 车牌号
     * @param parkCode 车场编码
     * @return 月票信息Map，包含 ticketName（月票名称）、userName（车主姓名）、userPhone（车主电话）
     */
    @ApiOperation("通过车牌号查询有效月票信息")
    @GetMapping("/getValidMonthTicketByPlate")
    public ResponseEntity getValidMonthTicketByPlate(
            @RequestParam String plateNumber,
            @RequestParam String parkCode) {
        try {
            System.out.println("🔍 [查询有效月票] 车牌: " + plateNumber + ", 车场: " + parkCode);
            
            // 调用外部接口获取月票列表（按车牌号查询）
            HashMap<String, Object> params = new HashMap<>();
            params.put("parkCodeList", Arrays.asList(parkCode));
            params.put("carCode", plateNumber); // 新接口使用 carCode 参数（车牌号码）
            params.put("pageNum", 1);
            params.put("pageSize", 10);
            params.put("effective", 0); // 只查询有效的月票
            
            JSONObject response = aikeConfig.downHandler(
                    AIKEConfig.AK_URL, 
                    AIKEConfig.AK_KEY, 
                    AIKEConfig.AK_SECRET, 
                    "getOnlineMonthTicketByCarCard", 
                    params);
            
            System.out.println("📥 [外部接口响应] response: " + response);
            
            // 修改判断逻辑：resultCode=0 表示成功
            if (response == null || response.getInteger("resultCode") == null || response.getInteger("resultCode") != 0) {
                System.err.println("❌ [外部接口调用失败] response: " + response);
                return ResponseEntity.ok(Result.error("外部接口调用失败"));
            }
            
            // 解析响应数据（新接口返回的是 monthTicketList）
            JSONObject data = response.getJSONObject("data");
            JSONArray monthTicketList = data.getJSONArray("monthTicketList");
            
            if (monthTicketList == null || monthTicketList.isEmpty()) {
                System.out.println("ℹ️ [未找到月票] 车牌: " + plateNumber);
                return ResponseEntity.ok(Result.error("未找到该车牌的月票信息"));
            }
            
            System.out.println("📋 [月票列表] 找到 " + monthTicketList.size() + " 条月票记录");
            
            // 直接返回第一条月票信息（effective=0 已过滤为生效中的数据）
            for (int i = 0; i < monthTicketList.size(); i++) {
                JSONObject monthTicket = monthTicketList.getJSONObject(i);
                String carCode = monthTicket.getString("carCode");
                
                System.out.println("🔎 [检查月票] 车牌: " + carCode + ", 有效状态: " + monthTicket.getInteger("validStatus"));

                // 检查车牌号是否匹配（兼容 carCode 包含多个车牌的情况）
                boolean isMatch = false;
                if (carCode != null) {
                    // 尝试拆分多个车牌号（用逗号分隔）
                    String[] carCodeList = carCode.split(",");
                    for (String code : carCodeList) {
                        code = code.trim(); // 去除空格
                        if (code.equals(plateNumber)) {
                            isMatch = true;
                            break;
                        }
                    }
                }

                if (isMatch) {
                    // 找到匹配的月票，直接返回信息
                    Map<String, Object> result = new HashMap<>();
                    result.put("ticketName", monthTicket.getString("ticketName"));
                    result.put("userName", monthTicket.getString("userName"));
                    result.put("userPhone", monthTicket.getString("userPhone"));
                    result.put("carCode", carCode);
                    
                    System.out.println("✅ [找到有效月票] 车主: " + result.get("userName") 
                            + ", 月票: " + result.get("ticketName")
                            + ", 电话: " + result.get("userPhone"));
                    
                    return ResponseEntity.ok(Result.success(result));
                }
            }
            
            System.out.println("ℹ️ [未找到月票] 车牌: " + plateNumber);
            return ResponseEntity.ok(Result.error("未找到该车牌的月票信息"));
            
        } catch (Exception e) {
            System.err.println("❌ [查询月票异常] 车牌: " + plateNumber + ", 错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Result.error("查询月票信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 计算匹配分数
     * @param keyword 关键词
     * @param plateNumber 车牌号
     * @return 匹配分数 (0.0 - 1.0)
     */
    private double calculateMatchScore(String keyword, String plateNumber) {
        if (keyword == null || plateNumber == null) {
            return 0.0;
        }
        
        keyword = keyword.toLowerCase();
        plateNumber = plateNumber.toLowerCase();
        
        if (plateNumber.equals(keyword)) {
            return 1.0;
        }
        
        if (plateNumber.startsWith(keyword)) {
            return 0.9;
        }
        
        if (plateNumber.contains(keyword)) {
            return 0.8;
        }
        
        // 简单的字符匹配度计算
        int matches = 0;
        for (char c : keyword.toCharArray()) {
            if (plateNumber.indexOf(c) >= 0) {
                matches++;
            }
        }
        
        return (double) matches / keyword.length() * 0.6;
    }
}

