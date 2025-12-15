package com.parkingmanage.service.impl;

import cn.hutool.core.date.DateTime;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.common.HttpClientUtil;
import com.parkingmanage.entity.*;
import com.parkingmanage.mapper.VehicleReservationMapper;
import com.parkingmanage.service.ReportCarInService;
import com.parkingmanage.service.ReportCarOutService;
import com.parkingmanage.service.VehicleReservationService;
import com.parkingmanage.service.YardInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 李子雄
 */
@Slf4j
@Service
public class VehicleReservationServiceImpl extends ServiceImpl<VehicleReservationMapper, VehicleReservation> implements VehicleReservationService {

    @Resource
    private VehicleReservationService vehicleReservationService;

    @Resource
    private ReportCarInService reportCarInService;

    @Resource
    private ReportCarOutService reportCarOutService;

    @Resource
    private YardInfoService yardInfoService;

    @Override
    public int duplicate(VehicleReservation vehicleReservation) {
        return baseMapper.duplicate(vehicleReservation);
    }

    @Override
    public List<VehicleReservation> queryListVehicleReservation(String plateNumber, String yardName) {
        LambdaQueryWrapper<VehicleReservation> queryWrapper = new LambdaQueryWrapper();
        if (StringUtils.hasLength(plateNumber)) {
            queryWrapper.like(VehicleReservation::getPlateNumber, plateNumber);
        }
        if (StringUtils.hasLength(yardName)) {
            queryWrapper.like(VehicleReservation::getYardName, yardName);
        }
        queryWrapper.eq(VehicleReservation::getAppointmentFlag, 0);
        List<VehicleReservation> vehicleReservations = vehicleReservationService.list(queryWrapper);
        return vehicleReservations;
    }

    @Override
    public List<VehicleReservation> queryListVehicleReservationSuccess(String plateNumber, String yardName) {
        LambdaQueryWrapper<VehicleReservation> queryWrapper = new LambdaQueryWrapper();
        if (StringUtils.hasLength(plateNumber)) {
            queryWrapper.like(VehicleReservation::getPlateNumber, plateNumber);
        }
        if (StringUtils.hasLength(yardName)) {
            queryWrapper.like(VehicleReservation::getYardName, yardName);
        }

        queryWrapper.eq(VehicleReservation::getReserveFlag, 1);
        List<VehicleReservation> vehicleReservations = vehicleReservationService.list(queryWrapper);
        return vehicleReservations;
    }

    public static String calculatePercentage(double num1, double num2) {
        double result = (num1 / num2) * 100;
        DecimalFormat df = new DecimalFormat("#.00");
        return df.format(result);
    }

