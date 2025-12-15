package com.parkingmanage.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.common.HttpClientUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ACMS VIP车主信息服务
 * 仅用于东北林业大学车场
 * 
 * @author System
 */
@Slf4j
@Service
public class AcmsVipService {

    @Value("${acms.api.url:}")
    private String acmsApiUrl;

    @Value("${acms.api.device_id:}")
    private String deviceId;

    @Value("${acms.api.sign_type:MD5}")
    private String signType;

    @Value("${acms.api.charset:UTF-8}")
    private String charset;

    private static final String DONGBEI_FORESTRY_UNIVERSITY = "东北林业大学";

    /**
     * 获取车主信息
     * 
     * @param plateNumber 车牌号
     * @param parkName 停车场名称
     * @return VIP车主信息
     */
    public VipOwnerInfo getOwnerInfo(String plateNumber, String parkName) {
        // 仅处理东北林业大学车场
        if (!DONGBEI_FORESTRY_UNIVERSITY.equals(parkName)) {
            return null;
        }
        try {
            // 构建请求参数
            AcmsRequest request = buildOwnerInfoRequest(plateNumber);
            // 🔧 使用 HttpClientUtil 调用ACMS接口（UTF-8编码已内置处理）
            String requestJson = JSON.toJSONString(request);
            log.info("📤 [ACMS请求-车主信息] plateNumber={}, url={}", plateNumber, acmsApiUrl + "/cxfService/external/extReq");
            System.out.println("request = " + requestJson);
            String response = HttpClientUtil.doPostJson(acmsApiUrl + "/cxfService/external/extReq", requestJson);
            log.info("📥 [ACMS响应-车主信息] plateNumber={}, response={}", plateNumber, response);
            System.out.println("response = " + response);
            // 解析响应
            return parseOwnerInfoResponse(response);
            
        } catch (Exception e) {
            log.error("调用ACMS获取车主信息失败，车牌号: {}", plateNumber, e);
            return null;
        }
    }

    /**
     * 获取车辆 VIP 票信息
     * 
     * @param plateNumber 车牌号
     * @param parkName 停车场名称
     * @return VIP票信息
     */
    public VipTicketInfo getVipTicketInfo(String plateNumber, String parkName) {
        // 仅处理东北林业大学车场
        if (!DONGBEI_FORESTRY_UNIVERSITY.equals(parkName)) {
            return null;
        }

        try {
            // 构建请求参数
            AcmsRequest request = buildVipTicketRequest(plateNumber);
            
            // 🔧 使用 HttpClientUtil 调用ACMS接口（UTF-8编码已内置处理）
            String requestJson = JSON.toJSONString(request);
            
            String response = HttpClientUtil.doPostJson(acmsApiUrl + "/cxfService/external/extReq", requestJson);
            
            // 解析响应
            return parseVipTicketResponse(response);
            
        } catch (Exception e) {
//            logger.error
            log.error("调用ACMS获取VIP票信息失败，车牌号: {}", plateNumber, e);
            return null;
        }
    }

    /**
     * 构建车主信息查询请求
     */
    private AcmsRequest buildOwnerInfoRequest(String plateNumber) {
        AcmsRequest request = new AcmsRequest();
        request.setCommand("GET_CUSTOMER");
        request.setMessage_id(generateMessageId());
        request.setDevice_id(deviceId);
        request.setSign_type(signType);
        request.setCharset(charset);
        request.setTimestamp(getCurrentTimestamp());
        
        OwnerInfoBizContent bizContent = new OwnerInfoBizContent();
        bizContent.setCar_code(plateNumber);
        bizContent.setPage_size(1000);
        bizContent.setPage_num(0);
        
        request.setBiz_content(bizContent);
        
        request.setSign("f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g"); 
        
        return request;
    }

    /**
     * 构建VIP票查询请求
     */
    private AcmsRequest buildVipTicketRequest(String plateNumber) {
        AcmsRequest request = new AcmsRequest();
        request.setCommand("GET_VIP_CAR");
        request.setMessage_id(generateMessageId());
        request.setDevice_id(deviceId);
        request.setSign_type(signType);
        request.setCharset(charset);
        request.setTimestamp(getCurrentTimestamp());
        
        VipTicketBizContent bizContent = new VipTicketBizContent();
        bizContent.setCar_no(plateNumber);
        bizContent.setValid_type("0"); // 查询所有状态
        
        request.setBiz_content(bizContent);
        
        request.setSign("f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g");
        
        return request;
    }

