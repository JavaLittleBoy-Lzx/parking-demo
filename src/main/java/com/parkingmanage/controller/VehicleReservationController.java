package com.parkingmanage.controller;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.common.AIKEResult;
import com.parkingmanage.common.HttpClientUtil;
import com.parkingmanage.common.Result;
import com.parkingmanage.common.config.AIKEConfig;
import com.parkingmanage.entity.*;
import com.parkingmanage.service.*;

import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.Controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 李子雄
 */
@RestController
@RequestMapping(value = "/parking/vehicleReservation", method = {RequestMethod.GET, RequestMethod.POST})
public class VehicleReservationController {

    public static final HashMap<String, String> HASH_MAP_GATE = new HashMap<>();
    @Resource
    private VehicleReservationService vehicleReservationService;

    @Autowired
    public AIKEConfig aikeConfig;

    @Autowired
    private ChannelInfoService channelInfoService;

    @Autowired
    private YardInfoService yardInfoService;

    @Autowired
    private PreVipTypeService preVipTypeService;

    @Autowired
    private VehicleClassificationService vehicleClassificationService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private ViolationsService violationsService;

    @Resource
    private ReportCarInService reportCarInService;

    @Resource
    private ReportCarOutService reportCarOutService;

    @Autowired
    private WeChatTemplateMessageService weChatTemplateMessageService;

    @Autowired
    private MonthTicketService monthTicketService;

    @Autowired
    private OwnerinfoService ownerinfoService;

    @Autowired
    private TempCarPreEntryService tempCarPreEntryService;

    @Setter
    @Getter
    private String enterPreVipType = "";

    private Logger logger = LoggerFactory.getLogger(Controller.class);
    // 车牌正则：第一个是汉字，第二个是字母
    private static final String LICENSE_REGEX = "^[\\u4e00-\\u9fff][A-Za-z][·\\-]?[A-Za-z0-9]{5,6}$";

    private static final Pattern pattern = Pattern.compile(LICENSE_REGEX);

    public static boolean isValidLicensePlate(String plate) {
        if (plate == null || plate.isEmpty()) {
            return false;
        }
        return pattern.matcher(plate).matches();
    }
    @ApiOperation("添加")
    @PostMapping("insert")
    public ResponseEntity<Result> insertVehicleReservation(@RequestBody VehicleReservation vehicleReservation) {
        vehicleReservation.setCreateTime(new Date());
        vehicleReservation.setAppointmentTime(new Date());
        vehicleReservation.setAppointmentFlag(0);
        VehicleReservation vehicleReservation1 = vehicleReservationService.selectByCarName(vehicleReservation.getPlateNumber());
        Result result = new Result();
        if (vehicleReservation1 == null) {
            vehicleReservationService.save(vehicleReservation);
            result.setCode("0");
            result.setMsg("添加成功！");
        } else {
            result.setCode("1");
            result.setMsg("数据重复，增加失败！");
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("获取欧洲新城指定期间未在黑名单中的疑似黑名单入口车牌")
    @GetMapping("/getEuXinBlackGatePlatesNotInBlacklist")
    public ResponseEntity<Result> getEuXinBlackGatePlatesNotInBlacklist() {
        Result result = new Result();
        try {
            // 固定查询条件
            String parkCode = "2KPL6XFF"; // 欧洲新城
            String startTime = "2025-10-13 00:00:00";
            String endTime = "2025-10-14 23:59:59";
            String vipNameFilter = "黑名单（欧洲新城3号门）";

            // 收集筛选出的车牌（去重并保持顺序）
            LinkedHashSet<String> candidatePlates = new LinkedHashSet<>();

            // 将时间范围切分为按天的窗口，确保每次调用不超过1个自然日
            java.time.LocalDate startDate = java.time.LocalDate.parse(startTime.substring(0, 10));
            java.time.LocalDate endDate = java.time.LocalDate.parse(endTime.substring(0, 10));
            java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (java.time.LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                String dayStart = d.format(df) + " 00:00:00";
                String dayEnd = d.format(df) + " 23:59:59";

                // 先请求第一页，读取total后再决定遍历次数
                int pageSizeVal = 1000;
                int totalPagesForDay = 1;

                HashMap<String, Object> firstParams = new HashMap<>();
                firstParams.put("parkCode", parkCode);
                firstParams.put("isPresence", "0");
                firstParams.put("startTime", convertDateFormat(dayStart));
                firstParams.put("endTime", convertDateFormat(dayEnd));
                firstParams.put("pageNum", 1);
                firstParams.put("pageSize", pageSizeVal);
                JSONObject firstData = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "getCarInList", firstParams);
                System.out.println("firstData = " + firstData);
                if (firstData != null) {
                    JSONObject fd1 = firstData.getJSONObject("data");
                    if (fd1 != null) {
                        System.out.println("fd1 = " + fd1);
                        JSONArray recordList = fd1.getJSONArray("recordInList");
                        System.out.println("recordInList = " + recordList);
                        if (recordList != null && recordList.size() > 0) {
                            for (int i = 0; i < recordList.size(); i++) {
                                JSONObject rec = recordList.getJSONObject(i);
                                if (rec == null) continue;
                                String enterCustomVipName = rec.getString("enterCustomVipName");
                                if (vipNameFilter.equals(enterCustomVipName)) {
                                    String carNo = rec.getString("carLicenseNumber");
                                    if (carNo != null && !carNo.contains("未识别")) {
                                        candidatePlates.add(processCarNo(carNo));
                                    }
                                }
                            }
                        }

                        Integer total = fd1.getInteger("total");
                        if (total != null && total > pageSizeVal) {
                            totalPagesForDay = (total + pageSizeVal - 1) / pageSizeVal;
                        }
                    }
                }

                for (int page = 2; page <= totalPagesForDay; page++) {
                    HashMap<String, Object> params = new HashMap<>();
                    params.put("parkCode", parkCode);
                    params.put("isPresence", "0");
                    params.put("startTime", convertDateFormat(dayStart));
                    params.put("endTime", convertDateFormat(dayEnd));
                    params.put("pageNum", page);
                    params.put("pageSize", pageSizeVal);
                    JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "getCarInList", params);
                    if (data == null) {
                        break;
                    }
                    JSONObject d1 = data.getJSONObject("data");
                    if (d1 == null) {
                        break;
                    }
                    JSONArray recordList = d1.getJSONArray("recordInList");
                    if (recordList == null || recordList.size() == 0) {
                        break;
                    }
                    System.out.println("recordList 2 = " + recordList);
                    for (int i = 0; i < recordList.size(); i++) {
                        JSONObject rec = recordList.getJSONObject(i);
                        if (rec == null) continue;
                        String enterCustomVipName = rec.getString("enterCustomVipName");
                        if (vipNameFilter.equals(enterCustomVipName)) {
                            String carNo = rec.getString("carLicenseNumber");
                            if (carNo != null && !carNo.contains("未识别")) {
                                candidatePlates.add(processCarNo(carNo));
                            }
                        }
                    }
                }
            }
            System.out.println("candidatePlates = " + candidatePlates.size());
            // 遍历candidatePlates
            for (String plate : candidatePlates) {
                System.out.println("plate = " + plate);
            }
            // 逐个车牌调用黑名单接口校验：不在黑名单中的才返回
            ArrayList<String> notInBlacklist = new ArrayList<>();
            for (String plate : candidatePlates) {
                HashMap<String, Object> blackParams = new HashMap<>();
                blackParams.put("parkCodeList", Arrays.asList(parkCode));
                blackParams.put("carCode", plate);
                blackParams.put("pageNum", 1);
                blackParams.put("pageSize", 1000);
                JSONObject blackData = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "getParkBlackList", blackParams);
                boolean existsInBlack = false;
                if (blackData != null) {
                    JSONObject b1 = blackData.getJSONObject("data");
                    if (b1 != null) {
                        Integer total = b1.getInteger("total");
                        if (total != null && total > 0) {
                            // 进一步校验记录中是否包含该车牌（防止模糊命中）
                            JSONArray recs = b1.getJSONArray("recordList");
                            System.out.println("recs = " + recs);
                            if (recs != null) {
                                for (int j = 0; j < recs.size(); j++) {
                                    JSONObject r = recs.getJSONObject(j);
                                    if (r != null && plate.equals(processCarNo(r.getString("carCode")))) {
                                        existsInBlack = true;
                                        break;
                                    }
                                }
                            } else {
                                existsInBlack = true; // 有total>0但无列表，保守认为存在
                            }
                        }
                    }
                }
                if (!existsInBlack) {
                    notInBlacklist.add(plate);
                }
            }

            result.setCode("0");
            result.setMsg("查询成功");
            result.setData(notInBlacklist);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.setCode("1");
            result.setMsg("查询失败: " + e.getMessage());
            result.setData(null);
            return ResponseEntity.ok(result);
        }
    }

    @ApiOperation("修改")
    @PostMapping("update")
    public ResponseEntity<Result> update(@RequestBody VehicleReservation vehicleReservation) {
        int num = vehicleReservationService.duplicate(vehicleReservation);
        Result result = new Result();
        if (num == 0) {
            vehicleReservationService.updateById(vehicleReservation);
            result.setCode("0");
            result.setMsg("修改成功！");
        } else {
            result.setCode("1");
            result.setMsg("数据重复，修改失败！");
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("删除")
    @PostMapping("/{id}")
    public ResponseEntity<Result> delete(@PathVariable String id) {
//        if(vehicleReservationService.removeById(id))
        boolean success = vehicleReservationService.removeById(id);
        if (!success) {
            return ResponseEntity.ok(Result.error("删除失败！"));
        }
        return ResponseEntity.ok(Result.success("删除成功！"));
    }

    @ApiOperation("批量删除")
    @PostMapping("/batchDelete")
    public ResponseEntity<Result> batchDelete(@RequestBody List<Integer> ids) {
        boolean success = vehicleReservationService.removeByIds(ids);
//        logger.info("success = " + success);
        if (!success) {
            return ResponseEntity.ok(Result.error("批量删除失败！"));
        }
        return ResponseEntity.ok(Result.success("批量删除成功！"));
    }

    @ApiOperation("查询所有")
    @GetMapping("/getAllVehicleReservation")
    public List<VehicleReservation> getAllVehicleReservation() {
        List<VehicleReservation> myquery = vehicleReservationService.list();
        return myquery;
    }

    @ApiOperation("清空超时车辆")
    @GetMapping("/timeOutCleanUp")
    public List<TimeOutVehicleList> timeOutCleanUp(Integer timeOutInterval) {
        ArrayList<TimeOutVehicleList> vehicleReservations = new ArrayList<>();
        // 调用SQL查询超时数据
        vehicleReservations = vehicleClassificationService.selectBytimeOutInterval(timeOutInterval);
        return vehicleReservations;
    }

    @ApiOperation("入场超时车辆查询")
    @GetMapping("/enterTimeOutCleanUp")
    public List<TimeOutVehicleList> enterTimeOutCleanUp(Integer timeOutInterval, String yardName) {
        ArrayList<TimeOutVehicleList> vehicleReservations = new ArrayList<>();
        // 调用SQL查询超时数据,将30分钟和小时进行区分
        vehicleReservations = vehicleClassificationService.selectByEnterTimeOutInterval(timeOutInterval, yardName);
        return vehicleReservations;
    }


    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<VehicleReservation> findPage(@RequestParam(required = false) String plateNumber, @RequestParam(required = false) String yardName, @RequestParam(required = false, defaultValue = "1") Integer pageNum, @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        Page<VehicleReservation> vehicleReservationPage = new Page<>(pageNum, pageSize);
        QueryWrapper<VehicleReservation> vehicleReservationQueryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(plateNumber)) {
            vehicleReservationQueryWrapper.like("plate_number", plateNumber);
        }
        if (StringUtils.isNotBlank(yardName)) {
            vehicleReservationQueryWrapper.like("yard_name", yardName);
        }
        // 按照预约时间倒序排序
        vehicleReservationQueryWrapper.orderByDesc("appointment_time");
        vehicleReservationQueryWrapper.eq("reserve_flag", 0);
//        vehicleReservationQueryWrapper.eq("reserve_flag", 0);
        return vehicleReservationService.page(vehicleReservationPage, vehicleReservationQueryWrapper);
    }

    @ApiOperation("放行的分页查询")
    @GetMapping("/reservationPage")
    public IPage<VehicleReservation> findReservationPage(@RequestParam(required = false) String plateNumber, @RequestParam(required = false) String yardName, @RequestParam(required = false, defaultValue = "1") Integer pageNum, @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        Page<VehicleReservation> vehicleReservationPage = new Page<>(pageNum, pageSize);
        QueryWrapper<VehicleReservation> vehicleReservationQueryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(plateNumber)) {
            vehicleReservationQueryWrapper.like("plate_number", plateNumber);
        }
        if (StringUtils.isNotBlank(yardName)) {
            vehicleReservationQueryWrapper.like("yard_name", yardName);
        }
        vehicleReservationQueryWrapper.eq("reserve_flag", 1);
        vehicleReservationQueryWrapper.orderByDesc("appointment_time");
        return vehicleReservationService.page(vehicleReservationPage, vehicleReservationQueryWrapper);
    }

    @ApiOperation("添加入场")
    @PostMapping("/addReservation")
    public ResponseEntity<Result> addReservation(@RequestBody VehicleReservation vehicleReservation) {//已经预约触发开闸
        //调用开关闸的接口参数
        HashMap<String, String> hashMapGate = new HashMap<>();
        hashMapGate.put("parkCode", vehicleReservation.getYardCode());
        hashMapGate.put("channelCode", vehicleReservation.getChannelName());
        hashMapGate.put("opType", "0");
        hashMapGate.put("operator", "自动操作员");
        String getGet = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/vehicleReservation/openGates", hashMapGate);
//        logger.info(vehicleReservation.getPlateNumber() + "手动抬杆开关闸：" + getGet);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        VehicleReservation reservation = vehicleReservationService.getById(vehicleReservation.getId());
        reservation.setReserveTime(dateFormat.format(new Date()));
        reservation.setReserveFlag(1);
        //若是手动放行的话，则se入场时间显示"手动放行"，放行时间显示当前时间
        reservation.setEnterTime("手动放行");
        vehicleReservationService.updateById(reservation);
        Result result = new Result();
        result.setMsg("添加入场成功！");
        result.setCode("0");
        return ResponseEntity.ok(result);
    }

    @ApiOperation("导出数据")
    @GetMapping(value = "/export")
    public void export(HttpServletResponse response, @RequestParam("startDate") String startDate, @RequestParam("endDate") String endDate, @RequestParam("yardName") String yardName, @RequestParam("channelName") String channelName) throws IOException, ParseException {
        vehicleReservationService.exportVehicleReservation(startDate, endDate, yardName, channelName, response);
    }

    @ApiOperation("开关闸机")
    @GetMapping("/openGates")
    public ResponseEntity openGates(String parkCode, String channelCode, Integer opType, String operator) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("parkCode", parkCode);
        params.put("channelCode", channelCode);
        params.put("opType", opType);
        params.put("operator", operator);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String operateTime = simpleDateFormat.format(new Date());
        params.put("operateTime", operateTime);
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "opGate", params);
        return ResponseEntity.ok(data);
    }

    @ApiOperation("获取授权停车场编码列表")
    @GetMapping("/getParkInfo")
    public ResponseEntity getParkInfo(String parkCode) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("parkCode", parkCode);
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "getParkInfo", params);
        return ResponseEntity.ok(data);
    }

    @ApiOperation("获取授权停车场通道列表")
    @GetMapping("/getChannelInfo")
    public ResponseEntity getChannelInfo(String parkCode) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("parkCode", parkCode);
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "getChannelInfo", params);
        return ResponseEntity.ok(data);
    }

    @ApiOperation("获取授权停车场通道列表")
    @GetMapping("/aikeGetChannelInfo")
    public List<ChannelInfo> aikeGetChannelInfo(String yardCode) {
        // 调用接口查询车场名称
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("parkCode", yardCode);
        System.out.println("hashMap = " + hashMap);
        String get = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/vehicleReservation/getChannelInfo", hashMap);
//        String get = HttpClientUtil.doGet("http://www.xuerparking.cn:8543/parking/vehicleReservation/getChannelInfo", hashMap);
        List<ChannelInfo> channelInfos = new ArrayList<>();
        JSONObject jsonObject = JSONObject.parseObject(get);
        JSONObject jsonObjectData = (JSONObject) jsonObject.get("data");
        JSONObject jsonObjectDataData = (JSONObject) jsonObjectData.get("data");
        JSONArray jsonObjectDataDataChannel = (JSONArray) jsonObjectDataData.get("recordList");
        String entranceName = "";
        for (int i = 0; i < jsonObjectDataDataChannel.size(); i++) {
            JSONObject jsonObjectChannelInfoList = (JSONObject) jsonObjectDataDataChannel.get(i);
            entranceName = (String) jsonObjectChannelInfoList.get("entranceName");
            String customCode = (String) jsonObjectChannelInfoList.get("customCode");
            Integer id = (Integer) jsonObjectChannelInfoList.get("id");
            ChannelInfo channelInfo = new ChannelInfo();
            channelInfo.setChannelId(String.valueOf(id));
            channelInfo.setChannelName(entranceName);
            channelInfo.setParkCode(customCode);
            channelInfos.add(channelInfo);
        }
        // 将查询到的结果存储到数据库中
        // 先查询一下，若有就直接查询，没有添加
        ChannelInfo channelInfo = channelInfoService.channelByName(entranceName);
        if (channelInfo == null) {
            channelInfoService.saveBatch(channelInfos);
        }
        return channelInfos;
    }

    @ApiOperation("获取进场时间段进场记录")
    @PostMapping("/getCarInList")
    public ResponseEntity getCarInList(String parkCode, String isPresence, String startTime, String endTime, int pageNum, int pageSize) throws ParseException {
        HashMap<String, Object> params = new HashMap<>();
        //enterTime格式必须是yyyy-MM-dd HH:mm:ss
        params.put("parkCode", parkCode);
        params.put("isPresence", isPresence);
        //将enterTime转为yyyy-MM-dd HH:mm:ss
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        params.put("startTime", simpleDateFormat.format(startTime));
        params.put("endTime", simpleDateFormat.format(endTime));
        params.put("pageNum", pageNum);
        params.put("pageSize", 1000);
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "getCarInList", params);
        return ResponseEntity.ok(data);
    }

    @ApiOperation("查询车牌线上月票开通记录")
    @GetMapping("/getOnlineMonthTicketByCarCard")
    public ResponseEntity getOnlineMonthTicketByCarCard(String carCode, int pageNum, int pageSize) {
        HashMap<String, Object> params = new HashMap<>();
        //enterTime格式必须是yyyy-MM-dd HH:mm:ss
        params.put("carCode", carCode);
        params.put("pageNum", pageNum);
        params.put("pageSize", pageSize);
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "getOnlineMonthTicketByCarCard", params);
        return ResponseEntity.ok(data);
    }

    @ApiOperation("通过月票ID查询线上月票开通记录")
    @GetMapping("/getOnlineVipTicket")
    public ResponseEntity getOnlineVipTicket(String monthTicketId) {
        HashMap<String, Object> params = new HashMap<>();
        //enterTime格式必须是yyyy-MM-dd HH:mm:ss
        params.put("monthTicketId", monthTicketId);
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "getOnlineVipTicket", params);
        return ResponseEntity.ok(data);
    }

    @ApiOperation("获取停车场在场车辆")
    @GetMapping("/getParkOnSiteCar")
    public ResponseEntity getParkOnSiteCar(String parkCodeList, String enterTimeFrom, String enterTimeTo, String pageNum, String pageSize) throws ParseException {
        HashMap<String, Object> params = new HashMap<>();
        String formatEnterTimeFrom = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        params.put("parkCodeList", Arrays.asList(parkCodeList));
        params.put("enterTimeFrom", enterTimeFrom);
        params.put("enterTimeTo", enterTimeTo);
        params.put("pageNum", Integer.valueOf(pageNum));
        params.put("pageSize", Integer.valueOf(pageSize));
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "getParkOnSiteCar", params);
        return ResponseEntity.ok(data);
    }

    @ApiOperation("获取具体车牌的在场车辆")
    @GetMapping("/getParkOnSiteCarByCarNo")
    public ResponseEntity getParkOnSiteCarByCarNo(String parkCodeList, String enterTimeFrom, String enterTimeTo, String carNo, String pageNum, String pageSize) throws ParseException {
        HashMap<String, Object> params = new HashMap<>();
        String formatEnterTimeFrom = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        params.put("parkCodeList", Arrays.asList(parkCodeList));
        params.put("enterTimeFrom", enterTimeFrom);
        params.put("enterTimeTo", enterTimeTo);
        params.put("carNo", carNo);
        params.put("pageNum", Integer.valueOf(pageNum));
        params.put("pageSize", Integer.valueOf(pageSize));
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "getParkOnSiteCar", params);
        return ResponseEntity.ok(data);
    }

    @ApiOperation("调用获取停车场在场车辆接口")
    @PostMapping("/getAKEParkOnSiteCar")
    public ResponseEntity<Result> getAKEParkOnSiteCar(@RequestBody CarOnSiteEntity carOnSiteEntity) throws ParseException {
        String formatEnterTimeFrom = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Result result = new Result();
        ArrayList<ParkOnSiteCar> parkOnSiteCars = new ArrayList<ParkOnSiteCar>();
        String resultParam = "";
        long diffInMillies = 0L;
        for (String parkingCodeList : carOnSiteEntity.getParkCodeList()) {
            for (int i = 0; i < 2; i++) {
                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put("parkCodeList", parkingCodeList);
                hashMap.put("enterTimeFrom", enterTimeFromAdd(convertDateFormat(String.valueOf(formatEnterTimeFrom))));
                hashMap.put("enterTimeTo", convertDateFormat(formatEnterTimeFrom));
                hashMap.put("pageNum", String.valueOf(i + 1));
                hashMap.put("pageSize", "1000");
                String get = HttpClientUtil.doGet("https://www.xuerparking.cn:8111/aketest/getParkOnSiteCar", hashMap);
//                String get = HttpClientUtil.doGet("http://www.xuerparking.cn:8543/parking/vehicleReservation/getParkOnSiteCar", hashMap);
                JSONObject jsonObject = JSONObject.parseObject(get);
                JSONObject jsonObjectDataCarIn = (JSONObject) jsonObject.get("data");
                JSONArray recordList = (JSONArray) jsonObjectDataCarIn.get("recordList");
                // 根据currCount进行遍历
//                System.out.println("currCount = " + jsonObjectDataCarIn.get("currCount"));
                for (int i1 = 0; i1 < recordList.size(); i1++) {
                    //每个对象进行存储
                    JSONObject jsonObject1 = recordList.getJSONObject(i1);
                    // 根据查询出来的预约表中的车牌号再进行存储
                    String enterCarLicenseNumber = (String) jsonObject1.getString("carNo");
                    if (!enterCarLicenseNumber.contains("未识别")) {
                        String string = jsonObject1.getString("enterTime");
                        String enterTime = convertDateFormatYY(string);
                        ParkOnSiteCar parkOnSiteCar = new ParkOnSiteCar();
                        parkOnSiteCar.setCarNo(processCarNo(jsonObject1.getString("carNo")));
                        parkOnSiteCar.setEnterTime(enterTime);
                        parkOnSiteCar.setParkName(jsonObject1.getString("parkName"));
                        // 计算停车时长
                        SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date enterTimeFormat = sdfTime.parse(enterTime);
                        Date leaveTimeFormat = sdfTime.parse(formatEnterTimeFrom);
                        diffInMillies = Math.abs(leaveTimeFormat.getTime() - enterTimeFormat.getTime());
                        long diffHours = diffInMillies / (60 * 60 * 1000);
                        long diffMinutes = (diffInMillies / (60 * 1000)) % 60;
                        long diffSeconds = (diffInMillies / 1000) % 60;
                        if (diffHours > 24) {
                            long days = diffHours / 24;
                            diffHours %= 24;
                            resultParam = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
                        } else {
                            if (diffHours == 0) {
                                resultParam = diffMinutes + "分钟" + diffSeconds + "秒";
                            } else {
                                resultParam = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
                            }
                        }
                        parkOnSiteCar.setParkingDuration(resultParam);
                        parkOnSiteCar.setParkingDurationTimes(diffInMillies);
                        parkOnSiteCars.add(parkOnSiteCar);
                    }
                }
            }
        }
        // 将30分钟、2小时、3小时转为毫秒
        // 30分钟 1800000
        // 2小时 7200000
        // 3小时 10800000
        // 48小时 172800000
        Integer timeOutInterval = carOnSiteEntity.getTimeOutInterval();
        Long timeDurationsTime = 0L;
        if (timeOutInterval == 30) {
            timeDurationsTime = 1800000L;
        } else if (timeOutInterval == 2) {
            timeDurationsTime = 7200000L;
        } else if (timeOutInterval == 3) {
            timeDurationsTime = 10800000L;
        }
        // 查询出指定的时间范围内的数据
        ArrayList<ParkOnSiteCar> parkShowOnSiteList = new ArrayList<>();
        for (ParkOnSiteCar parkOnSiteCar : parkOnSiteCars) {
            if (parkOnSiteCar.getParkingDurationTimes() <= 172800000L) {
                if (parkOnSiteCar.getParkingDurationTimes() >= timeDurationsTime) {
                    parkShowOnSiteList.add(parkOnSiteCar);
                }
            }
        }
        Comparator<ParkOnSiteCar> parkingDurationComparator = new Comparator<ParkOnSiteCar>() {
            @Override
            public int compare(ParkOnSiteCar o1, ParkOnSiteCar o2) {
                return Long.compare(o1.getParkingDurationTimes(), o2.getParkingDurationTimes());
            }
        };
        // 使用比较器对exportDataArrayList进行排序
        Collections.sort(parkShowOnSiteList, parkingDurationComparator);
        result.setMsg("查询成功！");
        result.setCode("0");
        result.setData(parkShowOnSiteList);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("转换时间格式")
    public static String convertDateFormat(String input) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = null;
        try {
            date = inputFormat.parse(input);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return outputFormat.format(date);
    }

    @ApiOperation("转换时间格式")
    public static String convertDateFormatYY(String input) {
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = null;
        try {
            date = inputFormat.parse(input);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return outputFormat.format(date);
    }

    public static String enterTimeFromAdd(String dateString) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date parse = sdf.parse(dateString);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(parse);
        calendar.add(Calendar.MONTH, -1); // 减去一个月
        calendar.add(Calendar.SECOND, 1); // 增加一秒
        String formattedDate = sdf.format(calendar.getTime()); // 转换为字符串格式
        return formattedDate;
    }

    @ApiOperation("查询单条停车记录详情")
    @GetMapping("/getParkDetail")
    public ResponseEntity getParkDetail(String parkCode, String carCode) {
        HashMap<String, Object> params = new HashMap<>();
        //enterTime格式必须是yyyy-MM-dd HH:mm:ss
        params.put("parkCode", parkCode);
        params.put("carCode", carCode);
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET, "getParkDetail", params);
//        logger.info("调用aike接口后的查询单条停车记录详情" + data);
        return ResponseEntity.ok(data);
    }

    @ApiOperation("预约车辆入场")
    @RequestMapping("/reportPreCarIn")