    @Override
    public void exportVehicleReservation(String startDate, String endDate, String yardName, String channelName, HttpServletResponse response) throws IOException, ParseException {
        System.out.println("开始时间" + startDate);
        System.out.println("结束时间" + endDate);
        System.out.println("车场名称" + yardName);
        String CarInParkCode = yardInfoService.selectParkCode(yardName);
        System.out.println("channelName = " + channelName);
        // 存储调用接口获取到的进场时间段的值
        ArrayList<getCarInData> getCarInDataArrayLists = new ArrayList<>();
        getCarInData getCarInData = new getCarInData();
        // 调用接口进行查询
        for (int i = 0; i < 2; i++) {
            System.out.println(String.valueOf(i + 1));
            // getCarInList
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("parkCode", CarInParkCode);
            hashMap.put("isPresence", "0");
            hashMap.put("startTime", startDate);
            hashMap.put("endTime", endDate);
            hashMap.put("pageNum", String.valueOf(i + 1));
            hashMap.put("pageSize", "1000");
//            String get = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/vehicleReservation/getCarInList", hashMap);
            String get = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/vehicleReservation/getCarInList", hashMap);
            JSONObject jsonObject = JSONObject.parseObject(get);
            System.out.println("jsonObject = " + get);
            System.out.println("jsonObject = " + jsonObject);
//            JSONObject jsonObjectDataCarIn = (JSONObject) jsonObject.get("data");
//            JSONObject jsonObjectDataCarInData = (JSONObject) jsonObjectDataCarIn.get("data");
//            Integer currCount = (Integer) jsonObjectDataCarIn.get("currCount");
//            JSONArray recordInList = (JSONArray) jsonObjectDataCarInData.get("recordInList");
//            // 根据currCount进行遍历
//            for (int i1 = 0; i1 < recordInList.size(); i1++) {
//                //每个对象进行存储
//                JSONObject jsonObject1 = recordInList.getJSONObject(i1);
//                getCarInData.setCarLicenseNumber(jsonObject1.getString("carLicenseNumber"));
//                getCarInData.setConfidence(jsonObject1.getInteger("confidence"));
//                getCarInData.setEnterCarLicenseColor(jsonObject1.getInteger("enterCarLicenseColor"));
//                getCarInData.setEnterCarLicenseNumber(jsonObject1.getString("enterCarLicenseNumber"));
//                getCarInData.setEnterChannelName(jsonObject1.getString("enterChannelName"));
//                getCarInData.setCarLicenseNumber(jsonObject1.getString("enterTime"));
//                getCarInData.setEnterType(jsonObject1.getInteger("enterType"));
//                getCarInData.setEnterVipType(jsonObject1.getInteger("enterVipType"));
//                getCarInData.setRecordType(jsonObject1.getInteger("recordType"));
//                getCarInDataArrayLists.add(getCarInData);
//            }
//            //遍历getCarInDataArrayLists中的值
//            for (int i1 = 0; i1 < getCarInDataArrayLists.size(); i1++) {
//                System.out.println("getCarInDataArrayLists---" + i1 +  "-----" + getCarInDataArrayLists.get(i1));
//            }

        }

//        //查询所有的VIP超位的车辆
//        String leaveVIPTime = "";
//        if (yardName.equals("万象上东")) {
//            // 导出万象的表
//            List<ReportCarOutReservation> reportCarOutReservationList = new ArrayList<>();
//            List<ReportCarOutReservation> reportCarOutLinShiReservationList = new ArrayList<>();
//            List<ReportCarInReservation> reportCarInReservationLists = reportCarInService.queryListReportOutExportWan(startDate, endDate, yardName, channelName);
//            List<ReportCarInReservation> reportCarInReservationList =
//                    reportCarInReservationLists.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(ReportCarInReservation::getParkingCode))), ArrayList::new));
//            System.out.println("reportCarInReservationList的长度" + reportCarInReservationList.size());
//            for (ReportCarInReservation reportCarInReservation : reportCarInReservationList) {
//                System.out.println("去重以后的数据reportCarOutReservation = " + reportCarInReservation);
//                // 对去重后的数据的车牌号码在离场表中进行查询，有的话取出离场时间，没有的话，离场时间为导表时间[未离场]
//                ReportCarOutReservation reportCarOutReservation = new ReportCarOutReservation();
//                reportCarOutReservation.setYardName(reportCarInReservation.getYardName());
//                reportCarOutReservation.setEnterCarLicenseNumber(reportCarInReservation.getEnterCarLicenseNumber());
//                reportCarOutReservation.setRemark(reportCarInReservation.getRemark());
//                reportCarOutReservation.setEnterChannelName(reportCarInReservation.getEnterChannelName());
//                reportCarOutReservation.setReleaseReason(reportCarInReservation.getReleaseReason());
//                reportCarOutReservation.setNotifierName(reportCarInReservation.getNotifierName());
//                reportCarOutReservation.setEnterTime(reportCarInReservation.getEnterTime());
//                // 查询离场表中的数据
//                // 判断当前车辆查询的离场表中的离场时间是否比当前导表时间要大
//                List<ReportCarOut> reportCarOut = reportCarOutService.selectLeaveTime(reportCarInReservation.getEnterCarLicenseNumber(), reportCarInReservation.getEnterTime(), startDate, endDate);
//                if (reportCarOut.size() == 0) {
//                    String res = endDate + "【未离场】";
//                    System.out.println("车辆暂未离场" + leaveVIPTime);
//                } else {
//                    reportCarOutReservation.setLeaveTime(reportCarOut.get(0).getLeaveTime());
//                }
//                reportCarOutReservationList.add(reportCarOutReservation);
//            }
//            //TODO 修改相同车牌的通知人和放行原因问题
//            //查询所有的临时车
//            List<ReportCarInReservation> reportCarOutLinShiReservationLists = reportCarInService.queryListReportCarOutExportLinShiWan(startDate, endDate, yardName, channelName);
//            List<ReportCarInReservation> reportCarInLinShiReservationList =
//                    reportCarOutLinShiReservationLists.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(ReportCarInReservation::getParkingCode))), ArrayList::new));
//            System.out.println("reportCarInLinShiReservationList的长度 = " + reportCarInLinShiReservationList.size());
//            for (ReportCarInReservation reportCarInLinShiReservation : reportCarInLinShiReservationList) {
//                System.out.println("去重之后reportCarInLinShiReservationList = " + reportCarInLinShiReservation);
//                ReportCarOutReservation reportCarOutReservation = new ReportCarOutReservation();
//                reportCarOutReservation.setYardName(reportCarInLinShiReservation.getYardName());
//                reportCarOutReservation.setEnterCarLicenseNumber(reportCarInLinShiReservation.getEnterCarLicenseNumber());
//                reportCarOutReservation.setEnterChannelName(reportCarInLinShiReservation.getEnterChannelName());
//                reportCarOutReservation.setReleaseReason(reportCarInLinShiReservation.getReleaseReason());
//                reportCarOutReservation.setRemark(reportCarInLinShiReservation.getRemark());
//                reportCarOutReservation.setNotifierName(reportCarInLinShiReservation.getNotifierName());
//                reportCarOutReservation.setEnterTime(reportCarInLinShiReservation.getEnterTime());
//                // 查询离场表中的数据
//                // 判断当前车辆查询的离场表中的离场时间是否比当前导表时间要大
//                List<ReportCarOut> reportCarOut = reportCarOutService.selectLeaveTime(reportCarInLinShiReservation.getEnterCarLicenseNumber(), reportCarInLinShiReservation.getEnterTime(), startDate, endDate);
//                if (reportCarOut.size() == 0) {
//                    String res = endDate + "【未离场】";
//                    //  reportCarOutReservation.setLeaveTime(res);
//                    System.out.println("车辆暂未离场" + res);
//                } else {
//                    reportCarOutReservation.setLeaveTime(reportCarOut.get(0).getLeaveTime());
//                }
//                reportCarOutLinShiReservationList.add(reportCarOutReservation);
//            }
//            //创建HSSFWorkbook对象
//            HSSFWorkbook wb = new HSSFWorkbook();
//            //创建备注解释的字符集合属性
//            ArrayList<String> strings = new ArrayList<>();
//            //建立sheet对象
//            HSSFSheet sheet = wb.createSheet("放行记录");
//            // 设置列宽
//            // sheet.setColumnWidth(1,25);
//            sheet.setColumnWidth(0, 25 * 100);
//            sheet.setColumnWidth(1, 25 * 150);
//            sheet.setColumnWidth(2, 25 * 256);
//            sheet.setColumnWidth(3, 25 * 256);
//            sheet.setColumnWidth(4, 25 * 256);
//            sheet.setColumnWidth(5, 25 * 256);
//            sheet.setColumnWidth(6, 25 * 400);
//            sheet.setColumnWidth(7, 25 * 350);
//            sheet.setColumnWidth(8, 25 * 350);
//            //设置行高
//            // 记住一点设置单元格样式相关的都是CellStyle来控制的,设置完之后只需set给单元格即可：cell.setCellStyle(cellStyle);
//            // 合并单元格后居中
//            CellStyle cellStyle = wb.createCellStyle();
//            // 垂直居中
//            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
//            cellStyle.setAlignment(HorizontalAlignment.CENTER);
//            // 设置字体
//            Font font = wb.createFont();
//            font.setFontName("宋体");
//            font.setItalic(false);
//            font.setStrikeout(false);
//            cellStyle.setFont(font);
//            // 设置背景色
//            // cellStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
//            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//            // 设置边框（一般标题不设置边框，是标题下的所有表格设置边框）
//            cellStyle.setBorderBottom(BorderStyle.THIN); //下边框
//            cellStyle.setBorderLeft(BorderStyle.THIN);//左边框
//            cellStyle.setBorderTop(BorderStyle.THIN);//上边框
//            cellStyle.setBorderRight(BorderStyle.THIN);//右边框
//            //在sheet里创建第一行，参数为行索引
//            HSSFRow row1 = sheet.createRow(0);
//            // 合并单元格：参数1：行号 参数2：起始列号 参数3：行号 参数4：终止列号
//            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
//            // 创建单元格
//            HSSFCell cell = row1.createCell(0);
//            cell.setCellStyle(cellStyle);
//            //设置单元格内容
//            String resTitle = "";
//            if (channelName.contains("万象上东")) {
//                resTitle = "万象上东";
//            } else if (channelName.contains("四季")) {
//                resTitle = "四季三期";
//            }
//            cell.setCellValue(resTitle + "" + startDate + "" + "至" + " " + endDate + " 临停车辆进场日报表");
//            row1.setHeight((short) 1400);
//            // 创建单元格
//            //在sheet里创建第二行
//            HSSFRow row2 = sheet.createRow(1);
//            //创建单元格并设置单元格内容
//            //第二行设置字体为黑体加粗
//            // 合并单元格后居中
//            CellStyle cellStyle1 = wb.createCellStyle();
//            // 设置字体
//            Font font1 = wb.createFont();
//            font1.setFontName("黑体");
//            cellStyle1.setVerticalAlignment(VerticalAlignment.CENTER);
//            //设置加粗
//            font1.setBold(true);
//            font1.setFontHeightInPoints((short) 11);
//            //设置字体大小
//            font1.setItalic(false);
//            font1.setStrikeout(false);
//            cellStyle1.setFont(font1);
//            //定义进场车辆统计数量变量 (report_car_in的总条数,但是得统计时间在选择的时间这个区间)
//            int reportCarInNumber = reportCarInService.countByDateWan(startDate, endDate, yardName, channelName);
//            //定义本地VIP统计数量变量 (在上述条件的基础上，筛选统计enterVipType的名称是'本地VIP')
//            int reportCarInVIPNumber = reportCarInService.countByDateVIPWan(startDate, endDate, yardName, channelName);
//            //合并两列居中
//            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 1));
//            sheet.addMergedRegion(new CellRangeAddress(1, 1, 2, 3));
//            sheet.addMergedRegion(new CellRangeAddress(1, 1, 4, 5));
//            //TODO 统计进场车辆数量
//            row2.createCell(0).getCellStyle().setFont(font1);
//            row2.createCell(0).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//            row2.createCell(0).getCellStyle().setBorderBottom(BorderStyle.THIN); //下边框
//            row2.createCell(0).getCellStyle().setBorderLeft(BorderStyle.THIN);//左边框
//            row2.createCell(0).getCellStyle().setBorderTop(BorderStyle.THIN);//上边框
//            row2.createCell(0).getCellStyle().setBorderRight(BorderStyle.THIN);//右边框
//            row2.createCell(0).setCellValue("进场车辆数量：" + reportCarInNumber);
//            //TODO 统计本地VIP数量
//            row2.createCell(2).getCellStyle().setBorderBottom(BorderStyle.THIN); //下边框
//            row2.createCell(2).getCellStyle().setBorderLeft(BorderStyle.THIN);//左边框
//            row2.createCell(2).getCellStyle().setBorderTop(BorderStyle.THIN);//上边框
//            row2.createCell(2).getCellStyle().setBorderRight(BorderStyle.THIN);//右边
//            row2.createCell(2).getCellStyle().setFont(font1);
//            row2.createCell(2).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//            row2.createCell(2).setCellValue("本地VIP数量：" + reportCarInVIPNumber);
//            //TODO 统计通知放行数量
//            row2.createCell(4).getCellStyle().setBorderBottom(BorderStyle.THIN); //下边框
//            row2.createCell(4).getCellStyle().setBorderLeft(BorderStyle.THIN);//左边框
//            row2.createCell(4).getCellStyle().setBorderTop(BorderStyle.THIN);//上边框
//            row2.createCell(4).getCellStyle().setBorderRight(BorderStyle.THIN);//右边
//            row2.createCell(4).getCellStyle().setFont(font1);
//            row2.createCell(4).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//            row2.createCell(4).setCellValue("通知放行数量：" + (reportCarInReservationList.size() + reportCarInLinShiReservationList.size()));
//            //TODO 统计通知放行VIP超位数量及占比
//            row2.createCell(6).getCellStyle().setBorderBottom(BorderStyle.THIN); //下边框
//            row2.createCell(6).getCellStyle().setBorderLeft(BorderStyle.THIN);//左边框
//            row2.createCell(6).getCellStyle().setBorderTop(BorderStyle.THIN);//上边框
//            row2.createCell(6).getCellStyle().setBorderRight(BorderStyle.THIN);//右边
//            row2.createCell(6).getCellStyle().setFont(font1);
//            row2.createCell(6).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//            double percentage = (double) (reportCarInNumber - reportCarInVIPNumber) / reportCarInNumber;
//            DecimalFormat df = new DecimalFormat("#.00");
//            row2.createCell(6).setCellValue("通知放行VIP超位数量及占比：" + df.format(percentage * 100) + "%");
//            System.out.println("通知放行VIP超位数量及占比：" + df.format(percentage * 100) + "%");
//            //TODO 统计通知放行临时车数量及占比
//            row2.createCell(7).getCellStyle().setFont(font1);
//            row2.createCell(7).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//            double percentageLinShi = 1 - percentage;
//            DecimalFormat dfLinShi = new DecimalFormat("#.00");
//            System.out.println("通知放行临时车数量及占比" + dfLinShi.format(percentageLinShi * 100) + "%");
//            row2.createCell(7).setCellValue("通知放行临时车数量及占比：" + dfLinShi.format(percentageLinShi * 100) + "%");
//            row2.setHeight((short) 1000);
//            //设置第三行
//            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 8));
//            //在sheet里创建第三行
//            HSSFRow row3 = sheet.createRow(2);
//            row3.setHeight((short) 800);
//            row3.createCell(0).setCellValue("超位车辆放行统计");
//            //设置第4行的cellStyle
//            // 合并单元格后居中
//            CellStyle cellStyle2 = wb.createCellStyle();
//            // 垂直居中
//            //设置合并两列居中
//            cellStyle2.setVerticalAlignment(VerticalAlignment.CENTER);
//            cellStyle2.setAlignment(HorizontalAlignment.CENTER);
//            // 设置字体
//            Font font2 = wb.createFont();
//            font2.setFontName("等线");
//            font2.setFontHeightInPoints((short) 11);
//            font2.setItalic(false);
//            font2.setStrikeout(false);
//            cellStyle2.setFont(font2);
//            //第4行写入数据
//            HSSFRow row4 = sheet.createRow(3);
//            for (int i = 0; i < 9; i++) {
//                row4.createCell(i).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                row4.createCell(i).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                row4.createCell(i).getCellStyle().setBorderBottom(BorderStyle.THIN); //下边框
//                row4.createCell(i).getCellStyle().setBorderLeft(BorderStyle.THIN);//左边框
//                row4.createCell(i).getCellStyle().setBorderTop(BorderStyle.THIN);//上边框
//                row4.createCell(i).getCellStyle().setBorderRight(BorderStyle.THIN);//右边框
//            }
//            row4.createCell(0).setCellValue("序号");
//            row4.createCell(1).setCellValue("车牌号码");
//            row4.createCell(2).setCellValue("通知人");
//            row4.createCell(3).setCellValue("放行原因");
//            row4.createCell(4).setCellValue("进场时间");
//            row4.createCell(5).setCellValue("离场时间");
//            row4.createCell(6).setCellValue("停车时长");
//            row4.createCell(7).setCellValue("备注");
//            row4.createCell(8).setCellValue("入场通道");
//            row4.setHeight((short) 500);
//            int size = reportCarOutReservationList.size();
//            DateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            //格式化Date数据
//            DateFormat formatter2 = new SimpleDateFormat("HH小时mm分钟ss秒");
//            //定义记录当前行数的index
//            int index = 0;
//            String dynamicCarportNumber = "";
//            // 定义超位VIP车辆的时间差
//            String timesVIP = "";
//            // 定义超位临时车车辆的时间差
//            String timesLinShi = "";
//            // 输出的时分秒格式的字符串
//            System.out.println("size = " + size);
//            if (size == 0) {
//                System.out.println("查询为空");
//            } else {
//                ArrayList<ExportData> exportDataArrayList = new ArrayList<>();
//                //首先遍历存储到集合中进行排序
//                for (int j1 = 4; j1 < size + 4; j1++) {
//                    ExportData exportData = new ExportData();
//                    exportData.setRemark(reportCarOutReservationList.get(j1 - 4).getRemark());
//                    exportData.setCarNumber(reportCarOutReservationList.get(j1 - 4).getEnterCarLicenseNumber());
//                    exportData.setNotifier(reportCarOutReservationList.get(j1 - 4).getNotifierName());
//                    exportData.setEnterTime(formatter1.format(formatter1.parse(reportCarOutReservationList.get(j1 - 4).getEnterTime())));
//                    exportData.setChannelName(reportCarOutReservationList.get(j1 - 4).getEnterChannelName());
//                    //做时间差
//                    if (reportCarOutReservationList.get(j1 - 4).getEnterTime() == null) {
//                        System.out.println("j1 = " + j1);
//                    } else if (reportCarOutReservationList.get(j1 - 4).getLeaveTime() == null) {
////                    rowIndex.createCell(6).setCellValue("暂无时间差");
//                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                        Date enterTime = dateFormat.parse(reportCarOutReservationList.get(j1 - 4).getEnterTime());
//                        Date leaveTime = dateFormat.parse(endDate);
//                        long diffInMillies = Math.abs(leaveTime.getTime() - enterTime.getTime());
//                        long diffHours = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
//                        long diffMinutes = TimeUnit.MINUTES.convert(diffInMillies - TimeUnit.HOURS.toMillis(diffHours), TimeUnit.MILLISECONDS);
//                        long diffSeconds = TimeUnit.SECONDS.convert(diffInMillies - TimeUnit.HOURS.toMillis(diffHours) - TimeUnit.MINUTES.toMillis(diffMinutes), TimeUnit.MILLISECONDS);
//                        System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                        String result = "";
//                        if (diffHours >= 24) {
//                            long days = diffHours / 24;
//                            diffHours %= 24;
//                            result = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                        } else {
//                            if (diffHours == 0) {
//                                result = diffMinutes + "分钟" + diffSeconds + "秒";
//                            } else {
//                                result = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                            }
//                        }
//                        exportData.setParkingDuration(result);
//                        exportData.setLeaveTime(null);
//                        exportData.setParkingDurationInMillies(diffInMillies);
//                    } else {
//                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                        Date enterTime = dateFormat.parse(reportCarOutReservationList.get(j1 - 4).getEnterTime());
//                        Date leaveTime = dateFormat.parse(reportCarOutReservationList.get(j1 - 4).getLeaveTime());
//                        exportData.setLeaveTime(formatter1.format(formatter1.parse(reportCarOutReservationList.get(j1 - 4).getLeaveTime())));
//                        long diffInMillies = Math.abs(leaveTime.getTime() - enterTime.getTime());
//                        long diffHours = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
//                        long diffMinutes = TimeUnit.MINUTES.convert(diffInMillies - TimeUnit.HOURS.toMillis(diffHours), TimeUnit.MILLISECONDS);
//                        long diffSeconds = TimeUnit.SECONDS.convert(diffInMillies - TimeUnit.HOURS.toMillis(diffHours) - TimeUnit.MINUTES.toMillis(diffMinutes), TimeUnit.MILLISECONDS);
//                        System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                        String result = "";
//                        if (diffHours >= 24) {
//                            long days = diffHours / 24;
//                            diffHours %= 24;
//                            result = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                        } else {
//                            if (diffHours == 0) {
//                                result = diffMinutes + "分钟" + diffSeconds + "秒";
//                            } else {
//                                result = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                            }
//                        }
//                        exportData.setParkingDuration(result);
//                        exportData.setParkingDurationInMillies(diffInMillies);
//                    }
//                    exportDataArrayList.add(exportData);
//                }
//                Comparator<ExportData> parkingDurationComparator = new Comparator<ExportData>() {
//                    @Override
//                    public int compare(ExportData o1, ExportData o2) {
//                        return Long.compare(o2.getParkingDurationInMillies(), o1.getParkingDurationInMillies());
//                    }
//                };
//                // 使用比较器对exportDataArrayList进行排序
//                Collections.sort(exportDataArrayList, parkingDurationComparator);
//                //遍历所有符合条件的数据
//                for (int i = 4; i < size + 4; i++) {
//                    //所有的行数
//                    HSSFRow rowIndex = sheet.createRow(i);
//                    rowIndex.setHeight((short) 500);
//                    rowIndex.createCell(0).getCellStyle().setFont(font2);
//                    rowIndex.createCell(0).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndex.createCell(0).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndex.createCell(0).setCellValue(i - 3);
//                    rowIndex.createCell(1).getCellStyle().setFont(font2);
//                    rowIndex.createCell(1).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndex.createCell(1).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndex.createCell(1).setCellValue(exportDataArrayList.get(i - 4).getCarNumber());
//                    rowIndex.createCell(2).getCellStyle().setFont(font2);
//                    rowIndex.createCell(2).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndex.createCell(2).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndex.createCell(2).setCellValue(exportDataArrayList.get(i - 4).getNotifier());
//                    rowIndex.createCell(3).getCellStyle().setFont(font2);
//                    rowIndex.createCell(3).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndex.createCell(3).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndex.createCell(3).setCellValue(exportDataArrayList.get(i - 4).getRemark());
//                    rowIndex.createCell(4).getCellStyle().setFont(font2);
//                    rowIndex.createCell(4).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndex.createCell(4).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    // 设置边框（一般标题不设置边框，是标题下的所有表格设置边框）
//                    if (reportCarOutReservationList.get(i - 4).getEnterTime() == null) {
//                        rowIndex.createCell(4).setCellValue("暂无入场时间");
//                    } else {
////                    System.out.println("VIP入场时间" + formatter1.format(formatter1.parse(reportCarOutReservationList.get(i - 4).getEnterTime())));
////                    Date parse = formatter1.parse(reportCarOutReservationList.get(i - 4).getEnterTime());
////                    rowIndex.createCell(4).setCellValue(formatter1.format((parse)));
//                        rowIndex.createCell(4).setCellValue(exportDataArrayList.get(i - 4).getEnterTime());
//                    }
//                    rowIndex.createCell(5).getCellStyle().setFont(font2);
//                    rowIndex.createCell(5).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndex.createCell(5).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    if (exportDataArrayList.get(i - 4).getLeaveTime() == null) {
//                        String res = endDate + "【未离场】";
//                        rowIndex.createCell(5).setCellValue(res);
//                    } else {
////                    Date parse = formatter1.parse(reportCarOutReservationList.get(i - 4).getLeaveTime());
////                    System.out.println("VIP离场时间" + formatter1.format(parse));
////                    rowIndex.createCell(5).setCellValue(formatter1.format(parse));
//                        rowIndex.createCell(5).setCellValue(exportDataArrayList.get(i - 4).getLeaveTime());
//                    }
//                    rowIndex.createCell(6).setCellValue(exportDataArrayList.get(i - 4).getParkingDuration());
//                    // TODO 备注查询当前车位之前的开通车牌[黑A98A98,黑AKC400]
//                    // TODO 例子：【车辆[黑A98A98]进场时间[2024-04-11 13:09:37] 离场时间[2024-04-16 13:23:56] 停车时长:[120小时14分钟19秒]】
//                    // 根据当前车辆的车牌号码查询出月票中剩余车辆的进出场时间
//                    // 调用车牌号码查询月票id，先查询出除了当前车辆以外的其余车辆
//                    // TODO 查询当前车辆是否为本地VIP超位车辆
//                    int pageNum = 1;
//                    int pageSize = 1000;
//                    // 将获取到的值存储到数组集合中，例如：
//                    ArrayList<String> values = new ArrayList<>();
//                    HashMap<String, String> hashMapPre = new HashMap<>();
//                    hashMapPre.put("carCode", exportDataArrayList.get(i - 4).getCarNumber());
//                    hashMapPre.put("pageNum", String.valueOf(pageNum));
//                    hashMapPre.put("pageSize", String.valueOf(pageSize));
//                    String getPre = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/vehicleReservation/getOnlineMonthTicketByCarCard", hashMapPre);
//                    JSONObject jsonObjectPre = JSONObject.parseObject(getPre);
//                    JSONObject jsonObjectDataPre = (JSONObject) jsonObjectPre.get("data");
//                    JSONObject jsonObjectDataDataPre = (JSONObject) jsonObjectDataPre.get("data");
//                    JSONArray monthTicketList = jsonObjectDataDataPre.getJSONArray("monthTicketList");
//                    // 将monthTicketList赋值给Arraylist
//                    ArrayList<MonthTick> monthTicks = new ArrayList<>();
//                    // 将  JSONArray monthTicketList 赋值给 monthTicks
//                    for (int j = 0; j < monthTicketList.size(); j++) {
//                        MonthTick monthTick = new MonthTick();
//                        JSONObject jsonObjectData = (JSONObject) monthTicketList.get(j);
//                        monthTick.setCarCode(jsonObjectData.getString("carCode"));
//                        monthTick.setTimeperiodList(jsonObjectData.getString("timeperiodList"));
//                        monthTick.setDynamicCarportNumber(jsonObjectData.getInteger("dynamicCarportNumber"));
//                        monthTicks.add(monthTick);
//                    }
//                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
//                    SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                    Date currentTime = sdf.parse(sdf.format(new Date()));
//                    //  找到当前生效的{}
//                    String VIPCarCodes = "";
//                    System.out.println("monthTicketList = " + monthTicketList.size());
//                    // 判断若monthTicketList的长度为1，则证明肯定是，找出来他的开通车牌；
//                    if (monthTicketList.size() == 1) {
//                        VIPCarCodes = monthTicketList.getJSONObject(0).getString("carCode");
//                        System.out.println("车牌号为：" + VIPCarCodes);
//                    } else {
//                        //遍历monthTicks每个值
//                        for (int i1 = 0; i1 < monthTicks.size(); i1++) {
//                            //查询所有的monTick中的getCarCodes是否符合条件
//                            String timeperiodList = monthTicks.get(i1).getTimeperiodList();
//                            if (!timeperiodList.contains(",")) {
//                                System.out.println("timeperiodList = " + timeperiodList);
//                                String[] startEnd = timeperiodList.split("\\*");
//                                System.out.println("startEnd的长度：" + startEnd.length);
//                                for (String s : startEnd) {
//                                    System.out.println("s = " + s);
//                                }
//                                if (startEnd.length == 2) {
//                                    Date startTime = sdf.parse(startEnd[0]);
//                                    Date endTime = sdf.parse(startEnd[1]);
//                                    if (currentTime.after(startTime) && currentTime.before(endTime)) {
//                                        VIPCarCodes = monthTicks.get(i1).getCarCode();
//                                        dynamicCarportNumber = String.valueOf(monthTicks.get(i1).getDynamicCarportNumber());
//                                        System.out.println("没有逗号的" + VIPCarCodes);
//                                    }
//                                } else {
//                                    System.out.println("没有startEnd");
//                                }
//                            } else {
//                                String[] timeRanges = timeperiodList.split(",");
//                                for (String timeRange : timeRanges) {
//                                    String[] startEnd = timeRange.split("\\*");
//                                    String start = startEnd[0];
//                                    long l = Long.parseLong(start);
//                                    Date startTime = sdf.parse(start);
//                                    // 格式化时间格式startTime Fri Apr 01 00:00:00 CST 2022 格式化为 ""
//                                    Date endTime = sdf.parse(startEnd[1]);
//                                    if (currentTime.after(startTime) && currentTime.before(endTime)) {
//                                        VIPCarCodes = monthTicks.get(i1).getCarCode();
//                                        dynamicCarportNumber = String.valueOf(monthTicks.get(i1).getDynamicCarportNumber());
//                                        System.out.println("有逗号的 = " + VIPCarCodes);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    System.out.println("VIPCarCodes = " + VIPCarCodes);
//                    String[] VIPCarCodesArr = VIPCarCodes.split(",");
//                    // 将VIPCarCodesArr中的值去除和当前车牌相同的车牌号
//                    ArrayList<String> VIPCarCodesList = new ArrayList<>();
//                    if (VIPCarCodesArr.length == 1) {
//                        System.out.println("月票车只开通了一个车牌" + VIPCarCodesArr);
//                        System.out.println("不是超位车辆" + VIPCarCodesList);
//                    } else {
//                        String carNo = exportDataArrayList.get(i - 4).getCarNumber();
//                        for (int j = 0; j < VIPCarCodesArr.length; j++) {
//                            if (!VIPCarCodesArr[j].equals(carNo)) {
//                                System.out.println("开通车牌号码为：" + VIPCarCodesArr[j]);
//                                VIPCarCodesList.add(VIPCarCodesArr[j]);
//                            }
//                        }
//                    }
//                    strings.clear();
//                    strings.add("开通车牌号码为：" + Arrays.toString(VIPCarCodesArr) + "\n");
//                    //去除当前车牌号的其余车牌号码，遍历VIPCarCodesList
//                    String enterCarLicenseNumber = exportDataArrayList.get(i - 4).getCarNumber();
//                    for (String carCode : VIPCarCodesList) {
//                        System.out.println("去除当前车牌号的其余车牌号码:" + carCode);
//                        // 查询当前车牌号码的进出场记录
//                        HashMap<String, String> hashMapDetail = new HashMap<>();
//                        // 调用yard_info根据传入的车场名称查询车场编号
////                        String parkCode = s1;
////                        System.out.println("parkCode = " + parkCode);
//                        hashMapDetail.put("parkCode",CarInParkCode);
//                        hashMapDetail.put("carCode", carCode);
//                        String getDetail = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/vehicleReservation/getParkDetail", hashMapDetail);
//                        JSONObject jsonObjectDetail = JSONObject.parseObject(getDetail);
//                        JSONObject jsonObjectDatagetDetail = (JSONObject) jsonObjectDetail.get("data");
//                        Integer resultCode = (Integer) jsonObjectDatagetDetail.get("resultCode");
//                        System.out.println("resultCode = " + resultCode);
//                        if (resultCode == 908) {
//                            String str = "【车辆[" + carCode + "暂无停车信息]】";
//                        } else if (resultCode == -1) {
//                            String str = "【车辆[" + carCode + "查询失败！]】";
//                        } else {
//                            JSONObject jsonObjectDataDataDetail = (JSONObject) jsonObjectDatagetDetail.get("data");
//                            String enterDate = (String) jsonObjectDataDataDetail.get("enterDate");
//                            String outDate = (String) jsonObjectDataDataDetail.get("outDate");
//                            System.out.println(carCode + "jsonObjectDataDataDetail = " + jsonObjectDataDataDetail);
//                            System.out.println(carCode + "enterDate = " + enterDate);
//                            System.out.println(carCode + "outDate = " + outDate);
//                            //判断进场时间是否在当前车辆的进场时间要早，早的话写进去否则不写
//                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
//                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                            Date startVIPDate = formatter.parse(exportDataArrayList.get(i - 4).getEnterTime());
//                            Date endVIPDate = formatter.parse(endDate);
//                            if (jsonObjectDataDataDetail.get("enterDate") == null) {
//                                System.out.println(carCode + "enterDate为：null");
//                            }else {
//                                Date enterVIPDate = dateFormat.parse(enterDate);
//                                String localEnterTime = formatter.format(enterVIPDate);
//                                if (outDate == null) {
//                                    if (formatter.parse(localEnterTime).before(startVIPDate)) {
//                                        long diffInMillies = Math.abs(endVIPDate.getTime() - enterVIPDate.getTime());
//                                        long diffHours = diffInMillies / (60 * 60 * 1000);
//                                        long diffMinutes = (diffInMillies / (60 * 1000)) % 60;
//                                        long diffSeconds = (diffInMillies / 1000) % 60;
//                                        String resultParam = "";
//                                        if (diffHours > 24) {
//                                            long days = diffHours / 24;
//                                            diffHours %= 24;
//                                            resultParam = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                        } else {
//                                            if (diffHours == 0) {
//                                                resultParam = diffMinutes + "分钟" + diffSeconds + "秒";
//                                            } else {
//                                                resultParam = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                            }
//                                        }
//                                        System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                                        Date enterDateParam = dateFormat.parse(enterDate);
//                                        String parse = formatter.format(enterDateParam);
//                                        System.out.println("enterDateParam = " + enterDateParam.toString());
//                                        System.out.println("parse = " + parse);
//                                        String str = "【车辆[" + carCode + "]进场时间[" + parse + "] 离场时间[" + endDate + "] 停车时长:[ " + resultParam + "]{未离场}】";
//                                        strings.add(str);
//                                    } else {
//                                        // 此车可能不是，查询此车的进场记录
//                                        System.out.println("超位车辆的进场时间在当前车辆以后进入！！");
//                                        // 查询本地数据库中此车的进出场记录
//                                        List<ReportCarIn> reportCarIns = reportCarInService.selectCarRecords(carCode, exportDataArrayList.get(i - 4).getEnterTime());
//                                        List<ReportCarOut> reportCarOuts = reportCarOutService.selectCarRecords(carCode, reportCarIns.get(0).getEnterTime());
//                                        if (reportCarIns.get(0).getEnterTime() != null && reportCarOuts.size() != 0) {
//                                            Date outEnterTimeParam = formatter.parse(reportCarIns.get(0).getEnterTime());
//                                            Date outLeaveTimeParam = formatter.parse(reportCarOuts.get(0).getLeaveTime());
//                                            // 判断reportCarOut.getLeaveTime()和enterCarLicenseNumber的大小
//                                            // 若reportCarIns有数据，reportCarOuts有数据，已离场
//                                            if (outEnterTimeParam.before(startVIPDate) && outLeaveTimeParam.after(startVIPDate)) {
//                                                long diffInMillies = Math.abs(outLeaveTimeParam.getTime() - outEnterTimeParam.getTime());
//                                                long diffHours = diffInMillies / (60 * 60 * 1000);
//                                                long diffMinutes = (diffInMillies / (60 * 1000)) % 60;
//                                                long diffSeconds = (diffInMillies / 1000) % 60;
//                                                String resultParam = "";
//                                                if (diffHours > 24) {
//                                                    long days = diffHours / 24;
//                                                    diffHours %= 24;
//                                                    resultParam = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                                } else {
//                                                    if (diffHours == 0) {
//                                                        resultParam = diffMinutes + "分钟" + diffSeconds + "秒";
//                                                    } else {
//                                                        resultParam = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                                    }
//                                                }
//                                                System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                                                //  Date enterDateParam = dateFormat.parse(enterDate);
//                                                String parse = formatter.format(outEnterTimeParam);
//                                                String parse1 = formatter.format(outLeaveTimeParam);
//                                                String str = "【车辆[" + carCode + "]进场时间[" + parse + "] 离场时间[" + parse1 + "] 停车时长:[ " + resultParam + "]】";
//                                                strings.add(str);
//                                            } else {
//                                                System.out.println(carCode + "时间不匹配！");
//                                            }
//                                        } else {
//                                            System.out.println(carCode + "空！");
//                                        }
//                                    }
//                                } else {
//                                    // 判断当前数据的进场时间是否在被占位车辆的进场时间之前：进场时间之前，离场时间之后的数据
//                                    Date parse3 = formatter.parse(exportDataArrayList.get(i - 4).getEnterTime());
//                                    Date outVIPDate = dateFormat.parse(outDate);
//                                    String localOutTime = formatter.format(outVIPDate);
//                                    if (formatter.parse(localEnterTime).before(parse3) && formatter.parse(localOutTime).after(parse3)) {
//                                        Date outLeaveTimeParam = formatter.parse(localOutTime);
//                                        Date outEnterTimeParam = formatter.parse(localEnterTime);
//                                        long diffInMillies = Math.abs(outLeaveTimeParam.getTime() - outEnterTimeParam.getTime());
//                                        long diffHours = diffInMillies / (60 * 60 * 1000);
//                                        long diffMinutes = (diffInMillies / (60 * 1000)) % 60;
//                                        long diffSeconds = (diffInMillies / 1000) % 60;
//                                        String resultParam = "";
//                                        if (diffHours > 24) {
//                                            long days = diffHours / 24;
//                                            diffHours %= 24;
//                                            resultParam = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                        } else {
//                                            if (diffHours == 0) {
//                                                resultParam = diffMinutes + "分钟" + diffSeconds + "秒";
//                                            } else {
//                                                resultParam = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                            }
//                                        }
//                                        System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                                        String parse = formatter.format(outEnterTimeParam);
//                                        String parse1 = formatter.format(outLeaveTimeParam);
//                                        String str = "【车辆[" + carCode + "]进场时间[" + parse + "] 离场时间[" + parse1 + "] 停车时长:[ " + resultParam + "]】";
//                                        strings.add(str);
//                                    } else {
//                                        // 查询本地数据库
//                                        // 此车可能不是，查询此车的进场记录
//                                        System.out.println("超位车辆的进场时间在当前车辆以后进入！！");
//                                        // 查询本地数据库中此车的进出场记录
//                                        List<ReportCarIn> reportCarIns = reportCarInService.selectCarRecords(carCode, exportDataArrayList.get(i - 4).getEnterTime());
//                                        if (reportCarIns.size() != 0) {
//                                            List<ReportCarOut> reportCarOuts = reportCarOutService.selectCarRecords(carCode, reportCarIns.get(0).getEnterTime());
//                                            // 判断reportCarOut.getLeaveTime()和enterCarLicenseNumber的大小
//                                            Date outEnterTimeParam = formatter.parse(reportCarIns.get(0).getEnterTime());
//                                            // 若reportCarIns有数据，reportCarOuts有数据，已离场
//                                            if (reportCarOuts.size() != 0) {
//                                                Date outLeaveTimeParam = formatter.parse(reportCarOuts.get(0).getLeaveTime());
//                                                if (outEnterTimeParam.before(startVIPDate) && outLeaveTimeParam.after(startVIPDate)) {
//                                                    long diffInMillies = Math.abs(outLeaveTimeParam.getTime() - outEnterTimeParam.getTime());
//                                                    long diffHours = diffInMillies / (60 * 60 * 1000);
//                                                    long diffMinutes = (diffInMillies / (60 * 1000)) % 60;
//                                                    long diffSeconds = (diffInMillies / 1000) % 60;
//                                                    String resultParam = "";
//                                                    if (diffHours > 24) {
//                                                        long days = diffHours / 24;
//                                                        diffHours %= 24;
//                                                        resultParam = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                                    } else {
//                                                        if (diffHours == 0) {
//                                                            resultParam = diffMinutes + "分钟" + diffSeconds + "秒";
//                                                        } else {
//                                                            resultParam = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                                        }
//                                                    }
//                                                    System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                                                    String parse = formatter.format(outEnterTimeParam);
//                                                    String parse1 = formatter.format(outLeaveTimeParam);
//                                                    String str = "【车辆[" + carCode + "]进场时间[" + parse + "] 离场时间[" + parse1 + "] 停车时长:[ " + resultParam + "]】";
//                                                    strings.add(str);
//                                                } else {
//                                                    System.out.println(carCode + "不满足!");
//                                                }
//                                            } else {
//                                                if (outEnterTimeParam.before(startVIPDate)) {
//                                                    // 若reportCarIns有数据，reportCarOuts无数据，未离场
//                                                    long diffInMillies = Math.abs(endVIPDate.getTime() - outEnterTimeParam.getTime());
//                                                    long diffHours = diffInMillies / (60 * 60 * 1000);
//                                                    long diffMinutes = (diffInMillies / (60 * 1000)) % 60;
//                                                    long diffSeconds = (diffInMillies / 1000) % 60;
//                                                    String resultParam = "";
//                                                    if (diffHours > 24) {
//                                                        long days = diffHours / 24;
//                                                        diffHours %= 24;
//                                                        resultParam = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                                    } else {
//                                                        if (diffHours == 0) {
//                                                            resultParam = diffMinutes + "分钟" + diffSeconds + "秒";
//                                                        } else {
//                                                            resultParam = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                                        }
//                                                    }
//                                                    System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                                                    String parse = formatter.format(outEnterTimeParam);
//                                                    String str = "【车辆[" + carCode + "]进场时间[" + parse + "] 离场时间[" + endDate + "] 停车时长:[ " + resultParam + "]{未离场}】";
//                                                    strings.add(str);
//                                                } else {
//                                                    System.out.println("outEnterTimeParam = " + outEnterTimeParam);
//                                                }
//
//                                            }
//                                        }
//                                        System.out.println("当前非占位车辆！");
//
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    rowIndex.createCell(7).getCellStyle().setFont(font2);
//                    rowIndex.createCell(7).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndex.createCell(7).setCellValue(strings.toString());
//                    rowIndex.createCell(8).getCellStyle().setFont(font2);
//                    rowIndex.createCell(8).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndex.createCell(8).setCellValue(exportDataArrayList.get(i - 4).getChannelName());
//                }
//            }
//            // 输出集合字符串
//            for (int i = 0; i < strings.size(); i++) {
//                System.out.println("各个字符串是：" + strings.get(i));
//            }
//            int linShiReservationSize = reportCarOutLinShiReservationList.size();
//            index = size + 4;
//            System.out.println("当前行数为：" + index);
//            //设置第三行
//            sheet.addMergedRegion(new CellRangeAddress(index, index, 0, 8));
//            //撰写临时车辆放行统计
//            HSSFRow rowTail = sheet.createRow(index);
//            rowTail.setHeight((short) 800);
//            rowTail.createCell(0).setCellValue("临时车辆放行统计");
//            HSSFRow rowTailTitle = sheet.createRow(index + 1);
//            for (int i = 0; i < 9; i++) {
//                rowTailTitle.createCell(i).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                rowTailTitle.createCell(i).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                rowTailTitle.createCell(i).getCellStyle().setBorderBottom(BorderStyle.THIN); //下边框
//                rowTailTitle.createCell(i).getCellStyle().setBorderLeft(BorderStyle.THIN);//左边框
//                rowTailTitle.createCell(i).getCellStyle().setBorderTop(BorderStyle.THIN);//上边框
//                rowTailTitle.createCell(i).getCellStyle().setBorderRight(BorderStyle.THIN);//右边框
//            }
//            rowTailTitle.createCell(0).setCellValue("序号");
//            rowTailTitle.createCell(1).setCellValue("车牌号码");
//            rowTailTitle.createCell(2).setCellValue("通知人");
//            rowTailTitle.createCell(3).setCellValue("放行原因");
//            rowTailTitle.createCell(4).setCellValue("进场时间");
//            rowTailTitle.createCell(5).setCellValue("离场时间");
//            rowTailTitle.createCell(6).setCellValue("停车时长");
//            rowTailTitle.createCell(7).setCellValue("备注");
//            rowTailTitle.createCell(8).setCellValue("入场通道");
//            rowTailTitle.setHeight((short) 500);
//            // 输出的时分秒格式的字符串
//            if (linShiReservationSize == 0) {
//                System.out.println("查询为空");
//            } else {
//                ArrayList<ExportData> exportDataTemporary = new ArrayList<>();
//                for (int i2 = (index + 2); i2 < (linShiReservationSize + index + 2); i2++) {
//                    ExportData exportData = new ExportData();
//                    exportData.setRemark(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getRemark());
//                    exportData.setCarNumber(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getEnterCarLicenseNumber());
//                    exportData.setNotifier(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getNotifierName());
//                    exportData.setEnterTime(formatter1.format(formatter1.parse(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getEnterTime())));
//                    exportData.setChannelName(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getEnterChannelName());
//                    //做时间差
//                    if (reportCarOutLinShiReservationList.get(i2 - (index + 2)).getEnterTime() == null) {
//                        System.out.println("i2 = " + i2);
//                    } else if (reportCarOutLinShiReservationList.get(i2 - (index + 2)).getLeaveTime() == null) {
////                    rowIndex.createCell(6).setCellValue("暂无时间差");
//                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                        Date enterTime = dateFormat.parse(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getEnterTime());
//                        Date leaveTime = dateFormat.parse(endDate);
//                        long diffInMillies = Math.abs(leaveTime.getTime() - enterTime.getTime());
//                        long diffHours = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
//                        long diffMinutes = TimeUnit.MINUTES.convert(diffInMillies - TimeUnit.HOURS.toMillis(diffHours), TimeUnit.MILLISECONDS);
//                        long diffSeconds = TimeUnit.SECONDS.convert(diffInMillies - TimeUnit.HOURS.toMillis(diffHours) - TimeUnit.MINUTES.toMillis(diffMinutes), TimeUnit.MILLISECONDS);
//                        System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                        String result = "";
//                        if (diffHours >= 24) {
//                            long days = diffHours / 24;
//                            diffHours %= 24;
//                            result = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                        } else {
//                            if (diffHours == 0) {
//                                result = diffMinutes + "分钟" + diffSeconds + "秒";
//                            } else {
//                                result = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                            }
//                        }
//                        exportData.setParkingDuration(result);
//                        exportData.setParkingDurationInMillies(diffInMillies);
//                    } else {
//                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                        Date enterTime = dateFormat.parse(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getEnterTime());
//                        Date leaveTime = dateFormat.parse(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getLeaveTime());
//                        exportData.setLeaveTime(formatter1.format(formatter1.parse(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getLeaveTime())));
//                        long diffInMillies = Math.abs(leaveTime.getTime() - enterTime.getTime());
//                        long diffHours = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
//                        long diffMinutes = TimeUnit.MINUTES.convert(diffInMillies - TimeUnit.HOURS.toMillis(diffHours), TimeUnit.MILLISECONDS);
//                        long diffSeconds = TimeUnit.SECONDS.convert(diffInMillies - TimeUnit.HOURS.toMillis(diffHours) - TimeUnit.MINUTES.toMillis(diffMinutes), TimeUnit.MILLISECONDS);
//                        System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                        String result = "";
//                        if (diffHours >= 24) {
//                            long days = diffHours / 24;
//                            diffHours %= 24;
//                            result = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                        } else {
//                            if (diffHours == 0) {
//                                result = diffMinutes + "分钟" + diffSeconds + "秒";
//                            } else {
//                                result = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                            }
//                        }
//                        exportData.setParkingDuration(result);
//                        exportData.setParkingDurationInMillies(diffInMillies);
//                    }
//                    exportDataTemporary.add(exportData);
//                }
//                Comparator<ExportData> parkingDurationComparator = new Comparator<ExportData>() {
//                    @Override
//                    public int compare(ExportData o1, ExportData o2) {
//                        return Long.compare(o2.getParkingDurationInMillies(), o1.getParkingDurationInMillies());
//                    }
//                };
//                // 使用比较器对exportDataArrayList进行排序
//                Collections.sort(exportDataTemporary, parkingDurationComparator);
//                //遍历所有符合条件的数据
//                for (int i = (index + 2); i < (linShiReservationSize + index + 2); i++) {
//                    //所有的行数
//                    System.out.println("index = " + index);
//                    HSSFRow rowIndexTail = sheet.createRow(i);
//                    System.out.println("rowIndex = " + rowIndexTail);
//                    rowIndexTail.setHeight((short) 500);
//                    rowIndexTail.createCell(0).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(0).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(0).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndexTail.createCell(0).setCellValue(i - (index + 1));
//                    rowIndexTail.createCell(1).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(1).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(1).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndexTail.createCell(1).setCellValue(exportDataTemporary.get(i - (index + 2)).getCarNumber());
//                    rowIndexTail.createCell(2).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(2).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(2).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndexTail.createCell(2).setCellValue(exportDataTemporary.get(i - (index + 2)).getNotifier());
//                    rowIndexTail.createCell(3).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(3).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(3).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndexTail.createCell(3).setCellValue(exportDataTemporary.get(i - (index + 2)).getRemark());
//                    rowIndexTail.createCell(4).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(4).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(4).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    // 设置边框（一般标题不设置边框，是标题下的所有表格设置边框）
//                    if (exportDataTemporary.get(i - (index + 2)).getEnterTime() == null) {
//                        rowIndexTail.createCell(4).setCellValue("暂无入场时间");
//                    } else {
////                    Date parse = formatter1.parse(reportCarOutLinShiReservationList.get(i - (index + 2)).getEnterTime());
////                    System.out.println("入场时间" + formatter1.format(parse));
//                        rowIndexTail.createCell(4).setCellValue(exportDataTemporary.get(i - (index + 2)).getEnterTime());
//                    }
////                rowIndexTail.createCell(4).setCellValue(formatter1.parse(exportDataTemporary.get(i - (index + 2)).getEnterTime()));
//                    rowIndexTail.createCell(5).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(5).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(5).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    if (exportDataTemporary.get(i - (index + 2)).getLeaveTime() == null) {
//                        String res = endDate + "【未离场】";
//                        rowIndexTail.createCell(5).setCellValue(res);
//                    } else {
////                    System.out.println("离场时间" + formatter1.format(formatter1.parse(reportCarOutLinShiReservationList.get(i - (index + 2)).getLeaveTime())));
////                    Date parse = formatter1.parse(reportCarOutLinShiReservationList.get(i - (index + 2)).getLeaveTime());
////                    rowIndexTail.createCell(5).setCellValue(formatter1.format(parse));
//                        rowIndexTail.createCell(5).setCellValue(formatter1.format(formatter1.parse(exportDataTemporary.get(i - (index + 2)).getLeaveTime())));
//                    }
//                    rowIndexTail.createCell(6).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(6).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(6).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndexTail.createCell(6).setCellValue(exportDataTemporary.get(i - (index + 2)).getParkingDuration());
//                    rowIndexTail.createCell(7).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(7).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(7).setCellValue(" ");
//                    //TODO 备注查询当前车位之前的开通车牌[黑A98A98,黑AKC400]
//                    //TODO 例子：【车辆[黑A98A98]进场时间[2024-04-11 13:09:37] 离场时间[2024-04-16 13:23:56] 停车时长:[120小时14分钟19秒]】
//                    rowIndexTail.createCell(8).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(8).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(8).setCellValue(exportDataTemporary.get(i - (index + 2)).getChannelName());
//                    System.out.println("exportDataTemporary.channelName = " + exportDataTemporary.get(i - (index + 2)).getChannelName());
//                }
//            }
//            //格式化Date日期格式数据
//            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
//            //将startDate字符串转为时间格式
//            Date startDate1 = formatter.parse(startDate);
//            String format = formatter.format(startDate1);
//            String channelNameRes = "";
//            if (channelName.contains("万象上东")) {
//                channelNameRes = "万象上东";
//            } else if (channelName.contains("四季上东")) {
//                channelNameRes = "四季三期";
//            }
//            String fileName = channelNameRes + format + "放行记录";
//            System.out.println("fileName = " + fileName);
//            //设置中文文件名与后缀
//            response.setContentType("application/vnd,gpenxmlformats-officedocument.spreadsheetml.sheet; charset=utf-8");
//             response.setHeader("Content-Disposition", "attachment; filename="+ fileName + ".xlsx");
//            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8") + ".xlsx");
//            ServletOutputStream outputStream = response.getOutputStream();
//            // 输出
//            wb.write(outputStream);
//            // 6.关闭流
//            outputStream.flush();
//            outputStream.close();
//            System.out.println("万象数据表导出成功！");
//        } else {
//            // 导出其余的表
//            List<ReportCarOutReservation> reportCarOutReservationList = new ArrayList<>();
//            List<ReportCarOutReservation> reportCarOutLinShiReservationList = new ArrayList<>();
//            List<ReportCarInReservation> reportCarInReservationLists = reportCarInService.queryListReportOutExport(startDate, endDate, yardName);
//            List<ReportCarInReservation> reportCarInReservationList =
//                    reportCarInReservationLists.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(ReportCarInReservation::getParkingCode))), ArrayList::new));
//            System.out.println("reportCarInReservationList的长度" + reportCarInReservationList.size());
//            for (ReportCarInReservation reportCarInReservation : reportCarInReservationList) {
//                System.out.println("去重以后的数据reportCarOutReservation = " + reportCarInReservation);
//                // 对去重后的数据的车牌号码在离场表中进行查询，有的话取出离场时间，没有的话，离场时间为导表时间[未离场]
//                ReportCarOutReservation reportCarOutReservation = new ReportCarOutReservation();
//                reportCarOutReservation.setYardName(reportCarInReservation.getYardName());
//                reportCarOutReservation.setEnterCarLicenseNumber(reportCarInReservation.getEnterCarLicenseNumber());
//                reportCarOutReservation.setRemark(reportCarInReservation.getRemark());
//                reportCarOutReservation.setEnterChannelName(reportCarInReservation.getEnterChannelName());
//                reportCarOutReservation.setReleaseReason(reportCarInReservation.getReleaseReason());
//                reportCarOutReservation.setNotifierName(reportCarInReservation.getNotifierName());
//                reportCarOutReservation.setEnterTime(reportCarInReservation.getEnterTime());
//                // 查询离场表中的数据
//                // 判断当前车辆查询的离场表中的离场时间是否比当前导表时间要大
//                List<ReportCarOut> reportCarOut = reportCarOutService.selectLeaveTime(reportCarInReservation.getEnterCarLicenseNumber(), reportCarInReservation.getEnterTime(), startDate, endDate);
//                if (reportCarOut.size() == 0) {
//                    String res = endDate + "【未离场】";
//                    // reportCarOutReservation.setLeaveTime(res);
//                    System.out.println("车辆暂未离场" + leaveVIPTime);
//                } else {
//                    reportCarOutReservation.setLeaveTime(reportCarOut.get(0).getLeaveTime());
//                }
//                reportCarOutReservationList.add(reportCarOutReservation);
//            }
//            //TODO 修改相同车牌的通知人和放行原因问题
//            //查询所有的临时车
//            List<ReportCarInReservation> reportCarOutLinShiReservationLists = reportCarInService.queryListReportCarOutExportLinShi(startDate, endDate, yardName);
//            List<ReportCarInReservation> reportCarInLinShiReservationList =
//                    reportCarOutLinShiReservationLists.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(ReportCarInReservation::getParkingCode))), ArrayList::new));
//            System.out.println("reportCarInLinShiReservationList的长度 = " + reportCarInLinShiReservationList.size());
//            for (ReportCarInReservation reportCarInLinShiReservation : reportCarInLinShiReservationList) {
//                System.out.println("去重之后reportCarInLinShiReservationList = " + reportCarInLinShiReservation);
//                ReportCarOutReservation reportCarOutReservation = new ReportCarOutReservation();
//                reportCarOutReservation.setYardName(reportCarInLinShiReservation.getYardName());
//                reportCarOutReservation.setEnterCarLicenseNumber(reportCarInLinShiReservation.getEnterCarLicenseNumber());
//                reportCarOutReservation.setEnterChannelName(reportCarInLinShiReservation.getEnterChannelName());
//                reportCarOutReservation.setReleaseReason(reportCarInLinShiReservation.getReleaseReason());
//                reportCarOutReservation.setRemark(reportCarInLinShiReservation.getRemark());
//                reportCarOutReservation.setNotifierName(reportCarInLinShiReservation.getNotifierName());
//                reportCarOutReservation.setEnterTime(reportCarInLinShiReservation.getEnterTime());
//                // 查询离场表中的数据
//                // 判断当前车辆查询的离场表中的离场时间是否比当前导表时间要大
//                List<ReportCarOut> reportCarOut = reportCarOutService.selectLeaveTime(reportCarInLinShiReservation.getEnterCarLicenseNumber(), reportCarInLinShiReservation.getEnterTime(), startDate, endDate);
//                if (reportCarOut.size() == 0) {
//                    String res = endDate + "【未离场】";
//                    //  reportCarOutReservation.setLeaveTime(res);
//                    System.out.println("车辆暂未离场" + res);
//                } else {
//                    reportCarOutReservation.setLeaveTime(reportCarOut.get(0).getLeaveTime());
//                }
//                reportCarOutLinShiReservationList.add(reportCarOutReservation);
//            }
//            //创建HSSFWorkbook对象
//            HSSFWorkbook wb = new HSSFWorkbook();
//            //创建备注解释的字符集合属性
//            ArrayList<String> strings = new ArrayList<>();
//            //建立sheet对象
//            HSSFSheet sheet = wb.createSheet("放行记录");
//            // 设置列宽
//            // sheet.setColumnWidth(1,25);
//            sheet.setColumnWidth(0, 25 * 100);
//            sheet.setColumnWidth(1, 25 * 150);
//            sheet.setColumnWidth(2, 25 * 256);
//            sheet.setColumnWidth(3, 25 * 256);
//            sheet.setColumnWidth(4, 25 * 256);
//            sheet.setColumnWidth(5, 25 * 256);
//            sheet.setColumnWidth(6, 25 * 400);
//            sheet.setColumnWidth(7, 25 * 350);
//            sheet.setColumnWidth(8, 25 * 350);
//            //设置行高
//            // 记住一点设置单元格样式相关的都是CellStyle来控制的,设置完之后只需set给单元格即可：cell.setCellStyle(cellStyle);
//            // 合并单元格后居中
//            CellStyle cellStyle = wb.createCellStyle();
//            // 垂直居中
//            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
//            cellStyle.setAlignment(HorizontalAlignment.CENTER);
//            // 设置字体
//            Font font = wb.createFont();
//            font.setFontName("宋体");
//            font.setItalic(false);
//            font.setStrikeout(false);
//            cellStyle.setFont(font);
//            // 设置背景色
//            // cellStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
//            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//            // 设置边框（一般标题不设置边框，是标题下的所有表格设置边框）
//            cellStyle.setBorderBottom(BorderStyle.THIN); //下边框
//            cellStyle.setBorderLeft(BorderStyle.THIN);//左边框
//            cellStyle.setBorderTop(BorderStyle.THIN);//上边框
//            cellStyle.setBorderRight(BorderStyle.THIN);//右边框
//            //在sheet里创建第一行，参数为行索引
//            HSSFRow row1 = sheet.createRow(0);
//            // 合并单元格：参数1：行号 参数2：起始列号 参数3：行号 参数4：终止列号
//            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
//            // 创建单元格
//            HSSFCell cell = row1.createCell(0);
//            cell.setCellStyle(cellStyle);
//            //设置单元格内容
//            cell.setCellValue(yardName + "" + startDate + "" + "至" + " " + endDate + " 临停车辆进场日报表");
//            row1.setHeight((short) 1400);
//            // 创建单元格
//            //在sheet里创建第二行
//            HSSFRow row2 = sheet.createRow(1);
//            //创建单元格并设置单元格内容
//            //第二行设置字体为黑体加粗
//            // 合并单元格后居中
//            CellStyle cellStyle1 = wb.createCellStyle();
//            // 设置字体
//            Font font1 = wb.createFont();
//            font1.setFontName("黑体");
//            cellStyle1.setVerticalAlignment(VerticalAlignment.CENTER);
//            //设置加粗
//            font1.setBold(true);
//            font1.setFontHeightInPoints((short) 11);
//            //设置字体大小
//            font1.setItalic(false);
//            font1.setStrikeout(false);
//            cellStyle1.setFont(font1);
//            //定义进场车辆统计数量变量 (report_car_in的总条数,但是得统计时间在选择的时间这个区间)
//            int reportCarInNumber = reportCarInService.countByDate(startDate, endDate, yardName);
//            //定义本地VIP统计数量变量 (在上述条件的基础上，筛选统计enterVipType的名称是'本地VIP')
//            int reportCarInVIPNumber = reportCarInService.countByDateVIP(startDate, endDate, yardName);
//            //合并两列居中
//            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 1));
//            sheet.addMergedRegion(new CellRangeAddress(1, 1, 2, 3));
//            sheet.addMergedRegion(new CellRangeAddress(1, 1, 4, 5));
//            //TODO 统计进场车辆数量
//            row2.createCell(0).getCellStyle().setFont(font1);
//            row2.createCell(0).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//            row2.createCell(0).getCellStyle().setBorderBottom(BorderStyle.THIN); //下边框
//            row2.createCell(0).getCellStyle().setBorderLeft(BorderStyle.THIN);//左边框
//            row2.createCell(0).getCellStyle().setBorderTop(BorderStyle.THIN);//上边框
//            row2.createCell(0).getCellStyle().setBorderRight(BorderStyle.THIN);//右边框
//            row2.createCell(0).setCellValue("进场车辆数量：" + reportCarInNumber);
//            //TODO 统计本地VIP数量
//            row2.createCell(2).getCellStyle().setBorderBottom(BorderStyle.THIN); //下边框
//            row2.createCell(2).getCellStyle().setBorderLeft(BorderStyle.THIN);//左边框
//            row2.createCell(2).getCellStyle().setBorderTop(BorderStyle.THIN);//上边框
//            row2.createCell(2).getCellStyle().setBorderRight(BorderStyle.THIN);//右边
//            row2.createCell(2).getCellStyle().setFont(font1);
//            row2.createCell(2).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//            row2.createCell(2).setCellValue("本地VIP数量：" + reportCarInVIPNumber);
//            //TODO 统计通知放行数量
//            row2.createCell(4).getCellStyle().setBorderBottom(BorderStyle.THIN); //下边框
//            row2.createCell(4).getCellStyle().setBorderLeft(BorderStyle.THIN);//左边框
//            row2.createCell(4).getCellStyle().setBorderTop(BorderStyle.THIN);//上边框
//            row2.createCell(4).getCellStyle().setBorderRight(BorderStyle.THIN);//右边
//            row2.createCell(4).getCellStyle().setFont(font1);
//            row2.createCell(4).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//            row2.createCell(4).setCellValue("通知放行数量：" + (reportCarInReservationList.size() + reportCarInLinShiReservationList.size()));
//            //TODO 统计通知放行VIP超位数量及占比
//            row2.createCell(6).getCellStyle().setBorderBottom(BorderStyle.THIN); //下边框
//            row2.createCell(6).getCellStyle().setBorderLeft(BorderStyle.THIN);//左边框
//            row2.createCell(6).getCellStyle().setBorderTop(BorderStyle.THIN);//上边框
//            row2.createCell(6).getCellStyle().setBorderRight(BorderStyle.THIN);//右边
//            row2.createCell(6).getCellStyle().setFont(font1);
//            row2.createCell(6).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//            double percentage = (double) (reportCarInNumber - reportCarInVIPNumber) / reportCarInNumber;
//            DecimalFormat df = new DecimalFormat("#.00");
//            row2.createCell(6).setCellValue("通知放行VIP超位数量及占比：" + df.format(percentage * 100) + "%");
//            System.out.println("通知放行VIP超位数量及占比：" + df.format(percentage * 100) + "%");
//            //TODO 统计通知放行临时车数量及占比
//            row2.createCell(7).getCellStyle().setFont(font1);
//            row2.createCell(7).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//            double percentageLinShi = 1 - percentage;
//            DecimalFormat dfLinShi = new DecimalFormat("#.00");
//            System.out.println("通知放行临时车数量及占比" + dfLinShi.format(percentageLinShi * 100) + "%");
//            row2.createCell(7).setCellValue("通知放行临时车数量及占比：" + dfLinShi.format(percentageLinShi * 100) + "%");
//            row2.setHeight((short) 1000);
//            //设置第三行
//            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 8));
//            //在sheet里创建第三行
//            HSSFRow row3 = sheet.createRow(2);
//            row3.setHeight((short) 800);
//            row3.createCell(0).setCellValue("超位车辆放行统计");
//            //设置第4行的cellStyle
//            // 合并单元格后居中
//            CellStyle cellStyle2 = wb.createCellStyle();
//            // 垂直居中
//            //设置合并两列居中
//            cellStyle2.setVerticalAlignment(VerticalAlignment.CENTER);
//            cellStyle2.setAlignment(HorizontalAlignment.CENTER);
//            // 设置字体
//            Font font2 = wb.createFont();
//            font2.setFontName("等线");
//            font2.setFontHeightInPoints((short) 11);
//            font2.setItalic(false);
//            font2.setStrikeout(false);
//            cellStyle2.setFont(font2);
//            //第4行写入数据
//            HSSFRow row4 = sheet.createRow(3);
//            for (int i = 0; i < 9; i++) {
//                row4.createCell(i).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                row4.createCell(i).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                row4.createCell(i).getCellStyle().setBorderBottom(BorderStyle.THIN); //下边框
//                row4.createCell(i).getCellStyle().setBorderLeft(BorderStyle.THIN);//左边框
//                row4.createCell(i).getCellStyle().setBorderTop(BorderStyle.THIN);//上边框
//                row4.createCell(i).getCellStyle().setBorderRight(BorderStyle.THIN);//右边框
//            }
//            row4.createCell(0).setCellValue("序号");
//            row4.createCell(1).setCellValue("车牌号码");
//            row4.createCell(2).setCellValue("通知人");
//            row4.createCell(3).setCellValue("放行原因");
//            row4.createCell(4).setCellValue("进场时间");
//            row4.createCell(5).setCellValue("离场时间");
//            row4.createCell(6).setCellValue("停车时长");
//            row4.createCell(7).setCellValue("备注");
//            row4.createCell(8).setCellValue("入场通道");
//            row4.setHeight((short) 500);
//            int size = reportCarOutReservationList.size();
//            DateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            //格式化Date数据
//            DateFormat formatter2 = new SimpleDateFormat("HH小时mm分钟ss秒");
//            //定义记录当前行数的index
//            int index = 0;
//            String dynamicCarportNumber = "";
//            // 定义超位VIP车辆的时间差
//            String timesVIP = "";
//            // 定义超位临时车车辆的时间差
//            String timesLinShi = "";
//            // 输出的时分秒格式的字符串
//            System.out.println("size = " + size);
//            if (size == 0) {
//                System.out.println("查询为空");
//            } else {
//                ArrayList<ExportData> exportDataArrayList = new ArrayList<>();
//                //首先遍历存储到集合中进行排序
//                for (int j1 = 4; j1 < size + 4; j1++) {
//                    ExportData exportData = new ExportData();
//                    exportData.setRemark(reportCarOutReservationList.get(j1 - 4).getRemark());
//                    exportData.setCarNumber(reportCarOutReservationList.get(j1 - 4).getEnterCarLicenseNumber());
//                    exportData.setNotifier(reportCarOutReservationList.get(j1 - 4).getNotifierName());
//                    exportData.setEnterTime(formatter1.format(formatter1.parse(reportCarOutReservationList.get(j1 - 4).getEnterTime())));
//                    exportData.setChannelName(reportCarOutReservationList.get(j1 - 4).getEnterChannelName());
//                    //做时间差
//                    if (reportCarOutReservationList.get(j1 - 4).getEnterTime() == null) {
//                        System.out.println("j1 = " + j1);
//                    } else if (reportCarOutReservationList.get(j1 - 4).getLeaveTime() == null) {
//                        //  rowIndex.createCell(6).setCellValue("暂无时间差");
//                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                        Date enterTime = dateFormat.parse(reportCarOutReservationList.get(j1 - 4).getEnterTime());
//                        Date leaveTime = dateFormat.parse(endDate);
//                        long diffInMillies = Math.abs(leaveTime.getTime() - enterTime.getTime());
//                        long diffHours = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
//                        long diffMinutes = TimeUnit.MINUTES.convert(diffInMillies - TimeUnit.HOURS.toMillis(diffHours), TimeUnit.MILLISECONDS);
//                        long diffSeconds = TimeUnit.SECONDS.convert(diffInMillies - TimeUnit.HOURS.toMillis(diffHours) - TimeUnit.MINUTES.toMillis(diffMinutes), TimeUnit.MILLISECONDS);
//                        System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                        String result = "";
//                        if (diffHours >= 24) {
//                            long days = diffHours / 24;
//                            diffHours %= 24;
//                            result = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                        } else {
//                            if (diffHours == 0) {
//                                result = diffMinutes + "分钟" + diffSeconds + "秒";
//                            } else {
//                                result = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                            }
//                        }
//                        exportData.setParkingDuration(result);
//                        exportData.setParkingDurationInMillies(diffInMillies);
//                    } else {
//                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                        Date enterTime = dateFormat.parse(reportCarOutReservationList.get(j1 - 4).getEnterTime());
//                        Date leaveTime = dateFormat.parse(reportCarOutReservationList.get(j1 - 4).getLeaveTime());
//                        exportData.setLeaveTime(formatter1.format(formatter1.parse(reportCarOutReservationList.get(j1 - 4).getLeaveTime())));
//                        long diffInMillies = Math.abs(leaveTime.getTime() - enterTime.getTime());
//                        long diffHours = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
//                        long diffMinutes = TimeUnit.MINUTES.convert(diffInMillies - TimeUnit.HOURS.toMillis(diffHours), TimeUnit.MILLISECONDS);
//                        long diffSeconds = TimeUnit.SECONDS.convert(diffInMillies - TimeUnit.HOURS.toMillis(diffHours) - TimeUnit.MINUTES.toMillis(diffMinutes), TimeUnit.MILLISECONDS);
//                        System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                        String result = "";
//                        if (diffHours >= 24) {
//                            long days = diffHours / 24;
//                            diffHours %= 24;
//                            result = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                        } else {
//                            if (diffHours == 0) {
//                                result = diffMinutes + "分钟" + diffSeconds + "秒";
//                            } else {
//                                result = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                            }
//                        }
//                        exportData.setParkingDuration(result);
//                        exportData.setParkingDurationInMillies(diffInMillies);
//                    }
//                    exportDataArrayList.add(exportData);
//                }
//                Comparator<ExportData> parkingDurationComparator = new Comparator<ExportData>() {
//                    @Override
//                    public int compare(ExportData o1, ExportData o2) {
//                        return Long.compare(o2.getParkingDurationInMillies(), o1.getParkingDurationInMillies());
//                    }
//                };
//                // 使用比较器对exportDataArrayList进行排序
//                Collections.sort(exportDataArrayList, parkingDurationComparator);
//                //遍历所有符合条件的数据
//                for (int i = 4; i < size + 4; i++) {
//                    //所有的行数
//                    HSSFRow rowIndex = sheet.createRow(i);
//                    rowIndex.setHeight((short) 500);
//                    rowIndex.createCell(0).getCellStyle().setFont(font2);
//                    rowIndex.createCell(0).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndex.createCell(0).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndex.createCell(0).setCellValue(i - 3);
//                    rowIndex.createCell(1).getCellStyle().setFont(font2);
//                    rowIndex.createCell(1).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndex.createCell(1).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndex.createCell(1).setCellValue(exportDataArrayList.get(i - 4).getCarNumber());
//                    rowIndex.createCell(2).getCellStyle().setFont(font2);
//                    rowIndex.createCell(2).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndex.createCell(2).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndex.createCell(2).setCellValue(exportDataArrayList.get(i - 4).getNotifier());
//                    rowIndex.createCell(3).getCellStyle().setFont(font2);
//                    rowIndex.createCell(3).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndex.createCell(3).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndex.createCell(3).setCellValue(exportDataArrayList.get(i - 4).getRemark());
//                    rowIndex.createCell(4).getCellStyle().setFont(font2);
//                    rowIndex.createCell(4).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndex.createCell(4).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    // 设置边框（一般标题不设置边框，是标题下的所有表格设置边框）
//                    if (reportCarOutReservationList.get(i - 4).getEnterTime() == null) {
//                        rowIndex.createCell(4).setCellValue("暂无入场时间");
//                    } else {
////                    System.out.println("VIP入场时间" + formatter1.format(formatter1.parse(reportCarOutReservationList.get(i - 4).getEnterTime())));
////                    Date parse = formatter1.parse(reportCarOutReservationList.get(i - 4).getEnterTime());
////                    rowIndex.createCell(4).setCellValue(formatter1.format((parse)));
//                        rowIndex.createCell(4).setCellValue(exportDataArrayList.get(i - 4).getEnterTime());
//                    }
//                    rowIndex.createCell(5).getCellStyle().setFont(font2);
//                    rowIndex.createCell(5).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndex.createCell(5).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    if (exportDataArrayList.get(i - 4).getLeaveTime() == null) {
//                        String res = endDate + "【未离场】";
//                        rowIndex.createCell(5).setCellValue(res);
//                    } else {
////                    Date parse = formatter1.parse(reportCarOutReservationList.get(i - 4).getLeaveTime());
////                    System.out.println("VIP离场时间" + formatter1.format(parse));
////                    rowIndex.createCell(5).setCellValue(formatter1.format(parse));
//                        rowIndex.createCell(5).setCellValue(exportDataArrayList.get(i - 4).getLeaveTime());
//                    }
//                    rowIndex.createCell(6).setCellValue(exportDataArrayList.get(i - 4).getParkingDuration());
//                    // TODO 备注查询当前车位之前的开通车牌[黑A98A98,黑AKC400]
//                    // TODO 例子：【车辆[黑A98A98]进场时间[2024-04-11 13:09:37] 离场时间[2024-04-16 13:23:56] 停车时长:[120小时14分钟19秒]】
//                    // 根据当前车辆的车牌号码查询出月票中剩余车辆的进出场时间
//                    // 调用车牌号码查询月票id，先查询出除了当前车辆以外的其余车辆
//                    // TODO 查询当前车辆是否为本地VIP超位车辆
//                    int pageNum = 1;
//                    int pageSize = 1000;
//                    // 将获取到的值存储到数组集合中，例如：
//                    ArrayList<String> values = new ArrayList<>();
//                    HashMap<String, String> hashMapPre = new HashMap<>();
//                    hashMapPre.put("carCode", exportDataArrayList.get(i - 4).getCarNumber());
//                    hashMapPre.put("pageNum", String.valueOf(pageNum));
//                    hashMapPre.put("pageSize", String.valueOf(pageSize));
//                    String getPre = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/vehicleReservation/getOnlineMonthTicketByCarCard", hashMapPre);
//                    JSONObject jsonObjectPre = JSONObject.parseObject(getPre);
//                    JSONObject jsonObjectDataPre = (JSONObject) jsonObjectPre.get("data");
//                    JSONObject jsonObjectDataDataPre = (JSONObject) jsonObjectDataPre.get("data");
//                    JSONArray monthTicketList = jsonObjectDataDataPre.getJSONArray("monthTicketList");
//                    // 将monthTicketList赋值给Arraylist
//                    ArrayList<MonthTick> monthTicks = new ArrayList<>();
//                    // 将  JSONArray monthTicketList 赋值给 monthTicks
//                    for (int j = 0; j < monthTicketList.size(); j++) {
//                        MonthTick monthTick = new MonthTick();
//                        JSONObject jsonObjectData = (JSONObject) monthTicketList.get(j);
//                        monthTick.setCarCode(jsonObjectData.getString("carCode"));
//                        monthTick.setTimeperiodList(jsonObjectData.getString("timeperiodList"));
//                        monthTick.setDynamicCarportNumber(jsonObjectData.getInteger("dynamicCarportNumber"));
//                        monthTicks.add(monthTick);
//                    }
//                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
//                    SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                    Date currentTime = sdf.parse(sdf.format(new Date()));
//                    //  找到当前生效的{}
//                    String VIPCarCodes = "";
//                    System.out.println("monthTicketList = " + monthTicketList.size());
//                    // 判断若monthTicketList的长度为1，则证明肯定是，找出来他的开通车牌；
//                    if (monthTicketList.size() == 1) {
//                        VIPCarCodes = monthTicketList.getJSONObject(0).getString("carCode");
//                        System.out.println("车牌号为：" + VIPCarCodes);
//                    } else {
//                        //遍历monthTicks每个值
//                        for (int i1 = 0; i1 < monthTicks.size(); i1++) {
//                            //查询所有的monTick中的getCarCodes是否符合条件
//                            String timeperiodList = monthTicks.get(i1).getTimeperiodList();
//                            if (!timeperiodList.contains(",")) {
//                                System.out.println("timeperiodList = " + timeperiodList);
//                                String[] startEnd = timeperiodList.split("\\*");
//                                System.out.println("startEnd的长度：" + startEnd.length);
//                                for (String s : startEnd) {
//                                    System.out.println("s = " + s);
//                                }
//                                if (startEnd.length == 2) {
//                                    Date startTime = sdf.parse(startEnd[0]);
//                                    Date endTime = sdf.parse(startEnd[1]);
//                                    if (currentTime.after(startTime) && currentTime.before(endTime)) {
//                                        VIPCarCodes = monthTicks.get(i1).getCarCode();
//                                        dynamicCarportNumber = String.valueOf(monthTicks.get(i1).getDynamicCarportNumber());
//                                        System.out.println("没有逗号的" + VIPCarCodes);
//                                    }
//                                } else {
//                                    System.out.println("没有startEnd");
//                                }
//                            } else {
//                                String[] timeRanges = timeperiodList.split(",");
//                                for (String timeRange : timeRanges) {
//                                    String[] startEnd = timeRange.split("\\*");
//                                    String start = startEnd[0];
//                                    long l = Long.parseLong(start);
//                                    Date startTime = sdf.parse(start);
//                                    // 格式化时间格式startTime Fri Apr 01 00:00:00 CST 2022 格式化为 ""
//                                    Date endTime = sdf.parse(startEnd[1]);
//                                    if (currentTime.after(startTime) && currentTime.before(endTime)) {
//                                        VIPCarCodes = monthTicks.get(i1).getCarCode();
//                                        dynamicCarportNumber = String.valueOf(monthTicks.get(i1).getDynamicCarportNumber());
//                                        System.out.println("有逗号的 = " + VIPCarCodes);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    System.out.println("VIPCarCodes = " + VIPCarCodes);
//                    String[] VIPCarCodesArr = VIPCarCodes.split(",");
//                    // 将VIPCarCodesArr中的值去除和当前车牌相同的车牌号
//                    ArrayList<String> VIPCarCodesList = new ArrayList<>();
//                    if (VIPCarCodesArr.length == 1) {
//                        System.out.println("月票车只开通了一个车牌" + VIPCarCodesArr);
//                        System.out.println("不是超位车辆" + VIPCarCodesList);
//                    } else {
//                        String carNo = exportDataArrayList.get(i - 4).getCarNumber();
//                        for (int j = 0; j < VIPCarCodesArr.length; j++) {
//                            if (!VIPCarCodesArr[j].equals(carNo)) {
//                                System.out.println("开通车牌号码为：" + VIPCarCodesArr[j]);
//                                VIPCarCodesList.add(VIPCarCodesArr[j]);
//                            }
//                        }
//                    }
//                    strings.clear();
//                    strings.add("开通车牌号码为：" + Arrays.toString(VIPCarCodesArr) + "\n");
//                    //去除当前车牌号的其余车牌号码，遍历VIPCarCodesList
//                    String enterCarLicenseNumber = exportDataArrayList.get(i - 4).getCarNumber();
//                    for (String carCode : VIPCarCodesList) {
//                        System.out.println("去除当前车牌号的其余车牌号码:" + carCode);
//                        // 查询当前车牌号码的进出场记录
//                        HashMap<String, String> hashMapDetail = new HashMap<>();
//                        // 调用yard_info根据传入的车场名称查询车场编号
//                        hashMapDetail.put("parkCode", CarInParkCode);
//                        hashMapDetail.put("carCode", carCode);
//                        String getDetail = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/vehicleReservation/getParkDetail", hashMapDetail);
//                        JSONObject jsonObjectDetail = JSONObject.parseObject(getDetail);
//                        JSONObject jsonObjectDatagetDetail = (JSONObject) jsonObjectDetail.get("data");
//                        Integer resultCode = (Integer) jsonObjectDatagetDetail.get("resultCode");
//                        System.out.println("resultCode = " + resultCode);
//                        if (resultCode == 908) {
//                            String str = "【车辆[" + carCode + "暂无停车信息]】";
//                        } else if (resultCode == -1) {
//                            String str = "【车辆[" + carCode + "查询失败！]】";
//                        } else {
//                            JSONObject jsonObjectDataDataDetail = (JSONObject) jsonObjectDatagetDetail.get("data");
//                            String enterDate = (String) jsonObjectDataDataDetail.get("enterDate");
//                            String outDate = (String) jsonObjectDataDataDetail.get("outDate");
//                            System.out.println(carCode + "jsonObjectDataDataDetail = " + jsonObjectDataDataDetail);
//                            System.out.println(carCode + "enterDate = " + enterDate);
//                            System.out.println(carCode + "outDate = " + outDate);
//                            //判断进场时间是否在当前车辆的进场时间要早，早的话写进去否则不写
//                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
//                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                            Date startVIPDate = formatter.parse(exportDataArrayList.get(i - 4).getEnterTime());
//                            Date endVIPDate = formatter.parse(endDate);
//                            Date enterVIPDate = dateFormat.parse(enterDate);
//                            String localEnterTime = formatter.format(enterVIPDate);
//                            if (outDate == null) {
//                                if (formatter.parse(localEnterTime).before(startVIPDate)) {
//                                    long diffInMillies = Math.abs(endVIPDate.getTime() - enterVIPDate.getTime());
//                                    long diffHours = diffInMillies / (60 * 60 * 1000);
//                                    long diffMinutes = (diffInMillies / (60 * 1000)) % 60;
//                                    long diffSeconds = (diffInMillies / 1000) % 60;
//                                    String resultParam = "";
//                                    if (diffHours > 24) {
//                                        long days = diffHours / 24;
//                                        diffHours %= 24;
//                                        resultParam = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                    } else {
//                                        if (diffHours == 0) {
//                                            resultParam = diffMinutes + "分钟" + diffSeconds + "秒";
//                                        } else {
//                                            resultParam = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                        }
//                                    }
//                                    System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                                    Date enterDateParam = dateFormat.parse(enterDate);
//                                    String parse = formatter.format(enterDateParam);
//                                    System.out.println("enterDateParam = " + enterDateParam.toString());
//                                    System.out.println("parse = " + parse);
//                                    String str = "【车辆[" + carCode + "]进场时间[" + parse + "] 离场时间[" + endDate + "] 停车时长:[ " + resultParam + "]{未离场}】";
//                                    strings.add(str);
//                                } else {
//                                    // 此车可能不是，查询此车的进场记录
//                                    System.out.println("超位车辆的进场时间在当前车辆以后进入！！");
//                                    // TODO 查询本地数据库中此车的进出场记录
//                                    List<ReportCarIn> reportCarIns = reportCarInService.selectCarRecords(carCode, exportDataArrayList.get(i - 4).getEnterTime());
//                                    List<ReportCarOut> reportCarOuts = reportCarOutService.selectCarRecords(carCode, reportCarIns.get(0).getEnterTime());
//                                    if (reportCarIns.get(0).getEnterTime() != null && reportCarOuts.size() != 0) {
//                                        Date outEnterTimeParam = formatter.parse(reportCarIns.get(0).getEnterTime());
//                                        Date outLeaveTimeParam = formatter.parse(reportCarOuts.get(0).getLeaveTime());
//                                        // 判断reportCarOut.getLeaveTime()和enterCarLicenseNumber的大小
//                                        // 若reportCarIns有数据，reportCarOuts有数据，已离场
//                                        if (outEnterTimeParam.before(startVIPDate) && outLeaveTimeParam.after(startVIPDate)) {
//                                            long diffInMillies = Math.abs(outLeaveTimeParam.getTime() - outEnterTimeParam.getTime());
//                                            long diffHours = diffInMillies / (60 * 60 * 1000);
//                                            long diffMinutes = (diffInMillies / (60 * 1000)) % 60;
//                                            long diffSeconds = (diffInMillies / 1000) % 60;
//                                            String resultParam = "";
//                                            if (diffHours > 24) {
//                                                long days = diffHours / 24;
//                                                diffHours %= 24;
//                                                resultParam = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                            } else {
//                                                if (diffHours == 0) {
//                                                    resultParam = diffMinutes + "分钟" + diffSeconds + "秒";
//                                                } else {
//                                                    resultParam = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                                }
//                                            }
//                                            System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                                            //  Date enterDateParam = dateFormat.parse(enterDate);
//                                            String parse = formatter.format(outEnterTimeParam);
//                                            String parse1 = formatter.format(outLeaveTimeParam);
//                                            String str = "【车辆[" + carCode + "]进场时间[" + parse + "] 离场时间[" + parse1 + "] 停车时长:[ " + resultParam + "]】";
//                                            strings.add(str);
//                                        } else {
//                                            System.out.println(carCode + "时间不匹配！");
//                                        }
//                                    } else {
//                                        System.out.println(carCode + "空！");
//                                    }
//                                }
//                            } else {
//                                Date outVIPDate = dateFormat.parse(outDate);
//                                String localOutTime = formatter.format(outVIPDate);
//                                // 判断当前数据的进场时间是否在被占位车辆的进场时间之前：进场时间之前，离场时间之后的数据
//                                Date parse3 = formatter.parse(exportDataArrayList.get(i - 4).getEnterTime());
//                                if (formatter.parse(localEnterTime).before(parse3) && formatter.parse(localOutTime).after(parse3)) {
//                                    Date outLeaveTimeParam = formatter.parse(localOutTime);
//                                    Date outEnterTimeParam = formatter.parse(localEnterTime);
//                                    long diffInMillies = Math.abs(outLeaveTimeParam.getTime() - outEnterTimeParam.getTime());
//                                    long diffHours = diffInMillies / (60 * 60 * 1000);
//                                    long diffMinutes = (diffInMillies / (60 * 1000)) % 60;
//                                    long diffSeconds = (diffInMillies / 1000) % 60;
//                                    String resultParam = "";
//                                    if (diffHours > 24) {
//                                        long days = diffHours / 24;
//                                        diffHours %= 24;
//                                        resultParam = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                    } else {
//                                        if (diffHours == 0) {
//                                            resultParam = diffMinutes + "分钟" + diffSeconds + "秒";
//                                        } else {
//                                            resultParam = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                        }
//                                    }
//                                    System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                                    String parse = formatter.format(outEnterTimeParam);
//                                    String parse1 = formatter.format(outLeaveTimeParam);
//                                    String str = "【车辆[" + carCode + "]进场时间[" + parse + "] 离场时间[" + parse1 + "] 停车时长:[ " + resultParam + "]】";
//                                    strings.add(str);
//                                } else {
//                                    // 查询本地数据库
//                                    // 此车可能不是，查询此车的进场记录
//                                    System.out.println("超位车辆的进场时间在当前车辆以后进入！！");
//                                    // 查询本地数据库中此车的进出场记录
//                                    List<ReportCarIn> reportCarIns = reportCarInService.selectCarRecords(carCode, exportDataArrayList.get(i - 4).getEnterTime());
//                                    if (reportCarIns.size() != 0) {
//                                        List<ReportCarOut> reportCarOuts = reportCarOutService.selectCarRecords(carCode, reportCarIns.get(0).getEnterTime());
//                                        // 判断reportCarOut.getLeaveTime()和enterCarLicenseNumber的大小
//                                        Date outEnterTimeParam = formatter.parse(reportCarIns.get(0).getEnterTime());
//                                        // 若reportCarIns有数据，reportCarOuts有数据，已离场
//                                        if (reportCarOuts.size() != 0) {
//                                            Date outLeaveTimeParam = formatter.parse(reportCarOuts.get(0).getLeaveTime());
//                                            if (outEnterTimeParam.before(startVIPDate) && outLeaveTimeParam.after(startVIPDate)) {
//                                                long diffInMillies = Math.abs(outLeaveTimeParam.getTime() - outEnterTimeParam.getTime());
//                                                long diffHours = diffInMillies / (60 * 60 * 1000);
//                                                long diffMinutes = (diffInMillies / (60 * 1000)) % 60;
//                                                long diffSeconds = (diffInMillies / 1000) % 60;
//                                                String resultParam = "";
//                                                if (diffHours > 24) {
//                                                    long days = diffHours / 24;
//                                                    diffHours %= 24;
//                                                    resultParam = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                                } else {
//                                                    if (diffHours == 0) {
//                                                        resultParam = diffMinutes + "分钟" + diffSeconds + "秒";
//                                                    } else {
//                                                        resultParam = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                                    }
//                                                }
//                                                System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                                                String parse = formatter.format(outEnterTimeParam);
//                                                String parse1 = formatter.format(outLeaveTimeParam);
//                                                String str = "【车辆[" + carCode + "]进场时间[" + parse + "] 离场时间[" + parse1 + "] 停车时长:[ " + resultParam + "]】";
//                                                strings.add(str);
//                                            } else {
//                                                System.out.println(carCode + "不满足!");
//                                            }
//                                        } else {
//                                            if (outEnterTimeParam.before(startVIPDate)) {
//                                                // 若reportCarIns有数据，reportCarOuts无数据，未离场
//                                                long diffInMillies = Math.abs(endVIPDate.getTime() - outEnterTimeParam.getTime());
//                                                long diffHours = diffInMillies / (60 * 60 * 1000);
//                                                long diffMinutes = (diffInMillies / (60 * 1000)) % 60;
//                                                long diffSeconds = (diffInMillies / 1000) % 60;
//                                                String resultParam = "";
//                                                if (diffHours > 24) {
//                                                    long days = diffHours / 24;
//                                                    diffHours %= 24;
//                                                    resultParam = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                                } else {
//                                                    if (diffHours == 0) {
//                                                        resultParam = diffMinutes + "分钟" + diffSeconds + "秒";
//                                                    } else {
//                                                        resultParam = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                                                    }
//                                                }
//                                                System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                                                String parse = formatter.format(outEnterTimeParam);
//                                                String str = "【车辆[" + carCode + "]进场时间[" + parse + "] 离场时间[" + endDate + "] 停车时长:[ " + resultParam + "]{未离场}】";
//                                                strings.add(str);
//                                            } else {
//                                                System.out.println("outEnterTimeParam = " + outEnterTimeParam);
//                                            }
//
//                                        }
//                                    }
//                                    System.out.println("当前非占位车辆！");
//
//                                }
//                            }
//                        }
//                    }
//                    rowIndex.createCell(7).getCellStyle().setFont(font2);
//                    rowIndex.createCell(7).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndex.createCell(7).setCellValue(strings.toString());
//                    rowIndex.createCell(8).getCellStyle().setFont(font2);
//                    rowIndex.createCell(8).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndex.createCell(8).setCellValue(exportDataArrayList.get(i - 4).getChannelName());
//                }
//            }
//            // 输出集合字符串
//            for (int i = 0; i < strings.size(); i++) {
//                System.out.println("各个字符串是：" + strings.get(i));
//            }
//            int linShiReservationSize = reportCarOutLinShiReservationList.size();
//            index = size + 4;
//            System.out.println("当前行数为：" + index);
//            //设置第三行
//            sheet.addMergedRegion(new CellRangeAddress(index, index, 0, 8));
//            //撰写临时车辆放行统计
//            HSSFRow rowTail = sheet.createRow(index);
//            rowTail.setHeight((short) 800);
//            rowTail.createCell(0).setCellValue("临时车辆放行统计");
//            HSSFRow rowTailTitle = sheet.createRow(index + 1);
//            for (int i = 0; i < 9; i++) {
//                rowTailTitle.createCell(i).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                rowTailTitle.createCell(i).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                rowTailTitle.createCell(i).getCellStyle().setBorderBottom(BorderStyle.THIN); //下边框
//                rowTailTitle.createCell(i).getCellStyle().setBorderLeft(BorderStyle.THIN);//左边框
//                rowTailTitle.createCell(i).getCellStyle().setBorderTop(BorderStyle.THIN);//上边框
//                rowTailTitle.createCell(i).getCellStyle().setBorderRight(BorderStyle.THIN);//右边框
//            }
//            rowTailTitle.createCell(0).setCellValue("序号");
//            rowTailTitle.createCell(1).setCellValue("车牌号码");
//            rowTailTitle.createCell(2).setCellValue("通知人");
//            rowTailTitle.createCell(3).setCellValue("放行原因");
//            rowTailTitle.createCell(4).setCellValue("进场时间");
//            rowTailTitle.createCell(5).setCellValue("离场时间");
//            rowTailTitle.createCell(6).setCellValue("停车时长");
//            rowTailTitle.createCell(7).setCellValue("备注");
//            rowTailTitle.createCell(8).setCellValue("入场通道");
//            rowTailTitle.setHeight((short) 500);
//            // 输出的时分秒格式的字符串
//            if (linShiReservationSize == 0) {
//                System.out.println("查询为空");
//            } else {
//                ArrayList<ExportData> exportDataTemporary = new ArrayList<>();
//                for (int i2 = (index + 2); i2 < (linShiReservationSize + index + 2); i2++) {
//                    ExportData exportData = new ExportData();
//                    exportData.setRemark(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getRemark());
//                    exportData.setCarNumber(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getEnterCarLicenseNumber());
//                    exportData.setNotifier(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getNotifierName());
//                    exportData.setEnterTime(formatter1.format(formatter1.parse(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getEnterTime())));
//                    exportData.setChannelName(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getEnterChannelName());
//                    //做时间差
//                    if (reportCarOutLinShiReservationList.get(i2 - (index + 2)).getEnterTime() == null) {
//                        System.out.println("i2 = " + i2);
//                    } else if (reportCarOutLinShiReservationList.get(i2 - (index + 2)).getLeaveTime() == null) {
////                    rowIndex.createCell(6).setCellValue("暂无时间差");
//                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                        Date enterTime = dateFormat.parse(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getEnterTime());
//                        Date leaveTime = dateFormat.parse(endDate);
//                        long diffInMillies = Math.abs(leaveTime.getTime() - enterTime.getTime());
//                        long diffHours = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
//                        long diffMinutes = TimeUnit.MINUTES.convert(diffInMillies - TimeUnit.HOURS.toMillis(diffHours), TimeUnit.MILLISECONDS);
//                        long diffSeconds = TimeUnit.SECONDS.convert(diffInMillies - TimeUnit.HOURS.toMillis(diffHours) - TimeUnit.MINUTES.toMillis(diffMinutes), TimeUnit.MILLISECONDS);
//                        System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                        String result = "";
//                        if (diffHours >= 24) {
//                            long days = diffHours / 24;
//                            diffHours %= 24;
//                            result = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                        } else {
//                            if (diffHours == 0) {
//                                result = diffMinutes + "分钟" + diffSeconds + "秒";
//                            } else {
//                                result = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                            }
//                        }
//                        exportData.setParkingDuration(result);
//                        exportData.setParkingDurationInMillies(diffInMillies);
//                    } else {
//                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                        Date enterTime = dateFormat.parse(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getEnterTime());
//                        Date leaveTime = dateFormat.parse(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getLeaveTime());
//                        exportData.setLeaveTime(formatter1.format(formatter1.parse(reportCarOutLinShiReservationList.get(i2 - (index + 2)).getLeaveTime())));
//                        long diffInMillies = Math.abs(leaveTime.getTime() - enterTime.getTime());
//                        long diffHours = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
//                        long diffMinutes = TimeUnit.MINUTES.convert(diffInMillies - TimeUnit.HOURS.toMillis(diffHours), TimeUnit.MILLISECONDS);
//                        long diffSeconds = TimeUnit.SECONDS.convert(diffInMillies - TimeUnit.HOURS.toMillis(diffHours) - TimeUnit.MINUTES.toMillis(diffMinutes), TimeUnit.MILLISECONDS);
//                        System.out.println(diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒");
//                        String result = "";
//                        if (diffHours >= 24) {
//                            long days = diffHours / 24;
//                            diffHours %= 24;
//                            result = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                        } else {
//                            if (diffHours == 0) {
//                                result = diffMinutes + "分钟" + diffSeconds + "秒";
//                            } else {
//                                result = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
//                            }
//                        }
//                        exportData.setParkingDuration(result);
//                        exportData.setParkingDurationInMillies(diffInMillies);
//                    }
//                    exportDataTemporary.add(exportData);
//                }
//                Comparator<ExportData> parkingDurationComparator = new Comparator<ExportData>() {
//                    @Override
//                    public int compare(ExportData o1, ExportData o2) {
//                        return Long.compare(o2.getParkingDurationInMillies(), o1.getParkingDurationInMillies());
//                    }
//                };
//                // 使用比较器对exportDataArrayList进行排序
//                Collections.sort(exportDataTemporary, parkingDurationComparator);
//                //遍历所有符合条件的数据
//                for (int i = (index + 2); i < (linShiReservationSize + index + 2); i++) {
//                    //所有的行数
//                    System.out.println("index = " + index);
//                    HSSFRow rowIndexTail = sheet.createRow(i);
//                    System.out.println("rowIndex = " + rowIndexTail);
//                    rowIndexTail.setHeight((short) 500);
//                    rowIndexTail.createCell(0).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(0).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(0).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndexTail.createCell(0).setCellValue(i - (index + 1));
//                    rowIndexTail.createCell(1).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(1).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(1).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndexTail.createCell(1).setCellValue(exportDataTemporary.get(i - (index + 2)).getCarNumber());
//                    rowIndexTail.createCell(2).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(2).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(2).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndexTail.createCell(2).setCellValue(exportDataTemporary.get(i - (index + 2)).getNotifier());
//                    rowIndexTail.createCell(3).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(3).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(3).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndexTail.createCell(3).setCellValue(exportDataTemporary.get(i - (index + 2)).getRemark());
//                    rowIndexTail.createCell(4).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(4).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(4).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    // 设置边框（一般标题不设置边框，是标题下的所有表格设置边框）
//                    if (exportDataTemporary.get(i - (index + 2)).getEnterTime() == null) {
//                        rowIndexTail.createCell(4).setCellValue("暂无入场时间");
//                    } else {
//                        rowIndexTail.createCell(4).setCellValue(exportDataTemporary.get(i - (index + 2)).getEnterTime());
//                    }
//                    rowIndexTail.createCell(5).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(5).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(5).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    if (exportDataTemporary.get(i - (index + 2)).getLeaveTime() == null) {
//                        String res = endDate + "【未离场】";
//                        rowIndexTail.createCell(5).setCellValue(res);
//                    } else {
//                        rowIndexTail.createCell(5).setCellValue(formatter1.format(formatter1.parse(exportDataTemporary.get(i - (index + 2)).getLeaveTime())));
//                    }
//                    rowIndexTail.createCell(6).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(6).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(6).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
//                    rowIndexTail.createCell(6).setCellValue(exportDataTemporary.get(i - (index + 2)).getParkingDuration());
//                    rowIndexTail.createCell(7).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(7).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(7).setCellValue(" ");
//                    //TODO 备注查询当前车位之前的开通车牌[黑A98A98,黑AKC400]
//                    //TODO 例子：【车辆[黑A98A98]进场时间[2024-04-11 13:09:37] 离场时间[2024-04-16 13:23:56] 停车时长:[120小时14分钟19秒]】
//                    rowIndexTail.createCell(8).getCellStyle().setFont(font2);
//                    rowIndexTail.createCell(8).getCellStyle().setVerticalAlignment(VerticalAlignment.CENTER);
//                    rowIndexTail.createCell(8).setCellValue(exportDataTemporary.get(i - (index + 2)).getChannelName());
//                }
//            }

            //格式化Date日期格式数据
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            //将startDate字符串转为时间格式
            Date startDate1 = formatter.parse(startDate);
            String format = formatter.format(startDate1);
            // 设置响应头信息
            System.out.println("yardName = " + yardName);
            String fileName = yardName + format + "放行记录";
            System.out.println("fileName = " + fileName);
            //设置中文文件名与后缀
            response.setContentType("application/vnd,gpenxmlformats-officedocument.spreadsheetml.sheet; charset=utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8") + ".xlsx");
            ServletOutputStream outputStream = response.getOutputStream();
            // 输出
//            wb.write(outputStream);
            // 6.关闭流
            outputStream.flush();
            outputStream.close();
            System.out.println("数据表导出成功！");
        }
