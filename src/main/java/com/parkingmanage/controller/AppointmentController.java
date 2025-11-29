package com.parkingmanage.controller;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.Result;
import com.parkingmanage.common.config.AIKEConfig;
import com.parkingmanage.entity.*;
import com.parkingmanage.entity.VisitorApplication;
import com.parkingmanage.query.SubAppointQuery;
import com.parkingmanage.query.VehicleQuery;
import com.parkingmanage.service.*;
import com.parkingmanage.utils.PageUtils;
import com.parkingmanage.utils.ResultUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

import com.baomidou.mybatisplus.extension.api.R;
import com.parkingmanage.utils.ResourceUtil;
import org.springframework.web.servlet.mvc.Controller;
import java.util.HashMap;
import java.util.Map;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author MLH
 * @since 2022-07-13
 */
@RestController
@RequestMapping("/parking/appointment")
@Api(tags = "入场预约")
public class AppointmentController {
    @Resource
    private AppointmentService appointmentService;
    @Resource
    private ButlerService butlerService;
    @Resource
    private CommunityService communityService;
    @Resource
    private VisitorApplicationService visitorApplicationService;
    @Resource
    private QrCodeUsageService qrCodeUsageService;
    @Resource
    private YardInfoService yardInfoService;
    @Resource
    private AIKEConfig aikeConfig;
    @Resource
    private ViolationsService violationsService;
    @Resource
    private WeChatTemplateMessageService weChatTemplateMessageService;
    private Logger logger = LoggerFactory.getLogger(Controller.class);
    @Autowired
    private OwnerinfoService ownerinfoService;