    /**
     * 解析车主信息响应
     */
    private VipOwnerInfo parseOwnerInfoResponse(String response) {
        if (!StringUtils.hasText(response)) {
            return null;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");
            
            if (bizContent == null || !"0".equals(bizContent.getString("code"))) {
                return null;
            }

            List<JSONObject> customers = bizContent.getJSONArray("customers").toJavaList(JSONObject.class);
            if (customers == null || customers.isEmpty()) {
                return null;
            }

            JSONObject customer = customers.get(0);
            VipOwnerInfo ownerInfo = new VipOwnerInfo();
            ownerInfo.setOwnerName(customer.getString("customer_name"));
            ownerInfo.setOwnerPhone(customer.getString("customer_telphone"));
            
            // 保存原始字段
            ownerInfo.setCustomerDepartment(customer.getString("customer_department"));
            ownerInfo.setCustomerAddress(customer.getString("customer_address"));
            ownerInfo.setCustomerCompany(customer.getString("customer_company"));
            ownerInfo.setCustomerRoomNumber(customer.getString("customer_room_number"));
            
            // 组合单位地址
            String address = buildOwnerAddress(
                customer.getString("customer_company"),
                customer.getString("customer_department"),
                customer.getString("customer_address"),
                customer.getString("customer_room_number")
            );
            ownerInfo.setOwnerAddress(address);
            
            return ownerInfo;
            
        } catch (Exception e) {
            log.error("解析车主信息响应失败", e);
            return null;
        }
    }

    /**
     * 解析VIP票响应
     */
    private VipTicketInfo parseVipTicketResponse(String response) {
        if (!StringUtils.hasText(response)) {
            return null;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");
            
            if (bizContent == null || !"0".equals(bizContent.getString("code"))) {
                return null;
            }

            List<JSONObject> carList = bizContent.getJSONArray("car_list").toJavaList(JSONObject.class);
            if (carList == null || carList.isEmpty()) {
                return null;
            }

            JSONObject car = carList.get(0);
            VipTicketInfo ticketInfo = new VipTicketInfo();
            
            // 获取字段值
            String vipTypeName = car.getString("vip_type_name");
            String ownerName = car.getString("car_owner");
            String ownerPhone = car.getString("car_owner_phone");
            
            // 🔧 打印调试信息，检查编码是否正确
            log.info("📝 [编码调试] VIP类型: {}, 车主: {}, 电话: {}", vipTypeName, ownerName, ownerPhone);
            System.out.println("ticketInfo = VipTicketInfo(vipTypeName=" + vipTypeName + 
                             ", ownerName=" + ownerName + 
                             ", ownerPhone=" + ownerPhone + ")");
            
            ticketInfo.setVipTypeName(vipTypeName);
            ticketInfo.setOwnerName(ownerName);
            ticketInfo.setOwnerPhone(ownerPhone);
            
            return ticketInfo;
            
        } catch (Exception e) {
            log.error("解析VIP票响应失败", e);
            return null;
        }
    }

    /**
     * 组合车主单位地址
     */
    private String buildOwnerAddress(String company, String department, String address, String roomNumber) {
        StringBuilder sb = new StringBuilder();
        
        if (StringUtils.hasText(company)) {
            sb.append(company);
        }
        if (StringUtils.hasText(department)) {
            if (sb.length() > 0) sb.append("-");
            sb.append(department);
        }
        if (StringUtils.hasText(address)) {
            if (sb.length() > 0) sb.append("-");
            sb.append(address);
        }
        if (StringUtils.hasText(roomNumber)) {
            if (sb.length() > 0) sb.append("-");
            sb.append(roomNumber);
        }
        
        return sb.toString();
    }

