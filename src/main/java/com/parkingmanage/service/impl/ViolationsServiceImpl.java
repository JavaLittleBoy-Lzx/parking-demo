package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.*;
import com.parkingmanage.mapper.*;
import com.parkingmanage.service.ViolationsService;
import com.parkingmanage.service.MonthlyTicketTimeoutConfigService;
import com.parkingmanage.service.OvernightParkingService;
import com.parkingmanage.service.AcmsVipService;
import com.parkingmanage.service.ViolationConfigService;
import com.parkingmanage.service.YardSmsTemplateRelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.parkingmanage.controller.AliyunSmsController;
import org.springframework.http.ResponseEntity;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 违规记录表 服务实现类
 * </p>
 *
 * @author MLH
 * @since 2025-01-31
 */
@Slf4j
@Service
public class ViolationsServiceImpl extends ServiceImpl<ViolationsMapper, Violations> implements ViolationsService {

    @Resource
    private ViolationsMapper violationsMapper;

    @Resource
    private OwnerinfoMapper ownerinfoMapper;

    @Resource
    private PatrolMapper patrolMapper;
    
    @Resource
    private ButlerMapper butlerMapper;

    @Resource
    private BlackListMapper blackListMapper;

    @Resource
    private MonthlyTicketTimeoutConfigService monthlyTicketTimeoutConfigService;
    
    @Resource
    private OvernightParkingService overnightParkingService;
    @Autowired
    private YardInfoMapper yardInfoMapper;
    
    @Resource
    private AcmsVipService acmsVipService;
    
    @Resource
    private ViolationProcessServiceImpl violationProcessService;
    
    @Resource
    private ViolationConfigService violationConfigService;

    @Resource
    private AliyunSmsController aliyunSmsController;
    
    @Resource
    private YardSmsTemplateRelationService yardSmsTemplateRelationService;
    
    /**
     * 🆕 根据车场名称获取短信模板配置
     * @param parkName 车场名称
     * @return 短信模板配置（Map包含signName和templateCode），如果未找到则返回默认配置
     */
    private Map<String, String> getSmsTemplateConfig(String parkName, String templateType) {
        Map<String, String> config = new HashMap<>();
        
        try {
            // 1. 根据车场名称查询车场ID
            LambdaQueryWrapper<YardInfo> yardWrapper = new LambdaQueryWrapper<>();
            yardWrapper.eq(YardInfo::getYardName, parkName);
            YardInfo yardInfo = yardInfoMapper.selectOne(yardWrapper);
            SmsTemplate template = null;
            if (yardInfo != null && yardInfo.getId() != null) {
                log.info("📋 [获取短信模板] 找到车场: {}, ID: {}", parkName, yardInfo.getId());
                
                // 2. 查询该车场关联的短信模板
                List<SmsTemplate> smsTemplates = yardSmsTemplateRelationService.getSmsTemplatesByYardId(yardInfo.getId());
                
                if (smsTemplates != null && !smsTemplates.isEmpty()) {
                    if (templateType.equals("warning")) {
                        template  = smsTemplates.stream()
                                .filter(t -> t.getTemplateType() != null && t.getTemplateName().equals("停车即将超时提醒")) // 1表示违规提醒类型
                                .findFirst()
                                .orElse(smsTemplates.get(0)); // 如果没找到特定类型，使用第一个模板
                    } else if (templateType.equals("blacklist")) {
                        template  = smsTemplates.stream()
                                .filter(t -> t.getTemplateType() != null && t.getTemplateName().equals("拉黑通知")) // 1表示违规提醒类型
                                .findFirst()
                                .orElse(smsTemplates.get(0)); // 如果没找到特定类型，使用第一个模板
                    }
                    // 3. 根据模板类型查找对应的短信模板
//                    SmsTemplate template = smsTemplates.stream()
//                            .filter(t -> t.getTemplateType() != null && t.getTemplateName().equals("停车即将超时提醒")) // 1表示违规提醒类型
//                            .findFirst()
//                            .orElse(smsTemplates.get(0)); // 如果没找到特定类型，使用第一个模板
                    
                    config.put("signName", template.getSignName());
                    config.put("templateCode", template.getTemplateCode());
                    
                    log.info("✅ [获取短信模板] 使用动态模板: {}, signName: {}, templateCode: {}", 
                            template.getTemplateName(), template.getSignName(), template.getTemplateCode());
                    
                    return config;
                }
            }
            
            log.warn("⚠️ [获取短信模板] 未找到车场 {} 的短信模板配置，使用默认配置", parkName);
        } catch (Exception e) {
            log.error("❌ [获取短信模板] 查询失败: {}, 使用默认配置", e.getMessage(), e);
        }
        
        // 默认配置
        config.put("signName", "东北林业大学");
        
        // 根据templateType返回不同的默认模板代码
        if ("warning".equals(templateType)) {
            config.put("templateCode", "SMS_498220005"); // 警告短信模板
        } else if ("blacklist".equals(templateType)) {
            config.put("templateCode", "SMS_498100004"); // 拉黑短信模板
        } else {
            config.put("templateCode", "SMS_496020098"); // 默认违规短信模板
        }
        
        log.info("ℹ️ [获取短信模板] 使用默认配置: signName: {}, templateCode: {}", 
                config.get("signName"), config.get("templateCode"));
        
        return config;
    }