//    }

    @Override
    public List<VehicleReservation> queryListVehicleReservationExport(String carNo, String startDate, String endDate, String yardName) {
        return baseMapper.queryListVehicleReservationExport(carNo, startDate, endDate, yardName);
    }

    @Override
    public List<ReportCarInData> findByLicenseNumber(String enterCarLicenseNumber) {
        return baseMapper.findByLicenseNumber(enterCarLicenseNumber);
    }

    @Override
    public int updateEnterTime(String enterCarLicenseNumber, DateTime parse) {

        return baseMapper.updateEnterTime(enterCarLicenseNumber, parse);
    }

    @Override
    public int updateByCarNumber(String carNumber, String reserveTime, String enterTime, String enterVipType) {
        return baseMapper.updateByCarNumber(carNumber, reserveTime, enterTime, enterVipType);
    }

    @Override
    public int updateEnterVipType(String enterCarLicenseNumber, int enterVipType) {
        return baseMapper.updateEnterVipType(enterCarLicenseNumber, enterVipType);
    }

    @Override
    public int countByDate(String startDate, String endDate, String yardName) {
        return baseMapper.countByDate(startDate, endDate, yardName);
    }

    @Override
    public int countByVIPOutIndex(String startDate, String endDate, String yardName) {
        return baseMapper.countByVIPOutIndex(startDate, endDate, yardName);
    }

    @Override
    public int countByLinShiOutIndex(String startDate, String endDate, String yardName) {
        return baseMapper.countByLinShiOutIndex(startDate, endDate, yardName);
    }

    @Override
    public List<VehicleReservation> queryListVehicleReservationExportLinShi(String startDate, String endDate, String yardName) {
        return baseMapper.queryListVehicleReservationExportLinShi(startDate, endDate, yardName);
    }

    @Override
    public VehicleReservation selectByCarName(String enterCarLicenseNumber) {
        return baseMapper.selectByCarName(enterCarLicenseNumber);
    }

    @Override
    public int batchDelete(List<Integer> ids) {
        return baseMapper.deleteBatchIds(ids);
    }

    @Override
    public VehicleReservation selectVehicleReservation(String enterCarLicenseNumber, String yardCode) {
        return baseMapper.selectVehicleReservation(enterCarLicenseNumber, yardCode);
    }

    /**
     * 获取所有不重复的车场名称
     * 从vehicle_reservation表中查询所有不重复的yard_name字段
     * 用于用户管理中的车场权限分配下拉框
     * 
     * @return 车场名称列表
     */
    @Override
    public List<String> getAllDistinctYardNames() {
        try {
            log.info("🔍 开始查询所有不重复的车场名称");
            
            // 查询所有记录
            List<VehicleReservation> allReservations = this.list();
            
            // 提取所有不重复的车场名称
            List<String> distinctYardNames = allReservations.stream()
                    .map(VehicleReservation::getYardName)
                    .filter(Objects::nonNull)
                    .filter(name -> !name.trim().isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            
            log.info("✅ 查询成功，共找到{}个不重复的车场", distinctYardNames.size());
            if (!distinctYardNames.isEmpty()) {
                log.info("📋 车场列表: {}", distinctYardNames);
            }
            
            return distinctYardNames;
        } catch (Exception e) {
            log.error("❌ 查询车场列表失败", e);
            return new ArrayList<>();
        }
    }
}