    private String generateMessageId() {
        return String.valueOf(System.currentTimeMillis());
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * ACMS请求对象
     */
    @Data
    public static class AcmsRequest {
        private String command;
        private String message_id;
        private String device_id;
        private String sign_type;
        private String sign;
        private String charset;
        private String timestamp;
        private Object biz_content;
    }

    /**
     * 车主信息查询业务内容
     */
    @Data
    public static class OwnerInfoBizContent {
        private String customer_id;
        private String name;
        private String telphone;
        private String identity_card_number;
        private String car_code;
        private Integer page_size;
        private Integer page_num;
    }

    /**
     * VIP票查询业务内容
     */
    @Data
    public static class VipTicketBizContent {
        private String valid_type;
        private String car_no;
    }

    /**
     * VIP车主信息
     */
    @Data
    public static class VipOwnerInfo {
        private String ownerName;
        private String ownerPhone;
        private String ownerAddress;
        private String customerDepartment;  // 部门（作为地址）
        private String customerAddress;     // 地址（作为车主类别）
        private String customerCompany;     // 单位
        private String customerRoomNumber;  // 房间号
    }

    /**
     * VIP票信息
     */
    @Data
    public static class VipTicketInfo {
        private String vipTypeName;
        private String ownerName;
        private String ownerPhone;
    }

    /**
     * 获取黑名单类型列表
     * 对应ACMS接口：GET_CAR_VIP_TYPE (4.25)
     * 
     * @param parkName 停车场名称
     * @return 黑名单类型列表
     */
    public List<BlacklistTypeInfo> getBlacklistTypes(String parkName) {
        // 仅处理东北林业大学车场
        if (!DONGBEI_FORESTRY_UNIVERSITY.equals(parkName)) {
            return null;
        }

        try {
            // 构建请求参数
            AcmsRequest request = buildBlacklistTypesRequest();
            
            // 调用ACMS接口
            String requestJson = JSON.toJSONString(request);
            log.info("📤 [ACMS请求-黑名单类型] url={}", acmsApiUrl + "/cxfService/external/extReq");
            System.out.println("request = " + requestJson);
            
            String response = HttpClientUtil.doPostJson(acmsApiUrl + "/cxfService/external/extReq", requestJson);
            
            log.info("📥 [ACMS响应-黑名单类型] response={}", response);
            System.out.println("response = " + response);
            
            // 解析响应
            return parseBlacklistTypesResponse(response);
            
        } catch (Exception e) {
            log.error("调用ACMS获取黑名单类型失败", e);
            return null;
        }
    }

    /**
     * 构建黑名单类型查询请求
     */
    private AcmsRequest buildBlacklistTypesRequest() {
        AcmsRequest request = new AcmsRequest();
        request.setCommand("GET_CAR_VIP_TYPE");
        request.setMessage_id(generateMessageId());
        request.setDevice_id(deviceId);
        request.setSign_type(signType);
        request.setCharset(charset);
        request.setTimestamp(getCurrentTimestamp());
        
        BlacklistTypesBizContent bizContent = new BlacklistTypesBizContent();
        bizContent.setVip_group_type("2"); // 根据实际情况调整，可能需要配置化
        
        request.setBiz_content(bizContent);
        request.setSign("f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g");
        
        return request;
    }

    /**
     * 解析黑名单类型响应
     */
    private List<BlacklistTypeInfo> parseBlacklistTypesResponse(String response) {
        if (!StringUtils.hasText(response)) {
            return null;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");
            
            if (bizContent == null || !"0".equals(bizContent.getString("code"))) {
                log.warn("⚠️ ACMS返回错误: {}", bizContent != null ? bizContent.getString("msg") : "无响应");
                return null;
            }

            List<JSONObject> customVips = bizContent.getJSONArray("custom_vips").toJavaList(JSONObject.class);
            if (customVips == null || customVips.isEmpty()) {
                log.warn("⚠️ 未找到任何VIP类型");
                return null;
            }

            // 筛选黑名单类型（名称中包含"黑名单"的）
            List<BlacklistTypeInfo> blacklistTypes = new java.util.ArrayList<>();
            for (JSONObject vip : customVips) {
                String vipName = vip.getString("custom_vip_name");
                
                // 筛选条件：名称包含"黑名单"
                if (vipName != null) {
                    BlacklistTypeInfo typeInfo = new BlacklistTypeInfo();
                    typeInfo.setCode(vip.getString("custom_vip_seq"));
                    typeInfo.setName(vipName);
                    typeInfo.setVipGroupType(vip.getString("vip_group_type"));
                    typeInfo.setVipType(vip.getString("vip_type"));
                    typeInfo.setDescription(getBlacklistDescription(vipName));
                    
                    blacklistTypes.add(typeInfo);
                    
                    log.info("✅ 找到黑名单类型: code={}, name={}", typeInfo.getCode(), typeInfo.getName());
                }
            }
            
            log.info("📊 共筛选出 {} 种黑名单类型", blacklistTypes.size());
            return blacklistTypes.isEmpty() ? null : blacklistTypes;
            
        } catch (Exception e) {
            log.error("解析黑名单类型响应失败", e);
            return null;
        }
    }

    /**
     * 根据黑名单类型名称生成描述
     */
    private String getBlacklistDescription(String typeName) {
        if (typeName == null) {
            return "其他原因被加入黑名单";
        }
        
        if (typeName.contains("违规")) {
            return "因违规停车被加入黑名单";
        } else if (typeName.contains("安全")) {
            return "因安全原因被加入黑名单";
        } else if (typeName.contains("恶意")) {
            return "因恶意行为被加入黑名单";
        } else {
            return "其他原因被加入黑名单";
        }
    }

    /**
     * 黑名单类型查询业务内容
     */
    @Data
    public static class BlacklistTypesBizContent {
        private String vip_group_type;
        private String custom_vip_name; // 可选，不填则查询所有
    }

    /**
     * 黑名单类型信息
     */
    @Data
    public static class BlacklistTypeInfo {
        private String code;          // VIP类型编码（custom_vip_seq）
        private String name;          // VIP类型名称（custom_vip_name）
        private String vipGroupType;  // VIP分组类型
        private String vipType;       // VIP类型
        private String description;   // 描述
    }

    /**
     * 添加黑名单到ACMS
     * 对应ACMS接口：ADD_BLACK_LIST_CAR (4.17)
     * 
     * @param request 黑名单添加请求
     * @return 是否添加成功
     */
    public boolean addBlacklistToAcms(AddBlacklistRequest request) {
        // 仅处理东北林业大学车场
        if (!DONGBEI_FORESTRY_UNIVERSITY.equals(request.getParkName())) {
            log.info("⏭️ [黑名单同步] 非东北林业大学车场，跳过ACMS同步: {}", request.getParkName());
            return false;
        }

        try {
            // 构建请求参数
            AcmsRequest acmsRequest = buildAddBlacklistRequest(request);
            
            // 调用ACMS接口
            String requestJson = JSON.toJSONString(acmsRequest);
            log.info("📤 [ACMS请求-添加黑名单] carCode={}, url={}", request.getCarCode(), acmsApiUrl + "/cxfService/external/extReq");
            log.info("📋 [请求详情] {}", requestJson);
            
            String response = HttpClientUtil.doPostJson(acmsApiUrl + "/cxfService/external/extReq", requestJson);
            
            log.info("📥 [ACMS响应-添加黑名单] carCode={}, response={}", request.getCarCode(), response);
            
            // 解析响应
            boolean success = parseAddBlacklistResponse(response);
            
            if (success) {
                log.info("✅ [黑名单同步成功] 车牌: {}, 类型: {}, 原因: {}", 
                        request.getCarCode(), request.getVipTypeName(), request.getReason());
            } else {
                log.warn("⚠️ [黑名单同步失败] 车牌: {}, ACMS返回失败", request.getCarCode());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("❌ [黑名单同步异常] 车牌: {}, 错误: {}", request.getCarCode(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建添加黑名单请求
     */
    private AcmsRequest buildAddBlacklistRequest(AddBlacklistRequest request) {
        AcmsRequest acmsRequest = new AcmsRequest();
        acmsRequest.setCommand("ADD_BLACK_LIST_CAR");
        acmsRequest.setMessage_id(generateMessageId());
        acmsRequest.setDevice_id(deviceId);
        acmsRequest.setSign_type(signType);
        acmsRequest.setCharset(charset);
        acmsRequest.setTimestamp(getCurrentTimestamp());
        
        AddBlacklistBizContent bizContent = new AddBlacklistBizContent();
        bizContent.setVip_type_name(request.getVipTypeName());
        bizContent.setCar_code(request.getCarCode());
        bizContent.setCar_owner(request.getCarOwner());
        bizContent.setReason(request.getReason());
        
        // 设置是否永久拉黑
        if ("permanent".equals(request.getDurationType())) {
            bizContent.setIs_permament(1);
            bizContent.setTime_period(null);
        } else if ("temporary".equals(request.getDurationType())) {
            bizContent.setIs_permament(0);
            
            // 设置时间段
            TimePeriod timePeriod = new TimePeriod();
            timePeriod.setStart_time(request.getStartTime());
            timePeriod.setEnd_time(request.getEndTime());
            bizContent.setTime_period(timePeriod);
        }
        
        // 设置备注和操作信息
//        bizContent.setRemark1(request.getRemark1());
        bizContent.setRemark2(request.getRemark2());
        bizContent.setOperator(request.getOperator());
        bizContent.setOperate_time(request.getOperateTime());
        
        acmsRequest.setBiz_content(bizContent);
        acmsRequest.setSign("f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g");
        
        return acmsRequest;
    }

    /**
     * 解析添加黑名单响应
     */
    private boolean parseAddBlacklistResponse(String response) {
        if (!StringUtils.hasText(response)) {
            log.warn("⚠️ ACMS响应为空");
            return false;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");
            
            if (bizContent == null) {
                log.warn("⚠️ ACMS响应缺少biz_content");
                return false;
            }
            
            String code = bizContent.getString("code");
            String msg = bizContent.getString("msg");
            
            log.info("📊 [ACMS响应解析] code={}, msg={}", code, msg);
            
            // code为"0"表示成功
            return "0".equals(code);
            
        } catch (Exception e) {
            log.error("❌ 解析添加黑名单响应失败", e);
            return false;
        }
    }

    /**
     * 添加黑名单请求参数
     */
    @Data
    public static class AddBlacklistRequest {
        private String parkName;        // 停车场名称（用于判断是否同步到ACMS）
        private String vipTypeCode;     // 黑名单类型编码
        private String vipTypeName;     // 黑名单类型名称
        private String carCode;         // 车牌号
        private String carOwner;        // 车主姓名
        private String reason;          // 拉黑原因
        private String durationType;    // 时长类型：permanent/temporary
        private String startTime;       // 开始时间（格式：yyyy-MM-dd HH:mm:ss）
        private String endTime;         // 结束时间（格式：yyyy-MM-dd HH:mm:ss）
        private String remark1;         // 备注1
        private String remark2;         // 备注2
        private String operator;        // 操作人
        private String operateTime;     // 操作时间（格式：yyyy-MM-dd HH:mm:ss）
    }

    /**
     * 添加黑名单业务内容
     */
    @Data
    public static class AddBlacklistBizContent {
        private String vip_type_code;      // 黑名单类型编码
        private String vip_type_name;      // 黑名单类型名称
        private String car_code;           // 车牌号
        private String car_owner;          // 车主
        private String reason;             // 原因
        private Integer is_permament;      // 是否永久：1-永久，0-临时
        private TimePeriod time_period;    // 时间段（临时拉黑时必填）
        private String remark1;            // 备注1
        private String remark2;            // 备注2
        private String operator;           // 操作人
        private String operate_time;       // 操作时间
    }

    /**
     * 时间段
     */
    @Data
    public static class TimePeriod {
        private String start_time;    // 开始时间（格式：yyyy-MM-dd HH:mm:ss）
        private String end_time;      // 结束时间（格式：yyyy-MM-dd HH:mm:ss）
    }

    /**
     * 查询车辆黑名单信息
     * 对应ACMS接口：GET_BLACK_LIST
     * 
     * @param plateNumber 车牌号
     * @param parkName 停车场名称
     * @return 黑名单信息列表
     */
    public List<BlacklistInfo> getBlacklistInfo(String plateNumber, String parkName) {
        // 仅处理东北林业大学车场
        if (!DONGBEI_FORESTRY_UNIVERSITY.equals(parkName)) {
            return null;
        }

        try {
            // 构建请求参数
            AcmsRequest request = buildBlacklistRequest(plateNumber);
            
            // 调用ACMS接口
            String requestJson = JSON.toJSONString(request);
            log.info("📤 [ACMS请求-黑名单查询] plateNumber={}, url={}", plateNumber, acmsApiUrl + "/cxfService/external/extReq");
            System.out.println("request = " + requestJson);
            
            String response = HttpClientUtil.doPostJson(acmsApiUrl + "/cxfService/external/extReq", requestJson);
            
            log.info("📥 [ACMS响应-黑名单查询] plateNumber={}, response={}", plateNumber, response);
            System.out.println("response = " + response);
            
            // 解析响应
            return parseBlacklistResponse(response);
            
        } catch (Exception e) {
            log.error("调用ACMS查询黑名单失败，车牌号: {}", plateNumber, e);
            return null;
        }
    }

    /**
     * 构建黑名单查询请求
     */
    private AcmsRequest buildBlacklistRequest(String plateNumber) {
        AcmsRequest request = new AcmsRequest();
        request.setCommand("GET_BLACK_LIST");
        request.setMessage_id(generateMessageId());
        request.setDevice_id(deviceId);
        request.setSign_type(signType);
        request.setCharset(charset);
        request.setTimestamp(getCurrentTimestamp());
        
        BlacklistQueryBizContent bizContent = new BlacklistQueryBizContent();
        bizContent.setCar_code(plateNumber);
        bizContent.setPage_size(100);
        bizContent.setPage_num(1);
        
        request.setBiz_content(bizContent);
        request.setSign("f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g");
        
        return request;
    }

    /**
     * 解析黑名单查询响应
     */
    private List<BlacklistInfo> parseBlacklistResponse(String response) {
        if (!StringUtils.hasText(response)) {
            return null;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");
            
            if (bizContent == null || !"0".equals(bizContent.getString("code"))) {
                log.warn("⚠️ ACMS返回错误或无数据: {}", bizContent != null ? bizContent.getString("msg") : "无响应");
                return null;
            }

            List<JSONObject> blackList = bizContent.getJSONArray("black_list").toJavaList(JSONObject.class);
            if (blackList == null || blackList.isEmpty()) {
                log.info("✅ 该车辆不在黑名单中");
                return null;
            }

            // 解析黑名单记录
            List<BlacklistInfo> blacklistInfos = new java.util.ArrayList<>();
            for (JSONObject black : blackList) {
                BlacklistInfo info = new BlacklistInfo();
                info.setCarCode(black.getString("car_code"));
                info.setCarOwner(black.getString("car_owner"));
                info.setVipTypeName(black.getString("vip_type_name"));
                info.setReason(black.getString("reason"));
                info.setIsPermanent(black.getInteger("is_permament"));
                
                // 解析时间段
                JSONObject timePeriod = black.getJSONObject("time_period");
                if (timePeriod != null) {
                    info.setStartTime(timePeriod.getString("start_time"));
                    info.setEndTime(timePeriod.getString("end_time"));
                }
                
                info.setRemark1(black.getString("remark1"));
                info.setRemark2(black.getString("remark2"));
                info.setOperator(black.getString("operator"));
                info.setOperateTime(black.getString("operate_time"));
                
                blacklistInfos.add(info);
                
                log.info("🚫 找到黑名单记录: 车牌={}, 车主={}, 类型={}, 原因={}", 
                        info.getCarCode(), info.getCarOwner(), info.getVipTypeName(), info.getReason());
            }
            
            return blacklistInfos;
            
        } catch (Exception e) {
            log.error("解析黑名单查询响应失败", e);
            return null;
        }
    }

    /**
     * 黑名单查询业务内容
     */
    @Data
    public static class BlacklistQueryBizContent {
        private String car_code;
        private Integer page_size;
        private Integer page_num;
    }

    /**
     * 黑名单信息
     */
    @Data
    public static class BlacklistInfo {
        private String carCode;         // 车牌号
        private String carOwner;        // 车主
        private String vipTypeName;     // 黑名单类型名称
        private String reason;          // 原因
        private Integer isPermanent;    // 是否永久：1-永久，0-临时
        private String startTime;       // 开始时间
        private String endTime;         // 结束时间
        private String remark1;         // 备注1
        private String remark2;         // 备注2
        private String operator;        // 操作人
        private String operateTime;     // 操作时间
    }
} 