//    public ResponseEntity<AIKEResult> reportPreCarIn(@RequestBody JSONObject data) {
//        logger.info("预进场数据 = " + data);
    public ResponseEntity<AIKEResult> reportPreCarIn(@RequestBody ReportPreCarIn data) {
        // 只判断不是未识别的车牌号码且车牌号码第一个字是汉字的
        if (!data.getEnterCarLicenseNumber().equals("未识别") && (isValidLicensePlate(data.getEnterCarLicenseNumber()))) {
            logger.info("reportPreCarInData = " + data.getEnterCarLicenseNumber() + "enterTime = " + data.getEnterTime()
            + "getEnterChannelCustomCode = " + data.getEnterChannelCustomCode() + "getParkCode = " + data.getParkCode());
//         先将数据存储到数据库，接着判断两次数据是否都正常操作
//         调用接口查询车场名称
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("parkCode", data.getParkCode());
            String get = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/vehicleReservation/getParkInfo", hashMap);
//        String get = HttpClientUtil.doGet("http://www.xuerparking.cn:8543/parking/vehicleReservation/getParkInfo", hashMap);
            JSONObject jsonObject = JSONObject.parseObject(get);
            JSONObject jsonObjectData = (JSONObject) jsonObject.get("data");
            JSONObject jsonObjectDataData = (JSONObject) jsonObjectData.get("data");
            String parkName = (String) jsonObjectDataData.get("parkName");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat sdfOutput = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyyMMddHHmmss");
            // 🆕 存储符合条件的临时车预进场数据
            // 条件：enterChannelCode=76F3EBH0Z 且 指定车场（欧洲新城）
            // 注：ReportPreCarIn类中没有enterVipType字段，所以这里先只判断通道编码和车场
            if ("76F3EBH0Z".equals(data.getEnterChannelCustomCode()) &&
                    "2KPL6XFF".equals(data.getParkCode())) {
                try {
                    // 转换时间格式为标准格式
                    Date formatDate = sdfInput.parse(data.getEnterTime());
                    String formattedEnterTime = sdfOutput.format(formatDate);
                    // 保存或更新临时车预进场数据（存在则更新，不存在则插入）
                    boolean saveResult = tempCarPreEntryService.saveTempCarPreEntry(
                            data.getEnterCarLicenseNumber(),
                            data.getParkCode(),
                            parkName,
                            data.getEnterChannelCustomCode(),
                            data.getEnterChannelId(),
                            1, // 假设这种情况下enterVipType为1（临时车）
                            formattedEnterTime
                    );
                    if (saveResult) {
                        logger.info("✅ [临时车预进场数据保存/更新成功] plateNumber={}, parkCode={}, enterChannelCode={}",
                                data.getEnterCarLicenseNumber(), data.getParkCode(), data.getEnterChannelCustomCode());
                    } else {
                        logger.warn("⚠️ [临时车预进场数据保存/更新失败] plateNumber={}", data.getEnterCarLicenseNumber());
                    }
                } catch (Exception e) {
                    logger.error("❌ [保存临时车预进场数据异常] plateNumber={}, error={}",
                            data.getEnterCarLicenseNumber(), e.getMessage(), e);
                }
            }
            // 查询车牌号是否存在
            // 通过查询车场对应的名称匹配预约记录对应的车场信息
            VehicleReservation vehicleReservation = vehicleReservationService.selectVehicleReservation(data.getEnterCarLicenseNumber(),data.getParkCode());
            //TODO  此处需要将小程序预约车场信息查询出车场名称传递
            Appointment appointment = appointmentService.getByQueryInfo(data.getEnterCarLicenseNumber(),parkName);
            if (vehicleReservation != null) {
                //此处为预约过的车辆，在此处判断车辆的预进场信息是否是四季上东、6号岗入口
                if (!(data.getParkCode().equals("2KUG6XLU")
                        && data.getEnterChannelCustomCode().equals("PSWONBU2"))) {
                    // 对于四季三期的1号岗的入口数据进行去除
                    if (!(data.getParkCode().equals("2KUG6XLU")
                            && data.getEnterChannelCustomCode().equals("PSWONBU1"))) {
                        // 已经预约触发开闸
                        // 调用开关闸的接口参数
                        // 查询是否已经入场
                        HashMap<String, String> HASH_MAP_GATE = new HashMap<>();
                        HASH_MAP_GATE.put("parkCode", data.getParkCode());
                        // 此处的getEnterChannelCustomCode为通道自定义编码，且接口的value也必须为通道自定义编码
                        HASH_MAP_GATE.put("channelCode", data.getEnterChannelCustomCode());
                        HASH_MAP_GATE.put("opType", "0");
                        HASH_MAP_GATE.put("operator", "自动操作员");
                        String getGet = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/vehicleReservation/openGates", HASH_MAP_GATE);
                        logger.info(data.getEnterCarLicenseNumber() + "预约车辆Get = " + getGet + new Date());
                        vehicleReservation.setReserveTime(dateFormat.format(new Date()));
                        vehicleReservation.setReserveFlag(1);
                        Date formatDate = null;
                        try {
                            formatDate = sdfInput.parse(data.getEnterTime());
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                        vehicleReservation.setEnterTime(sdfOutput.format(formatDate));
                        boolean num = vehicleReservationService.updateById(vehicleReservation);
                    } else {
                        logger.info(data.getEnterCarLicenseNumber() + "四季三期地库1号岗数据！！");
                    }
                } else {
                    logger.info(data.getEnterCarLicenseNumber() + "四季三期地库6号岗数据！！");
                }
            } else {
                // TODO 判断在小程序预约的项目中添加的预约数据
                if (appointment != null) {
                    HashMap<String, String> hashMapGate = new HashMap<>();
                    hashMapGate.put("parkCode", data.getParkCode());
                    hashMapGate.put("channelCode", data.getEnterChannelCustomCode());
                    hashMapGate.put("opType", "0");
                    hashMapGate.put("operator", "小程序开闸");
                    String getGet = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/vehicleReservation/openGates", hashMapGate);
                    logger.info(data.getEnterCarLicenseNumber() + "小程序开关闸：" + getGet);
                    appointment.setVenuestatus("已入场");
                    Date formatDate = null;
                    try {
                        formatDate = sdfInput.parse(data.getEnterTime());
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    appointment.setArrivedate(sdfOutput.format(formatDate));
                    boolean num = appointmentService.updateById(appointment);
                    logger.info("小程序num = " + num);
                    try {
                        // 通过车牌号查询appointment表获取审核人信息
                        Appointment appointmentForNotify = appointmentService.getByQueryInfo(data.getEnterCarLicenseNumber(), parkName);
                        if (appointmentForNotify != null && StringUtils.isNotBlank(appointmentForNotify.getAuditusername())) {
                            logger.info("🔔 [预进场] 准备发送微信通知给管家: {}", appointmentForNotify.getAuditusername());
                            // 通过enterChannelId查询通道信息
                            String enterChannel = channelInfoService.getByChnnelId(data.getEnterChannelId());
                            Map<String, Object> sendResult = weChatTemplateMessageService.sendParkingEnterNotification(
                                    data.getEnterCarLicenseNumber(),
                                    parkName,
                                    enterChannel,
                                    sdfOutput.format(formatDate),
                                    appointmentForNotify.getAuditusername()
                            );
                            if ((Boolean) sendResult.get("success")) {
                                logger.info("✅ [预进场] 微信通知发送成功");
                            } else {
                                logger.warn("⚠️ [预进场] 微信通知发送失败: {}", sendResult.get("message"));
                            }
                        } else {
                            logger.warn("⚠️ [预进场] 未找到审核人信息，跳过微信通知");
                        }
                    } catch (Exception e) {
                        logger.error("❌ [预进场] 发送微信通知异常", e);
                    }
                } else {
//                logger.info("走艾科系统");
                }
            }
        }

        //创建空集合
        HashMap<Object, Object> hashEmptyMap = new HashMap<>();
        return ResponseEntity.ok(AIKEResult.success(hashEmptyMap));
    }

    /**
     * AKE进场上报
     *
     * @param data
     * @return
     * @throws ParseException
     */
    @RequestMapping("/reportCarIn")
// public ResponseEntity<AIKEResult> reportCarIn(@RequestBody JSONObject data) throws ParseException {
    public ResponseEntity<AIKEResult> reportCarIn(@RequestBody ReportCarInData data) throws ParseException {
        // 存储数据
        ReportCarIn reportCarIn = new ReportCarIn();
        if (data.getEnterCarLicenseNumber() != null && !data.getEnterCarLicenseNumber().contains("未识别")) {
            //添加文字
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = inputFormat.parse(data.getEnterTime());
            String outputEnterTime = outputFormat.format(date);
            reportCarIn.setEnterTime(outputEnterTime);
            reportCarIn.setEnterChannelId(String.valueOf(data.getEnterChannelId()));
            //调用艾科接口查询车场名称
            // 调用接口查询车场名称
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("parkCode", data.getParkCode());
            String get = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/vehicleReservation/getParkInfo", hashMap);
            JSONObject jsonObject = JSONObject.parseObject(get);
            JSONObject jsonObjectData = (JSONObject) jsonObject.get("data");
            JSONObject jsonObjectDataData = (JSONObject) jsonObjectData.get("data");
            String parkName = (String) jsonObjectDataData.get("parkName");
            reportCarIn.setYardName(parkName);
            reportCarIn.setDeleted(0);
            reportCarIn.setAreaId(String.valueOf(data.getAreaId()));
            // 查询小程序预约表中的数据是否存在车牌，存在的话则将进场时间赋值
            int update = appointmentService.updateByCarNumber(data.getEnterCarLicenseNumber(), outputEnterTime);
            if (update != 0) {
                logger.info("小程序更新成功条数：" + update);
            }
        }
        //创建空集合
        HashMap<Object, Object> hashEmptyMap = new HashMap<>();
        return ResponseEntity.ok(AIKEResult.success(hashEmptyMap));
    }
    @ApiOperation("预出场")
    @RequestMapping("/reportPreCarOut")
//    public ResponseEntity<AIKEResult> reportPreCarOut(@RequestBody JSONObject data) {
//        logger.info("预离场数据 = " + data);
    public ResponseEntity<AIKEResult> reportPreCarOut(@RequestBody ReportPreCarOut data) {
        // 查询车牌号是否存在
        VehicleReservation vehicleReservation = vehicleReservationService.selectByCarName(data.getLeaveCarLicenseNumber());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 首先判断是否是未识别的车牌
        if (data.getLeaveCarLicenseNumber().contains("未识别")) {
            return ResponseEntity.ok(AIKEResult.successOpenGate());
        } else {
            if (vehicleReservation != null) {
                //此处为预约过的车辆，判断是否是太平桥百盛
                if (data.getParkCode().equals("2KUIN1CF")) {
                    // 已经预约触发开闸
                    // 调用开关闸的接口参数
                    // 查询是否已经入场
                    HashMap<String, String> HASH_MAP_GATE = new HashMap<>();
                    HASH_MAP_GATE.put("parkCode", data.getParkCode());
                    // 此处的getEnterChannelCustomCode为通道自定义编码，且接口的value也必须为通道自定义编码
                    HASH_MAP_GATE.put("channelCode", data.getLeaveChannelCustomCode());
                    HASH_MAP_GATE.put("opType", "0");
                    HASH_MAP_GATE.put("operator", "自动操作员");
                    String getGet = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/vehicleReservation/openGates", HASH_MAP_GATE);
                    logger.info(data.getLeaveCarLicenseNumber() + "离场车辆Get = " + getGet + new Date());
                } else if (data.getParkCode().equals("76AJJXOOM")) {
                    // 中央大街人和地下预约车辆
                    // 已经预约触发开闸
                    // 调用开关闸的接口参数
                    // 查询是否已经入场
                    HashMap<String, String> HASH_MAP_GATE = new HashMap<>();
                    HASH_MAP_GATE.put("parkCode", data.getParkCode());
                    // 此处的getEnterChannelCustomCode为通道自定义编码，且接口的value也必须为通道自定义编码
                    HASH_MAP_GATE.put("channelCode", data.getLeaveChannelCustomCode());
                    HASH_MAP_GATE.put("opType", "0");
                    HASH_MAP_GATE.put("operator", "自动操作员");
                    String getGet = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/vehicleReservation/openGates", HASH_MAP_GATE);
                    logger.info(data.getLeaveCarLicenseNumber() + "离场车辆Get = " + getGet + new Date());
                } else {
                    logger.info(data.getLeaveCarLicenseNumber() + "非太平桥百盛、中央大街人和地下预约车辆！！");
                }
            }

        }
        //创建空集合
        HashMap<Object, Object> hashEmptyMap = new HashMap<>();
        return ResponseEntity.ok(AIKEResult.success(hashEmptyMap));
    }

    @RequestMapping("/reportCarOut")
//    public ResponseEntity<AIKEResult> reportCarOut(@RequestBody JSONObject data) throws ParseException {
    public ResponseEntity<AIKEResult> reportCarOut(@RequestBody ReportCarOutData data) throws ParseException {
//        System.out.println("data = " + data);
        // 🆕 处理月票车超时检查（只针对特定车场）
        if (data.getLeaveCarLicenseNumber().contains("未识别")) {
            return ResponseEntity.ok(AIKEResult.successOpenGate());
        } else {
            if ("2KPL6XFF".equals(data.getParkCode()) && "三号门辅门出口".equals(data.getLeaveChannelName()) && "3号门入口".equals(data.getEnterChannelName())) {
                processMonthlyTicketTimeoutCheck(data);
                // 学院新城黑名单车辆拉黑条件：临时车过夜拉黑
            } else if ("76F1MLQKL".equals(data.getParkCode()) && data.getEnterVipType() == 1){
                // 学院新城黑名单车辆限制条件：临时车拉黑逻辑
                processCollegeNewCityBlacklist(data);
            }
            // 处理 appointment 表查询和微信通知（独立执行，不受其他操作影响）
            processAppointmentNotification(data);
            // 处理 violations 表更新（独立执行，即使失败也不影响上面的通知）
            processViolationsUpdate(data);
        }
        HashMap<Object, Object> hashEmptyMap = new HashMap<>();
        return ResponseEntity.ok(AIKEResult.success(hashEmptyMap));
    }

    /**
     * 接收黑名单上报数据
     * 
     * @param message 黑名单上报数据（JSON字符串格式）
     * @return 响应结果
     */
    @ApiOperation("接收黑名单上报数据")
    @RequestMapping("/reportBlacklist")
    public ResponseEntity<AIKEResult> reportBlacklist(@RequestBody String message) {
        try {
            logger.info("🚫 [黑名单上报] 接收到黑名单上报数据");
            
            // 解析JSON字符串
            JSONObject jsonData = JSONObject.parseObject(message);
            
            // 输出解析后的数据
            logger.info("✅ [黑名单上报] 解析后的数据:");
            logger.info("  📌 完整JSON: {}", jsonData.toJSONString());
            
            // 遍历并输出所有字段
            if (jsonData != null && !jsonData.isEmpty()) {
                for (String key : jsonData.keySet()) {
                    Object value = jsonData.get(key);
                    logger.info("  📌 字段 [{}] = {}", key, value);
                }
            } else {
                logger.warn("⚠️ [黑名单上报] JSON数据为空");
                HashMap<Object, Object> hashEmptyMap = new HashMap<>();
                return ResponseEntity.ok(AIKEResult.success(hashEmptyMap));
            }
            
            // 提取关键字段
            String carCode = jsonData.getString("carCode");
            Integer operateType = jsonData.getInteger("operateType");
            JSONArray parkList = jsonData.getJSONArray("parkList");
            
            logger.info("🚗 [黑名单上报] 车牌号: {}", carCode);
            logger.info("🔧 [黑名单上报] 操作类型: {}", operateType);
            logger.info("🏢 [黑名单上报] 车场列表: {}", parkList);
            
            // 验证必要参数
            if (carCode == null || carCode.isEmpty()) {
                logger.warn("⚠️ [黑名单上报] 车牌号为空，跳过处理");
                HashMap<Object, Object> hashEmptyMap = new HashMap<>();
                return ResponseEntity.ok(AIKEResult.success(hashEmptyMap));
            }
            
            if (parkList == null || parkList.isEmpty()) {
                logger.warn("⚠️ [黑名单上报] 车场列表为空，跳过处理");
                HashMap<Object, Object> hashEmptyMap = new HashMap<>();
                return ResponseEntity.ok(AIKEResult.success(hashEmptyMap));
            }
            
            // 处理删除操作
            if (operateType != null && operateType == 2) {
                logger.info("🗑️ [黑名单上报] 检测到删除操作 (operateType=2)，开始删除违规记录");
                
                int totalDeleted = 0;
                
                // 遍历车场列表，删除对应车场的违规记录
                for (int i = 0; i < parkList.size(); i++) {
                    JSONObject parkInfo = parkList.getJSONObject(i);
                    String parkName = parkInfo.getString("parkName");
                    String parkCode = parkInfo.getString("parkCode");
                    
                    if (parkName != null && !parkName.isEmpty()) {
                        logger.info("🏢 [违规记录删除] 开始删除车场 [{}] 中车牌 [{}] 的违规记录", parkName, carCode);
                        
                        try {
                            // 调用违规服务删除记录，使用parkCode作为参数
                            int deletedCount = violationsService.deleteViolationsByPlateAndPark(carCode, parkCode != null ? parkCode : parkName);
                            totalDeleted += deletedCount;
                            
                            logger.info("✅ [违规记录删除] 车场 [{}] 删除了 {} 条违规记录", parkName, deletedCount);
                            
                        } catch (Exception e) {
                            logger.error("❌ [违规记录删除] 删除车场 [{}] 违规记录时发生异常: {}", parkName, e.getMessage(), e);
                        }
                    } else {
                        logger.warn("⚠️ [违规记录删除] 车场名称为空，跳过删除");
                    }
                }
                
                logger.info("🎯 [黑名单上报] 删除操作完成，共删除 {} 条违规记录", totalDeleted);
                
                // 记录详细信息
                logger.info("📋 [黑名单详情] 车牌号: {}, 操作类型: 删除, 原因: {}", 
                        carCode, jsonData.getString("reason"));
                
            } else {
                // 非删除操作，仅记录日志
                logger.info("📝 [黑名单上报] 操作类型: {}, 非删除操作，仅记录数据", operateType);
                
                // 记录更多详细信息用于调试
                logger.info("📋 [黑名单详情] 车牌号: {}, 原因: {}, 类型: {}", 
                        carCode, 
                        jsonData.getString("reason"), 
                        jsonData.getString("blacklistTypeName"));
                
                if (jsonData.getString("blacklistStartTime") != null) {
                    logger.info("⏰ [黑名单时间] 开始时间: {}, 结束时间: {}", 
                            jsonData.getString("blacklistStartTime"), 
                            jsonData.getString("blacklistEndTime"));
                }
                
                if (jsonData.getInteger("blacklistForeverFlag") != null) {
                    logger.info("🔒 [黑名单标志] 永久拉黑: {}", 
                            jsonData.getInteger("blacklistForeverFlag") == 1 ? "是" : "否");
                }
            }
            
            logger.info("✅ [黑名单上报] 处理成功: carCode={}, operateType={}", 
                    carCode, operateType);
            
            HashMap<Object, Object> hashEmptyMap = new HashMap<>();
            return ResponseEntity.ok(AIKEResult.success(hashEmptyMap));
            
        } catch (Exception e) {
            logger.error("❌ [黑名单上报] 处理异常: {}", e.getMessage(), e);
            HashMap<Object, Object> hashEmptyMap = new HashMap<>();
            return ResponseEntity.ok(AIKEResult.success(hashEmptyMap));
        }
    }

    /**
     * 处理 appointment 表查询和微信通知（独立执行，不受其他操作影响）
     */
    private void processAppointmentNotification(ReportCarOutData data) {
        try {
            // 通过车场编号查询车场名称
            List<String> parkNameList = yardInfoService.selectByParkCode(data.getParkCode());
            String parkName = "";
            if (parkNameList.isEmpty() || parkNameList.size() == 0) {
                return;
            } else {
                parkName = parkNameList.get(0);
            }
            // 通过车牌号查询appointment表获取审核人信息
            Appointment appointmentForLeaveNotify = appointmentService.getByQueryInfo(data.getLeaveCarLicenseNumber(), parkName);
            if (appointmentForLeaveNotify != null && StringUtils.isNotBlank(appointmentForLeaveNotify.getAuditusername())) {
                HashMap<String, String> HASH_MAP_GATE = new HashMap<>();
                HASH_MAP_GATE.put("parkCode", data.getParkCode());
                // 此处的getEnterChannelCustomCode为通道自定义编码，且接口的value也必须为通道自定义编码
                HASH_MAP_GATE.put("channelCode", data.getLeaveChannelCustomCode());
                HASH_MAP_GATE.put("opType", "0");
                HASH_MAP_GATE.put("operator", "小程序操作员");
                String getGet = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/vehicleReservation/openGates", HASH_MAP_GATE);
                logger.info(data.getLeaveCarLicenseNumber() + "离场车辆Get = " + getGet + new Date());
                logger.info("🔔 [离场] 准备发送微信通知给管家: {}", appointmentForLeaveNotify.getAuditusername());
                // 格式话进场时间
                SimpleDateFormat sdfInputEnter = new SimpleDateFormat("yyyyMMddHHmmss");
                SimpleDateFormat sdfOutputEnter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date formatEnterDate = sdfInputEnter.parse(data.getEnterTime());
                String formattedEnterTime = sdfOutputEnter.format(formatEnterDate);
                // 格式化离场时间
                SimpleDateFormat sdfInputLeave = new SimpleDateFormat("yyyyMMddHHmmss");
                SimpleDateFormat sdfOutputLeave = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date formatLeaveDate = sdfInputLeave.parse(data.getLeaveTime());
                String formattedLeaveTime = sdfOutputLeave.format(formatLeaveDate);
                String leaveChannel = channelInfoService.getByChnnelId((int) data.getLeaveChannelId());
                // 更新appointment表中的离场时间和离场状态
                int result = appointmentService.updateLeaveTimeByCarNumber(data.getLeaveCarLicenseNumber(), formattedEnterTime, formattedLeaveTime);
                if (result != 0) {
                    logger.info("✅ [离场] appointment表更新成功: plateNumber={}", data.getLeaveCarLicenseNumber());
                }
                Map<String, Object> sendResultLeave = weChatTemplateMessageService.sendParkingLeaveNotification(
                        data.getLeaveCarLicenseNumber(),
                        parkName,
                        formattedEnterTime,
                        formattedLeaveTime,
                        appointmentForLeaveNotify.getAuditusername(),
                        leaveChannel
                );
                if ((Boolean) sendResultLeave.get("success")) {
                    logger.info("✅ [离场] 微信通知发送成功");
                } else {
                    logger.warn("⚠️ [离场] 微信通知发送失败: {}", sendResultLeave.get("message"));
                }
            } else {
                logger.warn("⚠️ [离场] 未找到审核人信息，跳过微信通知");
            }
        } catch (Exception e) {
            logger.error("❌ [离场] 发送微信通知异常", e);
        }
    }

    /**
     * 处理 violations 表更新（独立执行，即使失败也不影响其他操作）
     */
    private void processViolationsUpdate(ReportCarOutData data) {
        try {
            if (data.getLeaveCarLicenseNumber() != null && data.getParkCode() != null &&
                    data.getEnterTime() != null && data.getLeaveTime() != null) {

                logger.info("🔄 [离场] 开始处理violations表更新: plateNumber={}, parkCode={}, enterTime={}, leaveTime={}",
                        data.getLeaveCarLicenseNumber(), data.getParkCode(), data.getEnterTime(), data.getLeaveTime());
                // 调用violations服务更新离场时间
                boolean updateResult = violationsService.updateLeaveTimeByPlateAndTime(
                        data.getLeaveCarLicenseNumber(),
                        data.getParkCode(),
                        data.getEnterTime(),
                        data.getLeaveTime()
                );
                if (updateResult) {
                    logger.info("✅ [离场] violations表更新成功: plateNumber={}", data.getLeaveCarLicenseNumber());
                } else {
                    logger.warn("⚠️ [离场] violations表更新失败或未找到匹配记录: plateNumber={}", data.getLeaveCarLicenseNumber());
                }
            } else {
                logger.warn("⚠️ [离场] 缺少必要参数，跳过violations表更新");
            }
        } catch (Exception e) {
            logger.error("❌ [离场] violations表更新异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理月票车超时检查（只针对特定车场）
     */
    private void processMonthlyTicketTimeoutCheck(ReportCarOutData data) {
        try {
            logger.info("🎫 [车辆超时检查] 开始处理: plateNumber={}, parkCode={}",
                    data.getLeaveCarLicenseNumber(), data.getParkCode());

            // 🆕 处理临时车的特殊情况
            boolean isTemporaryCar = false;
            String actualEnterTime = data.getEnterTime();

            // 1. 通过enterCustomVipName字段判断是否为月票车
            String enterCustomVipName = data.getEnterCustomVipName();
            boolean isMonthTicketCar = (enterCustomVipName != null && !enterCustomVipName.trim().isEmpty());

            // 🆕 如果不是月票车，检查是否为临时车的两种情况
            if (!isMonthTicketCar) {
                // 检查临时车分类：
                // 第一类：有enterType字段且值为6的情况（有正常进场时间，不需要调整）
                // 第二类：没有enterType字段的情况（进场时间和离场时间一样，需要使用预进场数据）

                Integer enterType = data.getEnterType();
                if (enterType != null && enterType == 6) {
                    // 第一类：有正常进场时间的临时车，不需要调整
                    logger.info("ℹ️ [临时车-第一类] plateNumber={}, enterType={}, 有正常进场时间，无需调整",
                            data.getLeaveCarLicenseNumber(), enterType);
                    isTemporaryCar = true;
                } else if (enterType == null || data.getEnterTime().equals(data.getLeaveTime())) {
                    // 第二类：没有enterType或进场时间等于离场时间的情况
                    logger.info("🔄 [临时车-第二类] plateNumber={}, enterType={}, 进场时间可能不准确，尝试使用预进场数据",
                            data.getLeaveCarLicenseNumber(), enterType);

                    // 尝试从临时车预进场数据中获取真实的进场时间
                    String preEnterTime = tempCarPreEntryService.getLatestPreEnterTime(
                            data.getLeaveCarLicenseNumber(),
                            data.getParkCode()
                    );

                    if (preEnterTime != null) {
                        // 转换为yyyyMMddHHmmss格式
                        try {
                            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            SimpleDateFormat sdfOutput = new SimpleDateFormat("yyyyMMddHHmmss");
                            Date preEnterDate = sdfInput.parse(preEnterTime);
                            actualEnterTime = sdfOutput.format(preEnterDate);

                            logger.info("✅ [使用预进场时间] plateNumber={}, originalEnterTime={}, actualEnterTime={}",
                                    data.getLeaveCarLicenseNumber(), data.getEnterTime(), actualEnterTime);

                            // 标记该预进场记录为已使用
                            tempCarPreEntryService.markAsUsed(
                                    data.getLeaveCarLicenseNumber(),
                                    data.getParkCode(),
                                    preEnterTime
                            );
                            isTemporaryCar = true;
                        } catch (Exception e) {
                            logger.error("❌ [时间格式转换异常] preEnterTime={}, error={}",
                                    preEnterTime, e.getMessage(), e);
                        }
                    } else {
                        logger.warn("⚠️ [未找到预进场数据] plateNumber={}, parkCode={}, 使用原始进场时间",
                                data.getLeaveCarLicenseNumber(), data.getParkCode());
                        isTemporaryCar = true;
                    }
                } else {
                    // 其他情况的临时车
                    logger.info("ℹ️ [临时车-其他情况] plateNumber={}, enterType={}",
                            data.getLeaveCarLicenseNumber(), enterType);
                    isTemporaryCar = true;
                }
            }

            String ticketName = enterCustomVipName;
            boolean isExemptTicket = false;

            if (isMonthTicketCar) {
                // 检查是否为免检的月票类型
                isExemptTicket = isExemptMonthlyTicketType(data.getParkCode(), ticketName);
                logger.info("✅ [月票车] plateNumber={}, ticketName={}, isExempt={}",
                        data.getLeaveCarLicenseNumber(), ticketName, isExemptTicket);
            } else {
                logger.info("ℹ️ [非月票车] plateNumber={}, enterCustomVipName为空", data.getLeaveCarLicenseNumber());
            }

            // 2. 计算停车时长（使用实际进场时间）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            SimpleDateFormat outputSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Date enterTimeDate = sdf.parse(actualEnterTime); // 🆕 使用actualEnterTime
            Date leaveTimeDate = sdf.parse(data.getLeaveTime());

            String formattedEnterTime = outputSdf.format(enterTimeDate);
            String formattedLeaveTime = outputSdf.format(leaveTimeDate);

            // 转换为LocalDateTime用于过夜判定
            LocalDateTime enterDateTime = enterTimeDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime leaveDateTime = leaveTimeDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();

            // 计算停车时长（分钟）
            long parkingDurationMillis = leaveTimeDate.getTime() - enterTimeDate.getTime();
            long parkingDurationMinutes = parkingDurationMillis / (1000 * 60);
            long parkingDurationHours = parkingDurationMinutes / 60;

            logger.info("⏱️ [停车时长] plateNumber={}, duration={}分钟({}小时)",
                    data.getLeaveCarLicenseNumber(), parkingDurationMinutes, parkingDurationHours);

            // 3. 获取月票车超时配置（包含过夜配置）
            Map<String, Object> config = violationsService.getMonthlyTicketTimeoutConfig(data.getParkCode());
            Integer timeoutMinutes = (Integer) config.get("timeoutMinutes");
            // 4. 使用配置化的智能过夜判定
            boolean isOvernight = checkIntelligentOvernightViolation(data.getParkCode(), enterDateTime, leaveDateTime, parkingDurationMinutes);
            // 5. 判断是否超时或过夜
            boolean isViolation = false;
            String violationType = "";
            String violationReason = "";
            boolean shouldDirectBlacklist = false;
            boolean shouldCumulativeBlacklist = false; // 🆕 新增累计拉黑标识

            if (isOvernight) {
                logger.warn("🌙 [车辆过夜] plateNumber={}, duration={}小时, isExempt={}, isTemporaryCar={}",
                        data.getLeaveCarLicenseNumber(), parkingDurationHours, isExemptTicket, isTemporaryCar);
                // 🆕 免检月票类型过夜也不记录违规（根据用户需求）
                if (!isExemptTicket) {
                    isViolation = true;
                    if (isMonthTicketCar) {
                        violationType = "月票车过夜停车";
                    } else if (isTemporaryCar) {
                        violationType = "临时车过夜停车"; // 🆕 临时车过夜类型
                    } else {
                        violationType = "车辆过夜停车";
                    }
                    violationReason = "夜间时段停车违规";
                    shouldDirectBlacklist = true; // 过夜直接拉黑
                } else {
                    logger.info("🛡️ [免检处理] plateNumber={}, ticketName={}, 过夜但免检不处理",
                            data.getLeaveCarLicenseNumber(), ticketName);
                }
            } else if ((isMonthTicketCar || isTemporaryCar) && parkingDurationMinutes > timeoutMinutes) {
                // 月票车或临时车检查白天超时
                if (isMonthTicketCar) {
                    logger.warn("⏰ [月票车超时] plateNumber={}, duration={}分钟, limit={}分钟, isExempt={}",
                            data.getLeaveCarLicenseNumber(), parkingDurationMinutes, timeoutMinutes, isExemptTicket);

                    // 🆕 免检类型的月票车不记录超时违规（完全不处理）
                    if (!isExemptTicket) {
                        isViolation = true;
                        violationType = "月票车超时停车";
                        violationReason = "超时停车，需累计处理";
                        shouldDirectBlacklist = false; // 超时需累计处理
                        shouldCumulativeBlacklist = true;
                    } else {
                        logger.info("🛡️ [免检处理] plateNumber={}, ticketName={}, 超时但免检不处理",
                                data.getLeaveCarLicenseNumber(), ticketName);
                    }
                } else if (isTemporaryCar) {
                    // 🆕 临时车白天超时停车（累计违规处理）
                    logger.warn("⏰ [临时车白天超时] plateNumber={}, duration={}分钟, limit={}分钟",
                            data.getLeaveCarLicenseNumber(), parkingDurationMinutes, timeoutMinutes);
                    isViolation = true;
                    violationType = "临时车超时停车";
                    violationReason = "超时停车，需累计处理";
                    shouldDirectBlacklist = false; // 超时需累计处理
                    shouldCumulativeBlacklist = true;
                }
            } else {
                // 🆕 新增逻辑：检查凌晨进场的违规情况
                boolean isEarlyMorningEntry = checkEarlyMorningEntry(enterDateTime);
                if (isEarlyMorningEntry && parkingDurationMinutes > timeoutMinutes) {
                    if (isMonthTicketCar && !isExemptTicket) {
                        // 月票车凌晨进场超时
                        logger.warn("🌅 [月票车凌晨进场超时] plateNumber={}, duration={}分钟, dayLimit={}分钟",
                                data.getLeaveCarLicenseNumber(), parkingDurationMinutes, timeoutMinutes);
                        isViolation = true;
                        violationType = "月票车凌晨进场超时";
                        violationReason = "凌晨进场停车超时";
                        shouldDirectBlacklist = false;
                        shouldCumulativeBlacklist = true;
                    } else if (isTemporaryCar) {
                        // 🆕 临时车凌晨进场超时
                        logger.warn("🌅 [临时车凌晨进场超时] plateNumber={}, duration={}分钟, dayLimit={}分钟",
                                data.getLeaveCarLicenseNumber(), parkingDurationMinutes, timeoutMinutes);
                        isViolation = true;
                        violationType = "临时车凌晨进场超时";
                        violationReason = "凌晨进场停车超时";
                        shouldDirectBlacklist = false;
                        shouldCumulativeBlacklist = true;
                    } else if (!isMonthTicketCar && !isTemporaryCar) {
                        // 🆕 其他车辆凌晨进场超时
                        logger.warn("🌅 [其他车辆凌晨进场超时] plateNumber={}, duration={}分钟, dayLimit={}分钟",
                                data.getLeaveCarLicenseNumber(), parkingDurationMinutes, timeoutMinutes);
                        isViolation = true;
                        violationType = "其他车辆凌晨进场超时";
                        violationReason = "凌晨进场停车超时";
                        shouldDirectBlacklist = false;
                        shouldCumulativeBlacklist = true;
                    }
                }
            }

            if (isViolation) {
                // 6. 获取停车场名称
                List<String> parkNameList = yardInfoService.selectByParkCode(data.getParkCode());
                String parkName = parkNameList.isEmpty() ? "未知停车场" : parkNameList.get(0);

                // 7. 查询月票ID（通过车牌号码在month_tick表中查找）
                Integer monthTicketId = null;
                if (isMonthTicketCar) {
                    monthTicketId = queryMonthTicketIdByPlateNumber(data.getLeaveCarLicenseNumber());
                    if (monthTicketId != null) {
                        logger.info("✅ [查询到月票ID] plateNumber={}, monthTicketId={}",
                                data.getLeaveCarLicenseNumber(), monthTicketId);
                    } else {
                        logger.warn("⚠️ [未查询到月票ID] plateNumber={}, 将使用null", data.getLeaveCarLicenseNumber());
                    }
                }

                // 8. 检查是否已经存在相同时间段的违规记录（避免重复记录）
                boolean isDuplicateViolation = violationsService.checkDuplicateViolation(
                        data.getLeaveCarLicenseNumber(),
                        data.getParkCode(),
                        enterDateTime,
                        leaveDateTime
                );

                if (isDuplicateViolation) {
                    logger.warn("⚠️ [重复违规跳过] plateNumber={}, parkCode={}, enterTime={}, leaveTime={}, " +
                                "已存在相同时间段的违规记录，跳过自动记录避免重复",
                            data.getLeaveCarLicenseNumber(), data.getParkCode(),
                            enterDateTime, leaveDateTime);
                    return;
                }

                // 9. 记录违规
                String violationDescription = violationReason + "，停车时长：" + parkingDurationMinutes + "分钟";
                boolean recordResult = violationsService.recordViolation(
                        data.getLeaveCarLicenseNumber(),
                        data.getParkCode(),
                        parkName,
                        enterDateTime,
                        leaveDateTime,
                        parkingDurationMinutes,
                        violationType,
                        violationDescription,
                        monthTicketId, // 使用查询到的月票ID
                        shouldDirectBlacklist
                );

                if (recordResult) {
                    logger.info("✅ [违规记录成功] plateNumber={}, violationType={}",
                            data.getLeaveCarLicenseNumber(), violationType);

                    // 9. 分步骤处理拉黑逻辑
                    if (shouldDirectBlacklist) {
                        // 过夜停车直接拉黑
                        logger.warn("🚫 [过夜直接拉黑] plateNumber={}, reason={}",
                                data.getLeaveCarLicenseNumber(), violationReason);
                        boolean blacklistResult = violationsService.addToBlacklist(
                                data.getLeaveCarLicenseNumber(),
                                parkName,
                                violationType,
                                violationReason + "，系统自动拉黑"
                        );
                        if (blacklistResult) {
                            logger.info("✅ [拉黑成功] plateNumber={}", data.getLeaveCarLicenseNumber());
                            // 调用黑名单接口
                            callAddBlackListCarAPI(data.getLeaveCarLicenseNumber(), data.getParkCode(),
                                    parkName, violationType, violationReason + "，系统自动拉黑");
                        } else {
                            logger.error("❌ [拉黑失败] plateNumber={}", data.getLeaveCarLicenseNumber());
                        }
                    } else if (shouldCumulativeBlacklist) {
                        // 🆕 累计违规处理（包括超时停车和夜间停车超白天限制）
                        logger.info("📊 [累计违规处理] plateNumber={}, reason={}",
                                data.getLeaveCarLicenseNumber(), violationReason);
                        boolean shouldBlacklist = violationsService.checkAndProcessBlacklist(
                                data.getLeaveCarLicenseNumber(),
                                data.getParkCode()
                        );
                        if (shouldBlacklist) {
                            logger.warn("🚫 [累计拉黑] plateNumber={}", data.getLeaveCarLicenseNumber());
                            // 欧洲新城累计拉黑时，达到阈值也需要清理该车牌的违规记录
                            if ("2KPL6XFF".equals(data.getParkCode())) {
                                try {
                                    int deletedCount = violationsService.deleteViolationsByPlateAndPark(
                                            data.getLeaveCarLicenseNumber(),
                                            data.getParkCode()
                                    );
                                    logger.info("🧹 [累计拉黑清理] plateNumber={}, parkCode={}, deletedCount={}",
                                            data.getLeaveCarLicenseNumber(), data.getParkCode(), deletedCount);
                                } catch (Exception ex) {
                                    logger.error("❌ [累计拉黑清理异常] plateNumber={}, parkCode={}, error={}",
                                            data.getLeaveCarLicenseNumber(), data.getParkCode(), ex.getMessage(), ex);
                                }
                            }
                            // 调用黑名单接口
                            String blacklistType;
                            if (isMonthTicketCar) {
                                blacklistType = "月票车累计违规";
                            } else if (isTemporaryCar) {
                                blacklistType = "临时车累计违规"; // 🆕 临时车累计违规类型
                            } else {
                                blacklistType = "其他车辆累计违规";
                            }
                            callAddBlackListCarAPI(data.getLeaveCarLicenseNumber(), data.getParkCode(),
                                    parkName, blacklistType, violationReason + "累计达到阈值，系统自动拉黑");
                        } else {
                            logger.info("⚠️ [违规记录] plateNumber={}, 尚未达到拉黑阈值", data.getLeaveCarLicenseNumber());
                        }
                    } else {
                        // 🆕 不需要拉黑的情况（例如免检类型的车辆）
                        logger.info("ℹ️ [违规记录但不拉黑] plateNumber={}, reason={}",
                                data.getLeaveCarLicenseNumber(), violationReason);
                    }
                } else {
                    logger.error("❌ [违规记录失败] plateNumber={}", data.getLeaveCarLicenseNumber());
                }
            } else {
                logger.info("✅ [正常停车] plateNumber={}, duration={}分钟",
                        data.getLeaveCarLicenseNumber(), parkingDurationMinutes);
            }

        } catch (Exception e) {
            logger.error("❌ [月票车超时检查异常] plateNumber={}, error={}",
                    data.getLeaveCarLicenseNumber(), e.getMessage(), e);
        }
    }

    /**
     * 检查本地月票表是否存在对应车牌的月票记录，如果本地没有则调用接口查询并存储
     * @param plateNumber 车牌号
     * @param parkCode 车场编码
     * @return 如果存在返回月票ID，否则返回null
     */
    private Integer checkLocalMonthTicket(String plateNumber, String parkCode) {
        try {
            logger.info("🔍 [查询本地月票] 开始查询: plateNumber={}, parkCode={}", plateNumber, parkCode);

            // 1. 首先查询本地数据库
            Integer localResult = queryLocalMonthTicket(plateNumber);
            if (localResult != null) {
                return localResult;
            }
            // 2. 本地没有数据，调用接口查询并存储
            logger.info("📡 [查询月票] plateNumber={}, parkCode={}", plateNumber, parkCode);
            boolean importSuccess = importMonthTicketFromAPI(plateNumber, parkCode);
            if (importSuccess) {
                // 3. 重新查询本地数据库
                logger.info("🔄 [重新查询本地月票] plateNumber={}", plateNumber);
                return queryLocalMonthTicket(plateNumber);
            } else {
                logger.warn("⚠️ [接口查询失败] plateNumber={}, 无法获取月票数据", plateNumber);
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ [查询本地月票异常] plateNumber={}, error={}", plateNumber, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 查询本地月票数据
     * @param plateNumber 车牌号
     * @return 月票ID或null
     */
    private Integer queryLocalMonthTicket(String plateNumber) {
        try {
            // 使用 like 查询，因为 carNo 字段可能包含多个车牌号（逗号分隔）
            LambdaQueryWrapper<MonthTick> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.like(MonthTick::getCarNo, plateNumber)
                    .eq(MonthTick::getValidStatus, 1) // 只查询有效的月票
                    .orderByDesc(MonthTick::getId); // 按ID倒序，当有重复数据时返回ID最大的记录

            List<MonthTick> monthTickets = monthTicketService.list(queryWrapper);

            if (monthTickets != null && !monthTickets.isEmpty()) {
                // 进一步验证车牌号是否确实匹配（防止部分匹配的误判）
                for (MonthTick monthTick : monthTickets) {
                    String carNo = monthTick.getCarNo();
                    if (carNo != null) {
                        // 将逗号分隔的车牌号分割并检查是否包含目标车牌
                        String[] plateNumbers = carNo.split(",");
                        for (String plate : plateNumbers) {
                            if (plate.trim().equals(plateNumber)) {
                                logger.info("✅ [月票匹配成功] plateNumber={}, monthTicketId={}, ticketName={}",
                                        plateNumber, monthTick.getId(), monthTick.getTicketName());
                                return monthTick.getId();
                            }
                        }
                    }
                }
                logger.warn("⚠️ [月票匹配失败] plateNumber={}, 找到{}条记录但车牌号不完全匹配",
                        plateNumber, monthTickets.size());
            } else {
                logger.info("ℹ️ [未找到月票] plateNumber={}, 本地表中无对应记录", plateNumber);
            }
            return null;

        } catch (Exception e) {
            logger.error("❌ [查询本地月票数据异常] plateNumber={}, error={}", plateNumber, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 通过车牌号码查询月票ID
     * @param plateNumber 车牌号码
     * @return 月票ID，如果未找到返回null
     */
    private Integer queryMonthTicketIdByPlateNumber(String plateNumber) {
        try {
            logger.info("🔍 [查询月票ID] plateNumber={}", plateNumber);

            // 使用 like 查询，因为 carNo 字段可能包含多个车牌号（逗号分隔）
            LambdaQueryWrapper<MonthTick> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.like(MonthTick::getCarNo, plateNumber)
                    .eq(MonthTick::getValidStatus, 1) // 只查询有效的月票
                    .orderByDesc(MonthTick::getId); // 按ID倒序，获取最新的记录

            List<MonthTick> monthTickets = monthTicketService.list(queryWrapper);

            if (monthTickets != null && !monthTickets.isEmpty()) {
                // 进一步验证车牌号是否确实匹配（防止部分匹配的误判）
                for (MonthTick monthTick : monthTickets) {
                    String carNo = monthTick.getCarNo();
                    if (carNo != null) {
                        // 将逗号分隔的车牌号分割并检查是否包含目标车牌
                        String[] plateNumbers = carNo.split(",");
                        for (String plate : plateNumbers) {
                            if (plate.trim().equals(plateNumber)) {
                                logger.info("✅ [找到月票ID] plateNumber={}, monthTicketId={}, ticketName={}, carNo={}",
                                        plateNumber, monthTick.getId(), monthTick.getTicketName(), carNo);
                                return monthTick.getId();
                            }
                        }
                    }
                }
                logger.warn("⚠️ [月票ID匹配失败] plateNumber={}, 找到{}条记录但车牌号不完全匹配",
                        plateNumber, monthTickets.size());
            } else {
                logger.info("ℹ️ [未找到月票ID] plateNumber={}, 本地表中无对应记录", plateNumber);
            }

            return null;

        } catch (Exception e) {
            logger.error("❌ [查询月票ID异常] plateNumber={}, error={}", plateNumber, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从外部API导入月票数据
     * @param plateNumber 车牌号
     * @param parkCode 车场编码
     * @return 是否导入成功
     */
    private boolean importMonthTicketFromAPI(String plateNumber, String parkCode) {
        try {
            logger.info("📡 [开始从API导入月票数据] plateNumber={}, parkCode={}", plateNumber, parkCode);

            // 1. 获取车场名称
            String parkName = getParkNameByCode(parkCode);
            if (parkName == null) {
                logger.error("❌ [未知车场编码] parkCode={}", parkCode);
                return false;
            }

            // 2. 构建查询参数
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("parkCodeList", parkCode);
            hashMap.put("pageSize", "100");
            hashMap.put("validStatus", "1"); // 只查询有效的月票

            logger.info("🔗 [调用外部API] 参数: {}", hashMap);

            // 3. 调用外部接口获取第一页数据
            String response = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/monthTicket/getOnlineMonthTicketList", hashMap);
            JSONObject jsonObject = JSONObject.parseObject(response);

            if (jsonObject == null) {
                logger.error("❌ [API响应为空] parkCode={}", parkCode);
                return false;
            }

            JSONObject data1 = (JSONObject) jsonObject.get("data");
            if (data1 == null) {
                logger.error("❌ [API响应数据为空] parkCode={}", parkCode);
                return false;
            }

            JSONObject data2 = (JSONObject) data1.get("data");
            if (data2 == null) {
                logger.error("❌ [API响应内层数据为空] parkCode={}", parkCode);
                return false;
            }

            Integer total = data2.getInteger("total");
            JSONArray recordList = data2.getJSONArray("recordList");

            logger.info("📊 [API返回数据] total={}, 第一页记录数={}", total, recordList != null ? recordList.size() : 0);

            int totalImported = 0;
            boolean foundTargetPlate = false;

            // 4. 处理第一页数据
            if (recordList != null && recordList.size() > 0) {
                int[] pageResult = processImportMonthTicketData(recordList, parkName, plateNumber);
                totalImported += pageResult[0];
                foundTargetPlate = pageResult[1] > 0;
            }

            // 5. 如果还有更多页数据且还没找到目标车牌，继续查询
            if (!foundTargetPlate && total != null && total > 100) {
                int totalPages = (total + 99) / 100; // 向上取整
                logger.info("🔄 [需要查询更多页] 总页数={}", totalPages);

                for (int page = 2; page <= totalPages && !foundTargetPlate; page++) {
                    hashMap.put("pageNum", String.valueOf(page));
                    String pageResponse = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/monthTicket/getOnlineMonthTicketList", hashMap);
                    JSONObject pageJsonObject = JSONObject.parseObject(pageResponse);

                    if (pageJsonObject != null) {
                        JSONObject pageData1 = (JSONObject) pageJsonObject.get("data");
                        if (pageData1 != null) {
                            JSONObject pageData2 = (JSONObject) pageData1.get("data");
                            if (pageData2 != null) {
                                JSONArray pageRecordList = pageData2.getJSONArray("recordList");
                                if (pageRecordList != null && pageRecordList.size() > 0) {
                                    int[] pageResultNext = processImportMonthTicketData(pageRecordList, parkName, plateNumber);
                                    totalImported += pageResultNext[0];
                                    foundTargetPlate = pageResultNext[1] > 0;

                                    logger.info("📄 [处理第{}页] 导入{}条记录, 是否找到目标车牌={}", page, pageResultNext[0], foundTargetPlate);
                                }
                            }
                        }
                    }
                }
            }

            logger.info("✅ [API导入完成] 总共导入{}条记录, 是否找到目标车牌={}", totalImported, foundTargetPlate);
            return totalImported > 0 || foundTargetPlate;

        } catch (Exception e) {
            logger.error("❌ [从API导入月票数据异常] plateNumber={}, parkCode={}, error={}", plateNumber, parkCode, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 处理从API导入的月票数据
     * @param recordList API返回的记录列表
     * @param parkName 车场名称
     * @param targetPlateNumber 目标车牌号（用于判断是否找到）
     * @return int数组，[0]为导入数量，[1]为是否找到目标车牌（0或1）
     */
    private int[] processImportMonthTicketData(JSONArray recordList, String parkName, String targetPlateNumber) {
        int importedCount = 0;
        int foundTargetPlate = 0;

        try {
            logger.info("🔄 [开始处理月票数据] 记录数={}, 车场={}, 目标车牌={}", recordList.size(), parkName, targetPlateNumber);

            for (int i = 0; i < recordList.size(); i++) {
                JSONObject jsonObject1 = recordList.getJSONObject(i);

                // 创建MonthTick对象
                MonthTick monthTick = new MonthTick();

                // 设置基本信息
                monthTick.setCarNo(processCarNo(jsonObject1.getString("carNo")));
                monthTick.setCreateTime(jsonObject1.getString("createTime"));
                monthTick.setCreateBy(jsonObject1.getString("createBy"));
                monthTick.setTicketName(jsonObject1.getString("ticketName"));
                // 处理可能为null的字段
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
                        String startTime = convertDateFormatForMonthTicket(jsonObjectTime.getString("startTime"));
                        String endTime = convertDateFormatForMonthTicket(jsonObjectTime.getString("endTime"));
                        str.append("startTime:").append(startTime).append(",endTime:").append(endTime);
                        if (i2 < timePeriodList1.size() - 1) {
                            str.append(";");
                        }
                    }
                }
                monthTick.setTimePeriodList(str.toString());

                // 检查是否是目标车牌
                String carNo = monthTick.getCarNo();
                if (carNo != null && carNo.contains(targetPlateNumber)) {
                    foundTargetPlate = 1;
                    logger.info("🎯 [找到目标车牌] plateNumber={}, carNo={}", targetPlateNumber, carNo);
                }

                // 检查数据库中是否已存在
                LambdaQueryWrapper<MonthTick> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(MonthTick::getCarNo, monthTick.getCarNo())
                           .eq(MonthTick::getParkName, parkName);
                MonthTick existingRecord = monthTicketService.getOne(queryWrapper);

                if (existingRecord == null) {
                    // 新增记录
                    boolean saved = monthTicketService.save(monthTick);
                    if (saved) {
                        importedCount++;
                        logger.debug("➕ [新增月票记录] carNo={}, parkName={}", monthTick.getCarNo(), parkName);
                    }
                } else {
                    // 更新记录
                    monthTick.setId(existingRecord.getId());
                    boolean updated = monthTicketService.updateById(monthTick);
                    if (updated) {
                        importedCount++;
                        logger.debug("🔄 [更新月票记录] carNo={}, parkName={}", monthTick.getCarNo(), parkName);
                    }
                }
            }

            logger.info("✅ [处理月票数据完成] 导入数量={}, 是否找到目标车牌={}", importedCount, foundTargetPlate);

        } catch (Exception e) {
            logger.error("❌ [处理月票数据异常] error={}", e.getMessage(), e);
        }

        return new int[]{importedCount, foundTargetPlate};
    }

    /**
     * 月票数据专用的日期转换方法
     * @param input 输入的日期字符串
     * @return 转换后的日期字符串
     */
    private String convertDateFormatForMonthTicket(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = inputFormat.parse(input);
            return outputFormat.format(date);
        } catch (Exception e) {
            logger.warn("⚠️ [日期转换失败] input={}, 使用原值", input);
            return input;
        }
    }

    /**
     * 根据车场编码获取车场名称
     * @param parkCode 车场编码
     * @return 车场名称
     */
    private String getParkNameByCode(String parkCode) {
        switch (parkCode) {
            case "2KST9MNP":
                return "万象上东";
            case "2KUG6XLU":
                return "四季上东";
            case "2KPL6XFF":
                return "欧洲新城";
            default:
                logger.warn("⚠️ [未知车场编码] parkCode={}", parkCode);
                return null;
        }
    }

    /**
     * 判断是否过夜停车
     * 跨日期且超过12小时认为过夜
     */
    private boolean isOvernightParking(Date enterTime, Date leaveTime) {
        try {
            SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd");
            String enterDate = dateSdf.format(enterTime);
            String leaveDate = dateSdf.format(leaveTime);

            // 跨日期
            boolean crossDate = !enterDate.equals(leaveDate);

            // 超过12小时
            long durationHours = (leaveTime.getTime() - enterTime.getTime()) / (1000 * 60 * 60);
            boolean longParking = durationHours >= 12;

            return crossDate && longParking;
        } catch (Exception e) {
            logger.error("❌ [过夜判断异常] error={}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 🆕 智能过夜停车判定（基于新的过夜规则）
     *
     * 新规则：
     * 1. 进场时间必须在凌晨0点之前（前一天）
     * 2. 停车总时长超过配置的参数值则直接拉黑（如：晚上11:30进来，第二天凌晨2:30离场）
     * 3. 凌晨0点之后进场的（如：凌晨2:00进来，5点走）算作违规但不直接拉黑
     *
     * @param parkCode 车场编码
     * @param enterTime 进场时间
     * @param leaveTime 离场时间
     * @param parkingDurationMinutes 停车时长（分钟）
     * @return 是否为过夜违规（直接拉黑）
     */
    private boolean checkIntelligentOvernightViolation(String parkCode, LocalDateTime enterTime,
                                                     LocalDateTime leaveTime, long parkingDurationMinutes) {
        try {
            logger.info("🧠 [新版过夜判定] parkCode={}, enterTime={}, leaveTime={}, duration={}分钟",
                    parkCode, enterTime, leaveTime, parkingDurationMinutes);

            // 1. 获取车场的过夜停车配置
            Map<String, Object> config = violationsService.getMonthlyTicketTimeoutConfig(parkCode);

            if (config == null) {
                logger.warn("⚠️ [配置缺失] parkCode={} 未找到配置，使用传统过夜判定", parkCode);
                // 使用传统方法：跨日期且超过12小时
                Date enterDate = Date.from(enterTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
                Date leaveDate = Date.from(leaveTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
                return isOvernightParking(enterDate, leaveDate);
            }

            // 2. 从配置中获取过夜时长限制（小时）
            Integer timeoutMinutes = (Integer) config.get("timeoutMinutes");
            Integer nightTimeHours = 4; // 默认值

            if (timeoutMinutes != null) {
                nightTimeHours = timeoutMinutes / 60; // 转换为小时
            }

            // 尝试从描述中解析更精确的配置
            String description = (String) config.get("description");
            if (description != null && description.contains("过夜")) {
                try {
                    // 解析超过X小时的配置
                    int hoursIndex = description.indexOf("超过");
                    if (hoursIndex != -1) {
                        int hoursEndIndex = description.indexOf("小时", hoursIndex);
                        if (hoursEndIndex != -1) {
                            String hoursStr = description.substring(hoursIndex + 2, hoursEndIndex).trim();
                            try {
                                nightTimeHours = Integer.parseInt(hoursStr);
                                logger.info("🔧 [解析过夜时长限制] 从描述中提取: {}小时", nightTimeHours);
                            } catch (NumberFormatException e) {
                                logger.warn("⚠️ [解析过夜时长失败] 使用配置值: {}小时", nightTimeHours);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ [解析配置描述异常] 使用配置值: {}小时, error={}", nightTimeHours, e.getMessage());
                }
            }

            logger.info("🌙 [过夜配置] parkCode={}, 过夜时长限制: {}小时", parkCode, nightTimeHours);

            // 3. 新的过夜判定逻辑
            boolean isOvernightViolation = checkNewOvernightRule(enterTime, leaveTime,
                    parkingDurationMinutes, nightTimeHours);

            logger.info("📊 [过夜分析结果] parkCode={}, isViolation={}", parkCode, isOvernightViolation);

            return isOvernightViolation;

        } catch (Exception e) {
            logger.error("❌ [智能过夜判定异常] parkCode={}, error={}", parkCode, e.getMessage(), e);

            // 异常情况下使用传统判定方法
            Date enterDate = Date.from(enterTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
            Date leaveDate = Date.from(leaveTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
            return isOvernightParking(enterDate, leaveDate);
        }
    }

    /**
     * 🆕 新的过夜判定规则
     *
     * 规则说明：
     * 1. 进场时间在凌晨0点之前（前一天）+ 停车时长超过限制 = 直接拉黑
     * 2. 进场时间在凌晨0点之后（当天） = 违规但不直接拉黑
     *
     * @param enterTime 进场时间
     * @param leaveTime 离场时间
     * @param parkingDurationMinutes 停车时长（分钟）
     * @param nightTimeHours 过夜时长限制（小时）
     * @return 是否为过夜违规（直接拉黑）
     */
    private boolean checkNewOvernightRule(LocalDateTime enterTime, LocalDateTime leaveTime,
                                        long parkingDurationMinutes, int nightTimeHours) {
        try {
            logger.info("🔍 [新过夜规则判定] enterTime={}, leaveTime={}, duration={}分钟, limit={}小时",
                    enterTime, leaveTime, parkingDurationMinutes, nightTimeHours);

            // 获取进场时间的小时
            int enterHour = enterTime.getHour();

            // 判断是否跨日停车
            boolean isCrossDayParking = !enterTime.toLocalDate().equals(leaveTime.toLocalDate());

            // 停车时长（小时）
            double parkingDurationHours = parkingDurationMinutes / 60.0;

            logger.info("📊 [停车分析] enterHour={}, isCrossDayParking={}, durationHours={:.2f}",
                    enterHour, isCrossDayParking, parkingDurationHours);

            // 新规则判定
            if (isCrossDayParking) {
                // 跨日停车的情况
                if (enterHour < 24 && enterHour >= 0) { // 进场时间在凌晨0点之前（前一天任何时间）
                    // 检查停车时长是否超过限制
                    if (parkingDurationHours > nightTimeHours) {
                        logger.warn("🚫 [过夜直接拉黑] 前一天进场跨日停车超时: enterHour={}, duration={:.2f}小时 > limit={}小时",
                                enterHour, parkingDurationHours, nightTimeHours);
                        return true; // 直接拉黑
                    } else {
                        logger.info("✅ [跨日停车正常] 前一天进场但未超时: duration={:.2f}小时 <= limit={}小时",
                                parkingDurationHours, nightTimeHours);
                        return false; // 不拉黑
                    }
                }
            } else {
                // 同日停车的情况
                if (enterHour >= 0 && enterHour < 6) { // 凌晨0点到6点进场
                    logger.info("ℹ️ [凌晨进场] 当天凌晨进场，算作违规但不直接拉黑: enterHour={}", enterHour);
                    return false; // 违规但不直接拉黑，由调用方处理累计违规
                }
            }

            // 其他情况不算过夜违规
            logger.info("✅ [正常停车] 不符合过夜违规条件");
            return false;

        } catch (Exception e) {
            logger.error("❌ [新过夜规则判定异常] error={}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 🆕 检查是否在夜间时段内停车并超过限制时长
     *
     * @param enterTime 进场时间
     * @param leaveTime 离场时间
     * @param nightStartTime 夜间开始时间（如：19:00）
     * @param nightEndTime 夜间结束时间（如：05:00）
     * @param parkingDurationMinutes 停车时长（分钟）
     * @param nightTimeHours 夜间时段超过X小时算违规
     * @return 是否为夜间停车违规
     */
    private boolean isInNightTimeRange(LocalDateTime enterTime, LocalDateTime leaveTime,
                                      String nightStartTime, String nightEndTime,
                                      long parkingDurationMinutes, int nightTimeHours) {
        try {
            // 解析夜间时间配置
            String[] startParts = nightStartTime.split(":");
            String[] endParts = nightEndTime.split(":");
            int nightStartHour = Integer.parseInt(startParts[0]);
            int nightStartMinute = Integer.parseInt(startParts[1]);
            int nightEndHour = Integer.parseInt(endParts[0]);
            int nightEndMinute = Integer.parseInt(endParts[1]);

            logger.info("🕒 [夜间时段设置] 开始: {}:{}, 结束: {}:{}, 限制: {}小时",
                    nightStartHour, nightStartMinute, nightEndHour, nightEndMinute, nightTimeHours);

            // 检查进场时间和离场时间是否都在夜间时段内
            boolean enterInNight = isTimeInNightRange(enterTime, nightStartHour, nightStartMinute, nightEndHour, nightEndMinute);
            boolean leaveInNight = isTimeInNightRange(leaveTime, nightStartHour, nightStartMinute, nightEndHour, nightEndMinute);

            logger.info("⏰ [时间判断] 进场在夜间: {}, 离场在夜间: {}, 停车时长: {}分钟",
                    enterInNight, leaveInNight, parkingDurationMinutes);

            // 🆕 增加夜间停车但未超过夜间限制却超过白天限制的累计拉黑逻辑
            boolean isInNightPeriod = enterInNight || leaveInNight;
            boolean exceedsNightLimit = parkingDurationMinutes > (nightTimeHours * 60);

            // 如果在夜间时段停车且超过夜间限制，直接拉黑
            if (isInNightPeriod && exceedsNightLimit) {
                logger.warn("🌙 [夜间停车违规-直接拉黑] 停车时长{}分钟 > 夜间限制{}分钟",
                        parkingDurationMinutes, nightTimeHours * 60);
                return true;
            }

            // 注意：这个方法主要用于判断是否为过夜违规（直接拉黑）
            // 夜间停车但未超过夜间限制却超过白天限制的情况，在调用方法中单独处理
            // 此处不再处理累计拉黑的逻辑，保持职责单一

            // 特殊情况：跨夜间时段的停车（如：18:00进场，20:00离场，跨越了19:00夜间开始时间）
            if (!enterInNight && !leaveInNight) {
                // 检查是否跨越夜间时段
                boolean crossNightPeriod = doesCrossNightPeriod(enterTime, leaveTime,
                        nightStartHour, nightStartMinute, nightEndHour, nightEndMinute);
                if (crossNightPeriod && exceedsNightLimit) {
                    logger.warn("🌉 [跨夜间时段违规] 停车跨越夜间时段且超过限制时长");
                    return true;
                }
            }

            logger.info("✅ [夜间停车正常] 未违反夜间停车规定");
            return false;

        } catch (Exception e) {
            logger.error("❌ [夜间时段判断异常] error={}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 判断指定时间是否在夜间时段内
     */
    private boolean isTimeInNightRange(LocalDateTime dateTime, int nightStartHour, int nightStartMinute,
                                      int nightEndHour, int nightEndMinute) {
        int hour = dateTime.getHour();
        int minute = dateTime.getMinute();
        int timeInMinutes = hour * 60 + minute;
        int nightStartInMinutes = nightStartHour * 60 + nightStartMinute;
        int nightEndInMinutes = nightEndHour * 60 + nightEndMinute;

        // 夜间时段跨日期（如：19:00-05:00）
        if (nightStartInMinutes > nightEndInMinutes) {
            // 时间在晚上部分（19:00之后）或早上部分（05:00之前）
            return timeInMinutes >= nightStartInMinutes || timeInMinutes <= nightEndInMinutes;
        } else {
            // 夜间时段不跨日期（如：22:00-23:00）
            return timeInMinutes >= nightStartInMinutes && timeInMinutes <= nightEndInMinutes;
        }
    }

    /**
     * 判断停车时间是否跨越夜间时段
     */
    private boolean doesCrossNightPeriod(LocalDateTime enterTime, LocalDateTime leaveTime,
                                        int nightStartHour, int nightStartMinute,
                                        int nightEndHour, int nightEndMinute) {
        // 构建夜间时段的开始和结束时间
        LocalDateTime enterDate = enterTime.toLocalDate().atStartOfDay();
        LocalDateTime nightStart = enterDate.withHour(nightStartHour).withMinute(nightStartMinute);
        LocalDateTime nightEnd;

        if (nightStartHour > nightEndHour) {
            // 跨日期的夜间时段
            nightEnd = enterDate.plusDays(1).withHour(nightEndHour).withMinute(nightEndMinute);
        } else {
            // 同日期的夜间时段
            nightEnd = enterDate.withHour(nightEndHour).withMinute(nightEndMinute);
        }

        // 检查停车时间段是否与夜间时段有重叠
        return enterTime.isBefore(nightEnd) && leaveTime.isAfter(nightStart);
    }

    /**
     * 🆕 检查是否在夜间时段停车但未超过夜间限制
     */
    private boolean checkIfInNightPeriod(LocalDateTime enterTime, LocalDateTime leaveTime, Map<String, Object> config) {
        try {
            if (config == null) {
                return false;
            }

            // 从配置中获取夜间时间段设置
            String description = (String) config.get("description");
            String nightStartTime = "22:00";  // 默认值
            String nightEndTime = "06:00";    // 默认值

            // 解析配置描述信息，提取夜间时间段配置
            if (description != null && description.contains("过夜(")) {
                try {
                    int startIndex = description.indexOf("过夜(");
                    if (startIndex != -1) {
                        int endIndex = description.indexOf(")", startIndex);
                        if (endIndex != -1) {
                            String timeRange = description.substring(startIndex + 3, endIndex);
                            String[] timeParts = timeRange.split("-");
                            if (timeParts.length == 2) {
                                nightStartTime = timeParts[0].trim();
                                nightEndTime = timeParts[1].trim();
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ [解析夜间时间段失败] 使用默认配置: {}", e.getMessage());
                }
            }

            // 解析夜间时间配置
            String[] startParts = nightStartTime.split(":");
            String[] endParts = nightEndTime.split(":");
            int nightStartHour = Integer.parseInt(startParts[0]);
            int nightStartMinute = Integer.parseInt(startParts[1]);
            int nightEndHour = Integer.parseInt(endParts[0]);
            int nightEndMinute = Integer.parseInt(endParts[1]);

            // 检查进场时间和离场时间是否在夜间时段内
            boolean enterInNight = isTimeInNightRange(enterTime, nightStartHour, nightStartMinute, nightEndHour, nightEndMinute);
            boolean leaveInNight = isTimeInNightRange(leaveTime, nightStartHour, nightStartMinute, nightEndHour, nightEndMinute);

            return enterInNight || leaveInNight;

        } catch (Exception e) {
            logger.error("❌ [检查夜间时段异常] error={}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 调用黑名单接口添加车辆到黑名单
     * @param plateNumber 车牌号
     * @param parkCode 车场编码
     * @param parkName 车场名称
     * @param reason 拉黑原因
     * @param remark 备注信息
     */
    private void callAddBlackListCarAPI(String plateNumber, String parkCode, String parkName, String reason, String remark) {
        try {
            logger.info("🚫 [调用拉黑接口] plateNumber={}, parkCode={}, reason={}", plateNumber, parkCode, reason);

            // 查询月票表中的车主姓名
            String carOwner = getCarOwnerFromMonthTicket(plateNumber);
            if (carOwner == null || carOwner.trim().isEmpty()) {
                carOwner = "系统自动"; // 默认车主信息
                logger.warn("⚠️ [车主信息缺失] plateNumber={}, 使用默认车主: {}", plateNumber, carOwner);
            } else {
                logger.info("✅ [查询到车主] plateNumber={}, carOwner={}", plateNumber, carOwner);
            }

            HashMap<String, String> blacklistParams = new HashMap<>();
            blacklistParams.put("parkCode", parkCode);
            blacklistParams.put("carCode", plateNumber);
            blacklistParams.put("carOwner", carOwner);
            blacklistParams.put("isPermament", "1"); // 永久拉黑
            blacklistParams.put("reason", reason);
            blacklistParams.put("remark1", remark);
            blacklistParams.put("remark2", "月票车违规系统自动拉黑");
            blacklistParams.put("specialCarTypeId", "504127"); // 固定值

            String response = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/blackList/addBlackListCar", blacklistParams);
            JSONObject jsonObject = JSONObject.parseObject(response);
            if (jsonObject != null) {
                JSONObject jsonObjectData = (JSONObject) jsonObject.get("data");
                if (jsonObjectData != null) {
                    String message = jsonObjectData.getString("message");
                    String status = jsonObjectData.getString("status");
                    logger.info("✅ [拉黑接口调用结果] plateNumber={}, message={}, status={}", plateNumber, message, status);
                } else {
                    logger.warn("⚠️ [拉黑接口响应异常] plateNumber={}, response={}", plateNumber, response);
                }
            } else {
                logger.error("❌ [拉黑接口调用失败] plateNumber={}, response={}", plateNumber, response);
            }
        } catch (Exception e) {
            logger.error("❌ [调用拉黑接口异常] plateNumber={}, error={}", plateNumber, e.getMessage(), e);
        }
    }

    /**
     * 学院新城专用：调用黑名单接口添加车辆到黑名单（不带remark2，specialCarTypeId=504170）
     */
    private void callAddBlackListCarAPIForCollegeNewCity(String plateNumber, String parkCode, String parkName, String reason, String remark) {
        try {
            logger.info("🚫 [学院新城拉黑接口] plateNumber={}, parkCode={}, reason={}", plateNumber, parkCode, reason);

            String carOwner = getCarOwnerFromMonthTicket(plateNumber);
            if (carOwner == null || carOwner.trim().isEmpty()) {
                carOwner = "系统自动";
                logger.warn("⚠️ [学院新城-车主信息缺失] plateNumber={}, 使用默认车主: {}", plateNumber, carOwner);
            }

            HashMap<String, String> blacklistParams = new HashMap<>();
            blacklistParams.put("parkCode", parkCode);
            blacklistParams.put("carCode", plateNumber);
            blacklistParams.put("carOwner", carOwner);
            blacklistParams.put("isPermament", "1");
            blacklistParams.put("reason", reason);
            blacklistParams.put("remark1", remark);
            blacklistParams.put("specialCarTypeId", "504170");

            String response = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/blackList/addBlackListCar", blacklistParams);
            JSONObject jsonObject = JSONObject.parseObject(response);
            if (jsonObject != null) {
                JSONObject jsonObjectData = (JSONObject) jsonObject.get("data");
                if (jsonObjectData != null) {
                    String message = jsonObjectData.getString("message");
                    String status = jsonObjectData.getString("status");
                    logger.info("✅ [学院新城拉黑结果] plateNumber={}, message={}, status={}", plateNumber, message, status);
                } else {
                    logger.warn("⚠️ [学院新城拉黑响应异常] plateNumber={}, response={}", plateNumber, response);
                }
            } else {
                logger.error("❌ [学院新城拉黑接口失败] plateNumber={}, response={}", plateNumber, response);
            }
        } catch (Exception e) {
            logger.error("❌ [学院新城拉黑接口异常] plateNumber={}, error={}", plateNumber, e.getMessage(), e);
        }
    }

    /**
     * 从月票表中查询车主姓名
     * @param plateNumber 车牌号
     * @return 车主姓名，如果未找到返回null
     */
    private String getCarOwnerFromMonthTicket(String plateNumber) {
        try {
            logger.info("🔍 [查询车主姓名] plateNumber={}", plateNumber);

            // 使用 like 查询，因为 carNo 字段可能包含多个车牌号（逗号分隔）
            LambdaQueryWrapper<MonthTick> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.like(MonthTick::getCarNo, plateNumber)
                    .eq(MonthTick::getValidStatus, 1) // 只查询有效的月票
                    .orderByDesc(MonthTick::getId); // 按ID倒序，获取最新的记录

            List<MonthTick> monthTickets = monthTicketService.list(queryWrapper);

            if (monthTickets != null && !monthTickets.isEmpty()) {
                // 进一步验证车牌号是否确实匹配（防止部分匹配的误判）
                for (MonthTick monthTick : monthTickets) {
                    String carNo = monthTick.getCarNo();
                    if (carNo != null) {
                        // 将逗号分隔的车牌号分割并检查是否包含目标车牌
                        String[] plateNumbers = carNo.split(",");
                        for (String plate : plateNumbers) {
                            if (plate.trim().equals(plateNumber)) {
                                String userName = monthTick.getUserName();
                                if (userName != null && !userName.trim().isEmpty()) {
                                    logger.info("✅ [找到车主] plateNumber={}, carOwner={}, monthTicketId={}",
                                            plateNumber, userName, monthTick.getId());
                                    return userName.trim();
                                }
                            }
                        }
                    }
                }
                logger.warn("⚠️ [车主姓名为空] plateNumber={}, 找到{}条月票记录但车主姓名为空",
                        plateNumber, monthTickets.size());
            } else {
                logger.warn("⚠️ [未找到月票记录] plateNumber={}", plateNumber);
            }

            return null;

        } catch (Exception e) {
            logger.error("❌ [查询车主姓名异常] plateNumber={}, error={}", plateNumber, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 根据月票ID获取票类型名称
     * @param monthTicketId 月票ID
     * @return 票类型名称
     */
    private String getTicketNameByMonthTicketId(Integer monthTicketId) {
        try {
            if (monthTicketId == null) {
                return null;
            }

            MonthTick monthTick = monthTicketService.getById(monthTicketId);
            if (monthTick != null) {
                return monthTick.getTicketName();
            }
            return null;
        } catch (Exception e) {
            logger.error("❌ [查询月票类型异常] monthTicketId={}, error={}", monthTicketId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 检查是否为免检的月票类型
     * @param parkCode 车场编码
     * @param ticketName 月票类型名称
     * @return 是否免检
     */
    private boolean isExemptMonthlyTicketType(String parkCode, String ticketName) {
        try {
            if (ticketName == null || ticketName.trim().isEmpty()) {
                return false;
            }

            // 获取车场的月票超时配置
            Map<String, Object> config = violationsService.getMonthlyTicketTimeoutConfig(parkCode);
            if (config == null) {
                return false;
            }

            // 获取免检的月票类型列表
            @SuppressWarnings("unchecked")
            List<String> exemptTicketTypes = (List<String>) config.get("exemptTicketTypes");
            if (exemptTicketTypes != null && !exemptTicketTypes.isEmpty()) {
                boolean isExempt = exemptTicketTypes.contains(ticketName.trim());
                logger.info("🛡️ [免检检查] ticketName={}, exemptList={}, isExempt={}",
                        ticketName, exemptTicketTypes, isExempt);
                return isExempt;
            }

            return false;
        } catch (Exception e) {
            logger.error("❌ [免检检查异常] parkCode={}, ticketName={}, error={}",
                    parkCode, ticketName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 🆕 获取指定车场的所有月票类型
     * @param parkCode 车场编码
     * @return 月票类型列表
     */
    @ApiOperation("获取指定车场的月票类型列表")
    @GetMapping("/getTicketTypesByParkCode")
    public ResponseEntity<Result> getTicketTypesByParkCode(@RequestParam String parkCode) {
        try {
            logger.info("🎫 [查询月票类型] parkCode={}", parkCode);

            // 根据车场编码获取车场名称
            String parkName = getParkNameByCode(parkCode);
            if (parkName == null) {
                return ResponseEntity.ok(Result.error("未知车场编码: " + parkCode));
            }

            // 查询该车场的所有月票类型
            LambdaQueryWrapper<MonthTick> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MonthTick::getParkName, parkName)
                    .eq(MonthTick::getValidStatus, 1) // 只查询有效的月票
                    .isNotNull(MonthTick::getTicketName)
                    .ne(MonthTick::getTicketName, "")
                    .groupBy(MonthTick::getTicketName)
                    .select(MonthTick::getTicketName);

            List<MonthTick> monthTickets = monthTicketService.list(queryWrapper);

            // 提取唯一的票类型名称
            List<String> ticketTypes = monthTickets.stream()
                    .map(MonthTick::getTicketName)
                    .filter(ticketName -> ticketName != null && !ticketName.trim().isEmpty())
                    .distinct()
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());

            logger.info("✅ [查询成功] parkCode={}, parkName={}, ticketTypes={}",
                    parkCode, parkName, ticketTypes);

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("parkCode", parkCode);
            resultData.put("parkName", parkName);
            resultData.put("ticketTypes", ticketTypes);

            Result result = new Result();
            result.setCode("0");
            result.setMsg("查询成功");
            result.setData(resultData);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ [查询月票类型异常] parkCode={}, error={}", parkCode, e.getMessage(), e);
            return ResponseEntity.ok(Result.error("查询月票类型失败: " + e.getMessage()));
        }
    }

    /**
     * 处理车牌号码，移除特殊字符
     * @param carNo 原始车牌号码
     * @return 处理后的车牌号码
     */
    private String processCarNo(String carNo) {
        if (carNo == null || carNo.trim().isEmpty()) {
            return carNo;
        }
        // 移除车牌号码中的特殊字符，如 ●
        return carNo.replace("●", "").trim();
    }
    
    /**
     * 🆕 检查是否为凌晨进场
     * 
     * @param enterTime 进场时间
     * @return 是否为凌晨进场（0点到6点之间）
     */
    private boolean checkEarlyMorningEntry(LocalDateTime enterTime) {
        try {
            int enterHour = enterTime.getHour();
            
            // 凌晨0点到6点之间算作凌晨进场
            boolean isEarlyMorning = enterHour >= 0 && enterHour < 6;
            
            if (isEarlyMorning) {
                logger.info("🌅 [凌晨进场检测] enterTime={}, enterHour={}, 属于凌晨进场", 
                        enterTime, enterHour);
            } else {
                logger.debug("⏰ [正常时段进场] enterTime={}, enterHour={}", enterTime, enterHour);
            }
            
            return isEarlyMorning;
            
        } catch (Exception e) {
            logger.error("❌ [凌晨进场检测异常] enterTime={}, error={}", enterTime, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 处理学院新城黑名单车辆拉黑逻辑
     * 拉黑条件：
     * 1. 0点之前进入的车辆：停车时间在0点之后超过1小时就拉黑
     * 2. 0点之后进入的车辆：超过1小时就拉黑
     */
    private void processCollegeNewCityBlacklist(ReportCarOutData data) {
        try {
            logger.info("🏫 [学院新城拉黑检查] 开始处理: plateNumber={}, parkCode={}, enterVipType={}",
                    data.getLeaveCarLicenseNumber(), data.getParkCode(), data.getEnterVipType());

            // 解析进场和离场时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            Date enterTimeDate = sdf.parse(data.getEnterTime());
            Date leaveTimeDate = sdf.parse(data.getLeaveTime());

            // 转换为LocalDateTime用于时间计算
            LocalDateTime enterDateTime = enterTimeDate.toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime leaveDateTime = leaveTimeDate.toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();

            // 从配置表获取凌晨时间段和阈值
            Map<String, Object> config = violationsService.getMonthlyTicketTimeoutConfig(data.getParkCode());
            String nightEndTime = config != null && config.get("nightEndTime") != null
                    ? config.get("nightEndTime").toString()
                    : "07:00"; // 默认凌晨结束时间
            Integer nightTimeHours = config != null && config.get("nightTimeHours") != null
                    ? (Integer) config.get("nightTimeHours")
                    : 1; // 默认阈值1小时

            // 取离场当日的凌晨窗口 [00:00, nightEndTime)
            LocalDate leaveDate = leaveDateTime.toLocalDate();
            LocalDateTime windowStart = leaveDate.atStartOfDay();
            java.time.LocalTime endLt = java.time.LocalTime.parse(nightEndTime);
            LocalDateTime windowEnd = leaveDate.atTime(endLt);

            // 计算停车区间与凌晨窗口的重叠时长（分钟）
            LocalDateTime overlapStart = enterDateTime.isAfter(windowStart) ? enterDateTime : windowStart;
            LocalDateTime overlapEnd = leaveDateTime.isBefore(windowEnd) ? leaveDateTime : windowEnd;

            long overlapMinutes = 0;
            if (overlapEnd.isAfter(overlapStart)) {
                overlapMinutes = java.time.Duration.between(overlapStart, overlapEnd).toMinutes();
            }

            logger.info("🕐 [凌晨窗口] windowStart={}, windowEnd={}, overlapMinutes={}", windowStart, windowEnd, overlapMinutes);

            java.time.format.DateTimeFormatter hmFmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            String overlapStartStr = overlapMinutes > 0 ? overlapStart.format(hmFmt) : "--:--";
            String overlapEndStr = overlapMinutes > 0 ? overlapEnd.format(hmFmt) : "--:--";
            java.time.format.DateTimeFormatter dtFmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String parkingStartStr = enterDateTime.format(dtFmt);
            String parkingEndStr = leaveDateTime.format(dtFmt);
            long overlapHours = overlapMinutes / 60;
            long overlapRemainMinutes = overlapMinutes % 60;
            String durationHuman = overlapHours > 0
                    ? (overlapHours + "小时" + (overlapRemainMinutes > 0 ? overlapRemainMinutes + "分钟" : ""))
                    : (overlapRemainMinutes + "分钟");

            boolean shouldBlacklist = overlapMinutes > (nightTimeHours * 60L);
            String blacklistReason = String.format(
                    "夜间违停：停车区间[%s - %s]，凌晨窗口[00:00-%s]，重叠区间[%s-%s]，累计停车%s（%d分钟），超过阈值%d小时",
                    parkingStartStr, parkingEndStr, nightEndTime, overlapStartStr, overlapEndStr, durationHuman, overlapMinutes, nightTimeHours);

            if (shouldBlacklist) {
                logger.warn("🚫 [学院新城拉黑] 车牌={}, 原因={}",
                        data.getLeaveCarLicenseNumber(), blacklistReason);

                // 查询停车场名称
                java.util.List<String> parkNameList = yardInfoService.selectByParkCode(data.getParkCode());
                String parkName = parkNameList.isEmpty() ? "未知停车场" : parkNameList.get(0);

                // ① 先调用学院新城专用外部拉黑接口（无remark2，specialCarTypeId=504170）
                callAddBlackListCarAPIForCollegeNewCity(
                        data.getLeaveCarLicenseNumber(),
                        data.getParkCode(),
                        parkName,
                        "夜间违停",
                        blacklistReason + "，系统自动拉黑"
                );

                // ② 再加入本地黑名单
                boolean added = violationsService.addToBlacklist(
                        data.getLeaveCarLicenseNumber(),
                        parkName,
                        "夜间违停",
                        blacklistReason + "，系统自动拉黑"
                );

                if (added) {
                    logger.info("✅ [本地黑名单成功] 车牌={} parkName={}", data.getLeaveCarLicenseNumber(), parkName);
                } else {
                    logger.error("❌ [本地黑名单失败] 车牌={}", data.getLeaveCarLicenseNumber());
                }

                logger.info("✅ [拉黑完成] 车牌={} 已处理拉黑流程", data.getLeaveCarLicenseNumber());
            } else {
                logger.info("✅ [无需拉黑] 车牌={} 未达到拉黑条件 (阈值:{}小时, 重叠:{}分钟)",
                        data.getLeaveCarLicenseNumber(), nightTimeHours, overlapMinutes);
            }

        } catch (Exception e) {
            logger.error("❌ [学院新城拉黑检查失败] plateNumber={}, error={}", 
                    data.getLeaveCarLicenseNumber(), e.getMessage(), e);
        }
    }

    /**
     * 接收月票同步消息
     * 
     * @param message 月票同步消息（JSON字符串格式）
     * @return 处理结果
     */
    @ApiOperation("月票同步消息接收")
    @RequestMapping("/reportMonthTicketSyncMessage")
    public ResponseEntity<AIKEResult> reportMonthTicketSyncMessage(@RequestBody String message) {
        try {
            logger.info("📥 [月票同步] 接收到月票同步消息");
            logger.info("📋 [月票同步] 原始消息: {}", message);
            
            // 解析JSON字符串
            JSONObject jsonData = JSONObject.parseObject(message);
            
            // 输出解析后的数据
            logger.info("✅ [月票同步] 解析后的数据:");
            logger.info("  📌 完整JSON: {}", jsonData.toJSONString());
            
            // 遍历并输出所有字段
            if (jsonData != null && !jsonData.isEmpty()) {
                for (String key : jsonData.keySet()) {
                    Object value = jsonData.get(key);
                    logger.info("  📌 字段 [{}] = {}", key, value);
                }
            } else {
                logger.warn("⚠️ [月票同步] JSON数据为空");
            }
            
            // 提取常见的月票字段（如果存在）
            String carNumber = jsonData.getString("carNumber");
            String ticketType = jsonData.getString("ticketType");
            String parkCode = jsonData.getString("parkCode");
            String validFrom = jsonData.getString("validFrom");
            String validTo = jsonData.getString("validTo");
            
            if (carNumber != null) {
                logger.info("🚗 [月票同步] 车牌号: {}", carNumber);
            }
            if (ticketType != null) {
                logger.info("🎫 [月票同步] 月票类型: {}", ticketType);
            }
            if (parkCode != null) {
                logger.info("🅿️ [月票同步] 车场编码: {}", parkCode);
            }
            if (validFrom != null && validTo != null) {
                logger.info("📅 [月票同步] 有效期: {} 至 {}", validFrom, validTo);
            }
            
            // 返回成功响应
            logger.info("✅ [月票同步] 处理完成，返回成功响应");
            HashMap<Object, Object> hashEmptyMap = new HashMap<>();
            return ResponseEntity.ok(AIKEResult.success(hashEmptyMap));
            
        } catch (Exception e) {
            logger.error("❌ [月票同步] 处理异常: {}", e.getMessage(), e);
            HashMap<Object, Object> hashEmptyMap = new HashMap<>();
            return ResponseEntity.ok(AIKEResult.success(hashEmptyMap));
        }
    }

    /**
     * 接收车场月票上报数据
     * 
     * @param message 车场月票数据（JSON字符串格式）
     * @return 处理结果
     */
    @ApiOperation("车场月票数据上报接收")
    @RequestMapping("/reportParkMonthTicket")
    public ResponseEntity<AIKEResult> reportParkMonthTicket(@RequestBody String message) {
        try {
            logger.info("📥 [车场月票] 接收到车场月票上报数据");
            logger.info("📋 [车场月票] 原始消息: {}", message);
            
            // 解析JSON字符串
            JSONObject jsonData = JSONObject.parseObject(message);
            
            // 输出解析后的数据
            logger.info("✅ [车场月票] 解析后的数据:");
            logger.info("  📌 完整JSON: {}", jsonData.toJSONString());
            
            // 遍历并输出所有字段
            if (jsonData != null && !jsonData.isEmpty()) {
                for (String key : jsonData.keySet()) {
                    Object value = jsonData.get(key);
                    logger.info("  📌 字段 [{}] = {}", key, value);
                }
            } else {
                logger.warn("⚠️ [车场月票] JSON数据为空");
                HashMap<Object, Object> hashEmptyMap = new HashMap<>();
                return ResponseEntity.ok(AIKEResult.success(hashEmptyMap));
            }
            
            // 提取关键字段
            Integer ticketRecordType = jsonData.getInteger("ticketRecordType");
            
            logger.info("🔢 [车场月票] 流水类型: {}", ticketRecordType);
            logger.info("🏢 [车场月票] 车场名称: {}", jsonData.getString("parkName"));
            logger.info("🚗 [车场月票] 车牌号: {}", jsonData.getString("carNo"));
            
            if (ticketRecordType == null) {
                logger.warn("⚠️ [车场月票] ticketRecordType为空，跳过处理");
                HashMap<Object, Object> hashEmptyMap = new HashMap<>();
                return ResponseEntity.ok(AIKEResult.success(hashEmptyMap));
            }
            
            // 根据 ticketRecordType 处理不同的业务逻辑
            processMonthTicketByType(ticketRecordType, jsonData);
            
            // 返回成功响应
            logger.info("✅ [车场月票] 处理完成，返回成功响应");
            HashMap<Object, Object> hashEmptyMap = new HashMap<>();
            return ResponseEntity.ok(AIKEResult.success(hashEmptyMap));
            
        } catch (Exception e) {
            logger.error("❌ [车场月票] 处理异常: {}", e.getMessage(), e);
            HashMap<Object, Object> hashEmptyMap = new HashMap<>();
            return ResponseEntity.ok(AIKEResult.success(hashEmptyMap));
        }
    }
    
    /**
     * 根据流水类型处理月票数据
     * 
     * @param ticketRecordType 流水类型（0开通,1续期,2退款,3冻结,4解冻,5已恢复,6已删除,7修改）
     * @param jsonData 月票数据
     */
    private void processMonthTicketByType(Integer ticketRecordType, JSONObject jsonData) {
        try {
            String carNo = jsonData.getString("carNo");
            String parkName = jsonData.getString("parkName");
            String ticketCode = jsonData.getString("ticketCode");
            
            switch (ticketRecordType) {
                case 0: // 开通 - 新增月票数据
                    logger.info("🆕 [月票开通] 开始新增月票数据");
                    addNewMonthTicket(jsonData);
                    addNewOwnerInfo(jsonData);
                    break;
                    
                case 1: // 续期 - 修改月票有效期
                    logger.info("🔄 [月票续期] 开始更新有效期");
                    renewMonthTicket(jsonData);
                    break;
                    
                case 2: // 退款 - 删除月票数据
                    logger.info("💰 [月票退款] 开始删除月票数据");
                    deleteMonthTicket(carNo, parkName, ticketCode);
                    deleteOwnerInfo(carNo, jsonData.getString("userPhone"));
                    break;
                    
                case 3: // 冻结 - 修改冻结状态
                    logger.info("❄️ [月票冻结] 开始冻结月票");
                    updateMonthTicketFreezeStatus(carNo, parkName, ticketCode, 1);
                    break;
                    
                case 4: // 解冻 - 修改冻结状态
                    logger.info("🔥 [月票解冻] 开始解冻月票");
                    updateMonthTicketFreezeStatus(carNo, parkName, ticketCode, 0);
                    break;
                    
                case 5: // 已恢复 - 修改有效状态
                    logger.info("✅ [月票恢复] 开始恢复月票");
                    updateMonthTicketValidStatus(carNo, parkName, ticketCode, 1);
                    break;
                    
                case 6: // 已删除 - 删除月票数据
                    logger.info("🗑️ [月票删除] 开始删除月票数据");
                    deleteMonthTicket(carNo, parkName, ticketCode);
                    deleteOwnerInfo(carNo, jsonData.getString("userPhone"));
                    break;
                    
                case 7: // 修改 - 修改月票数据
                    logger.info("✏️ [月票修改] 开始修改月票数据");
                    updateMonthTicket(jsonData);
                    updateOwnerInfo(jsonData);
                    break;
                    
                default:
                    logger.warn("⚠️ [未知类型] ticketRecordType={} 不在处理范围内", ticketRecordType);
                    break;
            }
            
        } catch (Exception e) {
            logger.error("❌ [处理月票] 处理失败: ticketRecordType={}, error={}", 
                    ticketRecordType, e.getMessage(), e);
        }
    }
    
    /**
     * 新增月票数据
     */
    private void addNewMonthTicket(JSONObject jsonData) {
        try {
            MonthTick monthTick = new MonthTick();
            monthTick.setParkName(jsonData.getString("parkName"));
            monthTick.setCarNo(jsonData.getString("carNo"));
            monthTick.setTicketCode(jsonData.getString("ticketCode"));
            monthTick.setTicketName(jsonData.getString("ticketName"));
            monthTick.setUserName(jsonData.getString("userName"));
            monthTick.setUserPhone(jsonData.getString("userPhone"));
            monthTick.setTimePeriodList(jsonData.getString("timePeriodList"));
            monthTick.setCreateTime(jsonData.getString("createTime"));
            monthTick.setUpdateTime(jsonData.getString("updateTime"));
            monthTick.setIsFrozen(jsonData.getIntValue("isFrozen"));
            monthTick.setValidStatus(jsonData.getIntValue("validStatus"));
            monthTick.setConfigStatus(jsonData.getIntValue("configStatus"));
            monthTick.setCarNoNum(jsonData.getIntValue("carNoNum"));
            monthTick.setDynamicCarportNumber(jsonData.getIntValue("dynamicCarportNumber"));
            monthTick.setMonthTicketConfigId(jsonData.getLongValue("monthTicketConfigId"));
            monthTick.setCreateBy(jsonData.getString("createBy"));
            monthTick.setUpdateBy(jsonData.getString("updateBy"));
            monthTick.setRemark1(jsonData.getString("remark1"));
            monthTick.setRemark2(jsonData.getString("remark2"));
            monthTick.setRemark3(jsonData.getString("remark3"));
            
            boolean saved = monthTicketService.save(monthTick);
            if (saved) {
                logger.info("✅ [新增月票成功] carNo={}, parkName={}", 
                        monthTick.getCarNo(), monthTick.getParkName());
            } else {
                logger.error("❌ [新增月票失败] carNo={}, parkName={}", 
                        monthTick.getCarNo(), monthTick.getParkName());
            }
        } catch (Exception e) {
            logger.error("❌ [新增月票异常] error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 新增业主信息
     */
    private void addNewOwnerInfo(JSONObject jsonData) {
        try {
            Ownerinfo ownerinfo = new Ownerinfo();
            ownerinfo.setCommunity(jsonData.getString("parkName"));
            ownerinfo.setOwnername(jsonData.getString("userName"));
            ownerinfo.setOwnerphone(jsonData.getString("userPhone"));
            ownerinfo.setPlates(jsonData.getString("carNo"));
            ownerinfo.setBuilding(jsonData.getString("building"));
            ownerinfo.setUnits(jsonData.getString("units"));
            ownerinfo.setFloor(jsonData.getString("floor"));
            ownerinfo.setRoomnumber(jsonData.getString("roomnumber"));
            ownerinfo.setProvince(jsonData.getString("province"));
            ownerinfo.setCity(jsonData.getString("city"));
            ownerinfo.setDistrict(jsonData.getString("district"));
            
            boolean saved = ownerinfoService.save(ownerinfo);
            if (saved) {
                logger.info("✅ [新增业主成功] ownerName={}, phone={}", 
                        ownerinfo.getOwnername(), ownerinfo.getOwnerphone());
            } else {
                logger.error("❌ [新增业主失败] ownerName={}, phone={}", 
                        ownerinfo.getOwnername(), ownerinfo.getOwnerphone());
            }
        } catch (Exception e) {
            logger.error("❌ [新增业主异常] error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 续期月票 - 修改有效期
     */
    private void renewMonthTicket(JSONObject jsonData) {
        try {
            String carNo = jsonData.getString("carNo");
            String parkName = jsonData.getString("parkName");
            String ticketCode = jsonData.getString("ticketCode");
            String timePeriodList = jsonData.getString("timePeriodList");
            
            // 查询现有月票
            QueryWrapper<MonthTick> wrapper = new QueryWrapper<>();
            wrapper.eq("car_no", carNo)
                   .eq("park_name", parkName)
                   .eq("ticket_code", ticketCode);
            MonthTick monthTick = monthTicketService.getOne(wrapper);
            
            if (monthTick != null) {
                monthTick.setTimePeriodList(timePeriodList);
                monthTick.setUpdateTime(jsonData.getString("updateTime"));
                monthTick.setValidStatus(1); // 续期后设为有效
                
                boolean updated = monthTicketService.updateById(monthTick);
                if (updated) {
                    logger.info("✅ [续期成功] carNo={}, 新有效期={}", carNo, timePeriodList);
                } else {
                    logger.error("❌ [续期失败] carNo={}", carNo);
                }
            } else {
                logger.warn("⚠️ [续期失败] 未找到月票记录: carNo={}, parkName={}", carNo, parkName);
            }
        } catch (Exception e) {
            logger.error("❌ [续期异常] error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 删除月票
     */
    private void deleteMonthTicket(String carNo, String parkName, String ticketCode) {
        try {
            QueryWrapper<MonthTick> wrapper = new QueryWrapper<>();
            wrapper.eq("car_no", carNo)
                   .eq("park_name", parkName)
                   .eq("ticket_code", ticketCode);
            
            boolean removed = monthTicketService.remove(wrapper);
            if (removed) {
                logger.info("✅ [删除月票成功] carNo={}, parkName={}", carNo, parkName);
            } else {
                logger.warn("⚠️ [删除月票失败] 未找到记录: carNo={}, parkName={}", carNo, parkName);
            }
        } catch (Exception e) {
            logger.error("❌ [删除月票异常] error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 删除业主信息
     */
    private void deleteOwnerInfo(String carNo, String ownerPhone) {
        try {
            QueryWrapper<Ownerinfo> wrapper = new QueryWrapper<>();
            wrapper.eq("plates", carNo);
            if (ownerPhone != null && !ownerPhone.isEmpty()) {
                wrapper.eq("ownerphone", ownerPhone);
            }
            
            boolean removed = ownerinfoService.remove(wrapper);
            if (removed) {
                logger.info("✅ [删除业主成功] carNo={}", carNo);
            } else {
                logger.warn("⚠️ [删除业主失败] 未找到记录: carNo={}", carNo);
            }
        } catch (Exception e) {
            logger.error("❌ [删除业主异常] error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 更新月票冻结状态
     */
    private void updateMonthTicketFreezeStatus(String carNo, String parkName, String ticketCode, int isFrozen) {
        try {
            QueryWrapper<MonthTick> wrapper = new QueryWrapper<>();
            wrapper.eq("car_no", carNo)
                   .eq("park_name", parkName)
                   .eq("ticket_code", ticketCode);
            MonthTick monthTick = monthTicketService.getOne(wrapper);
            
            if (monthTick != null) {
                monthTick.setIsFrozen(isFrozen);
                boolean updated = monthTicketService.updateById(monthTick);
                if (updated) {
                    logger.info("✅ [更新冻结状态成功] carNo={}, isFrozen={}", carNo, isFrozen);
                } else {
                    logger.error("❌ [更新冻结状态失败] carNo={}", carNo);
                }
            } else {
                logger.warn("⚠️ [更新失败] 未找到月票记录: carNo={}", carNo);
            }
        } catch (Exception e) {
            logger.error("❌ [更新冻结状态异常] error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 更新月票有效状态
     */
    private void updateMonthTicketValidStatus(String carNo, String parkName, String ticketCode, int validStatus) {
        try {
            QueryWrapper<MonthTick> wrapper = new QueryWrapper<>();
            wrapper.eq("car_no", carNo)
                   .eq("park_name", parkName)
                   .eq("ticket_code", ticketCode);
            MonthTick monthTick = monthTicketService.getOne(wrapper);
            
            if (monthTick != null) {
                monthTick.setValidStatus(validStatus);
                boolean updated = monthTicketService.updateById(monthTick);
                if (updated) {
                    logger.info("✅ [更新有效状态成功] carNo={}, validStatus={}", carNo, validStatus);
                } else {
                    logger.error("❌ [更新有效状态失败] carNo={}", carNo);
                }
            } else {
                logger.warn("⚠️ [更新失败] 未找到月票记录: carNo={}", carNo);
            }
        } catch (Exception e) {
            logger.error("❌ [更新有效状态异常] error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 修改月票数据
     */
    private void updateMonthTicket(JSONObject jsonData) {
        try {
            String carNo = jsonData.getString("carNo");
            String parkName = jsonData.getString("parkName");
            String ticketCode = jsonData.getString("ticketCode");
            
            QueryWrapper<MonthTick> wrapper = new QueryWrapper<>();
            wrapper.eq("car_no", carNo)
                   .eq("park_name", parkName)
                   .eq("ticket_code", ticketCode);
            MonthTick monthTick = monthTicketService.getOne(wrapper);
            
            if (monthTick != null) {
                // 更新可修改的字段
                if (jsonData.containsKey("ticketName")) {
                    monthTick.setTicketName(jsonData.getString("ticketName"));
                }
                if (jsonData.containsKey("userName")) {
                    monthTick.setUserName(jsonData.getString("userName"));
                }
                if (jsonData.containsKey("userPhone")) {
                    monthTick.setUserPhone(jsonData.getString("userPhone"));
                }
                if (jsonData.containsKey("timePeriodList")) {
                    monthTick.setTimePeriodList(jsonData.getString("timePeriodList"));
                }
                if (jsonData.containsKey("remark1")) {
                    monthTick.setRemark1(jsonData.getString("remark1"));
                }
                if (jsonData.containsKey("remark2")) {
                    monthTick.setRemark2(jsonData.getString("remark2"));
                }
                if (jsonData.containsKey("remark3")) {
                    monthTick.setRemark3(jsonData.getString("remark3"));
                }
                monthTick.setUpdateTime(jsonData.getString("updateTime"));
                
                boolean updated = monthTicketService.updateById(monthTick);
                if (updated) {
                    logger.info("✅ [修改月票成功] carNo={}", carNo);
                } else {
                    logger.error("❌ [修改月票失败] carNo={}", carNo);
                }
            } else {
                logger.warn("⚠️ [修改失败] 未找到月票记录: carNo={}", carNo);
            }
        } catch (Exception e) {
            logger.error("❌ [修改月票异常] error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 修改业主信息
     */
    private void updateOwnerInfo(JSONObject jsonData) {
        try {
            String carNo = jsonData.getString("carNo");
            String ownerPhone = jsonData.getString("userPhone");
            
            QueryWrapper<Ownerinfo> wrapper = new QueryWrapper<>();
            wrapper.eq("plates", carNo);
            if (ownerPhone != null && !ownerPhone.isEmpty()) {
                wrapper.eq("ownerphone", ownerPhone);
            }
            Ownerinfo ownerinfo = ownerinfoService.getOne(wrapper);
            
            if (ownerinfo != null) {
                // 更新可修改的字段
                if (jsonData.containsKey("userName")) {
                    ownerinfo.setOwnername(jsonData.getString("userName"));
                }
                if (jsonData.containsKey("userPhone")) {
                    ownerinfo.setOwnerphone(jsonData.getString("userPhone"));
                }
                if (jsonData.containsKey("parkName")) {
                    ownerinfo.setCommunity(jsonData.getString("parkName"));
                }
                
                boolean updated = ownerinfoService.updateById(ownerinfo);
                if (updated) {
                    logger.info("✅ [修改业主成功] carNo={}", carNo);
                } else {
                    logger.error("❌ [修改业主失败] carNo={}", carNo);
                }
            } else {
                logger.warn("⚠️ [修改失败] 未找到业主记录: carNo={}", carNo);
            }
        } catch (Exception e) {
            logger.error("❌ [修改业主异常] error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 万象上东过夜拉黑检查（业主名下所有车牌批量拉黑）
     * 
     * 规则：
     * 1. 11点-6点进场的待检查VIP月票车
     * 2. 停车超过2小时
     * 3. 拉黑该业主名下的所有车牌号
     * 
     * @param carNo 触发拉黑的车牌号
     * @param parkCode 车场编码
     */
    public void processWanXiangBlacklistByOwner(String carNo, String parkCode) {
        try {
            logger.info("🏢 [万象上东批量拉黑] 开始处理车牌: {}", carNo);
            
            // 1. 获取配置
            Map<String, Object> config = violationsService.getMonthlyTicketTimeoutConfig(parkCode);
            if (config == null) {
                logger.warn("⚠️ [万象上东] 未找到配置");
                return;
            }
            
            String vipCheckMode = (String) config.get("vipCheckMode");
            if (!"include".equals(vipCheckMode)) {
                logger.info("ℹ️ [万象上东] 非待检查模式，跳过");
                return;
            }
            
            @SuppressWarnings("unchecked")
            List<String> vipTicketTypes = (List<String>) config.get("vipTicketTypes");
            Object blacklistDays = config.get("blacklistDays");
            String blacklistName = (String) config.get("blacklistName");
            
            // 2. 通过车牌号查询月票信息获取业主手机号
            QueryWrapper<MonthTick> carWrapper = new QueryWrapper<>();
            carWrapper.eq("car_no", carNo)
                     .eq("park_name", getParkNameByCode(parkCode));
            MonthTick triggerMonthTick = monthTicketService.getOne(carWrapper);
            
            if (triggerMonthTick == null) {
                logger.warn("⚠️ [万象上东] 未找到车牌 {} 的月票信息", carNo);
                return;
            }
            
            String userPhone = triggerMonthTick.getUserPhone();
            String ticketName = triggerMonthTick.getTicketName();
            
            // 3. 检查是否为待检查的VIP类型
            if (vipTicketTypes == null || !vipTicketTypes.contains(ticketName)) {
                logger.info("ℹ️ [万象上东] 月票类型 {} 不在待检查列表", ticketName);
                return;
            }
            
            logger.info("📱 [万象上东] 业主手机: {}, 月票类型: {}", userPhone, ticketName);
            
            // 4. 查询该业主名下的所有车牌号
            List<String> allCarNumbers = getAllCarNumbersByPhone(userPhone, getParkNameByCode(parkCode));
            
            if (allCarNumbers.isEmpty()) {
                logger.warn("⚠️ [万象上东] 未找到业主手机 {} 的车牌信息", userPhone);
                return;
            }
            
            logger.info("🚗 [万象上东] 查询到业主名下共 {} 个车牌: {}", allCarNumbers.size(), allCarNumbers);
            
            // 5. 批量拉黑所有车牌
            String parkName = getParkNameByCode(parkCode);
            String violationType = "待检VIP月票车夜间超时";
            String violationReason = String.format("业主 %s 名下车辆 %s 夜间停车超时，关联拉黑", 
                    triggerMonthTick.getUserName(), carNo);
            
            int successCount = 0;
            int failCount = 0;
            
            for (String plateNumber : allCarNumbers) {
                try {
                    logger.info("🚫 [万象上东拉黑] 开始拉黑车牌: {}", plateNumber);
                    
                    // 记录违规
                    boolean recordResult = violationsService.recordViolation(
                            plateNumber,
                            parkCode,
                            parkName,
                            LocalDateTime.now(),
                            LocalDateTime.now(),
                            0L,
                            violationType,
                            violationReason,
                            null,
                            true
                    );
                    
                    if (recordResult) {
                        // 本地拉黑
                        violationsService.addToBlacklist(plateNumber, parkName, violationType, violationReason);
                        
                        // 调用外部接口拉黑
                        String finalBlacklistName = blacklistName != null ? blacklistName : "万象上东违规月票车";
                        callAddBlackListCarAPIForWanXiang(
                                plateNumber, parkCode, parkName,
                                finalBlacklistName, violationReason, blacklistDays
                        );
                        
                        successCount++;
                        logger.info("✅ [拉黑成功] 车牌: {}", plateNumber);
                    } else {
                        failCount++;
                        logger.error("❌ [拉黑失败] 车牌: {}", plateNumber);
                    }
                    
                } catch (Exception e) {
                    failCount++;
                    logger.error("❌ [拉黑异常] 车牌: {}, error={}", plateNumber, e.getMessage(), e);
                }
            }
            
            logger.info("✅ [万象上东批量拉黑完成] 总数: {}, 成功: {}, 失败: {}", 
                    allCarNumbers.size(), successCount, failCount);
            
        } catch (Exception e) {
            logger.error("❌ [万象上东批量拉黑异常] carNo={}, error={}", carNo, e.getMessage(), e);
        }
    }
    
    /**
     * 根据业主手机号查询所有车牌号
     * 
     * @param userPhone 业主手机号
     * @param parkName 车场名称
     * @return 车牌号列表
     */
    private List<String> getAllCarNumbersByPhone(String userPhone, String parkName) {
        try {
            if (userPhone == null || userPhone.trim().isEmpty()) {
                logger.warn("⚠️ [查询车牌] 业主手机号为空");
                return new ArrayList<>();
            }
            
            // 从 month_tick 表查询该业主的所有车牌
            QueryWrapper<MonthTick> wrapper = new QueryWrapper<>();
            wrapper.eq("user_phone", userPhone)
                   .eq("park_name", parkName)
                   .eq("valid_status", 1); // 只查询有效的月票
            
            List<MonthTick> monthTickList = monthTicketService.list(wrapper);
            
            List<String> carNumbers = new ArrayList<>();
            for (MonthTick monthTick : monthTickList) {
                if (monthTick.getCarNo() != null && !monthTick.getCarNo().trim().isEmpty()) {
                    carNumbers.add(monthTick.getCarNo());
                }
            }
            
            logger.info("📋 [查询车牌] 手机号 {} 在 {} 共有 {} 个车牌", userPhone, parkName, carNumbers.size());
            return carNumbers;
            
        } catch (Exception e) {
            logger.error("❌ [查询车牌异常] userPhone={}, error={}", userPhone, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 调用外部拉黑接口（万象上东专用）
     * 
     * @param plateNumber 车牌号
     * @param parkCode 车场编码
     * @param parkName 车场名称
     * @param blacklistName 黑名单名称
     * @param reason 拉黑原因
     * @param blacklistDays 拉黑天数
     */
    private void callAddBlackListCarAPIForWanXiang(String plateNumber, String parkCode, 
                                                   String parkName, String blacklistName, 
                                                   String reason, Object blacklistDays) {
        try {
            HashMap<String, Object> params = new HashMap<>();
            params.put("parkCodeList", Arrays.asList(parkCode));
            params.put("carCode", plateNumber);
            params.put("blacklistType", blacklistName);
            params.put("reason", reason);
            
            // 处理拉黑天数
            if ("永久".equals(blacklistDays)) {
                params.put("validDays", 9999);
            } else if (blacklistDays instanceof Integer) {
                params.put("validDays", blacklistDays);
            } else {
                params.put("validDays", 7); // 默认7天
            }
            
            JSONObject response = aikeConfig.downHandler(
                    AIKEConfig.AK_URL,
                    AIKEConfig.AK_KEY,
                    AIKEConfig.AK_SECRET,
                    "addBlackListCar",
                    params
            );
            
            logger.info("📡 [外部拉黑接口] plateNumber={}, response={}", plateNumber, response);
            
        } catch (Exception e) {
            logger.error("❌ [外部拉黑接口异常] plateNumber={}, error={}", plateNumber, e.getMessage(), e);
        }
    }
}