    @Override
    public boolean createViolation(Violations violation) {
        log.info("🆕 [创建违规记录] 开始处理: plateNumber={}, appointmentId={}, ownerId={}", 
                violation.getPlateNumber(), violation.getAppointmentId(), violation.getOwnerId());
        
        // 🆕 如果有预约记录ID，优先使用预约记录中的业主信息
        if (violation.getAppointmentId() != null) {
            log.info("📅 [预约车违规] 使用预约记录ID: {}", violation.getAppointmentId());
            // 预约记录中已包含业主信息，直接使用前端传递的ownerId
            // 如果前端未传递ownerId，则查询预约记录获取业主信息
            if (violation.getOwnerId() == null) {
                log.warn("⚠️ [预约车违规] 前端未传递ownerId，查询预约记录获取业主信息");
                Map<String, Object> appointmentOwner = violationsMapper.selectOwnerByAppointmentId(violation.getAppointmentId());
                if (appointmentOwner != null) {
                    log.info("✅ [预约记录查询成功] 业主: {}, 预约类型: {}, 预约原因: {}, 审核人: {}", 
                            appointmentOwner.get("ownerName"),
                            appointmentOwner.get("appointmentType"),
                            appointmentOwner.get("appointmentReason"),
                            appointmentOwner.get("auditorName"));
                    // 注意：预约记录中的业主信息可能没有对应的ownerinfo表ID
                    // 这里可以设置为null，或者尝试匹配ownerinfo表
                } else {
                    log.warn("⚠️ [预约记录查询失败] appointmentId: {}", violation.getAppointmentId());
                }
            }
        } else {
            // 🔄 前端未传递appointmentId，尝试自动查找预约记录
            log.info("🚗 [自动查找预约记录] 根据车牌号查询: {}", violation.getPlateNumber());
            
            // 🆕 自动查找该车牌号的有效预约记录
            List<Map<String, Object>> appointmentRecords = violationsMapper.selectAppointmentRecordsByPlate(violation.getPlateNumber());
            if (appointmentRecords != null && !appointmentRecords.isEmpty()) {
                // 找到预约记录，使用第一个（最新的）
                Map<String, Object> latestAppointment = appointmentRecords.get(0);
                Integer appointmentId = (Integer) latestAppointment.get("id");
                violation.setAppointmentId(appointmentId);
                
                log.info("✅ [预约记录关联成功] plateNumber={}, appointmentId={}, status={}, community={}", 
                        violation.getPlateNumber(), appointmentId, 
                        latestAppointment.get("auditStatusText"), latestAppointment.get("community"));
                
                // 🆕 从预约记录中获取业主信息
                if (violation.getOwnerId() == null) {
                    Map<String, Object> appointmentOwner = violationsMapper.selectOwnerByAppointmentId(appointmentId);
                    if (appointmentOwner != null) {
                        log.info("✅ [从预约记录获取业主信息] 业主: {}, 预约类型: {}, 预约原因: {}, 审核人: {}", 
                                appointmentOwner.get("ownerName"),
                                appointmentOwner.get("appointmentType"),
                                appointmentOwner.get("appointmentReason"),
                                appointmentOwner.get("auditorName"));
                    }
                }
            } else {
                log.info("❌ [未找到预约记录] 按原有逻辑查询本地车主信息");
                // 原有逻辑：查询本地车主信息
                Integer ownerId = violationsMapper.selectOwnerIdByPlateNumber(violation.getPlateNumber());
                violation.setOwnerId(ownerId);
                log.info("🔍 [本地车主查询] plateNumber={}, ownerId={}", violation.getPlateNumber(), ownerId);
            }
        }
        
        // 🏢 [东北林业大学专用] 获取ACMS VIP车主信息并存入violations表
        if ("东北林业大学".equals(violation.getParkName())) {
            try {
                log.info("🔍 [ACMS查询] 开始获取VIP车主信息: plateNumber={}, parkName={}",
                        violation.getPlateNumber(), violation.getParkName());

                // 优先使用VIP票接口获取信息
                AcmsVipService.VipTicketInfo vipTicketInfo = acmsVipService.getVipTicketInfo(
                        violation.getPlateNumber(), violation.getParkName());

                if (vipTicketInfo != null) {
                    violation.setVipTypeName(vipTicketInfo.getVipTypeName());
                    violation.setOwnerName(vipTicketInfo.getOwnerName());
                    violation.setOwnerPhone(vipTicketInfo.getOwnerPhone());
                    log.info("✅ [VIP票信息获取成功] vipType={}, owner={}, phone={}",
                            vipTicketInfo.getVipTypeName(), vipTicketInfo.getOwnerName(), 
                            vipTicketInfo.getOwnerPhone());
                } else {
                    // 如果VIP票接口没有数据，尝试车主信息接口
                    log.info("🔄 [VIP票无数据] 尝试车主信息接口");
                    AcmsVipService.VipOwnerInfo ownerInfo = acmsVipService.getOwnerInfo(
                            violation.getPlateNumber(), violation.getParkName());
                    if (ownerInfo != null) {
                        violation.setOwnerName(ownerInfo.getOwnerName());
                        violation.setOwnerPhone(ownerInfo.getOwnerPhone());
                        violation.setOwnerAddress(ownerInfo.getOwnerAddress());
                        log.info("✅ [车主信息获取成功] owner={}, phone={}, address={}",
                                ownerInfo.getOwnerName(), ownerInfo.getOwnerPhone(), ownerInfo.getOwnerAddress());
                    } else {
                        log.info("❌ [ACMS无数据] 车牌号: {}", violation.getPlateNumber());
                    }
                }

            } catch (Exception e) {
                log.error("⚠️ [ACMS查询异常] 车牌号: {}, 错误: {}", violation.getPlateNumber(), e.getMessage());
                // 异常不影响违规记录的创建，继续后续流程
            }
        }
        
        // 🚫 如果需要拉黑，先调用ACMS接口，成功后才进行后续操作
        if (violation.getShouldBlacklist() != null && violation.getShouldBlacklist() == 1) {
            try {
                log.info("🚫 [拉黑操作] 开始调用ACMS接口: plateNumber={}, reason={}, type={}", 
                        violation.getPlateNumber(), violation.getBlacklistReason(), violation.getBlacklistTypeName());
                
                // 构建ACMS黑名单添加请求
                AcmsVipService.AddBlacklistRequest acmsRequest = new AcmsVipService.AddBlacklistRequest();
                acmsRequest.setParkName(violation.getParkName());
                acmsRequest.setVipTypeCode(violation.getBlacklistTypeCode());
                acmsRequest.setVipTypeName(violation.getBlacklistTypeName());
                acmsRequest.setCarCode(violation.getPlateNumber());
                acmsRequest.setCarOwner(violation.getOwnerName() != null ? violation.getOwnerName() : "未知车主");
                acmsRequest.setReason(violation.getBlacklistReason());
                acmsRequest.setDurationType(violation.getBlacklistDurationType());
                
                // 设置时间（转换为字符串格式）
                if ("temporary".equals(violation.getBlacklistDurationType())) {
                    if (violation.getBlacklistStartTime() != null) {
                        acmsRequest.setStartTime(violation.getBlacklistStartTime()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                    if (violation.getBlacklistEndTime() != null) {
                        acmsRequest.setEndTime(violation.getBlacklistEndTime()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                }
                
                // 设置备注和操作信息（违规记录ID暂时为0，后续会更新）
                acmsRequest.setRemark1("待保存违规记录");
                acmsRequest.setRemark2("移动巡检小程序添加");
                acmsRequest.setOperator(violation.getCreatedBy() != null ? violation.getCreatedBy() : "系统");
                acmsRequest.setOperateTime(LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                
                // 🔑 调用ACMS接口（必须成功）
                boolean acmsSuccess = acmsVipService.addBlacklistToAcms(acmsRequest);
                
                if (!acmsSuccess) {
                    log.error("❌ [ACMS拉黑失败] plateNumber={}, 终止后续操作", violation.getPlateNumber());
                    return false;
                }
                
                log.info("✅ [ACMS拉黑成功] plateNumber={}, 开始保存本地数据", violation.getPlateNumber());
                
            } catch (Exception acmsEx) {
                log.error("❌ [ACMS拉黑异常] plateNumber={}, error={}, 终止后续操作", 
                        violation.getPlateNumber(), acmsEx.getMessage(), acmsEx);
                return false;
            }
        }
        
        // 设置创建时间
        violation.setCreatedAt(LocalDateTime.now());
        violation.setUpdatedAt(LocalDateTime.now());
        
        // 🆕 设置处理状态初始值（如果前端没有传递）
        if (violation.getProcessStatus() == null || violation.getProcessStatus().trim().isEmpty()) {
            violation.setProcessStatus("pending"); // 默认为未处理
            log.info("🔧 [初始化处理状态] 设置为pending");
        }
        
        // 🔍 详细记录时间字段数据
        log.info("⏰ [时间字段详情] enterTime={}, leaveTime={}, appointmentTime={}", 
                violation.getEnterTime(), violation.getLeaveTime(), violation.getAppointmentTime());
        
        log.info("💾 [保存违规记录] 准备保存数据: appointmentId={}, ownerId={}, plateNumber={}, processStatus={}", 
                violation.getAppointmentId(), violation.getOwnerId(), violation.getPlateNumber(), violation.getProcessStatus());
        
        // 保存违规记录
        boolean result = this.save(violation);
        
        if (!result) {
            log.error("❌ [违规记录创建失败] plateNumber={}, appointmentId={}", 
                    violation.getPlateNumber(), violation.getAppointmentId());
            return false;
        }
        
        log.info("✅ [违规记录创建成功] id={}, appointmentId={}", violation.getId(), violation.getAppointmentId());
        
        // 如果保存成功，更新信用分
        if (violation.getOwnerId() != null) {
            updateCreditScoreBySeverity(violation.getOwnerId(), violation.getSeverity());
            log.info("📊 [信用分更新] ownerId={}, severity={}", violation.getOwnerId(), violation.getSeverity());
        }
        
        // 🆕 自动拉黑检查：统计违规次数，达到阈值自动拉黑
        try {
            // 只对东北林业大学的违规记录进行自动拉黑检查
            if ("东北林业大学".equals(violation.getParkName())) {
                log.info("🔍 [自动拉黑检查] 开始检查车牌: {}, parkName: {}", 
                        violation.getPlateNumber(), violation.getParkName());
                
                // 🎓 东北林业大学阈值配置：从配置表读取
                Integer maxViolationCount = 5; // 默认值
                String blacklistType = null;
                String blacklistTypeName = "违章黑名单"; // 默认名称
                String blacklistTypeCode = null;
                Boolean isPermanent = true; // 默认永久拉黑
                String blacklistStartTime = null;
                String blacklistEndTime = null;
                
                Integer blacklistValidDays = null;  // 有效天数
                
                try {
                    java.util.Map<String, Object> config = getNebuViolationConfig();
                    if (config != null) {
                        // 读取违规阈值
                        if (config.containsKey("maxViolationCount")) {
                            maxViolationCount = (Integer) config.get("maxViolationCount");
                        }
                        
                        // 读取黑名单类型（格式：code|name）
                        if (config.containsKey("blacklistType")) {
                            blacklistType = (String) config.get("blacklistType");
                            if (blacklistType != null && blacklistType.contains("|")) {
                                String[] parts = blacklistType.split("\\|");
                                blacklistTypeCode = parts[0];
                                blacklistTypeName = parts.length > 1 ? parts[1] : "违章黑名单";
                            } else {
                                blacklistTypeName = blacklistType != null ? blacklistType : "违章黑名单";
                            }
                        }
                        
                        // 读取是否永久拉黑
                        if (config.containsKey("isPermanent")) {
                            isPermanent = (Boolean) config.get("isPermanent");
                        }
                        
                        // 读取有效天数（临时拉黑时使用）
                        if (!isPermanent && config.containsKey("blacklistValidDays")) {
                            blacklistValidDays = (Integer) config.get("blacklistValidDays");
                        }
                        
                        log.info("📋 [从配置表读取东北林业大学配置] 阈值: {}, 黑名单类型: {}, 永久拉黑: {}, 有效天数: {}", 
                                maxViolationCount, blacklistTypeName, isPermanent, blacklistValidDays);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ [读取配置失败] 使用默认配置, error: {}", e.getMessage());
                }
                
                // 统计该车牌在该停车场的未处理违规次数（包括刚创建的这条）
                LambdaQueryWrapper<Violations> countWrapper = new LambdaQueryWrapper<>();
                countWrapper.eq(Violations::getPlateNumber, violation.getPlateNumber())
                           .eq(Violations::getParkName, violation.getParkName())
                           .eq(Violations::getProcessStatus, "pending"); // 只统计未处理的
                
                int unprocessedCount = this.count(countWrapper);
                
                log.info("📊 [违规统计] 车牌: {}, 停车场: {}, 未处理违规次数: {}, 阈值: {}", 
                        violation.getPlateNumber(), violation.getParkName(), unprocessedCount, maxViolationCount);
                
                // 🆕 3.5 判断是否达到警告阈值（阈值-1，即最后一次警告机会）
                if (unprocessedCount == maxViolationCount) {
                    log.info("⚠️ [触发违规警告] 车牌 {} 违规次数 {} 已达到警告阈值（阈值-1），发送警告短信", 
                            violation.getPlateNumber(), unprocessedCount);
                    
                    // 🆕 发送违规警告短信（使用动态模板）
                    try {
                        String phone = violation.getOwnerPhone();
                        if (StringUtils.hasText(phone) && phone.matches("^1[3-9]\\d{9}$")) {
                            // 🆕 获取动态短信模板配置
                            Map<String, String> smsConfig = getSmsTemplateConfig(violation.getParkName(), "warning");
                            String signName = smsConfig.get("signName");
                            String templateCode = smsConfig.get("templateCode");
                            
                            // 模板参数：${license_plate_number} 和 ${code}（已累计违规次数）
                            String templateJson = String.format(
                                    "{\"license_plate_number\":\"%s\",\"code\":\"%d\"}",
                                    violation.getPlateNumber(), 
                                    unprocessedCount
                            );
                            
                            // 使用动态短信模板
                            ResponseEntity<com.parkingmanage.common.Result> smsResp = aliyunSmsController.sendBlacklistMessage(
                                    phone,
                                    signName,
                                    templateCode,
                                    templateJson
                            );
                            log.info("📲 [违规警告短信] 发送成功 - 车牌: {}, 已违规: {}次, 模板: {}, 签名: {}", 
                                    violation.getPlateNumber(), unprocessedCount, templateCode, signName);
                        } else {
                            log.info("ℹ️ [违规警告短信] 未找到有效手机号，跳过发送: {}", phone);
                        }
                    } catch (Exception e) {
                        log.warn("⚠️ [违规警告短信发送失败] plateNumber={}, error={}", violation.getPlateNumber(), e.getMessage());
                    }
                }
                
                // 4. 判断是否达到拉黑阈值
                if (unprocessedCount > maxViolationCount) {
                    log.info("🚫 [触发自动拉黑] 车牌 {} 违规次数 {} 已达到阈值 {}, 黑名单类型: {}, 永久拉黑: {}", 
                            violation.getPlateNumber(), unprocessedCount, maxViolationCount, blacklistTypeName, isPermanent);
                    
                    // 5. 调用ACMS接口添加黑名单（使用配置的黑名单类型和时间）
                    try {
                        AcmsVipService.AddBlacklistRequest acmsRequest = new AcmsVipService.AddBlacklistRequest();
                        acmsRequest.setParkName(violation.getParkName());
                        
                        // 使用配置的黑名单类型
                        if (blacklistTypeCode != null) {
                            acmsRequest.setVipTypeCode(blacklistTypeCode);
                        }
                        acmsRequest.setVipTypeName(blacklistTypeName);
                        
                        acmsRequest.setCarCode(violation.getPlateNumber());
                        acmsRequest.setCarOwner(violation.getOwnerName() != null ? violation.getOwnerName() : "未知车主");
                        acmsRequest.setReason(String.format("累计违规%d次，系统自动拉黑", unprocessedCount));
                        
                        // 使用配置的拉黑时长
                        if (isPermanent) {
                            acmsRequest.setDurationType("permanent");
                        } else {
                            acmsRequest.setDurationType("temporary");
                            
                            // 🎯 关键修改：根据最后一次违规时间（当前时间）+ 有效天数计算拉黑时间段
                            LocalDateTime now = LocalDateTime.now();
                            LocalDateTime endTime = now.plusDays(blacklistValidDays != null ? blacklistValidDays : 30);
                            
                            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            blacklistStartTime = now.format(formatter);
                            blacklistEndTime = endTime.format(formatter);
                            
                            acmsRequest.setStartTime(blacklistStartTime);
                            acmsRequest.setEndTime(blacklistEndTime);
                            
                            log.info("⏰ [临时拉黑时间计算] 开始时间(最后违规时间)={}, 结束时间={}(+{}天)", 
                                    blacklistStartTime, blacklistEndTime, blacklistValidDays);
                        }
                        
                        acmsRequest.setRemark1(String.format("自动拉黑，违规次数: %d", unprocessedCount));
                        acmsRequest.setRemark2("系统自动触发");
                        acmsRequest.setOperator("SYSTEM");
                        acmsRequest.setOperateTime(LocalDateTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        
                        log.info("📤 [准备调用ACMS拉黑接口] parkName={}, typeCode={}, typeName={}, durationType={}, startTime={}, endTime={}", 
                                acmsRequest.getParkName(), acmsRequest.getVipTypeCode(), acmsRequest.getVipTypeName(),
                                acmsRequest.getDurationType(), acmsRequest.getStartTime(), acmsRequest.getEndTime());
                        
                        boolean acmsSuccess = acmsVipService.addBlacklistToAcms(acmsRequest);
                        
                        if (acmsSuccess) {
                            log.info("✅ [ACMS自动拉黑成功] 车牌: {}", violation.getPlateNumber());

                            // 🆕 自动拉黑成功后发送拉黑通知短信（使用动态模板）
                            try {
                                String phone = violation.getOwnerPhone();
                                if (StringUtils.hasText(phone) && phone.matches("^1[3-9]\\d{9}$")) {
                                    // 🆕 获取动态短信模板配置
                                    Map<String, String> smsConfig = getSmsTemplateConfig(violation.getParkName(), "blacklist");
                                    String signName = smsConfig.get("signName");
                                    String templateCode = smsConfig.get("templateCode");
                                    
                                    java.time.LocalDateTime nowTime = java.time.LocalDateTime.now();
                                    String yearStr = String.valueOf(nowTime.getYear());
                                    String monthStr = String.valueOf(nowTime.getMonthValue());
                                    String dayStr = String.valueOf(nowTime.getDayOfMonth());
                                    String timeStr = nowTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                                    String address = StringUtils.hasText(violation.getLocation()) ? violation.getLocation() : (StringUtils.hasText(violation.getParkName()) ? violation.getParkName() : "停车场");

                                    String templateJson = String.format(
                                            "{\"license_plate_number\":\"%s\",\"year\":\"%s\",\"month\":\"%s\",\"day\":\"%s\",\"time\":\"%s\",\"address\":\"%s\"}",
                                            violation.getPlateNumber(), yearStr, monthStr, dayStr, timeStr, address
                                    );

                                    // 🆕 使用动态短信模板
                                    ResponseEntity<com.parkingmanage.common.Result> smsResp = aliyunSmsController.sendBlacklistMessage(
                                            phone,
                                            signName,
                                            templateCode,
                                            templateJson
                                    );
                                    log.info("📲 [自动拉黑短信] 发送成功 - 车牌: {}, 已违规: {}次, 模板: {}, 签名: {}", 
                                            violation.getPlateNumber(), unprocessedCount, templateCode, signName);
                                } else {
                                    log.info("ℹ️ [自动拉黑短信] 未找到有效手机号，跳过发送: {}", phone);
                                }
                            } catch (Exception e) {
                                log.warn("⚠️ [自动拉黑短信发送失败] plateNumber={}, error={}", violation.getPlateNumber(), e.getMessage());
                            }
                            
                            // 6. 批量标记该车牌在该停车场的所有未处理违规记录为已处理
                            LambdaQueryWrapper<Violations> updateWrapper = new LambdaQueryWrapper<>();
                            updateWrapper.eq(Violations::getPlateNumber, violation.getPlateNumber())
                                       .eq(Violations::getParkName, violation.getParkName())
                                       .eq(Violations::getProcessStatus, "pending");
                            
                            Violations updateViolation = new Violations();
                            updateViolation.setProcessStatus("processed");
                            updateViolation.setProcessType("auto_blacklist"); // 自动拉黑类型
                            updateViolation.setProcessedAt(LocalDateTime.now());
                            updateViolation.setProcessedBy("SYSTEM");
                            updateViolation.setProcessRemark(String.format("累计违规%d次，系统自动拉黑", unprocessedCount));
                            updateViolation.setUpdatedAt(LocalDateTime.now());
                            
                            int updateCount = this.baseMapper.update(updateViolation, updateWrapper);
                            log.info("📝 [批量标记] 车牌 {} 的 {} 条违规记录已标记为已处理", 
                                    violation.getPlateNumber(), updateCount);
                            
                            // 7. 保存到本地黑名单表
                            LambdaQueryWrapper<BlackList> blacklistQuery = new LambdaQueryWrapper<>();
                            blacklistQuery.eq(BlackList::getCarCode, violation.getPlateNumber())
                                         .eq(BlackList::getParkName, violation.getParkName());
                            
                            BlackList existingBlackList = blackListMapper.selectOne(blacklistQuery);
                            
                            if (existingBlackList != null) {
                                // 更新已有记录
                                existingBlackList.setOwner(violation.getOwnerName() != null ? violation.getOwnerName() : "未知车主");
                                existingBlackList.setReason(String.format("累计违规%d次，系统自动拉黑", unprocessedCount));
                                existingBlackList.setSpecialCarTypeConfigName("违规自动拉黑");
                                existingBlackList.setBlacklistTypeCode("VIOLATION_AUTO");
                                existingBlackList.setBlackListForeverFlag("永久");
                                existingBlackList.setRemark1(String.format("自动拉黑，违规次数: %d", unprocessedCount));
                                existingBlackList.setRemark2("系统自动触发");
                                blackListMapper.updateById(existingBlackList);
                                log.info("✅ [本地黑名单更新成功] 车牌: {}", violation.getPlateNumber());
                            } else {
                                // 新增记录
                                BlackList blackList = new BlackList();
                                blackList.setParkName(violation.getParkName());
                                blackList.setCarCode(violation.getPlateNumber());
                                blackList.setOwner(violation.getOwnerName() != null ? violation.getOwnerName() : "未知车主");
                                blackList.setReason(String.format("累计违规%d次，系统自动拉黑", unprocessedCount));
                                blackList.setSpecialCarTypeConfigName("违规自动拉黑");
                                blackList.setBlacklistTypeCode("VIOLATION_AUTO");
                                blackList.setBlackListForeverFlag("永久");
                                blackList.setRemark1(String.format("自动拉黑，违规次数: %d", unprocessedCount));
                                blackList.setRemark2("系统自动触发");
                                blackListMapper.insert(blackList);
                                log.info("✅ [本地黑名单新增成功] 车牌: {}", violation.getPlateNumber());
                            }
                            
                        } else {
                            log.error("❌ [ACMS自动拉黑失败] 车牌: {}", violation.getPlateNumber());
                        }
                        
                    } catch (Exception acmsEx) {
                        log.error("❌ [自动拉黑异常] 车牌: {}, error: {}", 
                                violation.getPlateNumber(), acmsEx.getMessage(), acmsEx);
                    }
                    
                } else {
                    log.info("ℹ️ [未达阈值] 车牌 {} 违规次数 {} 未达到阈值 {}", 
                            violation.getPlateNumber(), unprocessedCount, maxViolationCount);
                }
            }
        } catch (Exception e) {
            log.error("❌ [自动拉黑检查异常] plateNumber={}, error={}", 
                violation.getPlateNumber(), e.getMessage(), e);
            // 自动拉黑检查失败不影响违规记录创建
        }
        
        // 🚫 如果需要手动拉黑，批量处理并保存/更新黑名单到本地数据库
        if (violation.getShouldBlacklist() != null && violation.getShouldBlacklist() == 1) {
            // 📝 将该车牌未处理的违规记录改为"已处理"（包括当前记录）
            // 🎓 [东北林业大学专用] 限制同一停车场的批量更新
            // 🏷️ 标记为"手动拉黑处理"，区别于自动拉黑
            try {
                LambdaQueryWrapper<Violations> updateWrapper = new LambdaQueryWrapper<>();
                updateWrapper.eq(Violations::getPlateNumber, violation.getPlateNumber())
                            .eq(Violations::getProcessStatus, "pending"); // 只更新未处理的记录
                
                // 🎓 如果是东北林业大学，只更新同一停车场的记录（包括历史记录和当前记录）
                if ("东北林业大学".equals(violation.getParkName())) {
                    updateWrapper.eq(Violations::getParkName, violation.getParkName());
                    log.info("🎓 [东北林业大学批量更新] plateNumber={}, parkName={}, 限制同一停车场", 
                            violation.getPlateNumber(), violation.getParkName());
                }
                
                Violations updateViolation = new Violations();
                updateViolation.setProcessStatus("processed"); // 标记为已处理
                updateViolation.setProcessType("manual_blacklist"); // 🏷️ 标记为手动拉黑处理
                updateViolation.setProcessedAt(LocalDateTime.now()); // 设置处理时间
                updateViolation.setProcessedBy(violation.getCreatedBy() != null ? violation.getCreatedBy() : "SYSTEM"); // 处理人
                updateViolation.setProcessRemark("小程序手动拉黑处理"); // 处理备注
                updateViolation.setUpdatedAt(LocalDateTime.now());
                
                int updateCount = this.baseMapper.update(updateViolation, updateWrapper);
                
                if (updateCount > 0) {
                    log.info("📝 [批量手动拉黑处理] plateNumber={}, parkName={}, 将 {} 条记录标记为手动拉黑处理（包括当前记录）", 
                            violation.getPlateNumber(), violation.getParkName(), updateCount);
                }
            } catch (Exception e) {
                log.error("❌ [批量手动拉黑处理异常] plateNumber={}, parkName={}, error={}", 
                        violation.getPlateNumber(), violation.getParkName(), e.getMessage(), e);
                // 状态更新失败不影响主流程
            }
            
            // 保存到本地黑名单表
            try {
                log.info("💾 [本地黑名单] 准备保存/更新到black_list表: plateNumber={}", violation.getPlateNumber());
                
                // 查询是否已存在该车牌的黑名单记录
                LambdaQueryWrapper<BlackList> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(BlackList::getCarCode, violation.getPlateNumber())
                           .eq(BlackList::getParkName, violation.getParkName());
                
                BlackList existingBlackList = blackListMapper.selectOne(queryWrapper);
                
                if (existingBlackList != null) {
                    // ✏️ 更新已有记录
                    log.info("🔄 [黑名单已存在] blacklistId={}, plateNumber={}, 执行更新操作", 
                            existingBlackList.getId(), violation.getPlateNumber());
                    
                    existingBlackList.setOwner(violation.getOwnerName() != null ? violation.getOwnerName() : "未知车主");
                    existingBlackList.setReason(violation.getBlacklistReason());
                    existingBlackList.setSpecialCarTypeConfigName(violation.getBlacklistTypeName());
                    existingBlackList.setBlacklistTypeCode(violation.getBlacklistTypeCode());
                    
                    // 更新拉黑时长标志
                    if ("permanent".equals(violation.getBlacklistDurationType())) {
                        existingBlackList.setBlackListForeverFlag("永久");
                        existingBlackList.setBlacklistStartTime(null);
                        existingBlackList.setBlacklistEndTime(null);
                    } else if ("temporary".equals(violation.getBlacklistDurationType())) {
                        existingBlackList.setBlackListForeverFlag("临时");
                        existingBlackList.setBlacklistStartTime(violation.getBlacklistStartTime());
                        existingBlackList.setBlacklistEndTime(violation.getBlacklistEndTime());
                    }
                    
                    // 更新备注信息
                    existingBlackList.setRemark1("违规记录ID: " + violation.getId());
                    existingBlackList.setRemark2("移动巡检小程序更新，操作人: " + violation.getCreatedBy());
                    
                    int updateResult = blackListMapper.updateById(existingBlackList);
                    
                    if (updateResult > 0) {
                        log.info("✅ [黑名单更新成功] blacklistId={}, plateNumber={}, type={}, duration={}", 
                                existingBlackList.getId(), existingBlackList.getCarCode(), 
                                existingBlackList.getSpecialCarTypeConfigName(), 
                                existingBlackList.getBlackListForeverFlag());
                    } else {
                        log.error("❌ [黑名单更新失败] blacklistId={}, plateNumber={}", 
                                existingBlackList.getId(), violation.getPlateNumber());
                    }
                    
                } else {
                    // ➕ 新增记录
                    log.info("➕ [黑名单不存在] plateNumber={}, 执行新增操作", violation.getPlateNumber());
                    
                    BlackList blackList = new BlackList();
                    blackList.setParkName(violation.getParkName());
                    blackList.setCarCode(violation.getPlateNumber());
                    blackList.setOwner(violation.getOwnerName() != null ? violation.getOwnerName() : "未知车主");
                    blackList.setReason(violation.getBlacklistReason());
                    
                    // 设置黑名单类型
                    blackList.setSpecialCarTypeConfigName(violation.getBlacklistTypeName());
                    blackList.setBlacklistTypeCode(violation.getBlacklistTypeCode());
                    
                    // 设置拉黑时长标志
                    if ("permanent".equals(violation.getBlacklistDurationType())) {
                        blackList.setBlackListForeverFlag("永久");
                    } else if ("temporary".equals(violation.getBlacklistDurationType())) {
                        blackList.setBlackListForeverFlag("临时");
                        blackList.setBlacklistStartTime(violation.getBlacklistStartTime());
                        blackList.setBlacklistEndTime(violation.getBlacklistEndTime());
                    }
                    
                    // 设置备注信息
                    blackList.setRemark1("违规记录ID: " + violation.getId());
                    blackList.setRemark2("移动巡检小程序添加，操作人: " + violation.getCreatedBy());
                    
                    int insertResult = blackListMapper.insert(blackList);
                    
                    if (insertResult > 0) {
                        log.info("✅ [黑名单新增成功] blacklistId={}, plateNumber={}, type={}, duration={}", 
                                blackList.getId(), blackList.getCarCode(), 
                                blackList.getSpecialCarTypeConfigName(), 
                                blackList.getBlackListForeverFlag());
                    } else {
                        log.error("❌ [黑名单新增失败] plateNumber={}", violation.getPlateNumber());
                    }
                }
                
            } catch (Exception e) {
                log.error("❌ [本地黑名单保存异常] plateNumber={}, error={}", 
                        violation.getPlateNumber(), e.getMessage(), e);
                // 本地黑名单保存失败不影响违规记录的创建（因为ACMS已经成功）
            }
        }
        
        return result;
    }

    @Override
    public IPage<Map<String, Object>> getViolationsWithOwnerInfo(Page<Map<String, Object>> page, String plateNumber, 
                                                                 String status, String violationType, 
                                                                 LocalDateTime startDate, LocalDateTime endDate, 
                                                                 String createdByFilter, String communityFilter, Boolean useDirectQuery) {
        
        // 🎓 东北林业大学特殊处理：使用直接查询模式
        if (Boolean.TRUE.equals(useDirectQuery)) {
            log.info("🎓 [东北林业大学直接查询] 使用violations表直接查询，不关联其他表");
            IPage<Map<String, Object>> result = violationsMapper.selectViolationsDirectQuery(
                page, plateNumber, status, violationType, startDate, endDate, createdByFilter, communityFilter);
            log.info("🎓 [查询结果] 共查询到 {} 条违规记录", result.getTotal());
            return result;
        }
        
        // 如果有创建者过滤条件，需要在查询中添加条件
        if (StringUtils.hasText(createdByFilter)) {
            // 暂时使用原有方法，后续可以扩展Mapper方法
            // 这里先返回基本查询结果，在业务层过滤
            IPage<Map<String, Object>> result = violationsMapper.selectViolationsWithOwnerInfo(
                page, plateNumber, status, violationType, startDate, endDate, createdByFilter, communityFilter);
            log.debug("查询结果数据长度 = {}", result.getTotal());
            return result;
        }
        return violationsMapper.selectViolationsWithOwnerInfo(page, plateNumber, status, violationType, 
                                                              startDate, endDate, createdByFilter, communityFilter);
    }

    @Override
    public boolean updateViolationStatus(Long id, String status, String remark, Integer handlerId) {
        Violations violation = new Violations();
        violation.setId(id);
        violation.setStatus(status);
        violation.setRemark(remark);
        violation.setHandlerId(handlerId);
        violation.setUpdatedAt(LocalDateTime.now());
        
        return this.updateById(violation);
    }

    @Override
    public boolean canUpdateViolation(Long violationId, String currentUserId, String userRole) {
        System.out.println("userRole = " + userRole);
        // 管理员、管家和巡逻员可以更新所有记录
        if ("manager".equals(userRole) || "housekeeper".equals(userRole) || "patrol".equals(userRole)) {
            return true;
        }
        
        // 普通用户只能更新自己创建的记录
        if ("resident".equals(userRole)) {
            Violations violation = this.getById(violationId);
            return violation != null && currentUserId.equals(violation.getCreatedBy());
        }
        
        return false;
    }

    @Override
    public boolean deleteViolation(Long violationId, String currentUserId) {
        try {
            log.info("🗑️ [开始删除违规记录] violationId={}, deletedBy={}", violationId, currentUserId);
            
            // 1. 验证违规记录是否存在
            Violations violation = this.getById(violationId);
            if (violation == null) {
                log.warn("⚠️ [违规记录不存在] violationId={}", violationId);
                return false;
            }
            
            log.info("📋 [违规记录信息] plateNumber={}, violationType={}, status={}, createdBy={}, createdAt={}", 
                    violation.getPlateNumber(), violation.getViolationType(), violation.getStatus(), 
                    violation.getCreatedBy(), violation.getCreatedAt());
            
            // 2. 执行物理删除（彻底删除记录）
            boolean result = this.removeById(violationId);
            
            if (result) {
                log.info("✅ [违规记录删除成功] violationId={}, plateNumber={}, deletedBy={}", 
                        violationId, violation.getPlateNumber(), currentUserId);
                
                // 3. 如果删除成功，可能需要调整信用分（如果有车主关联）
                if (violation.getOwnerId() != null) {
                    try {
                        // 删除违规记录后，可以考虑恢复一定的信用分
                        // 这里可以根据业务需求实现信用分恢复逻辑
                        log.info("💰 [信用分处理] 违规记录删除，车主ID: {}", violation.getOwnerId());
                        // 暂时不做信用分调整，因为删除操作可能是误操作的修正
                    } catch (Exception e) {
                        log.warn("⚠️ [信用分处理异常] ownerId={}, error={}", violation.getOwnerId(), e.getMessage());
                        // 信用分处理失败不影响删除操作的成功
                    }
                }
                
                return true;
            } else {
                log.error("❌ [违规记录删除失败] violationId={}, deletedBy={}", violationId, currentUserId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ [删除违规记录异常] violationId={}, deletedBy={}, error={}", 
                    violationId, currentUserId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getHighRiskVehicles(LocalDateTime startDate, LocalDateTime endDate, Integer limit, String createdByFilter, String communityFilter) {
        // 🎓 使用直接查询方法，支持创建者和小区过滤
        List<Map<String, Object>> result = violationsMapper.selectHighRiskVehiclesDirectQuery(
                startDate, endDate, limit, createdByFilter, communityFilter);
        
        log.info("🚗 [高风险车辆查询] startDate={}, endDate={}, limit={}, createdByFilter={}, communityFilter={}, 查询结果数量={}", 
                startDate, endDate, limit, createdByFilter, communityFilter, result.size());
        
        return result;
    }

    @Override
    public Map<String, Object> getViolationStatistics(LocalDateTime startDate, LocalDateTime endDate, String plateNumber, String createdByFilter, String communityFilter) {
        Map<String, Object> result = new HashMap<>();
        
        // 🎓 使用直接查询方法，支持创建者和小区过滤
        List<Map<String, Object>> typeStats = violationsMapper.selectViolationTypeStatsWithFilter(
                startDate, endDate, createdByFilter, communityFilter);
        
        List<Map<String, Object>> dailyStats = violationsMapper.selectDailyViolationStatsWithFilter(
                startDate, endDate, plateNumber, createdByFilter, communityFilter);
        
        List<Map<String, Object>> statusStats = violationsMapper.selectViolationStatisticsWithFilter(
                startDate, endDate, plateNumber, createdByFilter, communityFilter);
        
        log.info("📊 [违规统计查询] startDate={}, endDate={}, plateNumber={}, createdByFilter={}, communityFilter={}, typeStats数量={}, dailyStats数量={}, statusStats数量={}", 
                startDate, endDate, plateNumber, createdByFilter, communityFilter, typeStats.size(), dailyStats.size(), statusStats.size());
        
        result.put("typeStats", typeStats);
        result.put("dailyStats", dailyStats);
        result.put("statusStats", statusStats);
        
        return result;
    }

    @Override
    public Map<String, Object> getOwnerByPlateNumber(String plateNumber) {
        log.info("🔍 [查询月票车主信息] 车牌号: {}", plateNumber);
        
        try {
            // 🆕 只查询月票车主信息
            Map<String, Object> result = violationsMapper.selectOwnerByPlateNumber(plateNumber);
            
            if (result != null) {
                log.info("✅ [找到月票车主] 车牌: {}, 车主: {}, 月票: {}", 
                        plateNumber, result.get("ownerName"), result.get("monthTicketName"));
                        
                // 🔧 处理手机号脱敏
                String phone = (String) result.get("ownerPhone");
                if (phone != null && phone.length() > 6) {
                    result.put("ownerPhone", maskPhone(phone));
                }
                
                // 🔧 设置新能源车标识
                result.put("isNewEnergy", plateNumber.length() == 8);
                
                // 🔧 兼容性字段映射
                result.put("id", result.get("ownerId"));
                result.put("name", result.get("ownerName"));
                result.put("phone", result.get("ownerPhone"));
                result.put("address", result.get("ownerAddress"));
                
                return result;
            }
            
            log.warn("⚠️ [未找到月票车主] 车牌号: {}", plateNumber);
            return null;
            
        } catch (Exception e) {
            log.error("❌ [查询月票车主异常] 车牌号: {}, 错误: {}", plateNumber, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 🆕 获取管家所在小区
     */
    @Override
    public String getButlerCommunity(String userId) {
        try {
            log.info("🏘️ [获取管家小区] 开始查询 - userId: {}", userId);
            
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("⚠️ [获取管家小区] 用户ID为空");
                return null;
            }
            
            // 从butler表查询管家的小区信息
            QueryWrapper<Butler> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("usercode", userId)
                       .or()
                       .eq("openid", userId);
            
            Butler butler = butlerMapper.selectOne(queryWrapper);
            
            if (butler != null && butler.getCommunity() != null && !butler.getCommunity().trim().isEmpty()) {
                String community = butler.getCommunity().trim();
                log.info("✅ [获取管家小区] 成功获取小区: {}", community);
                return community;
            } else {
                log.warn("⚠️ [获取管家小区] 未找到管家小区信息 - userId: {}", userId);
                return null;
            }
            
        } catch (Exception e) {
            log.error("❌ [获取管家小区] 查询失败 - userId: {}, error: {}", userId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<Map<String, Object>> getPlateSuggestions(String keyword, String usercode) {
        log.info("🔍 [开始搜索] 车牌建议，关键词: {}, 用户: {}", keyword, usercode);
        
        // 🔒 权限控制：巡逻员只能查询负责小区的车辆
        String currentUserCommunity;
        if ("patrol".equals(usercode) || (usercode != null && usercode.startsWith("patrol_"))) {
            String tempCommunity = null;
            try {
                // 从用户信息中获取小区（这里可以根据实际的用户管理系统调整）
                // 暂时使用固定小区，后续可以从数据库查询用户对应的小区
                tempCommunity = "万象上东"; // 示例小区，实际应该从用户表或权限表查询
                log.info("🏘️ [巡逻员权限] 用户 {} 负责小区: {}", usercode, tempCommunity);
            } catch (Exception e) {
                log.warn("⚠️ [获取巡逻员小区失败] {}", e.getMessage());
            }
            currentUserCommunity = tempCommunity;
            
            // 🔒 如果是巡逻员但没有小区信息，直接返回空结果
            if (currentUserCommunity == null) {
                log.warn("🚫 [权限拒绝] 巡逻员 {} 没有小区信息，返回空结果", usercode);
                return new ArrayList<>();
            }
        } else {
            currentUserCommunity = null;
        }
        
        LambdaQueryWrapper<Ownerinfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ownerinfo::getIsaudit, "是");
        
        // 🔒 巡逻员权限：只查询负责小区的业主
        if (currentUserCommunity != null) {
            wrapper.eq(Ownerinfo::getCommunity, currentUserCommunity);
            log.info("🏘️ [小区过滤] 限制查询小区: {}", currentUserCommunity);
        }
        
        wrapper.and(w -> w.like(Ownerinfo::getPlates, keyword)
                         .or()
                         .like(Ownerinfo::getOwnername, keyword));
        wrapper.last("LIMIT 100"); // 🔧 增加数据库查询限制到100条，避免处理过多数据
        
        List<Ownerinfo> owners = ownerinfoMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Ownerinfo owner : owners) {
            // 🔍 二次验证：确保业主属于当前用户负责的小区
            if (currentUserCommunity != null && !currentUserCommunity.equals(owner.getCommunity())) {
                log.warn("🚫 [小区不匹配] 业主小区 {} 与巡逻员小区 {} 不符，跳过", owner.getCommunity(), currentUserCommunity);
                continue;
            }
            
            if (StringUtils.hasText(owner.getPlates())) {
                String[] plates = owner.getPlates().split(",");
                for (String plate : plates) {
                    plate = plate.trim();
                    if (plate.toLowerCase().contains(keyword.toLowerCase())) {
                        Map<String, Object> suggestion = new HashMap<>();
                        suggestion.put("plateNumber", plate);
                        suggestion.put("ownerName", owner.getOwnername());
                        suggestion.put("creditScore", owner.getCreditScore());
                        suggestion.put("isNewEnergy", plate.length() == 8);
                        suggestion.put("community", owner.getCommunity()); // 🆕 添加小区信息
                        result.add(suggestion);
                        
                        // 🔧 移除硬编码18条限制，改为使用合理的上限（50条）
                        if (result.size() >= 50) {
                            log.info("📊 [结果限制] 已达到50条结果上限，停止搜索");
                            break;
                        }
                    }
                }
                
                // 如果已经达到限制数量，退出外层循环
                if (result.size() >= 50) {
                    break;
                }
            }
        }
        
        log.info("✅ [搜索完成] 找到 {} 条车牌建议，关键词: {}", result.size(), keyword);
        return result;
    }

    @Override
    public List<Map<String, Object>> getViolationPlateSuggestions(String keyword, String parkCode) {
        log.info("🔍 [违规记录搜索] 车牌建议，关键词: {}, 车场: {}", keyword, parkCode);
        
        LambdaQueryWrapper<Violations> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Violations::getPlateNumber, keyword);
        
        // 如果指定了车场代码或车场名称，则按车场过滤
        if (StringUtils.hasText(parkCode)) {
            // 🔐 支持多个车场（逗号分隔）或单个车场
            if (parkCode.contains(",")) {
                // 多个车场：尝试作为车场名称或车场代码匹配
                String[] parks = parkCode.split(",");
                final String[] finalParks = parks; // 需要final变量用于lambda
                wrapper.and(w -> {
                    boolean first = true;
                    for (String park : finalParks) {
                        String trimmedPark = park.trim();
                        if (StringUtils.hasText(trimmedPark)) {
                            if (!first) {
                                w.or();
                            }
                            w.nested(ww -> ww.eq(Violations::getParkCode, trimmedPark).or().eq(Violations::getParkName, trimmedPark));
                            first = false;
                        }
                    }
                });
                log.info("🔐 [多车场过滤] parks: {}", java.util.Arrays.toString(parks));
            } else {
                // 单个车场：同时匹配车场代码和车场名称
                wrapper.and(w -> w.eq(Violations::getParkCode, parkCode).or().eq(Violations::getParkName, parkCode));
                log.info("🔐 [单车场过滤] parkCode or parkName: {}", parkCode);
            }
        }
        
        // 只查询最近6个月的违规记录，避免数据过多
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        wrapper.ge(Violations::getCreatedAt, sixMonthsAgo);
        
        // 按创建时间倒序排序，获取最新的违规记录
        wrapper.orderByDesc(Violations::getCreatedAt);
        
        // 查询所有匹配的违规记录
        List<Violations> violations = violationsMapper.selectList(wrapper);
        
        // 手动去重：使用LinkedHashMap保持顺序，按车牌号去重（保留第一条，即最新的）
        Map<String, Violations> uniqueViolations = new LinkedHashMap<>();
        for (Violations violation : violations) {
            String plateNumber = violation.getPlateNumber();
            if (!uniqueViolations.containsKey(plateNumber)) {
                uniqueViolations.put(plateNumber, violation);
            }
            // 只保留前20个不重复的车牌
            if (uniqueViolations.size() >= 20) {
                break;
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Violations violation : uniqueViolations.values()) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("plateNumber", violation.getPlateNumber());
            suggestion.put("ownerName", violation.getOwnerName());
            suggestion.put("ownerPhone", violation.getOwnerPhone());
            suggestion.put("parkCode", violation.getParkCode());
            suggestion.put("parkName", violation.getParkName());
            
            // 统计该车牌的违规次数
            LambdaQueryWrapper<Violations> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(Violations::getPlateNumber, violation.getPlateNumber());
            if (StringUtils.hasText(parkCode)) {
                // 🔐 支持多个车场或车场名称过滤
                if (parkCode.contains(",")) {
                    String[] parks = parkCode.split(",");
                    final String[] finalParks = parks;
                    countWrapper.and(w -> {
                        boolean first = true;
                        for (String park : finalParks) {
                            String trimmedPark = park.trim();
                            if (StringUtils.hasText(trimmedPark)) {
                                if (!first) {
                                    w.or();
                                }
                                w.nested(ww -> ww.eq(Violations::getParkCode, trimmedPark).or().eq(Violations::getParkName, trimmedPark));
                                first = false;
                            }
                        }
                    });
                } else {
                    countWrapper.and(w -> w.eq(Violations::getParkCode, parkCode).or().eq(Violations::getParkName, parkCode));
                }
            }
            countWrapper.ge(Violations::getCreatedAt, sixMonthsAgo);
            Integer violationCount = violationsMapper.selectCount(countWrapper);
            suggestion.put("violationCount", violationCount);
            
            result.add(suggestion);
        }
        
        log.info("✅ [违规记录搜索完成] 找到 {} 条车牌建议，关键词: {}", result.size(), keyword);
        return result;
    }

    @Override
    public List<Map<String, Object>> getOwnerVehicles(Integer ownerId) {
        Ownerinfo owner = ownerinfoMapper.selectById(ownerId);
        if (owner == null || !StringUtils.hasText(owner.getPlates())) {
            return new ArrayList<>();
        }
        
        String[] plates = owner.getPlates().split(",");
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (String plate : plates) {
            plate = plate.trim();
            if (StringUtils.hasText(plate)) {
                // 查询该车牌的违规统计
                LambdaQueryWrapper<Violations> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Violations::getPlateNumber, plate);
                
                long totalViolations = this.count(wrapper);
                
                wrapper.ge(Violations::getCreatedAt, LocalDateTime.now().minusMonths(1));
                long monthlyViolations = this.count(wrapper);
                
                Map<String, Object> vehicle = new HashMap<>();
                vehicle.put("plateNumber", plate);
                vehicle.put("isNewEnergy", plate.length() == 8);
                vehicle.put("vehicleType", "car");
                vehicle.put("totalViolations", totalViolations);
                vehicle.put("monthlyViolations", monthlyViolations);
                
                result.add(vehicle);
            }
        }
        
        return result;
    }

    @Override
    public boolean updateOwnerCreditScore(Integer ownerId, Integer creditScore) {
        if (creditScore < 0 || creditScore > 100) {
            return false;
        }
        
        Ownerinfo owner = new Ownerinfo();
        owner.setId(ownerId);
        owner.setCreditScore(creditScore);
        
        return ownerinfoMapper.updateById(owner) > 0;
    }

    /**
     * 根据严重程度更新信用分
     */
    private void updateCreditScoreBySeverity(Integer ownerId, String severity) {
        if (ownerId == null || severity == null) {
            return;
        }
        
        // 获取当前信用分
        Ownerinfo ownerinfo = ownerinfoMapper.selectById(ownerId);
        if (ownerinfo == null) {
            return;
        }
        
        int currentScore = ownerinfo.getCreditScore() != null ? ownerinfo.getCreditScore() : 100;
        int deduction = 0;
        
        // 根据严重程度扣分
        switch (severity.toLowerCase()) {
            case "severe":
                deduction = 10;
                break;
            case "moderate":
                deduction = 5;
                break;
            case "mild":
                deduction = 2;
                break;
            default:
                deduction = 2;
                break;
        }
        
        int newScore = Math.max(0, currentScore - deduction);
        
        // 更新信用分
        ownerinfo.setCreditScore(newScore);
        ownerinfoMapper.updateById(ownerinfo);
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 构建地址字符串
     */
    private String buildAddress(Ownerinfo owner) {
        StringBuilder address = new StringBuilder();
        if (StringUtils.hasText(owner.getBuilding())) {
            address.append(owner.getBuilding()).append("栋");
        }
        if (owner.getUnits() != null) {
            address.append(owner.getUnits()).append("单元");
        }
        if (owner.getRoomnumber() != null) {
            address.append(owner.getRoomnumber()).append("室");
        }
        return address.toString();
    }

    @Override
    public List<Map<String, Object>> getAppointmentRecordsByPlate(String plateNumber) {
        if (!StringUtils.hasText(plateNumber)) {
            return new ArrayList<>();
        }
        
        // 查询指定车牌号的预约记录，排除待审核状态，按时间倒序排列
        return violationsMapper.selectAppointmentRecordsByPlate(plateNumber.trim());
    }

    @Override
    public Map<String, Object> getAppointmentDetail(Integer appointmentId) {
        log.info("🔍 [查询预约详情] appointmentId={}", appointmentId);
        try {
            Map<String, Object> appointmentDetail = violationsMapper.selectOwnerByAppointmentId(appointmentId);
            if (appointmentDetail != null) {
                log.info("✅ [预约详情查询成功] appointmentId={}, visitorname={}, appointmentType={}, appointmentReason={}",
                        appointmentId, 
                        appointmentDetail.get("visitorname"),
                        appointmentDetail.get("appointmentType"),
                        appointmentDetail.get("appointmentReason"));
                
                // 🆕 格式化预约状态显示文本
                String appointmentStatus = (String) appointmentDetail.get("appointmentStatus");
                if (appointmentStatus != null) {
                    String statusText;
                    switch (appointmentStatus.toLowerCase()) {
                        case "approved":
                            statusText = "已审核通过";
                            break;
                        case "rejected":
                            statusText = "已拒绝";
                            break;
                        case "pending":
                            statusText = "待审核";
                            break;
                        default:
                            statusText = appointmentStatus;
                            break;
                    }
                    appointmentDetail.put("appointmentStatusText", statusText);
                }
                
                // 🆕 格式化预约类型显示文本
                String appointmentType = (String) appointmentDetail.get("appointmentType");
                if (appointmentType != null) {
                    String typeText;
                    switch (appointmentType.toLowerCase()) {
                        case "visitor":
                            typeText = "访客预约";
                            break;
                        case "resident":
                            typeText = "业主预约";
                            break;
                        case "delivery":
                            typeText = "送货预约";
                            break;
                        case "maintenance":
                            typeText = "维修预约";
                            break;
                        case "other":
                            typeText = "其他预约";
                            break;
                        default:
                            typeText = appointmentType;
                            break;
                    }
                    appointmentDetail.put("appointmentTypeText", typeText);
                }
                
            } else {
                log.warn("⚠️ [预约详情不存在] appointmentId={}", appointmentId);
            }
            return appointmentDetail;
        } catch (Exception e) {
            log.error("❌ [预约详情查询失败] appointmentId={}, error={}", appointmentId, e.getMessage());
            throw new RuntimeException("查询预约详情失败", e);
        }
    }

    @Override
    public Map<String, Object> analyzeViolationByPlate(String plateNumber) {
        Map<String, Object> result = new HashMap<>();
        
        if (!StringUtils.hasText(plateNumber)) {
            result.put("hasData", false);
            result.put("message", "车牌号不能为空");
            return result;
        }
        
        // 获取预约记录
        List<Map<String, Object>> appointments = getAppointmentRecordsByPlate(plateNumber.trim());
        result.put("appointmentRecords", appointments);
        result.put("hasData", !appointments.isEmpty());
        
        // 分析违规建议
        List<Map<String, Object>> suggestions = new ArrayList<>();
        
        for (Map<String, Object> appointment : appointments) {
            String arriveDate = (String) appointment.get("arrivedate");
            String leaveDate = (String) appointment.get("leavedate");
            // String visitDate = (String) appointment.get("visitdate"); // 暂未使用
            
            // 分析是否超时停车
            if (StringUtils.hasText(arriveDate) && StringUtils.hasText(leaveDate)) {
                try {
                    // 计算停车时长（假设时间格式正确）
                    double parkingHours = calculateParkingHours(arriveDate, leaveDate);
                    
                    if (parkingHours > 8) { // 超过8小时算超时
                        Map<String, Object> suggestion = new HashMap<>();
                        suggestion.put("type", "超时停车");
                        suggestion.put("description", String.format("停车时长 %.1f 小时，超过限制", parkingHours));
                        suggestion.put("severity", parkingHours > 12 ? "severe" : "moderate");
                        suggestion.put("appointmentId", appointment.get("id"));
                        suggestions.add(suggestion);
                    }
                } catch (Exception e) {
                    log.warn("计算停车时长失败: {}", e.getMessage());
                }
            }
            
            // 分析是否未按时离场（还在停车场）
            if (StringUtils.hasText(arriveDate) && !StringUtils.hasText(leaveDate)) {
                Map<String, Object> suggestion = new HashMap<>();
                suggestion.put("type", "未按时离场");
                suggestion.put("description", "车辆已进场但未记录离场时间");
                suggestion.put("severity", "moderate");
                suggestion.put("appointmentId", appointment.get("id"));
                suggestions.add(suggestion);
            }
        }
        
        result.put("violationSuggestions", suggestions);
        result.put("plateNumber", plateNumber.trim());
        
        return result;
    }

    /**
     * 计算停车时长（小时）
     */
    private double calculateParkingHours(String arriveDate, String leaveDate) {
        // 这里需要根据实际的时间格式进行解析
        // 简化处理，假设时间格式为 "yyyy-MM-dd HH:mm:ss" 或其他格式
        try {
            // 临时实现，实际应该解析具体的时间格式
            // 返回模拟的时长
            return 10.5; // 示例值
        } catch (Exception e) {
            log.warn("解析时间失败: arriveDate={}, leaveDate={}", arriveDate, leaveDate);
            return 0;
        }
    }

    /**
     * 🆕 通过业主信息关联查询预约记录
     * 关联ownerinfo表和appointment表，严格筛选与当前巡逻员相同小区的数据
     */
    @Override
    public List<Map<String, Object>> getAppointmentRecordsByOwnerInfo(String keyword, Integer page, Integer size, String usercode) {
        try {
            log.info("🔍 [Service] 开始查询预约记录关联: keyword={}, page={}, size={}, usercode={}", 
                    keyword, page, size, usercode);

            // 计算分页参数
            int offset = (page - 1) * size;

            // 获取当前用户的小区信息（用于权限过滤）
            final String currentUserCommunity;
            if (usercode != null && !usercode.trim().isEmpty()) {
                // 从patrol表获取巡逻员负责的小区
                String tempCommunity = null;
                try {
                    LambdaQueryWrapper<Patrol> patrolQuery = new LambdaQueryWrapper<>();
                    patrolQuery.eq(Patrol::getUsercode, usercode);
                    Patrol patrol = patrolMapper.selectOne(patrolQuery);
                    
                    if (patrol != null && patrol.getCommunity() != null) {
                        tempCommunity = patrol.getCommunity();
                        log.info("🏘️ [巡逻员小区] 从patrol表获取到小区: {}", tempCommunity);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ [获取巡逻员小区失败] {}", e.getMessage());
                }
                currentUserCommunity = tempCommunity;
            } else {
                currentUserCommunity = null;
            }

            // 🚨 重要：如果是巡逻员但没有获取到小区信息，返回空结果
            if (usercode != null && (currentUserCommunity == null || currentUserCommunity.trim().isEmpty())) {
                log.warn("⚠️ [权限控制] 巡逻员用户 {} 未找到负责的小区，返回空结果", usercode);
                return new ArrayList<>();
            }

            // 构建原生SQL查询 - 关联ownerinfo和appointment表，强制小区过滤
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT DISTINCT ");
            sql.append("    a.id as appointment_id, ");
            sql.append("    a.plate_number, ");
            sql.append("    a.arrive_date, ");
            sql.append("    a.leave_date, ");
            sql.append("    a.parking_space, ");
            sql.append("    a.status as appointment_status, ");
            sql.append("    a.created_at as appointment_created_at, ");
            sql.append("    o.id as owner_id, ");
            sql.append("    o.name as owner_name, ");
            sql.append("    o.phone as owner_phone, ");
            sql.append("    o.community, ");
            sql.append("    TIMESTAMPDIFF(HOUR, a.arrive_date, a.leave_date) as parking_hours ");
            sql.append("FROM appointment a ");
            sql.append("INNER JOIN ownerinfo o ON a.plate_number = o.plate_number ");
            sql.append("WHERE 1=1 ");

            // 🔒 严格的小区权限过滤：所有用户都需要小区匹配
            if (currentUserCommunity != null && !currentUserCommunity.trim().isEmpty()) {
                // 对于巡逻员：强制小区过滤
                sql.append("AND o.community = '").append(currentUserCommunity.trim()).append("' ");
                log.info("🔐 [小区权限过滤] 限制小区: {}", currentUserCommunity);
            } else if (usercode != null) {
                // 巡逻员但没有小区信息，直接返回空
                log.warn("⚠️ [权限拒绝] 巡逻员用户必须有小区信息");
                return new ArrayList<>();
            }

            // 额外的小区一致性检查：确保appointment表可能有的小区字段也匹配
            if (currentUserCommunity != null && !currentUserCommunity.trim().isEmpty()) {
                // 如果appointment表也有community字段，也要匹配
                sql.append("AND (a.community IS NULL OR a.community = '' OR a.community = '").append(currentUserCommunity.trim()).append("') ");
            }

            // 关键词搜索条件
            if (keyword != null && !keyword.trim().isEmpty()) {
                sql.append("AND (");
                sql.append("    a.plate_number LIKE '%").append(keyword.trim()).append("%' ");
                sql.append("    OR o.name LIKE '%").append(keyword.trim()).append("%' ");
                sql.append("    OR o.phone LIKE '%").append(keyword.trim()).append("%' ");
                sql.append(") ");
            }

            // 审核状态过滤：只查询已审核的业主
            sql.append("AND o.isaudit = '是' ");

            // 排序和分页
            sql.append("ORDER BY a.created_at DESC ");
            sql.append("LIMIT ").append(size).append(" OFFSET ").append(offset);

            log.info("🔍 [SQL查询] {}", sql.toString());

            // 执行原生SQL查询
            QueryWrapper<Violations> sqlQuery = new QueryWrapper<>();
            sqlQuery.apply(sql.toString());
            List<Map<String, Object>> records = violationsMapper.selectMaps(sqlQuery);

            log.info("✅ [Service] 查询完成: 共{}条记录", records.size());

            // 🔍 后处理：再次确认小区一致性（双重保险）
            List<Map<String, Object>> filteredRecords = records;
            if (usercode != null && currentUserCommunity != null) {
                filteredRecords = records.stream()
                    .filter(record -> {
                        String recordCommunity = (String) record.get("community");
                        boolean match = currentUserCommunity.equals(recordCommunity);
                        if (!match) {
                            log.warn("⚠️ [二次过滤] 发现小区不匹配的记录: 期望={}, 实际={}, 车牌={}", 
                                currentUserCommunity, recordCommunity, record.get("plate_number"));
                        }
                        return match;
                    })
                    .collect(Collectors.toList());
                
                log.info("🔍 [二次过滤] 原始记录{}条, 过滤后{}条", records.size(), filteredRecords.size());
            }

            // 数据处理和格式化
            return filteredRecords.stream().map(record -> {
                Map<String, Object> result = new HashMap<>();
                
                // 预约记录信息
                result.put("appointmentId", record.get("appointment_id"));
                result.put("plateNumber", record.get("plate_number"));
                result.put("arriveDate", record.get("arrive_date"));
                result.put("leaveDate", record.get("leave_date"));
                result.put("parkingSpace", record.get("parking_space"));
                result.put("appointmentStatus", record.get("appointment_status"));
                result.put("appointmentCreatedAt", record.get("appointment_created_at"));
                
                // 车主信息 - 支持本地车主和月票车主
                result.put("ownerId", record.get("owner_id"));
                result.put("ownerName", record.get("owner_name"));
                result.put("ownerPhone", record.get("owner_phone"));
                result.put("community", record.get("community"));
                
                // 新增：业主类型和月票相关信息
                result.put("ownerType", record.get("ownerType")); // local/monthly/unknown
                result.put("monthTicketName", record.get("monthTicketName"));
                result.put("parkName", record.get("parkName"));
                result.put("monthTicketStatus", record.get("monthTicketStatus"));
                
                // 停车时长（使用SQL计算的结果或手动计算）
                if (record.get("parking_hours") != null) {
                    result.put("parkingDuration", record.get("parking_hours"));
                } else if (record.get("arrive_date") != null && record.get("leave_date") != null) {
                    double duration = calculateParkingHours(
                        record.get("arrive_date").toString(),
                        record.get("leave_date").toString()
                    );
                    result.put("parkingDuration", duration);
                }
                
                return result;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ [Service] 查询预约记录关联失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询预约记录失败: " + e.getMessage());
        }
    }



    /**
     * 诊断patrol表查询问题
     */
    public void diagnosisPatrolQuery() {
        try {
            log.info("🔍 [诊断] 开始诊断patrol表查询问题");
            
            // 1. 检查patrol表是否存在数据
            QueryWrapper<Violations> countQuery = new QueryWrapper<>();
            countQuery.apply("SELECT COUNT(*) as count FROM patrol");
            List<Map<String, Object>> countResult = violationsMapper.selectMaps(countQuery);
            log.info("📊 [诊断] patrol表总记录数: {}", countResult.get(0).get("count"));
            
            // 2. 查看patrol表所有数据
            QueryWrapper<Violations> allQuery = new QueryWrapper<>();
            allQuery.apply("SELECT * FROM patrol LIMIT 10");
            List<Map<String, Object>> allResult = violationsMapper.selectMaps(allQuery);
            log.info("📋 [诊断] patrol表前10条记录: {}", allResult);
            
            // 3. 检查usercode字段的值
            QueryWrapper<Violations> usercodeQuery = new QueryWrapper<>();
            usercodeQuery.apply("SELECT DISTINCT usercode FROM patrol");
            List<Map<String, Object>> usercodeResult = violationsMapper.selectMaps(usercodeQuery);
            log.info("🔑 [诊断] patrol表中所有usercode值: {}", usercodeResult);
            
            // 4. 测试当前用户ID查询
            String currentUserId = "002"; // 示例usercode
            log.info("👤 [诊断] 当前用户ID: '{}', 类型: {}", currentUserId, currentUserId.getClass().getSimpleName());
            
            QueryWrapper<Violations> specificQuery = new QueryWrapper<>();
            specificQuery.apply("SELECT * FROM patrol WHERE usercode = {0}", currentUserId);
            List<Map<String, Object>> specificResult = violationsMapper.selectMaps(specificQuery);
            log.info("🎯 [诊断] 当前用户ID查询结果: {}", specificResult);
            
            // 5. 尝试不同的匹配方式
            QueryWrapper<Violations> likeQuery = new QueryWrapper<>();
            likeQuery.apply("SELECT * FROM patrol WHERE usercode LIKE '%{0}%'", currentUserId);
            List<Map<String, Object>> likeResult = violationsMapper.selectMaps(likeQuery);
            log.info("🔍 [诊断] 模糊匹配查询结果: {}", likeResult);
            
        } catch (Exception e) {
            log.error("❌ [诊断] 诊断过程出错: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean updateLeaveTimeByPlateAndTime(String plateNumber, String parkCode, String enterTime, String leaveTime) {
        try {
            log.info("🔄 [预离场更新] 开始处理: plateNumber={}, parkCode={}, enterTime={}, leaveTime={}", 
                    plateNumber, parkCode, enterTime, leaveTime);
            
            // 1. 转换时间格式：从 yyyyMMddHHmmss 转换为 LocalDateTime
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            Date enterDate = inputFormat.parse(enterTime);
            Date leaveDate = inputFormat.parse(leaveTime);
            
            LocalDateTime enterDateTime = enterDate.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
            LocalDateTime leaveDateTime = leaveDate.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
            
            log.info("⏰ [时间转换] enterDateTime={}, leaveDateTime={}", enterDateTime, leaveDateTime);
            
            // 2. 查询 violations 表中符合条件的记录
            LambdaQueryWrapper<Violations> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Violations::getPlateNumber, plateNumber)
                       .eq(Violations::getParkCode, parkCode)
                       .isNull(Violations::getLeaveTime); // leave_time 必须为空
            
            List<Violations> violations = this.list(queryWrapper);
            
            if (violations.isEmpty()) {
                log.warn("⚠️ [预离场更新] 未找到匹配的违规记录: plateNumber={}, parkCode={}", plateNumber, parkCode);
                return false;
            }
            
            log.info("🔍 [预离场更新] 找到 {} 条符合条件的记录", violations.size());
            
            // 3. 遍历记录，找到时间差不超过5秒的记录
            Violations targetViolation = null;
            long minTimeDiff = Long.MAX_VALUE;
            
            for (Violations violation : violations) {
                if (violation.getEnterTime() != null) {
                    // 计算时间差（毫秒）
                    long timeDiff = Math.abs(
                            violation.getEnterTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() -
                            enterDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    );
                    
                    log.info("⏱️ [时间比较] 记录ID={}, 数据库enterTime={}, 时间差={}ms", 
                            violation.getId(), violation.getEnterTime(), timeDiff);
                    
                    // 如果时间差不超过5秒（5000毫秒）
                    if (timeDiff <= 5000 && timeDiff < minTimeDiff) {
                        minTimeDiff = timeDiff;
                        targetViolation = violation;
                    }
                }
            }
            
            // 4. 如果找到匹配的记录，更新离场时间
            if (targetViolation != null) {
                log.info("✅ [预离场更新] 找到匹配记录: ID={}, 时间差={}ms", targetViolation.getId(), minTimeDiff);
                
                targetViolation.setLeaveTime(leaveDateTime);
                targetViolation.setUpdatedAt(LocalDateTime.now());
                
                boolean updateResult = this.updateById(targetViolation);
                
                if (updateResult) {
                    log.info("✅ [预离场更新] 更新成功: ID={}, leaveTime={}", targetViolation.getId(), leaveDateTime);
                    return true;
                } else {
                    log.error("❌ [预离场更新] 更新失败: ID={}", targetViolation.getId());
                    return false;
                }
            } else {
                log.warn("⚠️ [预离场更新] 未找到时间差不超过5秒的记录");
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ [预离场更新] 处理失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 🆕 获取月票车超时配置
     */
    @Override
    public Map<String, Object> getMonthlyTicketTimeoutConfig(String parkCode) {
        log.info("🔧 [获取月票车超时配置] parkCode={}", parkCode);
        
        try {
            // 使用专门的配置服务获取配置
            MonthlyTicketTimeoutConfig config = monthlyTicketTimeoutConfigService.getByParkCode(parkCode);
            
            Map<String, Object> result = new HashMap<>();
            if (config != null) {
                result.put("timeoutMinutes", config.getTimeoutMinutes());
                result.put("maxViolationCount", config.getMaxViolationCount());
                result.put("isActive", config.getIsActive());
                result.put("description", config.getDescription());
                
                // 🆕 新增过夜配置字段
                result.put("nightStartTime", config.getNightStartTime());
                result.put("nightEndTime", config.getNightEndTime());
                result.put("nightTimeHours", config.getNightTimeHours());
                result.put("enableOvernightCheck", config.getEnableOvernightCheck());
                
                // 🆕 尝试获取免检月票类型（从description或其他字段解析）
                java.util.List<String> exemptTicketTypes = parseExemptTicketTypes(config.getDescription());
                result.put("exemptTicketTypes", exemptTicketTypes);
            } else {
                // 设置默认值
                result.put("timeoutMinutes", 60); // 默认1小时
                result.put("maxViolationCount", 5); // 默认5次
                result.put("isActive", true);
                result.put("description", "默认配置");
                result.put("nightStartTime", "22:00");
                result.put("nightEndTime", "06:00");
                result.put("nightTimeHours", 4);
                result.put("enableOvernightCheck", 1);
                result.put("exemptTicketTypes", new java.util.ArrayList<>());
            }
            
            log.info("✅ [获取配置成功] parkCode={}, config={}", parkCode, result);
            return result;
            
        } catch (Exception e) {
            log.error("❌ [获取配置失败] parkCode={}, error={}", parkCode, e.getMessage(), e);
            
            // 返回默认配置
            Map<String, Object> defaultConfig = new HashMap<>();
            defaultConfig.put("timeoutMinutes", 60);
            defaultConfig.put("maxViolationCount", 5);
            defaultConfig.put("isActive", true);
            defaultConfig.put("description", "默认配置");
            return defaultConfig;
        }
    }

    /**
     * 🆕 保存月票车超时配置
     */
    @Override
    public boolean saveMonthlyTicketTimeoutConfig(String parkCode, Integer timeoutMinutes, Integer maxViolationCount, String operatorId) {
        log.info("💾 [保存月票车超时配置] parkCode={}, timeout={}分钟, maxCount={}, operator={}", 
                parkCode, timeoutMinutes, maxViolationCount, operatorId);
        
        // 使用专门的配置表而不是violations表
        return monthlyTicketTimeoutConfigService.saveOrUpdateConfig(parkCode, null, timeoutMinutes, maxViolationCount, operatorId);
    }
    
    @Override
    public boolean saveMonthlyTicketFullConfig(String parkCode, Integer timeoutMinutes, Integer maxViolationCount,
                                              String nightStartTime, String nightEndTime, Integer nightTimeHours,
                                              Boolean enableOvernightCheck, String operatorId) {
        log.info("💾 [保存月票车完整配置] parkCode={}, timeout={}分钟, maxCount={}, night={}:{}-{} {}小时, enabled={}, operator={}", 
                parkCode, timeoutMinutes, maxViolationCount, nightStartTime, nightEndTime, nightTimeHours, 
                enableOvernightCheck, operatorId);
        
        // 使用专门的配置表保存完整配置
        return monthlyTicketTimeoutConfigService.saveOrUpdateFullConfig(parkCode, null, timeoutMinutes, maxViolationCount,
                                                                        nightStartTime, nightEndTime, nightTimeHours, 
                                                                        enableOvernightCheck, operatorId);
    }
    
    // ==================== 🆕 东北林业大学违规阈值配置实现 ====================
    
    /**
     * 获取东北林业大学违规阈值配置
     */
    @Override
    public java.util.Map<String, Object> getNebuViolationConfig() {
        log.info("🎓 [获取东北林业大学违规阈值配置]");
        
        try {
            // 使用新的配置服务查询配置
            ViolationConfig config = violationConfigService.getByParkNameAndType(
                "东北林业大学", 
                "NEBU_AUTO_BLACKLIST"
            );
            
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            
            if (config != null) {
                // 从配置对象构建返回结果
                result.put("maxViolationCount", config.getMaxViolationCount() != null ? config.getMaxViolationCount() : 5);
                result.put("blacklistType", config.getBlacklistType());
                result.put("isPermanent", config.getIsPermanent() != null ? config.getIsPermanent() : true);
                result.put("blacklistValidDays", config.getBlacklistValidDays() != null ? config.getBlacklistValidDays() : 30);
                result.put("blacklistStartTime", config.getBlacklistStartTime());
                result.put("blacklistEndTime", config.getBlacklistEndTime());
                result.put("parkName", config.getParkName());
                result.put("parkCode", config.getParkCode());
                result.put("updateTime", config.getUpdatedAt());
                result.put("description", config.getDescription());
                
                log.info("✅ [配置查询成功] config={}", result);
            } else {
                // 无配置记录，返回默认值
                result.put("maxViolationCount", 5);
                result.put("parkName", "东北林业大学");
                result.put("isPermanent", true);
                result.put("blacklistValidDays", 30);
                log.info("ℹ️ [无配置记录] 使用默认值");
            }
            
            return result;
        } catch (Exception e) {
            log.error("❌ [获取配置失败]", e);
            // 返回默认配置
            java.util.Map<String, Object> defaultConfig = new java.util.HashMap<>();
            defaultConfig.put("maxViolationCount", 5);
            defaultConfig.put("parkName", "东北林业大学");
            defaultConfig.put("isPermanent", true);
            defaultConfig.put("blacklistValidDays", 30);
            return defaultConfig;
        }
    }
    
    /**
     * 保存东北林业大学违规阈值配置
     */
    @Override
    public boolean saveNebuViolationConfig(String parkName, Integer maxViolationCount, 
                                          String blacklistType, Boolean isPermanent,
                                          Integer blacklistValidDays,Integer reminderIntervalMinutes) {
        log.info("💾 [保存东北林业大学违规阈值配置] parkName={}, maxCount={}, blacklistType={}, isPermanent={}, validDays={}, reminderIntervalMinutes = {}",
                parkName, maxViolationCount, blacklistType, isPermanent, blacklistValidDays,reminderIntervalMinutes);
        
        try {
            // 使用新的配置服务保存配置（将有效天数保存到数据库）
            boolean result = violationConfigService.saveOrUpdateConfig(
                parkName,           // 车场名称
                null,              // 车场编码（东北林大使用车场名称）
                "NEBU_AUTO_BLACKLIST",  // 配置类型
                maxViolationCount,
                blacklistType,
                isPermanent,
                blacklistValidDays,  // 有效天数
                null               // 操作人ID（可从上下文获取）
                ,reminderIntervalMinutes
            );
            
            log.info("✅ [配置保存{}] parkName={}, validDays={}", result ? "成功" : "失败", parkName, blacklistValidDays);
            return result;
        } catch (Exception e) {
            log.error("❌ [保存配置失败]", e);
            return false;
        }
    }
    
    // ==================== 🆕 月票车超时配置实现 ====================

    // ==================== 🆕 学院新城拉黑规则配置实现 ====================
    @Override
    public java.util.Map<String, Object> getCollegeNewCityConfig(String parkCode) {
        log.info("🎓 [学院新城] 获取配置 parkCode={}", parkCode);
        try {
            ViolationConfig config = violationConfigService.getByParkCodeAndType(parkCode, "COLLEGE_NEW_CITY_OVERNIGHT");

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            if (config != null) {
                result.put("parkCode", config.getParkCode());
                result.put("parkName", config.getParkName());
                result.put("blacklistType", config.getBlacklistType());
                result.put("isPermanent", config.getIsPermanent());
                result.put("blacklistValidDays", config.getBlacklistValidDays());
                result.put("description", config.getDescription());
                result.put("updatedAt", config.getUpdatedAt());
            } else {
                result.put("parkCode", parkCode);
                result.put("blacklistType", "违章黑名单");
                result.put("isPermanent", true);
                result.put("blacklistValidDays", null);
            }
            return result;
        } catch (Exception e) {
            log.error("❌ [学院新城] 获取配置失败 parkCode={}", parkCode, e);
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("parkCode", parkCode);
            result.put("blacklistType", "违章黑名单");
            result.put("isPermanent", true);
            result.put("blacklistValidDays", null);
            return result;
        }
    }

    @Override
    public boolean saveCollegeNewCityConfig(String parkCode, String parkName, Integer blacklistTimeHours,
                                            String blacklistTypeName, Boolean isPermanent, Integer blacklistValidDays,
                                            Integer nightStartHour, Integer nightEndHour) {
        log.info("💾 [学院新城] 保存配置 parkCode={}, parkName={}, hours={}, typeName={}, permanent={}, validDays={}, nightStart={}, nightEnd={}",
                parkCode, parkName, blacklistTimeHours, blacklistTypeName, isPermanent, blacklistValidDays, nightStartHour, nightEndHour);
        try {
            boolean ok = violationConfigService.saveOrUpdateConfig(
                    parkName,
                    parkCode,
                    "COLLEGE_NEW_CITY_OVERNIGHT",
                    null,
                    blacklistTypeName,
                    isPermanent,
                    (isPermanent != null && isPermanent) ? null : blacklistValidDays,
                    null
                    , null
            );
            // 同步保存到 monthly_ticket_timeout_config，与“欧洲新城”一致
            Integer resolvedNightStart = nightStartHour != null ? nightStartHour : 0;
            Integer resolvedNightEnd = nightEndHour != null ? nightEndHour : 7;
            Integer resolvedOvernightHours = blacklistTimeHours != null ? blacklistTimeHours : 1;

            String nightStartTimeStr = String.format("%02d:00", resolvedNightStart);
            String nightEndTimeStr = String.format("%02d:00", resolvedNightEnd);

            // 参照“欧洲新城”默认：timeoutMinutes=60, maxViolationCount=5, 启用过夜检查
            boolean mtOk = monthlyTicketTimeoutConfigService.saveOrUpdateFullConfig(
                    parkCode,
                    parkName,
                    60,
                    5,
                    nightStartTimeStr,
                    nightEndTimeStr,
                    resolvedOvernightHours,
                    true,
                    "SYSTEM"
            );

            return ok && mtOk;
        } catch (Exception e) {
            log.error("❌ [学院新城] 保存配置失败 parkCode={}", parkCode, e);
            return false;
        }
    }

    @Override
    public boolean saveMonthlyTicketFullConfigWithExempt(String parkCode, Integer timeoutMinutes, Integer maxViolationCount,
                                                        Integer overnightTimeHours, Boolean enableOvernightCheck, 
                                                        java.util.List<String> exemptTicketTypes, String operatorId) {
        log.info("💾 [保存月票车完整配置含免检] parkCode={}, timeout={}分钟, maxCount={}, overnightHours={}, enabled={}, exempt={}, operator={}", 
                parkCode, timeoutMinutes, maxViolationCount, overnightTimeHours, 
                enableOvernightCheck, exemptTicketTypes, operatorId);
        
        // 🔍 详细调试免检类型参数
        log.info("🔍 [Service调试] 免检类型参数: {}", exemptTicketTypes);
        log.info("🔍 [Service调试] 免检类型是否为null: {}", exemptTicketTypes == null);
        log.info("🔍 [Service调试] 免检类型是否为空: {}", exemptTicketTypes != null ? exemptTicketTypes.isEmpty() : "参数为null");
        
        // 暂时使用现有方法保存配置，免检类型存储到description字段中
        String description = buildDescriptionWithExempt(timeoutMinutes, maxViolationCount, overnightTimeHours, exemptTicketTypes);
        log.info("🔍 [Service调试] 构建的description: {}", description);
        
        // 先保存基础配置（使用新的方法签名）
        boolean result = monthlyTicketTimeoutConfigService.saveOrUpdateOvernightConfig(parkCode, null, timeoutMinutes, maxViolationCount,
                                                                                       overnightTimeHours, enableOvernightCheck, operatorId);
        
        // 如果保存成功，更新description字段包含免检类型信息
        if (result) {
            MonthlyTicketTimeoutConfig config = monthlyTicketTimeoutConfigService.getByParkCode(parkCode);
            if (config != null) {
                config.setDescription(description);
                monthlyTicketTimeoutConfigService.updateById(config);
            }
        }
        
        return result;
    }


    @Override
    public boolean recordViolation(String plateNumber, String parkCode, String parkName, 
                                 LocalDateTime enterTime, LocalDateTime leaveTime, 
                                 Long parkingDurationMinutes, String violationType, String violationDescription,
                                 Integer monthTicketId, boolean shouldDirectBlacklist) {
        boolean isMonthlyTicket = monthTicketId != null;
        String vehicleType = isMonthlyTicket ? "月票车" : "非月票车";
        log.info("📝 [记录{}违规] plateNumber={}, parkCode={}, violationType={}, monthTicketId={}", 
                vehicleType, plateNumber, parkCode, violationType, monthTicketId);
        
        try {
            Violations violation = new Violations();
            violation.setPlateNumber(plateNumber);
            violation.setParkCode(parkCode);
            violation.setParkName(parkName);
            violation.setIsMonthlyTicket(isMonthlyTicket);
            violation.setMonthTicketId(monthTicketId);
            violation.setEnterTime(enterTime);
            violation.setLeaveTime(leaveTime);
            violation.setCreatedAt(LocalDateTime.now());
            violation.setUpdatedAt(LocalDateTime.now());
            violation.setStatus("PENDING");
            violation.setCreatedBy("SYSTEM");
            
            violation.setViolationType(violationType);
            violation.setDescription(violationDescription);
            violation.setLocation(parkName);
            
            // 🎫 [调用外部月票接口] 获取车主信息（仅月票车）
            if (parkCode != null && !parkCode.isEmpty()) {
                try {
                    log.info("🔍 [月票查询] 开始调用外部接口: plateNumber={}, parkCode={}", plateNumber, parkCode);
                    
                    // 调用外部月票接口
                    String apiUrl = "http://www.xuerparking.cn:8543/parking/monthTicket/getValidMonthTicketByPlate";
                    HashMap<String, String> params = new HashMap<>();
                    params.put("plateNumber", plateNumber);
                    params.put("parkCode", parkCode);
                    
                    String responseJson = com.parkingmanage.common.HttpClientUtil.doGet(apiUrl, params);
                    com.alibaba.fastjson.JSONObject response = com.alibaba.fastjson.JSONObject.parseObject(responseJson);
                    
                    log.info("📥 [接口响应] response: {}", response);
                    
                    if (response != null && "0".equals(response.getString("code"))) {
                        com.alibaba.fastjson.JSONObject outerData = response.getJSONObject("data");
                        
                        log.info("📦 [外层数据] outerData: {}", outerData);
                        
                        if (outerData != null && "0".equals(outerData.getString("code"))) {
                            // 实际的月票信息在嵌套的 data 字段中
                            com.alibaba.fastjson.JSONObject data = outerData.getJSONObject("data");
                            
                            log.info("📦 [实际数据] data: {}", data);
                            
                            if (data != null) {
                                String ticketName = data.getString("ticketName");
                                String userName = data.getString("userName");
                                String userPhone = data.getString("userPhone");
                                
                                log.info("🔍 [字段提取] ticketName={}, userName={}, userPhone={}", 
                                        ticketName, userName, userPhone);
                            
                            // 写入violations表
                            violation.setVipTypeName(ticketName);
                            violation.setOwnerName(userName);
                            violation.setOwnerPhone(userPhone);
                            
                            // 如果查到了月票信息，说明这是月票车（可能是转临时车的情况）
                            if (!isMonthlyTicket) {
                                // 修改违规类型，体现月票车转临时车
                                String originalType = violation.getViolationType();
                                if (originalType != null && originalType.contains("临时车")) {
                                    violation.setViolationType(originalType.replace("临时车", "月票车转临时车"));
                                    log.warn("⚠️ [月票车转临时车] 车牌: {}, 违规类型: {} -> {}", 
                                            plateNumber, originalType, violation.getViolationType());
                                } else {
                                    log.warn("⚠️ [月票车转临时车] 车牌: {}, 违规记录标记为临时车，但查到了有效月票信息", plateNumber);
                                }
                            }
                            
                                log.info("✅ [月票信息获取成功] 月票类型={}, 车主={}, 电话={}",
                                        ticketName, userName, userPhone);
                            } else {
                                log.info("ℹ️ [月票接口无数据] 车牌号: {}", plateNumber);
                            }
                        } else {
                            log.info("ℹ️ [月票查询失败] 车牌号: {}, outerData.code={}", 
                                    plateNumber, outerData != null ? outerData.getString("code") : "null");
                        }
                    } else {
                        log.info("ℹ️ [接口调用失败] 车牌号: {}, response.code={}", 
                                plateNumber, response != null ? response.getString("code") : "null");
                    }
                    
                } catch (Exception e) {
                    log.error("⚠️ [月票查询异常] 车牌号: {}, 错误: {}", plateNumber, e.getMessage());
                    // 异常不影响违规记录的创建，继续后续流程
                }
            }
            
            // 根据违规类型设置严重程度
            if (violationType.contains("过夜")) {
                violation.setSeverity("severe");
                violation.setShouldBlacklist(1); // 过夜直接标记拉黑
                violation.setBlacklistReason(violationType);
            } else if (shouldDirectBlacklist) {
                violation.setSeverity("severe");
                violation.setShouldBlacklist(1);
                violation.setBlacklistReason(violationType);
            } else {
                violation.setSeverity("moderate");
                violation.setShouldBlacklist(0); // 需累计处理
            }
            
            boolean result = baseMapper.insert(violation) > 0;
            
            if (result) {
                log.info("✅ [违规记录成功] plateNumber={}, violationType={}", plateNumber, violation.getViolationType());
            } else {
                log.error("❌ [违规记录失败] plateNumber={}", plateNumber);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ [违规记录异常] plateNumber={}, error={}", plateNumber, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 🆕 检查是否存在重复的违规记录
     */
    @Override
    public boolean checkDuplicateViolation(String plateNumber, String parkCode, LocalDateTime enterTime, LocalDateTime leaveTime) {
        log.info("🔍 [检查重复违规] plateNumber={}, parkCode={}, enterTime={}, leaveTime={}", 
                plateNumber, parkCode, enterTime, leaveTime);
        
        try {
            // 查询相同车牌、相同停车场、相同时间段内的违规记录
            // 考虑时间的误差范围，前后各允许5分钟的误差
            LocalDateTime enterTimeStart = enterTime.minusMinutes(5);
            LocalDateTime enterTimeEnd = enterTime.plusMinutes(5);
            LocalDateTime leaveTimeStart = leaveTime.minusMinutes(5);
            LocalDateTime leaveTimeEnd = leaveTime.plusMinutes(5);
            
            LambdaQueryWrapper<Violations> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Violations::getPlateNumber, plateNumber)
                    .eq(Violations::getParkCode, parkCode)
                    .between(Violations::getEnterTime, enterTimeStart, enterTimeEnd)
                    .in(Violations::getViolationType, "超时停车", "月票车超时停车", "月票车过夜停车", "临时车超时停车",
                    "月票车转临时车超时停车","非月票车夜间超白天限制", "月票车夜间超白天限制"); // 检查所有超时相关的违规类型
            
            // 对于离场时间的检查，需要考虑小程序手动添加的记录可能没有离场时间
            // 所以使用OR条件：要么离场时间在范围内，要么离场时间为空
            queryWrapper.and(wrapper -> wrapper
                    .between(Violations::getLeaveTime, leaveTimeStart, leaveTimeEnd)
                    .or()
                    .isNull(Violations::getLeaveTime)
            );
            
            List<Violations> existingViolations = baseMapper.selectList(queryWrapper);
            
            if (!existingViolations.isEmpty()) {
                log.warn("⚠️ [发现重复违规] plateNumber={}, 存在{}条相同时间段的违规记录:", plateNumber, existingViolations.size());
                for (Violations violation : existingViolations) {
                    log.warn("    - ID={}, 类型={}, 进场时间={}, 离场时间={}, 创建者={}", 
                            violation.getId(), violation.getViolationType(), 
                            violation.getEnterTime(), violation.getLeaveTime(), violation.getCreatedBy());
                }
                return true;
            } else {
                log.info("✅ [无重复违规] plateNumber={}, 未发现重复记录", plateNumber);
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ [检查重复违规异常] plateNumber={}, error={}", plateNumber, e.getMessage(), e);
            // 出现异常时，为了安全起见，返回false，允许记录违规
            return false;
        }
    }

    /**
     * 🆕 检查车辆违规次数并决定是否加入黑名单（支持月票车和非月票车）
     */
    @Override
    public boolean checkAndProcessBlacklist(String plateNumber, String parkCode) {
        log.info("🔍 [检查黑名单条件] plateNumber={}, parkCode={}", plateNumber, parkCode);
        
        try {
            // 获取配置
            Map<String, Object> config = getMonthlyTicketTimeoutConfig(parkCode);
            Integer maxViolationCount = (Integer) config.get("maxViolationCount");
            
            // 分别统计月票车和非月票车的违规次数
            QueryWrapper<Violations> monthlyTicketQuery = new QueryWrapper<>();
            monthlyTicketQuery.eq("plate_number", plateNumber)
                      .eq("park_code", parkCode)
                      .eq("is_monthly_ticket", true)
                      .in("violation_type", "月票车超时停车", "月票车夜间超白天限制", "月票车过夜停车");
//                      .ge("created_at", LocalDateTime.now().minusDays(30)); // 最近30天内的违规
            
            long monthlyTicketViolationCount = baseMapper.selectCount(monthlyTicketQuery);
            
            QueryWrapper<Violations> nonMonthlyTicketQuery = new QueryWrapper<>();
            nonMonthlyTicketQuery.eq("plate_number", plateNumber)
                      .eq("park_code", parkCode)
                      .eq("is_monthly_ticket", false)
                      .in("violation_type", "超时停车", "非月票车夜间超白天限制")
                      .ge("created_at", LocalDateTime.now().minusDays(30)); // 最近30天内的违规
            
            long nonMonthlyTicketViolationCount = baseMapper.selectCount(nonMonthlyTicketQuery);
            
            log.info("📊 [违规统计] plateNumber={}, parkCode={}, 月票车违规={}, 非月票车违规={}, 阈值={}", 
                    plateNumber, parkCode, monthlyTicketViolationCount, nonMonthlyTicketViolationCount, maxViolationCount);
            
            boolean shouldBlacklist = false;
            String reason = "";
            
            if (monthlyTicketViolationCount >= maxViolationCount) {
                shouldBlacklist = true;
                reason = String.format("月票车累计违规%d次", monthlyTicketViolationCount);
            } else if (nonMonthlyTicketViolationCount >= maxViolationCount) {
                shouldBlacklist = true;
                reason = String.format("非月票车累计违规%d次", nonMonthlyTicketViolationCount);
            }
            
            if (shouldBlacklist) {
                log.info("⚠️ [符合拉黑条件] plateNumber={}, reason={}", plateNumber, reason);
                
                // 查询停车场名称
                QueryWrapper<YardInfo> parkQuery = new QueryWrapper<>();
                parkQuery.eq("yard_code", parkCode)
                        .isNotNull("yard_name")
                        .orderByDesc("gmt_create")
                        .last("LIMIT 1");

                YardInfo parkInfo = yardInfoMapper.selectOne(parkQuery);
                String parkName = parkInfo != null ? parkInfo.getYardName() : "未知停车场";
                
                // 添加到黑名单
                return addToBlacklist(plateNumber, parkName, reason, "系统自动加入黑名单");
            } else {
                log.info("✅ [未达到拉黑条件] plateNumber={}, 月票车违规={}/{}, 非月票车违规={}/{}", 
                        plateNumber, monthlyTicketViolationCount, maxViolationCount, 
                        nonMonthlyTicketViolationCount, maxViolationCount);
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ [检查黑名单异常] plateNumber={}, error={}", plateNumber, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 🆕 添加车辆到黑名单
     */
    @Override
    public boolean addToBlacklist(String plateNumber, String parkName, String reason, String remark) {
        log.info("🚫 [添加黑名单] plateNumber={}, parkName={}, reason={}", plateNumber, parkName, reason);
        
        try {
            // 🆕 过夜拉黑前先清理该车牌的所有违规记录
            if (reason != null && (reason.contains("过夜") || reason.contains("夜间时段停车违规") || 
                reason.contains("月票车过夜停车") || reason.contains("临时车过夜停车") || reason.contains("车辆过夜停车"))) {
                log.info("🧹 [过夜拉黑清理] 开始清理车牌号 {} 的所有违规记录", plateNumber);
                int deletedCount = deleteViolationsByPlateAndPark(plateNumber, parkName);
                log.info("✅ [过夜拉黑清理] 成功清理车牌号 {} 的 {} 条违规记录", plateNumber, deletedCount);
            }
            
            // 检查是否已经在黑名单中
            QueryWrapper<BlackList> existQuery = new QueryWrapper<>();
            existQuery.eq("car_code", plateNumber)
                     .eq("park_name", parkName)
                     .eq("reason", reason);
            
            BlackList existing = blackListMapper.selectOne(existQuery);
            if (existing != null) {
                log.info("ℹ️ [已在黑名单] plateNumber={}, parkName={}", plateNumber, parkName);
                return true; // 已经存在，返回成功
            }
            
            // 创建黑名单记录
            BlackList blackList = new BlackList();
            blackList.setCarCode(plateNumber);
            blackList.setParkName(parkName);
            blackList.setReason(reason);
            blackList.setRemark1(remark);
            blackList.setRemark2("月票车违规自动加入");
            blackList.setBlackListForeverFlag("1"); // 永久有效
            blackList.setSpecialCarTypeConfigName("月票车违规");
            blackList.setOwner(""); // 可以后续完善
            
            boolean result = blackListMapper.insert(blackList) > 0;
            
            if (result) {
                log.info("✅ [黑名单添加成功] plateNumber={}, id={}", plateNumber, blackList.getId());
                
                // 同时更新violations表中相关记录的拉黑状态
                QueryWrapper<Violations> updateQuery = new QueryWrapper<>();
                updateQuery.eq("plate_number", plateNumber)
                          .eq("park_name", parkName)
                          .eq("is_monthly_ticket", true);
                
                Violations updateViolation = new Violations();
                updateViolation.setShouldBlacklist(1);
                updateViolation.setBlacklistReason(reason);
                updateViolation.setUpdatedAt(LocalDateTime.now());
                
                baseMapper.update(updateViolation, updateQuery);
                
            } else {
                log.error("❌ [黑名单添加失败] plateNumber={}", plateNumber);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ [黑名单添加异常] plateNumber={}, error={}", plateNumber, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 🆕 智能过夜停车判定（基于时间段配置）
     * 
     * @param plateNumber 车牌号
     * @param parkCode 车场编码
     * @param enterTime 进场时间
     * @param leaveTime 离场时间
     * @param parkingDurationMinutes 停车时长（分钟）
     * @return 是否为过夜违规
     */
    public boolean checkIntelligentOvernightViolation(String plateNumber, String parkCode, 
                                                     LocalDateTime enterTime, LocalDateTime leaveTime, 
                                                     Long parkingDurationMinutes) {
        
        log.info("🧠 [智能过夜判定] plateNumber={}, parkCode={}, enterTime={}, leaveTime={}, duration={}分钟", 
                plateNumber, parkCode, enterTime, leaveTime, parkingDurationMinutes);
        
        try {
            // 1. 获取车场的过夜停车配置
            MonthlyTicketTimeoutConfig config = monthlyTicketTimeoutConfigService.getByParkCode(parkCode);
            
            if (config == null) {
                log.warn("⚠️ [配置缺失] parkCode={} 未找到配置，使用默认过夜判定", parkCode);
                // 使用传统方法：跨日期且超过12小时
                return isTraditionalOvernightParking(enterTime, leaveTime);
            }
            
            // 2. 检查是否启用过夜检查
            if (config.getEnableOvernightCheck() == null || config.getEnableOvernightCheck() != 1) {
                log.info("⚠️ [过夜检查禁用] parkCode={} 未启用过夜检查", parkCode);
                return false;
            }
            
            // 3. 使用配置的时间段进行过夜判定
            String nightStartTime = config.getNightStartTime() != null ? config.getNightStartTime() : "22:00";
            String nightEndTime = config.getNightEndTime() != null ? config.getNightEndTime() : "06:00";
            Integer nightTimeHours = config.getNightTimeHours() != null ? config.getNightTimeHours() : 4;
            
            log.info("🌙 [过夜配置] parkCode={}, 夜间时段: {}-{}, 限制: {}小时", 
                    parkCode, nightStartTime, nightEndTime, nightTimeHours);
            
            // 4. 使用过夜停车服务进行精确判定
            OvernightParkingService.OvernightParkingAnalysis analysis = 
                    overnightParkingService.analyzeOvernightParking(enterTime, leaveTime, 
                                                                  nightStartTime, nightEndTime, nightTimeHours);
            
            log.info("📊 [过夜分析结果] plateNumber={}, 总时长: {}小时, 夜间时长: {}小时, 违规: {}, 原因: {}", 
                    plateNumber, analysis.getTotalParkingHours(), analysis.getNightParkingHours(), 
                    analysis.isViolation(), analysis.getViolationReason());
            
            return analysis.isViolation();
            
        } catch (Exception e) {
            log.error("❌ [智能过夜判定异常] plateNumber={}, parkCode={}, error={}", 
                     plateNumber, parkCode, e.getMessage(), e);
            
            // 异常情况下使用传统判定方法
            return isTraditionalOvernightParking(enterTime, leaveTime);
        }
    }
    
    /**
     * 传统过夜停车判定（向后兼容）
     */
    private boolean isTraditionalOvernightParking(LocalDateTime enterTime, LocalDateTime leaveTime) {
        try {
            if (enterTime == null || leaveTime == null) {
                return false;
            }
            
            // 跨日期判定
            boolean crossDate = !enterTime.toLocalDate().equals(leaveTime.toLocalDate());
            
            // 超过12小时判定
            long hours = java.time.Duration.between(enterTime, leaveTime).toHours();
            boolean longParking = hours >= 12;
            
            boolean isOvernight = crossDate && longParking;
            
            log.debug("🔄 [传统过夜判定] 跨日期: {}, 超过12小时: {}, 结果: {}", crossDate, longParking, isOvernight);
            
            return isOvernight;
            
        } catch (Exception e) {
            log.error("❌ [传统过夜判定异常] error={}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int deleteViolationsByPlateAndPark(String plateNumber, String parkCode) {
        try {
            log.info("🗑️ [删除违规记录] 开始删除车牌号: {}, 停车场编码: {} 的所有违规记录", plateNumber, parkCode);
            
            LambdaQueryWrapper<Violations> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Violations::getPlateNumber, plateNumber);
            
            if (StringUtils.hasText(parkCode)) {
                wrapper.eq(Violations::getParkCode, parkCode);
            }
            
            int deletedCount = violationsMapper.delete(wrapper);
            
            log.info("✅ [删除违规记录] 成功删除 {} 条记录，车牌号: {}, 停车场编码: {}", 
                    deletedCount, plateNumber, parkCode);
            
            return deletedCount;
            
        } catch (Exception e) {
            log.error("❌ [删除违规记录异常] 车牌号: {}, 停车场编码: {}, error={}", 
                    plateNumber, parkCode, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public boolean manualProcessViolation(Long violationId, String operatorName, String processRemark) {
        return violationProcessService.manualProcessViolation(violationId, operatorName, processRemark);
    }

    @Override
    public int batchProcessViolations(List<Long> violationIds, String operatorName, String processRemark) {
        return violationProcessService.batchProcessViolations(violationIds, operatorName, processRemark);
    }

    @Override
    public boolean checkAndAutoBlacklist(String plateNumber, String parkCode) {
        return violationProcessService.checkAndAutoBlacklist(plateNumber, parkCode);
    }

    @Override
    public int countUnprocessedViolations(String plateNumber) {
        return violationProcessService.countUnprocessedViolations(plateNumber);
    }

    /**
     * 🆕 分页查询违规记录（支持处理状态和处理方式筛选）
     */
    @Override
    public IPage<Map<String, Object>> getViolationsWithProcess(
            Page<Map<String, Object>> page,
            String plateNumber,
            String status,
            String violationType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String createdByFilter,
            String communityFilter,
            String processStatus,
            String processType,
            Boolean onlyUnprocessed
    ) {
        log.info("🔍 [查询违规记录-含处理状态] plateNumber={}, status={}, violationType={}, processStatus={}, processType={}, onlyUnprocessed={}", 
                plateNumber, status, violationType, processStatus, processType, communityFilter);
        
        try {
            // 使用Mapper方法查询（需要在ViolationsMapper中添加相应方法）
            IPage<Map<String, Object>> result = violationsMapper.selectViolationsDirectQueryWithProcess(
                    page, plateNumber, status, violationType,
                    startDate, endDate, createdByFilter, communityFilter,
                    processStatus, processType, onlyUnprocessed
            );
            
            log.info("✅ [查询结果] 共查询到 {} 条违规记录", result.getTotal());
            return result;
            
        } catch (Exception e) {
            log.error("❌ [查询违规记录异常] error={}", e.getMessage(), e);
            // 返回空结果集
            return new Page<>(page.getCurrent(), page.getSize());
        }
    }
    
    /**
     * 从描述字符串中解析免检月票类型
     * @param description 配置描述
     * @return 免检月票类型列表
     */
    private java.util.List<String> parseExemptTicketTypes(String description) {
        java.util.List<String> exemptTypes = new java.util.ArrayList<>();
        
        if (description != null && description.contains("免检类型:")) {
            try {
                String exemptSection = description.substring(description.indexOf("免检类型:") + 5);
                if (exemptSection.contains(",")) {
                    exemptSection = exemptSection.substring(0, exemptSection.indexOf(","));
                }
                if (exemptSection.contains(";")) {
                    exemptSection = exemptSection.substring(0, exemptSection.indexOf(";"));
                }
                
                if (!exemptSection.trim().isEmpty() && !exemptSection.trim().equals("无")) {
                    String[] types = exemptSection.split("\\|");
                    for (String type : types) {
                        if (type != null && !type.trim().isEmpty()) {
                            exemptTypes.add(type.trim());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ [解析免检类型失败] description={}, error={}", description, e.getMessage());
            }
        }
        
        return exemptTypes;
    }
    
    /**
     * 构建包含免检类型的描述字符串
     * @param timeoutMinutes 超时时间（分钟）
     * @param maxViolationCount 最大违规次数
     * @param exemptTicketTypes 免检月票类型列表
     * @return 描述字符串
     */
    private String buildDescriptionWithExempt(Integer timeoutMinutes, Integer maxViolationCount, 
                                             Integer overnightTimeHours, java.util.List<String> exemptTicketTypes) {
        log.info("🔍 [构建描述调试] 开始构建description, exemptTicketTypes={}", exemptTicketTypes);
        
        StringBuilder desc = new StringBuilder();
        desc.append("月票车配置: 超时").append(timeoutMinutes).append("分钟,累计").append(maxViolationCount).append("次拉黑; ");
        desc.append("过夜超过").append(overnightTimeHours).append("小时直接拉黑");
        
        desc.append(",免检类型:");
        if (exemptTicketTypes != null && !exemptTicketTypes.isEmpty()) {
            log.info("🔍 [构建描述调试] 免检类型不为空，内容: {}", exemptTicketTypes);
            String joinedTypes = String.join("|", exemptTicketTypes);
            log.info("🔍 [构建描述调试] 拼接后的免检类型: {}", joinedTypes);
            desc.append(joinedTypes);
        } else {
            log.info("🔍 [构建描述调试] 免检类型为空或null，使用'无'");
            desc.append("无");
        }
        
        String result = desc.toString();
        log.info("🔍 [构建描述调试] 最终构建的description: {}", result);
        return result;
    }

    // ==================== 📊 新增统计分析实现 ====================

    @Override
    public List<Map<String, Object>> getTopViolators(Integer days, Integer limit) {
        log.info("📊 查询高频违规车辆Top{}, 近{}天", limit, days);
        return violationsMapper.selectTopViolators(days, limit);
    }

    @Override
    public List<Map<String, Object>> getViolationTrend(Integer days) {
        log.info("📊 查询违规记录趋势, 近{}天", days);
        return violationsMapper.selectViolationTrend(days);
    }

    @Override
    public List<Map<String, Object>> getViolationTypeTrend(Integer days) {
        log.info("📊 查询违规类型趋势, 近{}天", days);
        return violationsMapper.selectViolationTypeTrend(days);
    }

    @Override
    public List<Map<String, Object>> getLocationFrequency(Integer days, String location) {
        log.info("📊 查询各位置违规频次, 近{}天, 位置过滤: {}", days, location);
        return violationsMapper.selectLocationFrequency(days, location);
    }

    @Override
    public List<Map<String, Object>> getRepeatViolators(Integer days, Integer threshold) {
        log.info("📊 查询重复违规车辆预警, 近{}天, 阈值: {}", days, threshold);
        return violationsMapper.selectRepeatViolators(days, threshold);
    }
}