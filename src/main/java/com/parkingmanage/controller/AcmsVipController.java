package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.service.AcmsVipService;
import com.parkingmanage.service.AcmsVipService.VipOwnerInfo;
import com.parkingmanage.service.AcmsVipService.VipTicketInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ACMS VIP车主信息接口控制器
 * 对接ACMS外部系统的VIP相关接口
 * 
 * @author System
 */
@Slf4j
@RestController
@RequestMapping("/parking/acms/vip")
@Api(tags = "ACMS VIP车主信息管理")
public class AcmsVipController {

    @Resource
    private AcmsVipService acmsVipService;

    /**
     * 测试
     * 获取车主信息
     * 对应ACMS接口：GET_CUSTOMER (4.6)
     * 
     * @param request 请求参数
     * @return 车主信息
     */
    @PostMapping("/owner-info")
    @ApiOperation(value = "获取车主信息", notes = "根据车牌号查询ACMS系统中的车主信息")
    public ResponseEntity<Result> getOwnerInfo(@RequestBody OwnerInfoRequest request) {
        log.info("🔍 [ACMS-车主信息] 开始查询 - 车牌号: {}, 车场: {}", request.getPlateNumber(), request.getParkName());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getPlateNumber())) {
                log.warn("⚠️ [ACMS-车主信息] 车牌号不能为空");
                return ResponseEntity.ok(Result.error("车牌号不能为空"));
            }
            
            if (!StringUtils.hasText(request.getParkName())) {
                log.warn("⚠️ [ACMS-车主信息] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            // 调用服务层
            VipOwnerInfo ownerInfo = acmsVipService.getOwnerInfo(
                request.getPlateNumber(), 
                request.getParkName()
            );
            
            if (ownerInfo == null) {
                log.info("📭 [ACMS-车主信息] 未找到车主信息 - 车牌号: {}", request.getPlateNumber());
                return ResponseEntity.ok(Result.error("未找到该车牌的车主信息"));
            }
            
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("ownerName", ownerInfo.getOwnerName());
            data.put("ownerPhone", ownerInfo.getOwnerPhone());
            data.put("ownerAddress", ownerInfo.getOwnerAddress());
            data.put("plateNumber", request.getPlateNumber());
            data.put("parkName", request.getParkName());
            
            log.info("✅ [ACMS-车主信息] 查询成功 - 车主: {}, 电话: {}", 
                ownerInfo.getOwnerName(), 
                ownerInfo.getOwnerPhone());
            
            return ResponseEntity.ok(Result.success(data));
            
        } catch (Exception e) {
            log.error("❌ [ACMS-车主信息] 查询失败 - 车牌号: {}", request.getPlateNumber(), e);
            return ResponseEntity.ok(Result.error("查询车主信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取VIP车辆信息
     * 对应ACMS接口：GET_VIP_CAR (4.13)
     * 
     * @param request 请求参数
     * @return VIP车辆信息
     */
    @PostMapping("/vip-ticket-info")
    @ApiOperation(value = "获取VIP车辆信息", notes = "根据车牌号查询ACMS系统中的VIP票信息")
    public ResponseEntity<Result> getVipTicketInfo(@RequestBody VipTicketRequest request) {
        log.info("🎫 [ACMS-VIP票] 开始查询 - 车牌号: {}, 车场: {}", request.getPlateNumber(), request.getParkName());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getPlateNumber())) {
                log.warn("⚠️ [ACMS-VIP票] 车牌号不能为空");
                return ResponseEntity.ok(Result.error("车牌号不能为空"));
            }
            
            if (!StringUtils.hasText(request.getParkName())) {
                log.warn("⚠️ [ACMS-VIP票] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            // 调用服务层
            VipTicketInfo ticketInfo = acmsVipService.getVipTicketInfo(
                request.getPlateNumber(), 
                request.getParkName()
            );
            
            if (ticketInfo == null) {
                log.info("📭 [ACMS-VIP票] 未找到VIP票信息 - 车牌号: {}", request.getPlateNumber());
                return ResponseEntity.ok(Result.error("未找到该车牌的VIP票信息"));
            }

            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("vipTypeName", ticketInfo.getVipTypeName());
            data.put("ownerName", ticketInfo.getOwnerName());
            data.put("ownerPhone", ticketInfo.getOwnerPhone());
            data.put("plateNumber", request.getPlateNumber());
            data.put("parkName", request.getParkName());
            
            log.info("✅ [ACMS-VIP票] 查询成功 - VIP类型: {}, 车主: {}", 
                ticketInfo.getVipTypeName(), 
                ticketInfo.getOwnerName());
            
            return ResponseEntity.ok(Result.success(data));
            
        } catch (Exception e) {
            log.error("❌ [ACMS-VIP票] 查询失败 - 车牌号: {}", request.getPlateNumber(), e);
            return ResponseEntity.ok(Result.error("查询VIP票信息失败: " + e.getMessage()));
        }
    }

    /**
     * 综合查询车主和VIP票信息
     * 
     * @param request 请求参数
     * @return 车主信息和VIP票信息
     */
    @PostMapping("/comprehensive-info")
    @ApiOperation(value = "综合查询车主和VIP票信息", notes = "同时查询车主信息和VIP票信息")
    public ResponseEntity<Result> getComprehensiveInfo(@RequestBody ComprehensiveRequest request) {
        log.info("🔍 [ACMS-综合查询] 开始查询 - 车牌号: {}, 车场: {}", request.getPlateNumber(), request.getParkName());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getPlateNumber())) {
                log.warn("⚠️ [ACMS-综合查询] 车牌号不能为空");
                return ResponseEntity.ok(Result.error("车牌号不能为空"));
            }
            
            if (!StringUtils.hasText(request.getParkName())) {
                log.warn("⚠️ [ACMS-综合查询] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            // 并行查询车主信息和VIP票信息
            VipOwnerInfo ownerInfo = acmsVipService.getOwnerInfo(
                request.getPlateNumber(), 
                request.getParkName()
            );
            
            VipTicketInfo ticketInfo = acmsVipService.getVipTicketInfo(
                request.getPlateNumber(), 
                request.getParkName()
            );
            
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("plateNumber", request.getPlateNumber());
            data.put("parkName", request.getParkName());
            
            // 车主信息
            if (ownerInfo != null) {
                Map<String, Object> ownerData = new HashMap<>();
                ownerData.put("ownerName", ownerInfo.getOwnerName());
                ownerData.put("ownerPhone", ownerInfo.getOwnerPhone());
                ownerData.put("ownerAddress", ownerInfo.getOwnerAddress());
                data.put("ownerInfo", ownerData);
                data.put("hasOwnerInfo", true);
            } else {
                data.put("ownerInfo", null);
                data.put("hasOwnerInfo", false);
            }
            
            // VIP票信息
            if (ticketInfo != null) {
                Map<String, Object> ticketData = new HashMap<>();
                ticketData.put("vipTypeName", ticketInfo.getVipTypeName());
                ticketData.put("ownerName", ticketInfo.getOwnerName());
                ticketData.put("ownerPhone", ticketInfo.getOwnerPhone());
                data.put("ticketInfo", ticketData);
                data.put("hasTicketInfo", true);
            } else {
                data.put("ticketInfo", null);
                data.put("hasTicketInfo", false);
            }
            
            log.info("✅ [ACMS-综合查询] 查询完成 - 车主信息: {}, VIP票信息: {}", 
                (ownerInfo != null ? "有" : "无"), 
                (ticketInfo != null ? "有" : "无"));
            
            return ResponseEntity.ok(Result.success(data));
            
        } catch (Exception e) {
            log.error("❌ [ACMS-综合查询] 查询失败 - 车牌号: {}", request.getPlateNumber(), e);
            return ResponseEntity.ok(Result.error("综合查询失败: " + e.getMessage()));
        }
    }

    /**
     * 融合查询VIP月票和车主详细信息
     * 先查询VIP月票信息（vip_type_name、car_owner、car_owner_phone）
     * 再查询车主详细信息（customer_department作为地址，customer_address作为车主类别）
     * 
     * @param request 请求参数
     * @return 融合后的信息
     */
    @PostMapping("/merged-info")
    @ApiOperation(value = "融合查询VIP月票和车主信息", notes = "先查VIP票，再查车主详情，返回融合数据")
    public ResponseEntity<Result> getMergedVipAndOwnerInfo(@RequestBody MergedInfoRequest request) {
        log.info("🔄 [ACMS-融合查询] 开始查询 - 车牌号: {}, 车场: {}", request.getPlateNumber(), request.getParkName());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getPlateNumber())) {
                log.warn("⚠️ [ACMS-融合查询] 车牌号不能为空");
                return ResponseEntity.ok(Result.error("车牌号不能为空"));
            }
            
            if (!StringUtils.hasText(request.getParkName())) {
                log.warn("⚠️ [ACMS-融合查询] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            // 第一步：查询VIP月票信息
            VipTicketInfo ticketInfo = acmsVipService.getVipTicketInfo(
                request.getPlateNumber(), 
                request.getParkName()
            );
            System.out.println("ticketInfo = " + ticketInfo);
            // 第二步：查询车主详细信息（使用必需参数）
            VipOwnerInfo ownerInfo = acmsVipService.getOwnerInfo(
                request.getPlateNumber(), 
                request.getParkName()
            );
            System.out.println("ownerInfo = " + ownerInfo);
            // 构建融合数据
            Map<String, Object> data = new HashMap<>();
            data.put("plateNumber", request.getPlateNumber());
            data.put("parkName", request.getParkName());
            
            // VIP月票信息
            if (ticketInfo != null) {
                data.put("vipTypeName", ticketInfo.getVipTypeName());      // 月票名称
                data.put("ownerName", ticketInfo.getOwnerName());          // 车主姓名（来自VIP票）
                data.put("ownerPhone", ticketInfo.getOwnerPhone());        // 车主手机号（来自VIP票）
            } else {
                log.warn("📭 [ACMS-融合查询] 未找到VIP票信息");
            }
            
            // 车主详细信息（根据你的需求映射）
            if (ownerInfo != null) {
                // customer_department 作为地址
                data.put("ownerAddress", ownerInfo.getCustomerDepartment());
                
                // customer_address 作为车主类别
                data.put("ownerCategory", ownerInfo.getCustomerAddress());
                
                // 额外的详细信息
                data.put("customerCompany", ownerInfo.getCustomerCompany());
                data.put("customerRoomNumber", ownerInfo.getCustomerRoomNumber());
                
                // 如果VIP票中没有车主信息，使用车主详情中的
                if (ticketInfo == null) {
                    data.put("ownerName", ownerInfo.getOwnerName());
                    data.put("ownerPhone", ownerInfo.getOwnerPhone());
                }
            } else {
                log.warn("📭 [ACMS-融合查询] 未找到车主详细信息");
            }
            
            // 判断是否至少有一个数据源
            if (ticketInfo == null && ownerInfo == null) {
                log.info("📭 [ACMS-融合查询] 未找到任何信息 - 车牌号: {}", request.getPlateNumber());
                return ResponseEntity.ok(Result.error("未找到该车牌的任何信息"));
            }
            
            log.info("✅ [ACMS-融合查询] 查询成功 - 月票: {}, 车主: {}, 地址: {}, 类别: {}", 
                data.get("vipTypeName"),
                data.get("ownerName"),
                data.get("ownerAddress"),
                data.get("ownerCategory"));
            
            return ResponseEntity.ok(Result.success(data));
            
        } catch (Exception e) {
            log.error("❌ [ACMS-融合查询] 查询失败 - 车牌号: {}", request.getPlateNumber(), e);
            return ResponseEntity.ok(Result.error("融合查询失败: " + e.getMessage()));
        }
    }

    /**
     * GET方式查询车主信息（简化接口）
     * 
     * @param plateNumber 车牌号
     * @param parkName 车场名称
     * @return 车主信息
     */
    @GetMapping("/owner-info")
    @ApiOperation(value = "GET方式获取车主信息", notes = "根据车牌号查询车主信息（GET方式）")
    public ResponseEntity<Result> getOwnerInfoByGet(
            @ApiParam(value = "车牌号", required = true) @RequestParam String plateNumber,
            @ApiParam(value = "车场名称", required = true) @RequestParam String parkName) {
        OwnerInfoRequest request = new OwnerInfoRequest();
        request.setPlateNumber(plateNumber);
        request.setParkName(parkName);
        return getOwnerInfo(request);
    }

    /**
     * GET方式查询VIP票信息（简化接口）
     * 
     * @param plateNumber 车牌号
     * @param parkName 车场名称
     * @return VIP票信息
     */
    @GetMapping("/vip-ticket-info")
    @ApiOperation(value = "GET方式获取VIP票信息", notes = "根据车牌号查询VIP票信息（GET方式）")
    public ResponseEntity<Result> getVipTicketInfoByGet(
            @ApiParam(value = "车牌号", required = true) @RequestParam String plateNumber,
            @ApiParam(value = "车场名称", required = true) @RequestParam String parkName) {
        VipTicketRequest request = new VipTicketRequest();
        request.setPlateNumber(plateNumber);
        request.setParkName(parkName);
        return getVipTicketInfo(request);
    }

    /**
     * 获取黑名单类型列表
     * 对应ACMS接口：GET_CAR_VIP_TYPE (4.25)
     * 
     * @param request 请求参数
     * @return 黑名单类型列表
     */
    @PostMapping("/blacklist-types")
    @ApiOperation(value = "获取黑名单类型列表", notes = "从ACMS系统获取所有黑名单类型，用于下拉选择")
    public ResponseEntity<Result> getBlacklistTypes(@RequestBody BlacklistTypesRequest request) {
        log.info("📋 [ACMS-黑名单类型] 开始查询 - 车场: {}", request.getParkName());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getParkName())) {
                log.warn("⚠️ [ACMS-黑名单类型] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            // 调用服务层
            List<AcmsVipService.BlacklistTypeInfo> blacklistTypes = acmsVipService.getBlacklistTypes(request.getParkName());
            
            if (blacklistTypes == null || blacklistTypes.isEmpty()) {
                log.info("📭 [ACMS-黑名单类型] 未找到黑名单类型 - 车场: {}", request.getParkName());
                
                // 返回默认黑名单类型（兜底方案）
                Map<String, Object> data = new HashMap<>();
                data.put("blacklistTypes", getDefaultBlacklistTypes());
                data.put("isDefault", true);
                data.put("message", "ACMS系统未配置黑名单类型，已返回默认选项");
                
                return ResponseEntity.ok(Result.success(data));
            }
            
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("blacklistTypes", blacklistTypes);
            data.put("isDefault", false);
            data.put("count", blacklistTypes.size());
            
            log.info("✅ [ACMS-黑名单类型] 查询成功 - 共 {} 种类型", blacklistTypes.size());
            
            return ResponseEntity.ok(Result.success(data));
            
        } catch (Exception e) {
            log.error("❌ [ACMS-黑名单类型] 查询失败 - 车场: {}", request.getParkName(), e);
            
            // 发生异常时返回默认类型
            Map<String, Object> data = new HashMap<>();
            data.put("blacklistTypes", getDefaultBlacklistTypes());
            data.put("isDefault", true);
            data.put("message", "查询失败，已返回默认选项");
            
            return ResponseEntity.ok(Result.success(data));
        }
    }

    /**
     * GET方式获取黑名单类型列表（简化接口）
     * 
     * @param parkName 车场名称
     * @return 黑名单类型列表
     */
    @GetMapping("/blacklist-types")
    @ApiOperation(value = "GET方式获取黑名单类型列表", notes = "从ACMS系统获取所有黑名单类型（GET方式）")
    public ResponseEntity<Result> getBlacklistTypesByGet(
            @ApiParam(value = "车场名称", required = true) @RequestParam String parkName) {
        
        BlacklistTypesRequest request = new BlacklistTypesRequest();
        request.setParkName(parkName);
        
        return getBlacklistTypes(request);
    }

    /**
     * 获取默认黑名单类型（兜底方案）
     * 当ACMS系统未配置或查询失败时返回
     */
    private List<Map<String, String>> getDefaultBlacklistTypes() {
        List<Map<String, String>> defaultTypes = new java.util.ArrayList<>();
        
        Map<String, String> type1 = new HashMap<>();
        type1.put("code", "default_violation");
        type1.put("name", "违规黑名单");
        type1.put("vipGroupType", "1");
        type1.put("vipType", "2");
        type1.put("description", "因违规停车被加入黑名单");
        defaultTypes.add(type1);
        
        Map<String, String> type2 = new HashMap<>();
        type2.put("code", "default_security");
        type2.put("name", "安全黑名单");
        type2.put("vipGroupType", "1");
        type2.put("vipType", "2");
        type2.put("description", "因安全原因被加入黑名单");
        defaultTypes.add(type2);
        
        Map<String, String> type3 = new HashMap<>();
        type3.put("code", "default_malicious");
        type3.put("name", "恶意黑名单");
        type3.put("vipGroupType", "1");
        type3.put("vipType", "2");
        type3.put("description", "因恶意行为被加入黑名单");
        defaultTypes.add(type3);
        
        return defaultTypes;
    }

    // ==================== 请求参数对象 ====================

    /**
     * 车主信息查询请求
     */
    @Data
    public static class OwnerInfoRequest {
        @ApiParam(value = "车牌号", required = true)
        private String plateNumber;
        
        @ApiParam(value = "车场名称", required = true)
        private String parkName;
    }

    /**
     * VIP票查询请求
     */
    @Data
    public static class VipTicketRequest {
        @ApiParam(value = "车牌号", required = true)
        private String plateNumber;
        
        @ApiParam(value = "车场名称", required = true)
        private String parkName;
    }

    /**
     * 综合查询请求
     */
    @Data
    public static class ComprehensiveRequest {
        @ApiParam(value = "车牌号", required = true)
        private String plateNumber;
        
        @ApiParam(value = "车场名称", required = true)
        private String parkName;
    }

    /**
     * 融合查询请求
     */
    @Data
    public static class MergedInfoRequest {
        @ApiParam(value = "车牌号", required = true)
        private String plateNumber;
        
        @ApiParam(value = "车场名称", required = true)
        private String parkName;
    }

    /**
     * 黑名单类型查询请求
     */
    @Data
    public static class BlacklistTypesRequest {
        @ApiParam(value = "车场名称", required = true)
        private String parkName;
    }

    /**
     * 查询车辆黑名单信息
     * 对应ACMS接口：GET_BLACK_LIST
     * 
     * @param request 请求参数
     * @return 黑名单信息列表
     */
    @PostMapping("/blacklist-info")
    @ApiOperation(value = "查询车辆黑名单信息", notes = "根据车牌号查询ACMS系统中的黑名单信息")
    public ResponseEntity<Result> getBlacklistInfo(@RequestBody BlacklistInfoRequest request) {
        log.info("🚫 [ACMS-黑名单查询] 开始查询 - 车牌号: {}, 车场: {}", request.getPlateNumber(), request.getParkName());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getPlateNumber())) {
                log.warn("⚠️ [ACMS-黑名单查询] 车牌号不能为空");
                return ResponseEntity.ok(Result.error("车牌号不能为空"));
            }
            
            if (!StringUtils.hasText(request.getParkName())) {
                log.warn("⚠️ [ACMS-黑名单查询] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            // 调用服务层
            List<AcmsVipService.BlacklistInfo> blacklistInfos = acmsVipService.getBlacklistInfo(
                request.getPlateNumber(), 
                request.getParkName()
            );
            
            if (blacklistInfos == null || blacklistInfos.isEmpty()) {
                log.info("✅ [ACMS-黑名单查询] 未找到黑名单信息 - 车牌号: {}", request.getPlateNumber());
                return ResponseEntity.ok(Result.error("该车辆不在黑名单中"));
            }
            
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("blacklistInfos", blacklistInfos);
            data.put("count", blacklistInfos.size());
            data.put("plateNumber", request.getPlateNumber());
            data.put("parkName", request.getParkName());
            
            log.info("🚫 [ACMS-黑名单查询] 查询成功 - 找到 {} 条黑名单记录", blacklistInfos.size());
            
            return ResponseEntity.ok(Result.success(data));
            
        } catch (Exception e) {
            log.error("❌ [ACMS-黑名单查询] 查询失败 - 车牌号: {}", request.getPlateNumber(), e);
            return ResponseEntity.ok(Result.error("查询黑名单信息失败: " + e.getMessage()));
        }
    }

    /**
     * GET方式查询黑名单信息（简化接口）
     * 
     * @param plateNumber 车牌号
     * @param parkName 车场名称
     * @return 黑名单信息列表
     */
    @GetMapping("/blacklist-info")
    @ApiOperation(value = "GET方式查询车辆黑名单信息", notes = "根据车牌号查询黑名单信息（GET方式）")
    public ResponseEntity<Result> getBlacklistInfoByGet(
            @ApiParam(value = "车牌号", required = true) @RequestParam String plateNumber,
            @ApiParam(value = "车场名称", required = true) @RequestParam String parkName) {
        BlacklistInfoRequest request = new BlacklistInfoRequest();
        request.setPlateNumber(plateNumber);
        request.setParkName(parkName);
        return getBlacklistInfo(request);
    }

    /**
     * 黑名单查询请求
     */
    @Data
    public static class BlacklistInfoRequest {
        @ApiParam(value = "车牌号", required = true)
        private String plateNumber;
        
        @ApiParam(value = "车场名称", required = true)
        private String parkName;
    }
} 