    @ApiOperation("添加")
    @PostMapping("/insertAppointment")
    public R<Object> insertAppointment(@RequestBody Appointment appointment) {
        // 🚫 第一步：车牌号黑名单校验
        String plateNumber = appointment.getPlatenumber();
        if (plateNumber != null && !plateNumber.trim().isEmpty()) {
            try {
                logger.info("🚫 开始车牌号黑名单校验: {}", plateNumber);
                boolean isBlacklisted = checkBlacklistStatus(plateNumber, appointment.getCommunity());
                if (isBlacklisted) {
                    logger.warn("🚫 车牌号 {} 在黑名单中，拒绝预约", plateNumber);
                    return R.failed("该车牌号已被列入黑名单，无法进行预约");
                }
                logger.info("✅ 车牌号 {} 黑名单校验通过", plateNumber);
            } catch (Exception e) {
                logger.error("🚫 黑名单校验异常: {}", e.getMessage(), e);
                return R.failed("黑名单校验失败，请稍后重试");
            }
            // 🚫 第二步：违规记录校验
            try {
                logger.info("🚫 开始违规记录校验: {}", plateNumber);
                boolean hasUnhandledViolations = checkViolationStatus(plateNumber);
                if (hasUnhandledViolations) {
                    logger.warn("🚫 车牌号 {} 存在未处理的违规记录，拒绝预约", plateNumber);
                    return R.failed("该车牌号存在未处理的违规记录，请先处理违规记录后再进行预约");
                }
                logger.info("✅ 车牌号 {} 违规记录校验通过", plateNumber);
            } catch (Exception e) {
                logger.error("🚫 违规记录校验异常: {}", e.getMessage(), e);
                return R.failed("违规记录校验失败，请稍后重试");
            }
        }
        // 通过手机号查询地址信息和业主信息（优先访客手机，其次业主手机）
        try {
            VisitorApplication foundApplication = null;
            String queryPhone = null;
            // 首先尝试通过访客手机号查询
            String visitorPhone = appointment.getVisitorphone();
            if (visitorPhone != null && !visitorPhone.isEmpty()) {
                foundApplication = visitorApplicationService.getByPhone(visitorPhone);
                queryPhone = visitorPhone;
                logger.info("通过访客手机号查询访客申请记录: phone={}, found={}",
                           visitorPhone, foundApplication != null);
            }
            // 如果访客手机号没有找到合适的记录，尝试业主手机号
            if ((foundApplication == null || !"已通过".equals(foundApplication.getAuditstatus())) 
                && appointment.getOwnerphone() != null && !appointment.getOwnerphone().isEmpty()) {
                String ownerPhone = appointment.getOwnerphone();
                VisitorApplication ownerApplication = visitorApplicationService.getByPhone(ownerPhone);
                if (ownerApplication != null && "已通过".equals(ownerApplication.getAuditstatus())) {
                    foundApplication = ownerApplication;
                    queryPhone = ownerPhone;
                    logger.info("通过业主手机号查询访客申请记录: phone={}, found=true", ownerPhone);
                }
            }
            // 记录查询结果
            if (foundApplication != null) {
                logger.info("找到访客申请记录: applicationNo={}, auditstatus={}, ownerName={}, ownerPhone={}", 
                           foundApplication.getApplicationNo(), foundApplication.getAuditstatus(),
                           foundApplication.getOwnerName(), foundApplication.getOwnerPhone());
            } else {
                logger.warn("未找到合适的访客申请记录，无法自动填充业主信息");
            }
            // 如果找到了合适的申请记录，使用其地址信息和业主信息
            if (foundApplication != null && "已通过".equals(foundApplication.getAuditstatus())) {
                // 只有当前端没有提供对应字段时，才使用查询到的信息进行填充
                if (appointment.getProvince() == null || appointment.getProvince().isEmpty()) {
                    appointment.setProvince(foundApplication.getProvince());
                }
                if (appointment.getCity() == null || appointment.getCity().isEmpty()) {
                    appointment.setCity(foundApplication.getCity());
                }
                if (appointment.getDistrict() == null || appointment.getDistrict().isEmpty()) {
                    appointment.setDistrict(foundApplication.getDistrict());
                }
                if (appointment.getCommunity() == null || appointment.getCommunity().isEmpty()) {
                    appointment.setCommunity(foundApplication.getCommunity());
                }
                if (appointment.getBuilding() == null || appointment.getBuilding().isEmpty()) {
                    appointment.setBuilding(foundApplication.getBuilding());
                }
                if (appointment.getUnits() == null || appointment.getUnits().isEmpty()) {
                    appointment.setUnits(foundApplication.getUnits());
                }
                if (appointment.getFloor() == null || appointment.getFloor().isEmpty()) {
                    appointment.setFloor(foundApplication.getFloor());
                }
                if (appointment.getRoom() == null || appointment.getRoom().isEmpty()) {
                    appointment.setRoom(foundApplication.getRoomnumber());
                }
                // 访客预约时，从visitor_application表中填充业主信息到appointment表
                if (appointment.getAppointtype() != null && 
                    (appointment.getAppointtype().equals("自助") || appointment.getAppointtype().equals("邀请"))) {
                    // 填充业主姓名（如果前端没有提供）
                    if (appointment.getOwnername() == null || appointment.getOwnername().isEmpty()) {
                        if (foundApplication.getOwnerName() != null && !foundApplication.getOwnerName().isEmpty()) {
                            appointment.setOwnername(foundApplication.getOwnerName());
                            logger.info("从访客申请记录中填充业主姓名: {}", foundApplication.getOwnerName());
                        }
                    }
                    // 填充业主手机号（如果前端没有提供）
                    if (appointment.getOwnerphone() == null || appointment.getOwnerphone().isEmpty()) {
                        if (foundApplication.getOwnerPhone() != null && !foundApplication.getOwnerPhone().isEmpty()) {
                            appointment.setOwnerphone(foundApplication.getOwnerPhone());
                            logger.info("从访客申请记录中填充业主手机号: {}", foundApplication.getOwnerPhone());
                        }
                    }
                    logger.info("访客预约业主信息填充完成 - 业主姓名: {}, 业主手机: {}", 
                               appointment.getOwnername(), appointment.getOwnerphone());
                }
            }
        } catch (Exception e) {
            logger.error("通过手机号查询地址信息失败: " + e.getMessage());
            e.printStackTrace();
        }
        // 检查关键地址字段是否为空
        boolean hasCompleteAddress = appointment.getBuilding() != null && !appointment.getBuilding().isEmpty() &&
                                   appointment.getUnits() != null && !appointment.getUnits().isEmpty() &&
                                   appointment.getFloor() != null && !appointment.getFloor().isEmpty() &&
                                   appointment.getRoom() != null && !appointment.getRoom().isEmpty();
        if (!hasCompleteAddress) {
            logger.warn("地址信息不完整，缺少栋/单元/楼层/房间号");
        }
        // 统一处理省市区信息查询
        try {
            // 如果前端没有传递省市区信息，则根据小区名称查询
            if (appointment.getProvince() == null || appointment.getProvince().isEmpty()) {
                // 查询小区对应的省市区信息
                Community communityInfo = communityService.findProvinceByCommunityName(
                    appointment.getCommunity(),
                    appointment.getBuilding(),
                    appointment.getUnits(),
                    appointment.getFloor(),
                    appointment.getRoom()
                );
                if (communityInfo != null) {
                    appointment.setProvince(communityInfo.getProvince());
                    appointment.setCity(communityInfo.getCity());
                    appointment.setDistrict(communityInfo.getDistrict());
                } else {
                    // 如果查询不到，设置默认值
                    appointment.setProvince("黑龙江省");
                    appointment.setCity("哈尔滨市");
                    appointment.setDistrict("道里区");
                }
            }
        } catch (Exception e) {
            logger.error("查询省市区信息失败: " + e.getMessage());
            e.printStackTrace();
            // 设置默认省市区信息
            appointment.setProvince("黑龙江省");
            appointment.setCity("哈尔滨市");
            appointment.setDistrict("道里区");
        }
        // 根据预约类型设置不同的状态和信息
        if(appointment.getAppointtype().equals("自助")) {
            // 基于时间段判断是否需要审核
            boolean needsAudit = checkIfTimeSlotNeedsAudit(appointment.getCommunity(), appointment.getVisitdate());
            if (!needsAudit) {
                // 预约时间段不需要审核
                appointment.setAuditstatus("不审核");
                appointment.setVenuestatus("待入场");
                logger.info("预约时间段不需要审核: 社区={}, 时间段={}", appointment.getCommunity(), appointment.getVisitdate());
            } else {
                appointment.setAuditstatus("待审批");
                appointment.setVenuestatus("待审批");
                logger.info("预约时间段需要审核: 社区={}, 时间段={}", appointment.getCommunity(), appointment.getVisitdate());
                
                // 🔔 需要审核时发送待审核提醒
                try {
                    sendBookingPendingNotification(appointment);
                } catch (Exception e) {
                    logger.warn("⚠️ [预约待审核提醒发送失败] plateNumber={}, error={}", appointment.getPlatenumber(), e.getMessage());
                    // 通知发送失败不影响预约创建的成功状态
                }
            }
        } else if (appointment.getAppointtype().equals("业主")) {
            // 业主预约：需要审核（不受时间段配置影响）
            appointment.setAuditstatus("待审批");
            appointment.setVenuestatus("待审批");
            logger.info("业主预约：设置为待审批状态，社区={}, 时间段={}", appointment.getCommunity(), appointment.getVisitdate());
            
            // 🔔 业主预约需要审核，发送待审核提醒
            try {
                sendBookingPendingNotification(appointment);
            } catch (Exception e) {
                logger.warn("⚠️ [业主预约待审核提醒发送失败] plateNumber={}, error={}", appointment.getPlatenumber(), e.getMessage());
                // 通知发送失败不影响预约创建的成功状态
            }
        } else if (appointment.getAppointtype().equals("代人")) {
            appointment.setVisitreason("管家代为预约");
            appointment.setAuditstatus("已通过");
            appointment.setVenuestatus("待入场");
            appointment.setAuditdate(LocalDateTime.now()); // 设置审核时间为当前时间
            
            // 🆕 管家代人预约：直接使用前端传递的管家nickname和openid
            String managerNickname = appointment.getManagerNickname();
            String managerOpenid = appointment.getManagerOpenid();
            
            if (managerNickname != null && !managerNickname.isEmpty() && 
                managerOpenid != null && !managerOpenid.isEmpty()) {
                // 直接使用前端传递的管家信息
                appointment.setAuditusername(managerNickname);
                appointment.setAuditopenid(managerOpenid);
                logger.info("管家代人预约 - 使用前端传递的管家信息: nickname={}, openid={}", managerNickname, managerOpenid);
            } else {
                // 兜底逻辑：如果前端没有传递，使用原有逻辑
                appointment.setAuditopenid(appointment.getOpenid());
                Butler butler = butlerService.getButlerByOpenId(appointment.getOpenid());
                if (butler != null) {
                    appointment.setAuditusername(butler.getUsername());
                    logger.info("管家代人预约 - 使用兜底逻辑查询管家信息: username={}", butler.getUsername());
                } else {
                    appointment.setAuditusername("管家用户");
                    logger.warn("管家代人预约 - 未找到管家信息，使用默认值");
                }
            }
        }else if (appointment.getAppointtype().equals("邀请")) {
            // 邀请类型（访客扫描管家二维码）无需审核，审核人是发放二维码的管家
            appointment.setAuditstatus("已通过");
            appointment.setVenuestatus("待入场");
            appointment.setAuditdate(LocalDateTime.now()); // 设置审核时间为当前时间

            // 🆕 优先使用前端传递的管家姓名查询管家信息
            String butlerName = appointment.getButlerName();
            boolean foundButler = false;
            
            if (butlerName != null && !butlerName.isEmpty()) {
                try {
                    // 通过管家姓名查询管家信息
                    Butler butler = butlerService.getButlerByName(butlerName);
                    if (butler != null) {
                        appointment.setAuditopenid(butler.getOpenid());
                        appointment.setAuditusername(butler.getUsername());
                        foundButler = true;
                        logger.info("邀请预约 - 通过前端传递的管家姓名查询到管家信息: 管家姓名={}, 管家openid={}", butler.getUsername(), butler.getOpenid());
                    } else {
                        logger.warn("邀请预约 - 未通过管家姓名查询到管家信息: butlerName={}", butlerName);
                    }
                } catch (Exception e) {
                    logger.error("邀请预约 - 通过管家姓名查询管家信息失败: butlerName={}, error={}", butlerName, e.getMessage());
                }
            }

            // 兜底逻辑：如果通过前端传递的管家姓名没有找到，使用原有逻辑
            if (!foundButler) {
                try {
                    // 首先尝试从用户信息中获取管家信息
                    String butlerOpenid = getButlerOpenidFromVisitor(appointment.getOpenid());
                    if (butlerOpenid != null) {
                        Butler butler = butlerService.getButlerByOpenId(butlerOpenid);
                        if (butler != null) {
                            appointment.setAuditopenid(butlerOpenid);
                            appointment.setAuditusername(butler.getUsername());
                            logger.info("邀请预约 - 兜底逻辑设置管家审核信息: 管家={}, 管家openid={}", butler.getUsername(), butlerOpenid);
                        } else {
                            logger.warn("邀请预约 - 兜底逻辑未找到管家信息，使用默认审核人: butlerOpenid={}", butlerOpenid);
                            appointment.setAuditopenid(butlerOpenid);
                            appointment.setAuditusername("管家");
                        }
                    } else {
                        logger.warn("邀请预约 - 兜底逻辑未能获取管家openid，使用默认审核信息");
                        appointment.setAuditopenid("system");
                        appointment.setAuditusername("系统自动审核");
                    }
                } catch (Exception e) {
                    logger.error("邀请预约 - 兜底逻辑获取管家审核信息失败: " + e.getMessage(), e);
                    appointment.setAuditopenid("system");
                    appointment.setAuditusername("系统自动审核");
                }
            }

            logger.info("邀请预约无需审核，已自动通过: 社区={}, 审核人={}", appointment.getCommunity(), appointment.getAuditusername());
        }
        
        try {
            boolean saveResult = appointmentService.save(appointment);
            if (saveResult) {
                // 🆕 预约成功后，返回包含状态信息的预约对象
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("auditstatus", appointment.getAuditstatus());
                responseData.put("venuestatus", appointment.getVenuestatus());
                responseData.put("appointtype", appointment.getAppointtype());
                responseData.put("auditusername", appointment.getAuditusername());
                responseData.put("id", appointment.getId());

                logger.info("预约创建成功，返回状态信息: auditstatus={}, venuestatus={}, appointtype={}",
                           appointment.getAuditstatus(), appointment.getVenuestatus(), appointment.getAppointtype());

                return R.ok(responseData);
            } else {
                return R.failed("预约保存失败");
            }
        } catch (Exception e) {
            logger.error("预约保存过程发生异常: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    @ApiOperation("根据车场名称查询车场编码")
    @GetMapping("/yardCode")
    public List<String> getYardCodeByName(@RequestParam(required = false) String yardName) {
        return yardInfoService.yardCode(yardName);
    }

    @GetMapping("/getAppointmentPlateNumber")
    @ResponseBody
    public R<Map<String, Object>> getAppointmentPlateNumber(@RequestParam(required = false) String plateNumber) {
        logger.info("🔍 [预约车搜索] 开始查询，车牌号: {}", plateNumber);
        Integer count = 0;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        ArrayList<Object> appointmentList = new ArrayList<>();
        List<Appointment> appointmentAll = appointmentService.getAppointmentPlateNumber(plateNumber);
        logger.info("🔍 [预约车搜索] 数据库查询结果数量: {}", appointmentAll.size());
        Iterator<Appointment> iter = appointmentAll.iterator();
        while (iter.hasNext()) {
            Appointment appointment = iter.next();
            logger.info("🔍 [预约车搜索] 找到记录 - 车牌: {}, 状态: {}, 预约时间: {}", 
                appointment.getPlatenumber(), appointment.getVenuestatus(), appointment.getVisitdate());
            Map<String, Object> appointmentMap = new HashMap<>();
            appointmentMap.put("id", appointment.getId());
            appointmentMap.put("province", appointment.getProvince());
            appointmentMap.put("city", appointment.getCity());
            appointmentMap.put("district", appointment.getDistrict());
            appointmentMap.put("community", appointment.getCommunity());
            // visitdate 现在是 String 类型，可能包含区间格式，直接使用不需要转换
            appointmentMap.put("visitdate", appointment.getVisitdate());
            appointmentMap.put("recorddate", ResultUtil.convertDate(appointment.getRecorddate().toString()));
            appointmentMap.put("visitorphone", appointment.getVisitorphone());
            appointmentMap.put("cartype", appointment.getCartype());
            appointmentMap.put("platenumber", appointment.getPlatenumber());
            appointmentMap.put("status", appointment.getStatus());
            appointmentMap.put("openid", appointment.getOpenid());
            appointmentMap.put("building", appointment.getBuilding());
            appointmentMap.put("units", appointment.getUnits());
            appointmentMap.put("floor", appointment.getFloor());
            appointmentMap.put("room", appointment.getRoom());
            appointmentMap.put("ordernumber", appointment.getOrdernumber());
            appointmentMap.put("ownername", appointment.getOwnername());
            appointmentMap.put("ownerphone", appointment.getOwnerphone());
            appointmentMap.put("visitreason", appointment.getVisitreason());
            appointmentMap.put("appointtype", appointment.getAppointtype());
            appointmentMap.put("auditstatus", appointment.getAuditstatus());
            appointmentMap.put("refusereason", appointment.getRefusereason());
            appointmentMap.put("venuestatus", appointment.getVenuestatus());
            
            // 添加缺失的arrivedate和leavedate字段
            if (appointment.getArrivedate() == null) {
                appointmentMap.put("arrivedate", "");
            } else {
                appointmentMap.put("arrivedate", ResultUtil.convertDate(appointment.getArrivedate().toString()));
            }
            
            if (appointment.getLeavedate() == null) {
                appointmentMap.put("leavedate", "");
            } else {
                appointmentMap.put("leavedate", ResultUtil.convertDate(appointment.getLeavedate().toString()));
            }
            
            if (appointment.getAuditdate() == null) {
                appointmentMap.put("auditdate", "");
            } else {
                appointmentMap.put("auditdate", ResultUtil.convertDate(appointment.getAuditdate().toString()));
            }
            appointmentList.add(appointmentMap);
        }
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("data", appointmentList);
        return R.ok(dataMap);
    }

    @GetMapping("/getList")
    @ResponseBody
    public R<Map<String, Object>> getList() throws ParseException {

        Integer count = 0;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ArrayList<Object> appointmentList = new ArrayList<>();
        List<Appointment> appointmentAll = appointmentService.list();

        Iterator<Appointment> iter = appointmentAll.iterator();
        while (iter.hasNext()) {
            Appointment appointment = iter.next();
            Map<String, Object> appointmentMap = new HashMap<>();
            appointmentMap.put("id", appointment.getId());
            appointmentMap.put("province", appointment.getProvince());
            appointmentMap.put("city", appointment.getCity());
            appointmentMap.put("district", appointment.getDistrict());
            appointmentMap.put("community", appointment.getCommunity());
            // visitdate 现在是 String 类型，可能包含区间格式，直接使用不需要转换
            appointmentMap.put("visitdate", appointment.getVisitdate());
            if (appointment.getRecorddate() == null) {
                appointmentMap.put("recorddate", "");
            } else {
                appointmentMap.put("recorddate", ResultUtil.convertDate(appointment.getRecorddate().toString()));
            }
            appointmentMap.put("visitorphone", appointment.getVisitorphone());
            appointmentMap.put("cartype", appointment.getCartype());
            appointmentMap.put("platenumber", appointment.getPlatenumber());
            appointmentMap.put("status", appointment.getStatus());
            appointmentMap.put("openid", appointment.getOpenid());
            appointmentMap.put("building", appointment.getBuilding());
            appointmentMap.put("units", appointment.getUnits());
            appointmentMap.put("floor", appointment.getFloor());
            appointmentMap.put("room", appointment.getRoom());
            appointmentMap.put("ordernumber", appointment.getOrdernumber());
            appointmentMap.put("ownername", appointment.getOwnername());
            appointmentMap.put("ownerphone", appointment.getOwnerphone());
            appointmentMap.put("visitreason", appointment.getVisitreason());
            appointmentMap.put("appointtype", appointment.getAppointtype());
            appointmentMap.put("auditstatus", appointment.getAuditstatus());
            appointmentMap.put("refusereason", appointment.getRefusereason());
            appointmentMap.put("venuestatus", appointment.getVenuestatus());
            if (appointment.getArrivedate() == null) {
                appointmentMap.put("arrivedate", "");
            } else {
                String arriveDateStr = appointment.getArrivedate().toString();
                appointmentMap.put("arrivedate", ResultUtil.convertDate(arriveDateStr));
            }
            if (appointment.getLeavedate() == null) {
                appointmentMap.put("leavedate", "");
            } else {
                String leaveDateStr = appointment.getLeavedate().toString();
                appointmentMap.put("leavedate", ResultUtil.convertDate(leaveDateStr));
            }
            appointmentMap.put("auditusername", appointment.getAuditusername());
            if (appointment.getAuditdate() == null) {
                appointmentMap.put("auditdate", "");
            } else {
                String auditDateStr = appointment.getAuditdate().toString();
                appointmentMap.put("auditdate", ResultUtil.convertDate(auditDateStr));
            }
            appointmentList.add(appointmentMap);
        }
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("data", appointmentList);
        return R.ok(dataMap);
    }

    @ApiOperation("查询所有待审批")
    @GetMapping("/listAppointNoAudit")
    public IPage<Appointment> listAppointNoAudit(
            @RequestParam(required = false) String community,
            @RequestParam(required = false) String ownername,
            @RequestParam(required = false) String recorddate,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Appointment> appointmentList = appointmentService.listAppointNoAudit(community, ownername, recorddate);
        //按照设备名和申请日期排序
        List<Appointment> asServices = appointmentList.stream().sorted(Comparator.comparing(Appointment::getProvince).
                thenComparing(Appointment::getCity).thenComparing(Appointment::getDistrict).thenComparing(Appointment::getCommunity).
                thenComparing(Appointment::getBuilding).thenComparing(Appointment::getUnits)
                .thenComparing(Appointment::getRecorddate)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);

    }

    @GetMapping("/visitorList/{openid}")
    @ResponseBody
    public R<Map<String, Object>> visitorList(@PathVariable String openid) throws ParseException {
        Integer count = 0;
        ArrayList<Object> appointmentList = new ArrayList<>();
        List<Appointment> appointmentAll = appointmentService.visitorList(openid);
        Iterator<Appointment> iter = appointmentAll.iterator();
        while (iter.hasNext()) {
            Appointment appointment = iter.next();
            Map<String, Object> appointmentMap = new HashMap<>();
            appointmentMap.put("id", appointment.getId());
            appointmentMap.put("province", appointment.getProvince());
            appointmentMap.put("city", appointment.getCity());
            appointmentMap.put("district", appointment.getDistrict());
            appointmentMap.put("community", appointment.getCommunity());
            // visitdate 现在是 String 类型，可能包含区间格式，直接使用不需要转换
            appointmentMap.put("visitdate", appointment.getVisitdate());
            appointmentMap.put("recorddate", ResultUtil.convertDate(appointment.getRecorddate().toString()));
            appointmentMap.put("visitorphone", appointment.getVisitorphone());
            appointmentMap.put("cartype", appointment.getCartype());
            appointmentMap.put("platenumber", appointment.getPlatenumber());
            appointmentMap.put("status", appointment.getStatus());
            appointmentMap.put("openid", appointment.getOpenid());
            appointmentMap.put("building", appointment.getBuilding());
            appointmentMap.put("units", appointment.getUnits());
            appointmentMap.put("floor", appointment.getFloor());
            appointmentMap.put("room", appointment.getRoom());
            appointmentMap.put("ordernumber", appointment.getOrdernumber());
            appointmentMap.put("ownername", appointment.getOwnername());
            appointmentMap.put("ownerphone", appointment.getOwnerphone());
            appointmentMap.put("visitreason", appointment.getVisitreason());
            appointmentMap.put("appointtype", appointment.getAppointtype());
            appointmentMap.put("auditstatus", appointment.getAuditstatus());
            appointmentMap.put("refusereason", appointment.getRefusereason());
            appointmentMap.put("venuestatus", appointment.getVenuestatus());
            if (appointment.getArrivedate() == null) {
                appointmentMap.put("arrivedate", "");
            } else {
                appointmentMap.put("arrivedate", ResultUtil.convertDate(appointment.getArrivedate().toString()));
            }
            if (appointment.getLeavedate() == null) {
                appointmentMap.put("leavedate", "");
            } else {
                appointmentMap.put("leavedate", ResultUtil.convertDate(appointment.getLeavedate().toString()));
            }
            appointmentMap.put("auditusername", appointment.getAuditusername());
            if (appointment.getAuditdate() == null) {
                appointmentMap.put("auditdate", "");
            } else {
                appointmentMap.put("auditdate", ResultUtil.convertDate(appointment.getAuditdate().toString()));
            }
            appointmentList.add(appointmentMap);
        }
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("data", appointmentList);
        return R.ok(dataMap);
    }

    @GetMapping("/managerList/{openid}")
    @ResponseBody
    public R<Map<String, Object>> managerList(@PathVariable String openid) throws ParseException {
        // 添加详细的调试日志
        System.out.println("🔍 [管家预约查询] 开始查询，openid: " + openid);

        Integer count = 0;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ArrayList<Object> appointmentList = new ArrayList<>();
        List<Appointment> appointmentAll = appointmentService.managerList(openid);

        System.out.println("🔍 [管家预约查询] SQL查询结果数量: " + appointmentAll.size());
        if (appointmentAll.isEmpty()) {
            System.out.println("⚠️ [管家预约查询] 查询结果为空，可能原因:");
            System.out.println("   1. Area表中没有该openid的权限配置");
            System.out.println("   2. Appointment表中没有对应区域的预约数据");
            System.out.println("   3. 地址信息不匹配");
        }

        Iterator<Appointment> iter = appointmentAll.iterator();
        while (iter.hasNext()) {
            Appointment appointment = iter.next();
            Map<String, Object> appointmentMap = new HashMap<>();
            appointmentMap.put("id", appointment.getId());
            appointmentMap.put("province", appointment.getProvince());
            appointmentMap.put("city", appointment.getCity());
            appointmentMap.put("district", appointment.getDistrict());
            appointmentMap.put("community", appointment.getCommunity());
            // visitdate 现在是 String 类型，可能包含区间格式，直接使用不需要转换
            appointmentMap.put("visitdate", appointment.getVisitdate());
            appointmentMap.put("recorddate", ResultUtil.convertDate(appointment.getRecorddate().toString()));
            appointmentMap.put("visitorphone", appointment.getVisitorphone());
            appointmentMap.put("cartype", appointment.getCartype());
            appointmentMap.put("platenumber", appointment.getPlatenumber());
            appointmentMap.put("status", appointment.getStatus());
            appointmentMap.put("openid", appointment.getOpenid());
            appointmentMap.put("building", appointment.getBuilding());
            appointmentMap.put("units", appointment.getUnits());
            appointmentMap.put("floor", appointment.getFloor());
            appointmentMap.put("room", appointment.getRoom());
            appointmentMap.put("ordernumber", appointment.getOrdernumber());
            appointmentMap.put("ownername", appointment.getOwnername());
            appointmentMap.put("ownerphone", appointment.getOwnerphone());
            appointmentMap.put("visitreason", appointment.getVisitreason());
            appointmentMap.put("appointtype", appointment.getAppointtype());
            appointmentMap.put("auditstatus", appointment.getAuditstatus());
            appointmentMap.put("refusereason", appointment.getRefusereason());
            appointmentMap.put("venuestatus", appointment.getVenuestatus());
            if (appointment.getArrivedate() == null) {
                appointmentMap.put("arrivedate", "");
            } else {
                appointmentMap.put("arrivedate", ResultUtil.convertDate(appointment.getArrivedate().toString()));
            }
            if (appointment.getLeavedate() == null) {
                appointmentMap.put("leavedate", "");
            } else {
                appointmentMap.put("leavedate", ResultUtil.convertDate(appointment.getLeavedate().toString()));
            }
            appointmentMap.put("auditusername", appointment.getAuditusername());
            if (appointment.getAuditdate() == null) {
                appointmentMap.put("auditdate", "");
            } else {
                appointmentMap.put("auditdate", ResultUtil.convertDate(appointment.getAuditdate().toString()));
            }
            appointmentList.add(appointmentMap);
        }
        HashMap<String, Object> dataMap = new HashMap<>();
        //查询判断展示的auditstatus为待审核的数据
        dataMap.put("data", appointmentList);
        return R.ok(dataMap);
    }

    @ApiOperation("调试管家权限和数据状态")
    @GetMapping("/debugManagerData/{openid}")
    @ResponseBody
    public R<Map<String, Object>> debugManagerData(@PathVariable String openid) {
        Map<String, Object> debugInfo = new HashMap<>();

        try {
            // 1. 检查Area表中的权限配置
            System.out.println("🔍 [调试API] 1. 检查Area表权限配置...");
            // 这里需要注入AreaService，暂时用日志记录
            debugInfo.put("step1", "检查Area表权限配置");
            debugInfo.put("openid", openid);

            // 2. 检查Appointment表总数据量
            System.out.println("🔍 [调试API] 2. 检查Appointment表总数据量...");
            List<Appointment> allAppointments = appointmentService.list();
            debugInfo.put("totalAppointments", allAppointments.size());
            System.out.println("🔍 [调试API] Appointment表总记录数: " + allAppointments.size());

            // 3. 执行managerList查询
            System.out.println("🔍 [调试API] 3. 执行managerList查询...");
            List<Appointment> managerAppointments = appointmentService.managerList(openid);
            debugInfo.put("managerAppointments", managerAppointments.size());
            System.out.println("🔍 [调试API] 管家查询结果数: " + managerAppointments.size());

            // 4. 分析前几条Appointment记录的地址信息
            System.out.println("🔍 [调试API] 4. 分析Appointment记录地址信息...");
            List<Map<String, Object>> sampleAppointments = new ArrayList<>();
            for (int i = 0; i < Math.min(5, allAppointments.size()); i++) {
                Appointment apt = allAppointments.get(i);
                Map<String, Object> aptInfo = new HashMap<>();
                aptInfo.put("id", apt.getId());
                aptInfo.put("province", apt.getProvince());
                aptInfo.put("city", apt.getCity());
                aptInfo.put("district", apt.getDistrict());
                aptInfo.put("community", apt.getCommunity());
                aptInfo.put("building", apt.getBuilding());
                aptInfo.put("units", apt.getUnits());
                aptInfo.put("floor", apt.getFloor());
                aptInfo.put("plateNumber", apt.getPlatenumber());
                sampleAppointments.add(aptInfo);
            }
            debugInfo.put("sampleAppointments", sampleAppointments);

            // 5. 提供调试建议
            List<String> suggestions = new ArrayList<>();
            if (managerAppointments.isEmpty()) {
                suggestions.add("管家查询结果为空，请检查:");
                suggestions.add("1. Area表中是否有openid=" + openid + "的权限记录");
                suggestions.add("2. Area表中的地址信息是否与Appointment表匹配");
                suggestions.add("3. 地址匹配需要省市区小区楼栋单元楼层完全一致");
                suggestions.add("4. 检查SQL: SELECT * FROM area WHERE openid='" + openid + "'");
            } else {
                suggestions.add("管家查询成功，找到 " + managerAppointments.size() + " 条记录");
            }
            debugInfo.put("suggestions", suggestions);

            debugInfo.put("success", true);
            debugInfo.put("message", "调试信息收集完成");

        } catch (Exception e) {
            System.err.println("🔍 [调试API] 调试过程出错: " + e.getMessage());
            e.printStackTrace();
            debugInfo.put("success", false);
            debugInfo.put("error", e.getMessage());
        }

        return R.ok(debugInfo);
    }

    @ApiOperation("根据手机号查询预约记录")
    @GetMapping("/listByPhone")
    @ResponseBody
    public R<Map<String, Object>> listByPhone(@RequestParam String phone) throws ParseException {
        ArrayList<Object> appointmentList = new ArrayList<>();
        
        try {
            List<Appointment> appointmentAll = appointmentService.listByPhone(phone);
            
            if (appointmentAll != null && !appointmentAll.isEmpty()) {
                Iterator<Appointment> iter = appointmentAll.iterator();
                while (iter.hasNext()) {
                    Appointment appointment = iter.next();
                    Map<String, Object> appointmentMap = new HashMap<>();
                    appointmentMap.put("id", appointment.getId());
                    appointmentMap.put("province", appointment.getProvince());
                    appointmentMap.put("city", appointment.getCity());
                    appointmentMap.put("district", appointment.getDistrict());
                    appointmentMap.put("community", appointment.getCommunity());
                    // visitdate 现在是 String 类型，可能包含区间格式，直接使用不需要转换
                    appointmentMap.put("visitdate", appointment.getVisitdate());
                    appointmentMap.put("recorddate", ResultUtil.convertDate(appointment.getRecorddate().toString()));
                    appointmentMap.put("visitorphone", appointment.getVisitorphone());
                    appointmentMap.put("cartype", appointment.getCartype());
                    appointmentMap.put("platenumber", appointment.getPlatenumber());
                    appointmentMap.put("status", appointment.getStatus());
                    appointmentMap.put("openid", appointment.getOpenid());
                    appointmentMap.put("building", appointment.getBuilding());
                    appointmentMap.put("units", appointment.getUnits());
                    appointmentMap.put("floor", appointment.getFloor());
                    appointmentMap.put("room", appointment.getRoom());
                    appointmentMap.put("ordernumber", appointment.getOrdernumber());
                    appointmentMap.put("ownername", appointment.getOwnername());
                    appointmentMap.put("ownerphone", appointment.getOwnerphone());
                    appointmentMap.put("visitreason", appointment.getVisitreason());
                    appointmentMap.put("appointtype", appointment.getAppointtype());
                    appointmentMap.put("auditstatus", appointment.getAuditstatus());
                    appointmentMap.put("refusereason", appointment.getRefusereason());
                    appointmentMap.put("venuestatus", appointment.getVenuestatus());
                    if (appointment.getArrivedate() == null) {
                        appointmentMap.put("arrivedate", "");
                    } else {
                        appointmentMap.put("arrivedate", ResultUtil.convertDate(appointment.getArrivedate().toString()));
                    }
                    if (appointment.getLeavedate() == null) {
                        appointmentMap.put("leavedate", "");
                    } else {
                        appointmentMap.put("leavedate", ResultUtil.convertDate(appointment.getLeavedate().toString()));
                    }
                    appointmentMap.put("auditusername", appointment.getAuditusername());
                    if (appointment.getAuditdate() == null) {
                        appointmentMap.put("auditdate", "");
                    } else {
                        appointmentMap.put("auditdate", ResultUtil.convertDate(appointment.getAuditdate().toString()));
                    }
                    appointmentList.add(appointmentMap);
                }
            }
            
        } catch (Exception e) {
            logger.error("查询过程发生异常: " + e.getMessage(), e);
            throw e;
        }
        
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("data", appointmentList);
        
        return R.ok(dataMap);
    }

    @GetMapping("/vehicleQuery")
    @ResponseBody
    public R<Map<String, Object>> vehicleQueryList(VehicleQuery vehicleQuery) throws ParseException {
        Integer count = 0;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ArrayList<Object> appointmentList = new ArrayList<>();
        List<Appointment> appointmentAll = appointmentService.vehicleQueryList(vehicleQuery.getOpenid(), vehicleQuery.getPlatenumber(), vehicleQuery.getLeavedate());
        Iterator<Appointment> iter = appointmentAll.iterator();
        while (iter.hasNext()) {
            Appointment appointment = iter.next();
            Map<String, Object> appointmentMap = new HashMap<>();
            appointmentMap.put("id", appointment.getId());
            appointmentMap.put("province", appointment.getProvince());
            appointmentMap.put("city", appointment.getCity());
            appointmentMap.put("district", appointment.getDistrict());
            appointmentMap.put("community", appointment.getCommunity());
            // visitdate 现在是 String 类型，可能包含区间格式，直接使用不需要转换
            appointmentMap.put("visitdate", appointment.getVisitdate());
            appointmentMap.put("recorddate", ResultUtil.convertDate(appointment.getRecorddate().toString()));
            appointmentMap.put("visitorphone", appointment.getVisitorphone());
            appointmentMap.put("cartype", appointment.getCartype());
            appointmentMap.put("platenumber", appointment.getPlatenumber());
            appointmentMap.put("status", appointment.getStatus());
            appointmentMap.put("openid", appointment.getOpenid());
            appointmentMap.put("building", appointment.getBuilding());
            appointmentMap.put("units", appointment.getUnits());
            appointmentMap.put("floor", appointment.getFloor());
            appointmentMap.put("room", appointment.getRoom());
            appointmentMap.put("ordernumber", appointment.getOrdernumber());
            appointmentMap.put("ownername", appointment.getOwnername());
            appointmentMap.put("ownerphone", appointment.getOwnerphone());
            appointmentMap.put("visitreason", appointment.getVisitreason());
            appointmentMap.put("appointtype", appointment.getAppointtype());
            appointmentMap.put("auditstatus", appointment.getAuditstatus());
            appointmentMap.put("refusereason", appointment.getRefusereason());
            appointmentMap.put("venuestatus", appointment.getVenuestatus());
            // 解析原始日期时间字符串
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime arriveDateTime = LocalDateTime.parse(appointment.getArrivedate().toString(), inputFormatter);
            LocalDateTime LeaveDateTime = LocalDateTime.parse(appointment.getLeavedate().toString(), inputFormatter);
            // 格式化新的日期时间对象为目标格式的字符串
            // 计算停车时长 arrivedate 进场时间 leavedate 离场时间
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedArriveDateTime = arriveDateTime.format(outputFormatter);
            String formattedLeaveDateTime = LeaveDateTime.format(outputFormatter);
            Duration duration = Duration.between(LocalDateTime.parse(formattedArriveDateTime,outputFormatter), LocalDateTime.parse(formattedLeaveDateTime,outputFormatter));
            long millis = duration.toMillis();
            long diffHours = TimeUnit.HOURS.convert(millis, TimeUnit.MILLISECONDS);
            long diffMinutes = TimeUnit.MINUTES.convert(millis - TimeUnit.HOURS.toMillis(diffHours), TimeUnit.MILLISECONDS);
            long diffSeconds = TimeUnit.SECONDS.convert(millis - TimeUnit.HOURS.toMillis(diffHours) - TimeUnit.MINUTES.toMillis(diffMinutes), TimeUnit.MILLISECONDS);
            String result = "";
            if (diffHours >= 24) {
                long days = diffHours / 24;
                diffHours %= 24;
                result = days + "天" + diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
            } else {
                if (diffHours == 0) {
                    result = diffMinutes + "分钟" + diffSeconds + "秒";
                } else {
                    result = diffHours + "小时" + diffMinutes + "分钟" + diffSeconds + "秒";
                }
            }

            if (appointment.getArrivedate() == null) {
                appointmentMap.put("arrivedate", "");
            } else {
                appointmentMap.put("arrivedate", ResultUtil.convertDate(appointment.getArrivedate().toString()));
            }
            if (appointment.getLeavedate() == null) {
                appointmentMap.put("leavedate", "");
            } else {
                appointmentMap.put("leavedate", ResultUtil.convertDate(appointment.getLeavedate().toString()));
            }
            appointmentMap.put("auditusername", appointment.getAuditusername());
            if (appointment.getAuditdate() == null) {
                appointmentMap.put("auditdate", "");
            } else {
                appointmentMap.put("auditdate", ResultUtil.convertDate(appointment.getAuditdate().toString()));
            }
            appointmentMap.put("result",result);
            appointmentList.add(appointmentMap);
        }
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("data", appointmentList);
        return R.ok(dataMap);


    }

    @GetMapping("/subAppointQueryList")
    @ResponseBody
    public R<Map<String, Object>> subAppointQueryList(SubAppointQueryDuration subAppointQueryDuration) throws ParseException {
        ArrayList<Object> appointmentList = new ArrayList<>();
        ArrayList<Appointment> appointments = new ArrayList<>();
        List<Appointment> appointmentAll = appointmentService.subAppointQueryList(subAppointQueryDuration.getOpenid(), subAppointQueryDuration.getPlatenumber(), subAppointQueryDuration.getVisitorphone(),
                subAppointQueryDuration.getVisitdateBegin(), subAppointQueryDuration.getVisitdateEnd(),subAppointQueryDuration.getRecorddateBegin(),subAppointQueryDuration.getRecorddateEnd());
        Iterator<Appointment> iter = appointmentAll.iterator();
        while (iter.hasNext()) {
            Appointment appointment = iter.next();
            Map<String, Object> appointmentMap = new HashMap<>();
            appointmentMap.put("id", appointment.getId());
            appointmentMap.put("province", appointment.getProvince());
            appointmentMap.put("city", appointment.getCity());
            appointmentMap.put("district", appointment.getDistrict());
            appointmentMap.put("community", appointment.getCommunity());
            // visitdate 现在是 String 类型，可能包含区间格式，直接使用不需要转换
            appointmentMap.put("visitdate", appointment.getVisitdate());
            appointmentMap.put("recorddate", ResultUtil.convertDate(appointment.getRecorddate().toString()));
            appointmentMap.put("visitorphone", appointment.getVisitorphone());
            appointmentMap.put("cartype", appointment.getCartype());
            appointmentMap.put("platenumber", appointment.getPlatenumber());
            appointmentMap.put("status", appointment.getStatus());
            appointmentMap.put("openid", appointment.getOpenid());
            appointmentMap.put("building", appointment.getBuilding());
            appointmentMap.put("units", appointment.getUnits());
            appointmentMap.put("floor", appointment.getFloor());
            appointmentMap.put("room", appointment.getRoom());
            appointmentMap.put("ordernumber", appointment.getOrdernumber());
            appointmentMap.put("ownername", appointment.getOwnername());
            appointmentMap.put("ownerphone", appointment.getOwnerphone());
            appointmentMap.put("visitreason", appointment.getVisitreason());
            appointmentMap.put("appointtype", appointment.getAppointtype());
            appointmentMap.put("auditstatus", appointment.getAuditstatus());
            appointmentMap.put("refusereason", appointment.getRefusereason());
            appointmentMap.put("venuestatus", appointment.getVenuestatus());
            if (appointment.getArrivedate() == null) {
                appointmentMap.put("arrivedate", "");
            } else {
                appointmentMap.put("arrivedate", ResultUtil.convertDate(appointment.getArrivedate().toString()));
            }
            if (appointment.getLeavedate() == null) {
                appointmentMap.put("leavedate", "");
            } else {
                appointmentMap.put("leavedate", ResultUtil.convertDate(appointment.getLeavedate().toString()));
            }
            appointmentMap.put("auditusername", appointment.getAuditusername());
            if (appointment.getAuditdate() == null) {
                appointmentMap.put("auditdate", "");
            } else {
                appointmentMap.put("auditdate", ResultUtil.convertDate(appointment.getAuditdate().toString()));
            }
            appointmentList.add(appointmentMap);
        }
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("data", appointmentList);
        return R.ok(dataMap);
    }

    @GetMapping("/auditQueryList")
    @ResponseBody
    public R<Map<String, Object>> auditQueryList(SubAppointQueryDuration subAppointQueryDuration) throws ParseException {
        ArrayList<Object> appointmentList = new ArrayList<>();
        List<Appointment> appointmentAll = appointmentService.auditQueryList(subAppointQueryDuration.getOpenid(), subAppointQueryDuration.getPlatenumber(), subAppointQueryDuration.getVisitorphone(),
                subAppointQueryDuration.getVisitdateBegin(), subAppointQueryDuration.getVisitdateEnd(),subAppointQueryDuration.getRecorddateBegin(),subAppointQueryDuration.getRecorddateEnd());
        Iterator<Appointment> iter = appointmentAll.iterator();
        while (iter.hasNext()) {
            Appointment appointment = iter.next();
            Map<String, Object> appointmentMap = new HashMap<>();
            appointmentMap.put("id", appointment.getId());
            appointmentMap.put("province", appointment.getProvince());
            appointmentMap.put("city", appointment.getCity());
            appointmentMap.put("district", appointment.getDistrict());
            appointmentMap.put("community", appointment.getCommunity());
            // visitdate 现在是 String 类型，可能包含区间格式，直接使用不需要转换
            appointmentMap.put("visitdate", appointment.getVisitdate());
            appointmentMap.put("recorddate", ResultUtil.convertDate(appointment.getRecorddate().toString()));
            appointmentMap.put("visitorphone", appointment.getVisitorphone());
            appointmentMap.put("cartype", appointment.getCartype());
            appointmentMap.put("platenumber", appointment.getPlatenumber());
            appointmentMap.put("status", appointment.getStatus());
            appointmentMap.put("openid", appointment.getOpenid());
            appointmentMap.put("building", appointment.getBuilding());
            appointmentMap.put("units", appointment.getUnits());
            appointmentMap.put("floor", appointment.getFloor());
            appointmentMap.put("room", appointment.getRoom());
            appointmentMap.put("ordernumber", appointment.getOrdernumber());
            appointmentMap.put("ownername", appointment.getOwnername());
            appointmentMap.put("ownerphone", appointment.getOwnerphone());
            appointmentMap.put("visitreason", appointment.getVisitreason());
            appointmentMap.put("appointtype", appointment.getAppointtype());
            appointmentMap.put("auditstatus", appointment.getAuditstatus());
            appointmentMap.put("refusereason", appointment.getRefusereason());
            appointmentMap.put("parkingDuration", appointment.getParkingDuration());
            appointmentMap.put("venuestatus", appointment.getVenuestatus());
            if (appointment.getArrivedate() == null) {
                appointmentMap.put("arrivedate", "");
            } else {
                appointmentMap.put("arrivedate", ResultUtil.convertDate(appointment.getArrivedate()));
            }
            if (appointment.getLeavedate() == null) {
                appointmentMap.put("leavedate", "");
            } else {
                appointmentMap.put("leavedate", appointment.getLeavedate());
            }
            appointmentMap.put("auditusername", appointment.getAuditusername());
            if (appointment.getAuditdate() == null) {
                appointmentMap.put("auditdate", "");
            } else {
                appointmentMap.put("auditdate", appointment.getAuditdate().toString());
            }
            appointmentList.add(appointmentMap);
        }
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("data", appointmentList);
        return R.ok(dataMap);
    }

    @ApiOperation("审批")
    @PostMapping("/auditAppoint")
    public ResponseEntity<Result> auditAppoint(@RequestBody Appointment appointment) {
        if (appointment.getAuditstatus().equals("已通过")) {
            // 审核状态修改
            Appointment appointmentTmp = appointmentService.getById(appointment.getId());
            appointmentTmp.setAuditopenid(appointment.getAuditopenid());
            appointmentTmp.setAuditusername(appointment.getAuditusername());
            appointmentTmp.setAuditdate(appointment.getAuditdate());
            appointmentTmp.setAuditstatus(appointment.getAuditstatus());
            appointmentTmp.setVenuestatus("待入场");
            appointmentTmp.setRefusereason(appointment.getRefusereason());
            appointmentService.updateById(appointmentTmp);
            
            // 🔔 发送预约审核通过通知给访客
            try {
                sendAppointmentAuditResultNotification(appointmentTmp, "已通过", appointment.getRefusereason(), appointment.getAuditusername());
            } catch (Exception e) {
                logger.warn("⚠️ [预约审核通过通知发送失败] 预约ID={}, 车牌={}, 错误={}", 
                    appointmentTmp.getId(), appointmentTmp.getPlatenumber(), e.getMessage());
                // 通知发送失败不影响审核操作的成功状态
            }
        } else if (appointment.getAuditstatus().equals("未通过")) {
            Appointment appointmentTmp = appointmentService.getById(appointment.getId());
            appointmentTmp.setAuditopenid(appointment.getAuditopenid());
            appointmentTmp.setAuditusername(appointment.getAuditusername());
            appointmentTmp.setAuditdate(appointment.getAuditdate());
            appointmentTmp.setAuditstatus(appointment.getAuditstatus());
            appointmentTmp.setVenuestatus("未进场");
            appointmentTmp.setRefusereason(appointment.getRefusereason());
            appointmentService.updateById(appointmentTmp);
            
            // 🔔 发送预约审核驳回通知给访客
            try {
                sendAppointmentAuditResultNotification(appointmentTmp, "未通过", appointment.getRefusereason(), appointment.getAuditusername());
            } catch (Exception e) {
                logger.warn("⚠️ [预约审核驳回通知发送失败] 预约ID={}, 车牌={}, 错误={}", 
                    appointmentTmp.getId(), appointmentTmp.getPlatenumber(), e.getMessage());
                // 通知发送失败不影响审核操作的成功状态
            }
        }
        return ResponseEntity.ok(Result.success());
    }

    @ApiOperation("分页查询")
    @GetMapping("/allpage")
    public IPage<Appointment> allPage(
            @RequestParam(required = false) String community,
            @RequestParam(required = false) String plateNumber,
            @RequestParam(required = false) String visitdate,
            @RequestParam(required = false) String auditstatus,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Appointment> appointmentList = appointmentService.allpage(community, plateNumber, visitdate, auditstatus);
        //按照设备名和申请日期排序
        List<Appointment> asServices = appointmentList.stream().sorted(Comparator.comparing(Appointment::getProvince).
                thenComparing(Appointment::getCity).thenComparing(Appointment::getDistrict).thenComparing(Appointment::getCommunity).
                thenComparing(Appointment::getBuilding).thenComparing(Appointment::getUnits)
                .thenComparing(Appointment::getVisitdate)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }

    @ApiOperation("入场查询")
    @GetMapping("/venuepage")
    public IPage<Appointment> venuePage(
            @RequestParam(required = false) String community,
            @RequestParam(required = false) String plateNumber,
            @RequestParam(required = false) String arrivedate,
            @RequestParam(required = false) String leavedate,
            @RequestParam(required = false) String venuestatus,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Appointment> appointmentList = appointmentService.venuepage(community, plateNumber, arrivedate, leavedate, venuestatus);
        //按照设备名和申请日期排序
        List<Appointment> asServices = appointmentList.stream().sorted(Comparator.comparing(Appointment::getProvince).
                thenComparing(Appointment::getCity).thenComparing(Appointment::getDistrict).thenComparing(Appointment::getCommunity).
                thenComparing(Appointment::getBuilding).thenComparing(Appointment::getUnits)
        ).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }

    @ApiOperation("按地址查询预约记录")
    @GetMapping("/listByAddress")
    @ResponseBody
    public Result listByAddress(
            @RequestParam(required = false) String community,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String units,
            @RequestParam(required = false) String floor,
            @RequestParam(required = false) String room) {
        logger.info("🏠 [地址查询] 查询参数: community={}, building={}, units={}, floor={}, room={}",
                   community, building, units, floor, room);
        try {
            List<Appointment> appointmentList = appointmentService.listByAddress(community, building, units, floor, room);
            logger.info("🏠 [地址查询] 查询结果数量: {}", appointmentList.size());
            return Result.success(appointmentList);
        } catch (Exception e) {
            logger.error("🏠 [地址查询] 查询失败: ", e);
            return Result.error("查询地址预约记录失败: " + e.getMessage());
        }
    }

    /**
     * 基于时间段判断是否需要审核
     * @param communityName 社区名称
     * @param visitDate 预约时间段（格式：YYYY-MM-DD HH:mm:ss - YYYY-MM-DD HH:mm:ss）
     * @return true-需要审核，false-不需要审核
     */
    private boolean checkIfTimeSlotNeedsAudit(String communityName, String visitDate) {
        try {
            // 1. 查询社区的审核配置信息
            Community community = communityService.findProvinceByCommunityName(communityName, null, null, null, null);
            if (community == null) {
                logger.warn("未找到社区配置信息: {}", communityName);
                // 如果找不到配置，回退到原有逻辑
                String isAudit = communityService.findIsAuditByCommunityName(communityName);
                return "是".equals(isAudit);
            }
            
            // 2. 检查是否启用了基于时间的审核（这里需要根据实际的Community字段调整）
            // 首先尝试获取审核时间配置，如果没有配置则回退到简单审核模式
            String auditStartTime = getAuditStartTime(community);
            String auditEndTime = getAuditEndTime(community);
            
            if (auditStartTime == null || auditEndTime == null || 
                auditStartTime.trim().isEmpty() || auditEndTime.trim().isEmpty()) {
                // 没有时间段配置，回退到原有的简单审核逻辑
                String isAudit = communityService.findIsAuditByCommunityName(communityName);
                boolean needsAudit = "是".equals(isAudit);
                logger.info("使用简单审核模式: 社区={}, 需要审核={}", communityName, needsAudit);
                return needsAudit;
            }
            
            // 3. 解析预约时间段
            TimeSlot appointmentTimeSlot = parseTimeSlot(visitDate);
            if (appointmentTimeSlot == null) {
                logger.error("预约时间段格式错误: {}", visitDate);
                return true; // 格式错误时默认需要审核
            }
            
            // 4. 解析审核时间段
            TimeSlot auditTimeSlot = parseAuditTimeSlot(auditStartTime, auditEndTime);
            if (auditTimeSlot == null) {
                logger.warn("社区审核时间段配置无效: {}", communityName);
                return false; // 配置无效时默认不需要审核
            }
            
            // 5. 判断预约时间是否在审核时间段内
            boolean needsAudit = isTimeSlotOverlap(appointmentTimeSlot, auditTimeSlot);
            
            logger.info("时间段审核判断结果: 社区={}, 预约时间={}, 审核时间段={}-{}, 需要审核={}", 
                communityName, visitDate, auditStartTime, auditEndTime, needsAudit);
            
            return needsAudit;
            
        } catch (Exception e) {
            logger.error("判断时间段审核失败: communityName={}, visitDate={}, error={}", 
                communityName, visitDate, e.getMessage(), e);
            // 异常情况下回退到原有逻辑
            try {
                String isAudit = communityService.findIsAuditByCommunityName(communityName);
                return "是".equals(isAudit);
            } catch (Exception fallbackException) {
                logger.error("回退到简单审核逻辑也失败", fallbackException);
                return true; // 最终异常情况下默认需要审核
            }
        }
    }
    
    /**
     * 从Community对象中获取审核开始时间
     * 注意：这个方法需要根据实际的Community实体字段进行调整
     */
    private String getAuditStartTime(Community community) {
        // TODO: 根据实际的Community实体字段调整
        // 示例：可能的字段名
        try {
            // 尝试通过反射或直接方法调用获取审核开始时间
            // 这里需要根据实际的Community实体字段名调整
            
            // 方案1：如果有直接的getter方法
            // return community.getAuditStartTime();
            
            // 方案2：如果字段名不同，需要使用反射或其他方式
            // 暂时返回null，表示没有配置时间段审核
            return null;
            
        } catch (Exception e) {
            logger.warn("获取审核开始时间失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从Community对象中获取审核结束时间
     * 注意：这个方法需要根据实际的Community实体字段进行调整
     */
    private String getAuditEndTime(Community community) {
        // TODO: 根据实际的Community实体字段调整
        try {
            // 方案1：如果有直接的getter方法
            // return community.getAuditEndTime();
            
            // 方案2：暂时返回null，表示没有配置时间段审核
            return null;

        } catch (Exception e) {
            logger.warn("获取审核结束时间失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析预约时间段
     * @param visitDate 预约时间段字符串（如：2025-12-28 08:00:00 - 2025-12-28 10:00:00）
     * @return TimeSlot对象
     */
    private TimeSlot parseTimeSlot(String visitDate) {
        try {
            if (visitDate == null || visitDate.trim().isEmpty()) {
                return null;
            }
            
            // 检查是否包含时间区间分隔符
            if (visitDate.contains(" - ")) {
                String[] parts = visitDate.split(" - ");
                if (parts.length == 2) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date startTime = sdf.parse(parts[0].trim());
                    Date endTime = sdf.parse(parts[1].trim());
                    return new TimeSlot(startTime, endTime);
                }
            } else {
                // 如果不是区间格式，尝试解析为单个时间点
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date time = sdf.parse(visitDate.trim());
                // 单个时间点视为1小时时间段
                Date endTime = new Date(time.getTime() + 60 * 60 * 1000);
                return new TimeSlot(time, endTime);
            }
            
        } catch (ParseException e) {
            logger.error("解析预约时间段失败: {}", visitDate, e);
        }
        return null;
    }
    
    /**
     * 解析审核时间段
     * @param auditStartTime 审核开始时间字符串（如：08:00:00）
     * @param auditEndTime 审核结束时间字符串（如：18:00:00）
     * @return TimeSlot对象
     */
    private TimeSlot parseAuditTimeSlot(String auditStartTime, String auditEndTime) {
        try {
            if (auditStartTime == null || auditEndTime == null || 
                auditStartTime.trim().isEmpty() || auditEndTime.trim().isEmpty()) {
                return null;
            }
            // 解析时间格式（假设格式为 HH:mm:ss 或 HH:mm）
            SimpleDateFormat timeFormat = auditStartTime.contains(":") && auditStartTime.split(":").length >= 3 
                ? new SimpleDateFormat("HH:mm:ss") 
                : new SimpleDateFormat("HH:mm");
            // 创建今天的日期作为基准
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String todayStr = dateFormat.format(new Date());
            SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            // 构建完整的开始和结束时间
            String startTimeStr = todayStr + " " + auditStartTime + (auditStartTime.split(":").length < 3 ? ":00" : "");
            String endTimeStr = todayStr + " " + auditEndTime + (auditEndTime.split(":").length < 3 ? ":00" : "");
            Date startTime = fullFormat.parse(startTimeStr);
            Date endTime = fullFormat.parse(endTimeStr);
            // 处理跨天的情况（如果结束时间小于开始时间，则结束时间是第二天）
            if (endTime.before(startTime)) {
                endTime = new Date(endTime.getTime() + 24 * 60 * 60 * 1000);
            }
            return new TimeSlot(startTime, endTime);
        } catch (Exception e) {
            logger.error("解析审核时间段失败: auditStartTime={}, auditEndTime={}, error={}", 
                auditStartTime, auditEndTime, e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * 判断两个时间段是否有重叠
     * @param appointmentSlot 预约时间段
     * @param auditSlot 审核时间段
     * @return true-有重叠（需要审核），false-无重叠（不需要审核）
     */
    private boolean isTimeSlotOverlap(TimeSlot appointmentSlot, TimeSlot auditSlot) {
        try {
            // 提取时间部分进行比较（忽略日期）
            int appointmentStart = getTimeOfDay(appointmentSlot.startTime);
            int appointmentEnd = getTimeOfDay(appointmentSlot.endTime);
            int auditStart = getTimeOfDay(auditSlot.startTime);
            int auditEnd = getTimeOfDay(auditSlot.endTime);
            
            // 处理跨天的审核时间段
            if (auditEnd < auditStart) {
                // 审核时间跨天（如：22:00 - 06:00）
                return (appointmentStart >= auditStart || appointmentEnd <= auditEnd) ||
                       (appointmentEnd > auditStart || appointmentStart < auditEnd);
            } else {
                // 正常的审核时间段（如：08:00 - 18:00）
                return !(appointmentEnd <= auditStart || appointmentStart >= auditEnd);
            }
            
        } catch (Exception e) {
            logger.error("判断时间段重叠失败", e);
            return true; // 异常情况下默认需要审核
        }
    }
    
    /**
     * 获取一天中的时间（分钟数）
     * @param time 时间
     * @return 从0点开始的分钟数
     */
    private int getTimeOfDay(Date time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }
    
    /**
     * 从访客openid获取关联的管家openid
     * 通过查询二维码使用记录表，找到生成二维码的管家信息
     * @param visitorOpenid 访客的openid
     * @return 管家的openid，如果未找到则返回null
     */
    private String getButlerOpenidFromVisitor(String visitorOpenid) {
        try {
            System.out.println("visitorOpenid = " + visitorOpenid);
            // 通过访客openid查询二维码使用记录
            QrCodeUsage qrCodeUsage = qrCodeUsageService.findByVisitorOpenid(visitorOpenid);
            if (qrCodeUsage != null && qrCodeUsage.getButlerPhone() != null) {
                // 通过管家手机号查询管家信息，获取openid
                Butler butler = butlerService.getButlerByPhone(qrCodeUsage.getButlerPhone());
                if (butler != null) {
                    logger.info("通过二维码记录找到管家: 管家手机号={}, 管家姓名={}, 管家openid={}",
                        qrCodeUsage.getButlerPhone(), butler.getUsername(), butler.getOpenid());
                    return butler.getOpenid();
                } else {
                    logger.warn("未找到管家信息: 管家手机号={}", qrCodeUsage.getButlerPhone());
                }
            } else {
                logger.warn("未找到访客的二维码使用记录: visitorOpenid={}", visitorOpenid);
            }
        } catch (Exception e) {
            logger.error("获取管家openid失败: visitorOpenid=" + visitorOpenid, e);
        }
        return null;
    }

    /**
     * 时间段内部类
     */
    private static class TimeSlot {
        Date startTime;
        Date endTime;

        TimeSlot(Date startTime, Date endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    /**
     * 检查车牌号是否在黑名单中
     * @param plateNumber 车牌号
     * @param community 社区名称
     * @return true-在黑名单中，false-不在黑名单中
     */
    private boolean checkBlacklistStatus(String plateNumber, String community) {
        try {
            // 根据社区名称获取停车场编码
            String parkCode = getParkCodeByCommunity(community);

            // 构建请求参数，参考月票查询的方式
            HashMap<String, Object> params = new HashMap<>();
            params.put("parkCodeList", Arrays.asList(parkCode));
            params.put("pageNum", 1);
            params.put("pageSize", 1000);
            params.put("carCode", plateNumber);

            logger.info("🚫 调用黑名单查询接口，参数: {}", params);

            // 调用黑名单查询接口
            JSONObject response = aikeConfig.downHandler(
                AIKEConfig.AK_URL,
                AIKEConfig.AK_KEY,
                AIKEConfig.AK_SECRET,
                "getParkBlackList",
                params
            );

            logger.info("🚫 黑名单查询接口响应: {}", response);

            // 解析响应结果
            if (response != null) {
                // 检查响应状态 - 使用 resultCode 而不是 code
                Integer resultCode = response.getInteger("resultCode");
                Integer status = response.getInteger("status");

                if ((resultCode != null && resultCode == 0) && (status != null && status == 1)) {
                    // 获取数据部分 - 直接从 data 中获取 recordList
                    JSONObject data = response.getJSONObject("data");
                    if (data != null) {
                        JSONArray recordList = data.getJSONArray("recordList");
                        if (recordList != null && recordList.size() > 0) {
                            // 找到黑名单记录，检查车牌号是否匹配
                            for (int i = 0; i < recordList.size(); i++) {
                                JSONObject record = recordList.getJSONObject(i);
                                String carCode = record.getString("carCode");
                                if (plateNumber.equals(carCode)) {
                                    logger.warn("🚫 发现黑名单车牌: {} - 原因: {}", plateNumber, record.getString("reason"));
                                    return true;
                                }
                            }
                        }
                        logger.info("🚫 黑名单查询完成，车牌 {} 不在黑名单中，共查询到 {} 条记录",
                                   plateNumber, recordList != null ? recordList.size() : 0);
                    }
                } else {
                    logger.warn("🚫 黑名单查询接口返回错误: resultCode={}, status={}, message={}",
                               resultCode, status, response.getString("message"));
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("🚫 黑名单校验异常: {}", e.getMessage(), e);
            // 异常情况下，为了安全起见，可以选择拒绝预约或者允许预约
            // 这里选择允许预约，但记录异常日志
            return false;
        }
    }

    /**
     * 根据社区名称获取停车场编码
     * @param community 社区名称
     * @return 停车场编码
     */
    private String getParkCodeByCommunity(String community) {
        try {
            // 使用 YardInfoService 动态查询车场编码
            List<String> yardCodes = yardInfoService.yardCode(community);

            if (yardCodes != null && !yardCodes.isEmpty()) {
                // 如果查询到结果，返回第一个车场编码
                String parkCode = yardCodes.get(0);
                logger.info("通过车场名称 {} 查询到车场编码: {}", community, parkCode);
                return parkCode;
            } else {
                logger.warn("未找到车场名称 {} 对应的车场编码，使用默认编码", community);
            }
        } catch (Exception e) {
            logger.error("查询车场编码失败，车场名称: {}, 错误: {}", community, e.getMessage(), e);
        }

        // 如果查询失败或没有结果，使用原有的硬编码映射作为备用方案
        if ("万象上东".equals(community)) {
            return "2KST9MNP";
        } else if ("四季上东".equals(community)) {
            return "2KUG6XLU";
        } else {
            // 默认使用四季上东的编码
            return "2KUG6XLU";
        }
    }

    /**
     * 检查车牌号是否存在未处理的违规记录
     * @param plateNumber 车牌号
     * @return true-存在未处理的违规记录，false-无未处理的违规记录
     */
    private boolean checkViolationStatus(String plateNumber) {
        try {
            // 查询该车牌号的未处理违规记录（状态为：待处理、处理中等）
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.parkingmanage.entity.Violations> queryWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            queryWrapper.eq("plate_number", plateNumber)
                       .in("status", "待处理", "处理中", "未处理", "pending", "processing");
            
            long count = violationsService.count(queryWrapper);
            
            if (count > 0) {
                logger.warn("🚫 发现车牌 {} 存在 {} 条未处理的违规记录", plateNumber, count);
                return true;
            }
            
            logger.info("✅ 车牌 {} 无未处理的违规记录", plateNumber);
            return false;
        } catch (Exception e) {
            logger.error("🚫 违规记录查询异常: {}", e.getMessage(), e);
            // 异常情况下，为了安全起见，选择拒绝预约
            return true;
        }
    }

    /**
     * 🆕 更新预约记录的访客微信姓名
     * @param requestBody 包含appointmentId和visitorName的请求体
     * @return 更新结果
     */
    @PostMapping("/updateVisitorName")
    @ApiOperation("更新预约记录的访客微信姓名")
    public ResponseEntity<Result> updateVisitorName(@RequestBody Map<String, Object> requestBody) {
        try {
            Integer appointmentId = (Integer) requestBody.get("appointmentId");
            String visitorName = (String) requestBody.get("visitorName");
            
            logger.info("🆕 [更新访客姓名] 收到请求 - appointmentId: {}, visitorName: {}", appointmentId, visitorName);
            
            // 参数校验
            if (appointmentId == null || visitorName == null || visitorName.trim().isEmpty()) {
                logger.warn("❌ [更新访客姓名] 参数无效 - appointmentId: {}, visitorName: {}", appointmentId, visitorName);
                return ResponseEntity.ok(Result.error("参数无效：预约ID和访客姓名不能为空"));
            }
            
            // 更新数据库
            int result = appointmentService.updateVisitorNameByAppointment(appointmentId, visitorName.trim());
            
            if (result > 0) {
                logger.info("✅ [更新访客姓名] 更新成功 - appointmentId: {}, visitorName: {}", appointmentId, visitorName);
                
                // 返回成功结果
                Map<String, Object> data = new HashMap<>();
                data.put("updated", true);
                data.put("appointmentId", appointmentId);
                data.put("visitorName", visitorName.trim());
                
                return ResponseEntity.ok(Result.success(data));
            } else {
                logger.warn("⚠️ [更新访客姓名] 未找到对应的预约记录 - appointmentId: {}", appointmentId);
                return ResponseEntity.ok(Result.error("未找到对应的预约记录，请检查预约ID是否正确"));
            }
        } catch (Exception e) {
            logger.error("❌ [更新访客姓名] 更新失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("更新访客姓名失败，请稍后重试"));
        }
    }
    
    /**
     * 🆕 根据手机号更新最新预约记录的访客微信姓名
     * @param requestBody 包含visitorPhone和visitorName的请求体
     * @return 更新结果
     */
    @PostMapping("/updateVisitorNameByPhone")
    @ApiOperation("根据手机号更新最新预约记录的访客微信姓名")
    public ResponseEntity<Result> updateVisitorNameByPhone(@RequestBody Map<String, Object> requestBody) {
        try {
            String visitorPhone = (String) requestBody.get("visitorPhone");
            String visitorName = (String) requestBody.get("visitorName");
            
            logger.info("🆕 [根据手机号更新访客姓名] 收到请求 - visitorPhone: {}, visitorName: {}", visitorPhone, visitorName);
            
            // 参数校验
            if (visitorPhone == null || visitorPhone.trim().isEmpty() || 
                visitorName == null || visitorName.trim().isEmpty()) {
                logger.warn("❌ [根据手机号更新访客姓名] 参数无效 - visitorPhone: {}, visitorName: {}", visitorPhone, visitorName);
                return ResponseEntity.ok(Result.error("参数无效：手机号和访客姓名不能为空"));
            }
            
            // 更新数据库
            int result = appointmentService.updateVisitorNameByPhone(visitorPhone.trim(), visitorName.trim());
            
            if (result > 0) {
                logger.info("✅ [根据手机号更新访客姓名] 更新成功 - visitorPhone: {}, visitorName: {}", visitorPhone, visitorName);
                
                // 返回成功结果
                Map<String, Object> data = new HashMap<>();
                data.put("updated", true);
                data.put("visitorPhone", visitorPhone.trim());
                data.put("visitorName", visitorName.trim());
                
                return ResponseEntity.ok(Result.success(data));
            } else {
                logger.warn("⚠️ [根据手机号更新访客姓名] 未找到对应的预约记录 - visitorPhone: {}", visitorPhone);
                return ResponseEntity.ok(Result.error("未找到对应的预约记录，请检查手机号是否正确"));
            }
        } catch (Exception e) {
            logger.error("❌ [根据手机号更新访客姓名] 更新失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("更新访客姓名失败，请稍后重试"));
        }
    }
    
    /**
     * 🆕 根据openid更新最新预约记录的访客微信姓名
     * @param requestBody 包含openid和visitorName的请求体
     * @return 更新结果
     */
    @PostMapping("/updateVisitorNameByOpenid")
    @ApiOperation("根据openid更新最新预约记录的访客微信姓名")
    public ResponseEntity<Result>  updateVisitorNameByOpenid(@RequestBody Map<String, Object> requestBody) {
        try {
            String openid = (String) requestBody.get("openid");
            String visitorName = (String) requestBody.get("visitorName");
            
            logger.info("🆕 [根据openid更新访客姓名] 收到请求 - openid: {}, visitorName: {}", openid, visitorName);
            
            // 参数校验
            if (openid == null || openid.trim().isEmpty() || 
                visitorName == null || visitorName.trim().isEmpty()) {
                logger.warn("❌ [根据openid更新访客姓名] 参数无效 - openid: {}, visitorName: {}", openid, visitorName);
                return ResponseEntity.ok(Result.error("参数无效：openid和访客姓名不能为空"));
            }
            
            // 更新数据库
            int result = appointmentService.updateVisitorNameByOpenid(openid.trim(), visitorName.trim());
            
            if (result > 0) {
                logger.info("✅ [根据openid更新访客姓名] 更新成功 - openid: {}, visitorName: {}", openid, visitorName);
                
                // 返回成功结果
                Map<String, Object> data = new HashMap<>();
                data.put("updated", true);
                data.put("openid", openid.trim());
                data.put("visitorName", visitorName.trim());
                
                return ResponseEntity.ok(Result.success(data));
            } else {
                logger.warn("⚠️ [根据openid更新访客姓名] 未找到对应的预约记录 - openid: {}", openid);
                return ResponseEntity.ok(Result.error("未找到对应的预约记录，请检查openid是否正确"));
            }
        } catch (Exception e) {
            logger.error("❌ [根据openid更新访客姓名] 更新失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("更新访客姓名失败，请稍后重试"));
        }
    }
    
    /**
     * 🔔 发送预约待审核微信提醒给所有管家
     * @param appointment 预约记录
     */
    private void sendBookingPendingNotification(Appointment appointment) {
        try {
            logger.info("🔔 [开始发送预约待审核提醒] 车牌: {}, 预约类型: {}", 
                    appointment.getPlatenumber(), appointment.getAppointtype());
            
            // 参数校验
            if (appointment.getPlatenumber() == null || appointment.getPlatenumber().trim().isEmpty()) {
                logger.warn("⚠️ [预约待审核提醒跳过] 车牌号为空");
                return;
            }
            
            // 构建通知参数
            String plateNumber = appointment.getPlatenumber();
            String parkName = appointment.getCommunity() != null ? appointment.getCommunity() : "停车场";
//            String bookerName = appointment.getVisitorname() != null ? appointment.getVisitorname() : "访客";
            String contactPhone = appointment.getVisitorphone() != null ? appointment.getVisitorphone() : "";
            
            // 获取社区的所有管家
            List<Butler> butlers = getAllManagersForAppointment(appointment);
            if (butlers == null || butlers.isEmpty()) {
                logger.warn("⚠️ [预约待审核提醒跳过] 未找到社区 {} 的管家信息 - 车牌: {}", appointment.getCommunity(), plateNumber);
                return;
            }
            
            logger.info("🔔 [预约待审核提醒] 找到 {} 个管家，准备发送通知", butlers.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            // 给每个管家发送通知
            for (Butler butler : butlers) {
                try {
                    String managerNickname = butler.getUsername() != null ? butler.getUsername() : "管家";
                    
                    logger.info("🔔 [预约待审核提醒] 正在给管家 {} 发送通知...", managerNickname);
                    
                    // 发送微信通知
                    Map<String, Object> result = weChatTemplateMessageService.sendBookingPendingNotification(
                            plateNumber,
                            parkName,
                            contactPhone,
                            managerNickname
                    );
                    
                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                        logger.info("✅ [预约待审核提醒发送成功] 车牌: {}, 管家: {}", plateNumber, managerNickname);
                        successCount++;
                    } else {
                        String message = result != null ? (String) result.get("message") : "未知错误";
                        logger.warn("⚠️ [预约待审核提醒发送失败] 车牌: {}, 管家: {}, 原因: {}", plateNumber, managerNickname, message);
                        failureCount++;
                    }
                    
                } catch (Exception e) {
                    logger.error("❌ [预约待审核提醒发送异常] 车牌: {}, 管家: {}, 错误: {}", 
                            plateNumber, butler.getUsername(), e.getMessage(), e);
                    failureCount++;
                }
            }
            
            logger.info("🔔 [预约待审核提醒汇总] 车牌: {}, 总管家数: {}, 成功发送: {}, 发送失败: {}", 
                    plateNumber, butlers.size(), successCount, failureCount);
            
        } catch (Exception e) {
            logger.error("❌ [预约待审核提醒发送异常] 车牌: {}, 错误: {}", appointment.getPlatenumber(), e.getMessage(), e);
        }
    }

    /**
     * 🔍 获取预约记录对应社区的所有管家列表
     * @param appointment 预约记录
     * @return 管家列表
     */
    private List<Butler> getAllManagersForAppointment(Appointment appointment) {
        try {
            // 根据社区名称查询所有管家
            if (appointment.getCommunity() != null && !appointment.getCommunity().trim().isEmpty()) {
                logger.info("🔍 根据社区 {} 查询所有管家信息", appointment.getCommunity());
                List<Butler> butlers = butlerService.getAllButlersByCommunity(appointment.getCommunity());
                logger.info("✅ [获取社区所有管家] 获取成功 - 社区: {}, 管家数量: {}", 
                        appointment.getCommunity(), butlers != null ? butlers.size() : 0);
                return butlers;
            } else {
                logger.warn("⚠️ [获取社区所有管家] 社区名称为空");
                return null;
            }

        } catch (Exception e) {
            logger.error("❌ 查询社区所有管家信息异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 🔔 发送预约审核结果通知给访客
     * @param appointment 预约记录
     * @param auditResult 审核结果（已通过/未通过）
     * @param auditReason 审核备注（通过时为空，驳回时为驳回原因）
     * @param managerName 审核管家姓名
     */
    private void sendAppointmentAuditResultNotification(Appointment appointment, String auditResult, 
            String auditReason, String managerName) {
        try {
            logger.info("🔔 [开始发送预约审核结果通知] 预约ID: {}, 车牌: {}, 审核结果: {}", 
                appointment.getId(), appointment.getPlatenumber(), auditResult);
            
            // 参数校验
            if (appointment.getPlatenumber() == null || appointment.getPlatenumber().trim().isEmpty()) {
                logger.warn("⚠️ [预约审核结果通知跳过] 车牌号为空");
                return;
            }
            
            if (appointment.getVisitorname() == null || appointment.getVisitorname().trim().isEmpty()) {
                logger.warn("⚠️ [预约审核结果通知跳过] 访客姓名为空 - 车牌: {}", appointment.getPlatenumber());
                return;
            }
            
            // 构建通知参数
            String plateNumber = appointment.getPlatenumber();
            String parkName = appointment.getCommunity() != null ? appointment.getCommunity() : "停车场";
            String appointmentTime = appointment.getVisitdate() != null ? appointment.getVisitdate() : "";
            String visitorName = appointment.getVisitorname();
            
            logger.info("🔔 [预约审核结果通知] 准备发送 - 车牌: {}, 停车场: {}, 访客: {}, 管家: {}", 
                plateNumber, parkName, visitorName, managerName);
            
            // 调用微信模板消息服务
            Map<String, Object> result = weChatTemplateMessageService.sendAppointmentAuditResultNotification(
                    plateNumber,
                    parkName,
                    auditResult,
                    auditReason,
                    appointmentTime,
                    visitorName,
                    managerName
            );
            
            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                logger.info("✅ [预约审核结果通知发送成功] 车牌: {}, 访客: {}, 审核结果: {}", 
                    plateNumber, visitorName, auditResult);
            } else {
                String message = result != null ? (String) result.get("message") : "未知错误";
                logger.warn("⚠️ [预约审核结果通知发送失败] 车牌: {}, 访客: {}, 原因: {}", 
                    plateNumber, visitorName, message);
            }
            
        } catch (Exception e) {
            logger.error("❌ [预约审核结果通知发送异常] 预约ID: {}, 车牌: {}, 错误: {}", 
                appointment.getId(), appointment.getPlatenumber(), e.getMessage(), e);
        }
    }
}