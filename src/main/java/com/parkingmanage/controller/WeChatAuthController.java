package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Butler;
import com.parkingmanage.entity.Member;
import com.parkingmanage.entity.Ownerinfo;
import com.parkingmanage.entity.UserMapping;
import com.parkingmanage.service.ButlerService;
import com.parkingmanage.service.MemberService;
import com.parkingmanage.service.OwnerinfoService;
import com.parkingmanage.service.OwnerRoleVerificationService;
import com.parkingmanage.service.UserMappingService;
import com.parkingmanage.utils.WeChatInfo;
import com.parkingmanage.utils.WeChatUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.converter.StringHttpMessageConverter;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信小程序授权控制器
 * 支持五层角色判断：管家 → 业主(本地) → 业主(外部API) → 访客申请状态 → 访客
 * 
 * @author MLH
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/parking/wechat")
@CrossOrigin(origins = "*")  // 🆕 添加跨域支持，允许静态HTML文件访问
@Api(tags = "微信小程序授权")
public class WeChatAuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(WeChatAuthController.class);
    
    // 微信网页授权配置
    @Value("${wechat.oauth.appid}")
    private String oauthAppId;
    
    @Value("${wechat.oauth.appsecret}")
    private String oauthAppSecret;
    
    @Value("${wechat.oauth.redirect-uri}")
    private String oauthRedirectUri;
    
    // REST客户端（配置UTF-8编码支持）
    private final RestTemplate restTemplate = createRestTemplate();
    
    /**
     * 创建配置了UTF-8编码的RestTemplate
     */
    private static RestTemplate createRestTemplate() {
        RestTemplate template = new RestTemplate();
        // 添加UTF-8字符编码支持，解决中文乱码问题
        template.getMessageConverters()
            .stream()
            .filter(converter -> converter instanceof StringHttpMessageConverter)
            .forEach(converter -> ((StringHttpMessageConverter) converter)
                .setDefaultCharset(StandardCharsets.UTF_8));
        return template;
    }
    
    @Resource
    private ButlerService butlerService;
    
    @Resource
    private OwnerinfoService ownerinfoService;
    
    @Resource
    private MemberService memberService;
    
    @Resource
    private OwnerRoleVerificationService ownerRoleVerificationService;
    
    @Resource
    private UserMappingService userMappingService;

    // ============== 微信网页授权接口 ==============
    
    /**
     * 获取微信网页授权URL
     */
    @ApiOperation("获取微信网页授权URL")
    @GetMapping("/auth-url")
    public ResponseEntity<Result> getAuthUrl(
            @RequestParam(defaultValue = "snsapi_userinfo") String scope,
            @RequestParam(defaultValue = "") String state) {
        Result result = new Result();
        
        try {
            logger.info("📥 生成微信网页授权URL请求 - scope: [{}], state: [{}]", scope, state);
            
            String authUrl = generateAuthUrl(scope, state);
            
            result.setData(authUrl);
            result.setCode("0");
            result.setMsg("获取授权URL成功");
            
            logger.info("✅ 生成授权URL成功: [{}]", authUrl);
            
        } catch (Exception e) {
            logger.error("❌ 生成授权URL失败", e);
            result.setCode("1");
            result.setMsg("生成授权URL失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 处理微信网页授权回调
     */
    @ApiOperation("处理微信网页授权回调")
    @PostMapping("/auth")
    public ResponseEntity<Result> handleWebAuth(@RequestBody Map<String, String> request) {
        Result result = new Result();
        
        try {
            String code = request.get("code");
            String state = request.get("state");
            
            logger.info("📥 接收到网页授权回调 - code: [{}], state: [{}]", 
                code != null ? code.substring(0, Math.min(8, code.length())) + "..." : "null", 
                state);
            
            // 验证必需参数
            if (code == null || code.trim().isEmpty()) {
                throw new IllegalArgumentException("缺少授权码code");
            }
            
            // 1. 通过code换取access_token
            Map<String, Object> tokenResponse = getAccessToken(code);
            
            // 检查返回是否包含错误
            if (tokenResponse.containsKey("errcode")) {
                Integer errcode = (Integer) tokenResponse.get("errcode");
                if (errcode != null && errcode != 0) {
                    String errorMsg = (String) tokenResponse.get("errmsg");
                    throw new RuntimeException("微信API错误: " + errorMsg + " (errcode: " + errcode + ")");
                }
            }
            
            String accessToken = (String) tokenResponse.get("access_token");
            String openid = (String) tokenResponse.get("openid");
            String scopeFromResponse = (String) tokenResponse.get("scope");
            
            if (accessToken == null || openid == null) {
                throw new RuntimeException("获取access_token或openid失败");
            }
            
            logger.info("✅ 获取access_token成功 - openid: [{}], scope: [{}]", 
                openid.substring(0, Math.min(8, openid.length())) + "...", scopeFromResponse);
            
            // 2. 获取用户信息（如果scope是snsapi_userinfo）
            Map<String, Object> userInfo = null;
            if ("snsapi_userinfo".equals(scopeFromResponse) || request.containsKey("getUserInfo")) {
                userInfo = getUserInfo(accessToken, openid);
                
                // 检查用户信息获取是否成功
                if (userInfo.containsKey("errcode")) {
                    Integer errcode = (Integer) userInfo.get("errcode");
                    if (errcode != null && errcode != 0) {
                        String errorMsg = (String) userInfo.get("errmsg");
                        logger.warn("⚠️ 获取用户信息失败: {} (errcode: {})", errorMsg, errcode);
                        // 用户信息获取失败时，仍然可以返回基本的openid信息
                        userInfo = new HashMap<>();
                        userInfo.put("openid", openid);
                        userInfo.put("nickname", "微信用户");
                    }
                }
                
                logger.info("✅ 获取用户信息成功 - nickname: [{}]", 
                    userInfo.get("nickname"));
            } else {
                // scope为snsapi_base时，只返回openid
                userInfo = new HashMap<>();
                userInfo.put("openid", openid);
            }
            
            // 3. 构造返回数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("userInfo", userInfo);
            responseData.put("accessToken", accessToken);
            responseData.put("state", state);
            responseData.put("authScope", scopeFromResponse);
            
            // 4. 存储用户信息到user_mapping表
            if (userInfo.containsKey("openid")) {
                try {
                    String nickname = (String) userInfo.get("nickname");
                    String avatarUrl = (String) userInfo.get("headimgurl"); // 微信返回的头像字段名
                    
                    // 如果没有昵称，使用默认值
                    if (nickname == null || nickname.trim().isEmpty()) {
                        nickname = "微信用户";
                    }
                    
                    UserMapping savedUser = userMappingService.saveOrUpdateFromWebAuth(openid, nickname, avatarUrl);
                    logger.info("✅ 已保存用户信息到user_mapping表 - openid: {}, nickname: {}, avatarUrl: {}, id: {}", 
                        openid.substring(0, Math.min(8, openid.length())) + "...", nickname, 
                        avatarUrl != null ? "已设置" : "未设置", savedUser.getId());
                    
                    // 可以将保存的用户信息添加到响应中
                    responseData.put("userMappingId", savedUser.getId());
                    responseData.put("isFollowed", savedUser.getIsFollowed());
                    responseData.put("avatarUrl", savedUser.getAvatarUrl());
                } catch (Exception e) {
                    logger.warn("⚠️ 保存用户信息到user_mapping表时出错", e);
                }
            }
            
            // 5. 可选：与现有系统集成，查询用户角色信息
            if (userInfo.containsKey("openid")) {
                try {
                    // 查询该openid是否在Member表中存在
                    Member member = memberService.getMemberByOpenId(openid);
                    if (member != null) {
                        responseData.put("localUser", member);
                        responseData.put("isRegistered", true);
                        logger.info("✅ 找到本地用户记录");
                    } else {
                        responseData.put("isRegistered", false);
                        logger.info("ℹ️ 未找到本地用户记录");
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ 查询本地用户记录时出错", e);
                    responseData.put("isRegistered", false);
                }
            }
            
            result.setData(responseData);
            result.setCode("0");
            result.setMsg("网页授权成功");
            
            logger.info("✅ 网页授权处理完成");
            
        } catch (Exception e) {
            logger.error("❌ 网页授权处理失败", e);
            result.setCode("1");
            result.setMsg("网页授权失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 生成微信授权URL
     */
    private String generateAuthUrl(String scope, String state) {
        try {
            String redirectUri = URLEncoder.encode(oauthRedirectUri, "UTF-8");
            return String.format(
                "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s#wechat_redirect",
                oauthAppId, redirectUri, scope, state
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("URL编码失败", e);
        }
    }
    
    /**
     * 通过code获取access_token
     */
    private Map<String, Object> getAccessToken(String code) {
        String url = String.format(
            "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
            oauthAppId, oauthAppSecret, code
        );
        
        logger.info("🔗 调用微信API获取access_token: {}", url.replaceAll("secret=[^&]+", "secret=***"));
        
        try {
            // 先获取原始字符串响应，避免Content-Type不匹配的问题
            String responseString = restTemplate.getForObject(url, String.class);
            logger.info("📥 微信API原始响应: {}", responseString);
            
            // 手动解析JSON
            Map<String, Object> response = parseJsonResponse(responseString);
            logger.info("📥 微信API解析后响应: {}", response);
            
            return response;
        } catch (Exception e) {
            logger.error("❌ 调用微信API失败", e);
            // 返回错误信息
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("errcode", -1);
            errorResponse.put("errmsg", "调用微信API异常: " + e.getMessage());
            return errorResponse;
        }
    }
    
    /**
     * 解析JSON响应字符串为Map
     */
    private Map<String, Object> parseJsonResponse(String jsonString) {
        try {
            if (jsonString == null || jsonString.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("errcode", -1);
                errorResponse.put("errmsg", "响应内容为空");
                return errorResponse;
            }
            
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            logger.error("❌ JSON解析失败: {}", e.getMessage());
            // 如果JSON解析失败，可能是微信返回了非JSON格式的错误信息
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("errcode", -1);
            errorResponse.put("errmsg", "JSON解析失败，原始响应: " + jsonString);
            return errorResponse;
        }
    }
    
    /**
     * 获取用户信息
     */
    private Map<String, Object> getUserInfo(String accessToken, String openid) {
        String url = String.format(
            "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s&lang=zh_CN",
            accessToken, openid
        );
        
        logger.info("🔗 调用微信API获取用户信息");
        
        try {
            // 先获取原始字符串响应，避免Content-Type不匹配的问题
            String responseString = restTemplate.getForObject(url, String.class);
            logger.info("📥 用户信息API原始响应: {}", responseString);
            
            // 手动解析JSON
            Map<String, Object> response = parseJsonResponse(responseString);
            logger.info("📥 用户信息API解析后响应: {}", response);
            
            return response;
        } catch (Exception e) {
            logger.error("❌ 调用微信用户信息API失败", e);
            // 返回错误信息
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("errcode", -1);
            errorResponse.put("errmsg", "调用微信用户信息API异常: " + e.getMessage());
            return errorResponse;
        }
    }

    // ============== 微信小程序授权接口 ==============

    /**
     * 微信手机号授权登录
     */
    @ApiOperation("微信手机号授权登录")
    @PostMapping("/phoneAuth")
    public ResponseEntity<Result> phoneAuth(@RequestBody Map<String, String> request) {
        Result result = new Result();
        
        try {
            String code = request.get("code");
            String encryptedData = request.get("encryptedData");
            String iv = request.get("iv");
            String parkName = request.get("parkName"); // 获取停车场名称
            
            // 验证必需参数
            if (code == null || code.trim().isEmpty()) {
                throw new IllegalArgumentException("缺少微信登录凭证code");
            }
            
            // 如果没有传递停车场名称，默认使用"四季上东"
            if (parkName == null || parkName.trim().isEmpty()) {
                parkName = "欧洲新城";
                logger.info("📍 未传递停车场信息，默认使用: [{}]", parkName);
            }
            
            // 注意：在开发模式下，encryptedData和iv可能为空，因为前端可能没有真实的加密数据
            logger.info("📥 接收到授权请求 - code: [{}], encryptedData: [{}], iv: [{}], parkName: [{}]", 
                code != null ? code.substring(0, Math.min(8, code.length())) + "..." : "null",
                encryptedData != null ? "已提供" : "未提供",
                iv != null ? "已提供" : "未提供",
                parkName);
            
            // 1. 调用微信API获取完整的登录信息
            WeChatInfo weChatInfo = WeChatUtils.getWeChatInfo(code);
            
            // 验证微信API调用结果
            if (!weChatInfo.hasValidInfo()) {
                throw new RuntimeException("微信授权失败: " + weChatInfo.getErrorDescription());
            }
            
            String sessionKey = weChatInfo.getSessionKey();
            String openid = weChatInfo.getOpenid();
            
            // 2. 解密手机号
            String phoneNumber;
            try {
                phoneNumber = WeChatUtils.decryptPhoneNumber(encryptedData, sessionKey, iv);
                
                // 验证手机号格式
                if (!WeChatUtils.isValidPhoneNumber(phoneNumber)) {
                    throw new IllegalArgumentException("获取到的手机号格式不正确: " + phoneNumber);
                }
                logger.info("✅ 成功解密用户手机号: [{}]", phoneNumber);
            } catch (Exception e) {
                logger.error("❌ 手机号解密失败，原因: {}", e.getMessage());
                throw new RuntimeException("手机号解密失败: " + e.getMessage(), e);
            }
            
            logger.info("🔐 微信授权成功 - 手机号: [{}], openid: [{}], unionid: [{}]", 
                phoneNumber, 
                openid.substring(0, Math.min(8, openid.length())) + "...",
                weChatInfo.getUnionid() != null ? weChatInfo.getUnionid().substring(0, Math.min(8, weChatInfo.getUnionid().length())) + "..." : "null");
            // 3. 五层角色判断（传递停车场信息）
            Map<String, Object> userInfo = determineUserRole(phoneNumber, openid, weChatInfo.getUnionid(), parkName);
            result.setData(userInfo);
            result.setCode("0");
            result.setMsg("授权成功");
            
            logger.info("✅ 最终返回给前端的响应: code={}, msg={}, data={}",
                result.getCode(), result.getMsg(), result.getData());
            
        } catch (Exception e) {
            logger.error("❌ 微信授权失败", e);
            result.setCode("1");
            result.setMsg("授权失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 五层角色判断逻辑
     * 1. Butler表（管家） → 2. Ownerinfo表（业主-本地） → 3. 外部API（业主-外部） → 4. VisitorApplication表（访客申请状态） → 5. Member表（访客）
     */
    private Map<String, Object> determineUserRole(String phoneNumber, String openid, String unionid, String parkName) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("phone", phoneNumber);
        userInfo.put("openid", openid);
        userInfo.put("parkName", parkName);
        if (unionid != null) {
            userInfo.put("unionid", unionid);
        }
        
        logger.info("🔍 开始角色查询（业主角色已暂时禁用），手机号: [{}], openid: [{}], 停车场: [{}]", phoneNumber, openid, parkName);
        
        // 第一层：查询管家表 (最高优先级) - 只验证手机号
        try {
            logger.info("🔍 第一层查询：Butler表（管家）- 只验证手机号");
            List<Butler> butlerList = butlerService.list();
            logger.info("📊 查询到管家总数: {}", butlerList.size());

            // 只匹配手机号
            Butler butler = butlerList.stream()
                .filter(b -> phoneNumber.equals(b.getPhone()))
                .findFirst()
                .orElse(null);

            if (butler != null) {
                logger.info("✅ 第一层查询成功：找到管家角色（手机号匹配）");
                userInfo.put("role", "manager");
                userInfo.put("roleText", "管家");
                userInfo.put("userInfo", butler);
                userInfo.put("permissions", Arrays.asList(getManagerPermissions()));
                userInfo.put("source", "butler_table");
                userInfo.put("verification", "phone_only");
                return userInfo;
            }

            logger.info("❌ 第一层查询：未找到管家（手机号不匹配），继续下一层查询");
        } catch (Exception e) {
            logger.error("❌ 第一层查询异常，继续下一层", e);
        }
        
        // 🚫 第二层：查询业主表（本地数据）- 已暂时注释
        // 注释原因：根据需求暂时禁用业主角色判定
        // 注释时间：2024-12-03
        /*
        try {
            logger.info("🔍 第二层查询：Ownerinfo表（业主-本地）- 验证手机号");
            List<Ownerinfo> ownerList = ownerinfoService.phoneNumberOwnerInfo(phoneNumber);
            logger.info("📊 业主表查询结果数量: {}", ownerList.size());
            
            if (!ownerList.isEmpty()) {
                Ownerinfo owner = ownerList.get(0);
                logger.info("✅ 第二层查询成功：找到业主角色（本地数据，手机号匹配）");
                userInfo.put("role", "owner");
                userInfo.put("roleText", "门岗登记");
                userInfo.put("userInfo", owner);
                userInfo.put("permissions", Arrays.asList(getOwnerPermissions()));
                userInfo.put("source", "ownerinfo_table");
                userInfo.put("verification", "phone_only");
                return userInfo;
            }
            
            logger.info("❌ 第二层查询：本地业主表无记录，继续外部API查询");
        } catch (Exception e) {
            logger.error("❌ 第二层查询异常，继续下一层", e);
        }
        */
        logger.info("⏭️ 第二层查询：业主表（本地）- 已跳过（暂时禁用）");
        
        // 🚫 第三层：查询外部API（业主补充验证）- 已暂时注释
        // 注释原因：根据需求暂时禁用业主角色判定
        // 注释时间：2024-12-03
        /*
        try {
            logger.info("🔍 第三层查询：外部API（业主-外部）- 验证手机号，停车场: [{}]", parkName);
            boolean isOwnerFromAPI = ownerRoleVerificationService.isOwnerByPhoneNumberInPark(phoneNumber, parkName);
            
            if (isOwnerFromAPI) {
                logger.info("✅ 第三层查询成功：找到业主角色（外部API，停车场: [{}]，手机号匹配）", parkName);
                
                // 获取外部API的业主详细信息
                Map<String, Object> apiOwnerInfo = ownerRoleVerificationService.getOwnerDetailsByPark(phoneNumber, parkName);
                
                userInfo.put("role", "owner");
                userInfo.put("roleText", "门岗登记");
                userInfo.put("userInfo", apiOwnerInfo);
                userInfo.put("permissions", Arrays.asList(getOwnerPermissions()));
                userInfo.put("source", "external_api");
                userInfo.put("verification", "phone_only");
                userInfo.put("parkName", parkName);
                userInfo.put("needSync", true); // 标记需要同步到本地数据库
                return userInfo;
            }
            
            logger.info("❌ 第三层查询：外部API无记录（停车场: [{}]），继续Member表查询", parkName);
        } catch (Exception e) {
            logger.warn("⚠️ 第三层查询：外部API调用异常（停车场: [{}]），继续下一层查询", parkName, e);
        }
        */
        logger.info("⏭️ 第三层查询：外部API（业主-外部）- 已跳过（暂时禁用）");
        
        // 第四层和第五层：不再查询访客相关表（visitor_application、Member）
        // 所有非管家用户统一返回unregistered，由前端根据扫码信息处理
        logger.info("📝 角色查询完毕（仅管家）：用户未找到，返回unregistered");
        logger.info("📝 访客不再需要后端注册，由前端根据扫码信息判断访客类型：");
        logger.info("   - 受邀访客：扫描管家邀请码（butlerId/applyKind）");
        logger.info("   - 外来访客：扫描车场二维码（qrId + 时间戳验证）");
        logger.info("   - 未知访客：无扫码信息，前端拒绝访问");
        
        userInfo.put("role", "unregistered");
        userInfo.put("roleText", "未注册");
        userInfo.put("userInfo", null);
        userInfo.put("permissions", Arrays.asList(new String[]{}));
        userInfo.put("source", "none");
        
        return userInfo;
    }
    
    /**
     * 通过code获取用户openid
     */
    @ApiOperation("通过code获取用户openid")
    @GetMapping("/getOpenid")
    public ResponseEntity<Result> getOpenidByCode(@RequestParam String code) {
        Result result = new Result();
        
        try {
            logger.info("📥 接收到获取openid请求 - code: [{}]", 
                code != null ? code.substring(0, Math.min(8, code.length())) + "..." : "null");
            
            // 调用微信API获取完整的登录信息
            WeChatInfo weChatInfo = WeChatUtils.getWeChatInfo(code);
            
            // 验证微信API调用结果
            if (!weChatInfo.hasValidInfo()) {
                throw new RuntimeException("微信授权失败: " + weChatInfo.getErrorDescription());
            }
            
            String openid = weChatInfo.getOpenid();
            String unionid = weChatInfo.getUnionid();
            
            logger.info("✅ 成功获取用户openid: [{}], unionid: [{}]", 
                openid.substring(0, Math.min(8, openid.length())) + "...",
                unionid != null ? unionid.substring(0, Math.min(8, unionid.length())) + "..." : "null");
            
            Map<String, String> data = new HashMap<>();
            data.put("openid", openid);
            if (unionid != null) {
                data.put("unionid", unionid);
            }
            
            result.setData(data);
            result.setCode("0");
            result.setMsg("获取openid成功");
            
        } catch (Exception e) {
            logger.error("❌ 获取openid失败", e);
            result.setCode("1");
            result.setMsg("获取openid失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 检查用户是否关注了公众号
     */
    @ApiOperation("检查用户是否关注了公众号")
    @GetMapping("/checkSubscription")
    public ResponseEntity<Result> checkUserSubscription(@RequestParam String openid) {
        Result result = new Result();
        
        try {
            logger.info("📥 接收到检查公众号关注状态请求 - openid: [{}]", 
                openid != null ? openid.substring(0, Math.min(8, openid.length())) + "..." : "null");
            
            // 调用服务检查用户是否关注了公众号
//            boolean isFollowed = wechatMessageService.checkUserSubscription(openid);
            
//            logger.info("✅ 用户 [{}] 公众号关注状态: {}",
//                openid.substring(0, Math.min(8, openid.length())) + "...",
//                isFollowed ? "已关注" : "未关注");
//
            Map<String, Object> data = new HashMap<>();
//            data.put("isFollowed", isFollowed);
            data.put("openid", openid);
            // 如果已关注，可以添加关注时间等信息（需要微信API支持）
            
            result.setData(data);
            result.setCode("0");
            result.setMsg("检查公众号关注状态成功");
            
        } catch (Exception e) {
            logger.error("❌ 检查公众号关注状态失败", e);
            result.setCode("1");
            result.setMsg("检查公众号关注状态失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 通过UnionID检查用户关注状态
     */
    @ApiOperation("通过UnionID检查用户关注状态")
    @GetMapping("/checkSubscriptionByUnionid")
    public ResponseEntity<Result> checkSubscriptionByUnionid(@RequestParam String unionid) {
        Result result = new Result();
        
        try {
            logger.info("📥 接收到UnionID关注状态检查请求 - unionid: [{}]", 
                unionid != null ? unionid.substring(0, Math.min(8, unionid.length())) + "..." : "null");

            
            Map<String, Object> data = new HashMap<>();
            data.put("unionid", unionid);

            
            result.setData(data);
            result.setCode("0");
            result.setMsg("UnionID关注状态检查成功");
            
        } catch (Exception e) {
            logger.error("❌ UnionID关注状态检查失败", e);
            result.setCode("1");
            result.setMsg("UnionID关注状态检查失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 生成临时绑定码（用于公众号和小程序关联）
     */
    @ApiOperation("生成临时绑定码")
    @PostMapping("/generateBindingCode")
    public ResponseEntity<Result> generateBindingCode(@RequestBody Map<String, Object> request) {
        Result result = new Result();
        
        try {
            String miniAppOpenid = (String) request.get("miniAppOpenid");
            String unionid = (String) request.get("unionid");
            String phone = (String) request.get("phone");
            
            logger.info("📱 生成绑定码请求 - 小程序openid: [{}], unionid: [{}]", 
                miniAppOpenid != null ? miniAppOpenid.substring(0, Math.min(8, miniAppOpenid.length())) + "..." : "null",
                unionid != null ? unionid.substring(0, Math.min(8, unionid.length())) + "..." : "null");
            
            // 生成临时绑定码（6位数字）
            String bindingCode = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
            
            // 保存绑定信息到缓存（15分钟有效期）
            Map<String, Object> bindingInfo = new HashMap<>();
            bindingInfo.put("miniAppOpenid", miniAppOpenid);
            bindingInfo.put("unionid", unionid);
            bindingInfo.put("phone", phone);
            bindingInfo.put("createTime", System.currentTimeMillis());
            bindingInfo.put("status", "waiting"); // waiting, bound
            
            // 这里应该使用Redis缓存，暂时用内存模拟
            // redisTemplate.opsForValue().set("binding:" + bindingCode, bindingInfo, 15, TimeUnit.MINUTES);
            
            // 生成带参数的公众号二维码URL
            String qrcodeUrl = "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=" + bindingCode;
            
            Map<String, Object> data = new HashMap<>();
            data.put("bindingCode", bindingCode);
            data.put("qrcodeUrl", qrcodeUrl);
            data.put("expireTime", System.currentTimeMillis() + 15 * 60 * 1000); // 15分钟后过期
            data.put("tips", "请在15分钟内扫描二维码关注公众号并回复绑定码：" + bindingCode);
            
            result.setData(data);
            result.setCode("0");
            result.setMsg("生成绑定码成功");
            
            logger.info("✅ 生成绑定码成功 - 绑定码: {}", bindingCode);
            
        } catch (Exception e) {
            logger.error("❌ 生成绑定码失败", e);
            result.setCode("1");
            result.setMsg("生成绑定码失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 检查绑定状态
     */
    @ApiOperation("检查绑定状态")
    @GetMapping("/checkBindingStatus")
    public ResponseEntity<Result> checkBindingStatus(@RequestParam String bindingCode) {
        Result result = new Result();
        
        try {
            logger.info("🔍 检查绑定状态 - 绑定码: {}", bindingCode);
            
            // 从缓存中获取绑定信息
            // Map<String, Object> bindingInfo = (Map<String, Object>) redisTemplate.opsForValue().get("binding:" + bindingCode);
            
            // 暂时模拟已绑定状态（实际应该从缓存读取）
            Map<String, Object> data = new HashMap<>();
            data.put("isBound", false);
            data.put("bindingCode", bindingCode);
            data.put("message", "绑定码有效，等待用户关注公众号");
            
            result.setData(data);
            result.setCode("0");
            result.setMsg("检查绑定状态成功");
            
        } catch (Exception e) {
            logger.error("❌ 检查绑定状态失败", e);
            result.setCode("1");
            result.setMsg("检查绑定状态失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取用户UnionID信息（静默登录）
     */
    @ApiOperation("获取用户UnionID信息")
    @PostMapping("/getUnionInfo")
    public ResponseEntity<Result> getUnionInfo(@RequestBody Map<String, Object> request) {
        Result result = new Result();
        
        try {
            String code = (String) request.get("code");
            
            logger.info("📥 接收到获取UnionID请求 - code: [{}]", 
                code != null ? code.substring(0, Math.min(8, code.length())) + "..." : "null");
            
            // 调用微信工具类获取信息
            WeChatInfo wechatInfo = WeChatUtils.getWeChatInfo(code);
            
            if (wechatInfo.hasValidInfo()) {
                Map<String, Object> data = new HashMap<>();
                data.put("openid", wechatInfo.getOpenid());
                data.put("sessionKey", wechatInfo.getSessionKey());
                data.put("unionid", wechatInfo.getUnionid());

                
                result.setData(data);
                result.setCode("0");
                result.setMsg("获取UnionID信息成功");
                
                logger.info("✅ 获取UnionID信息成功 - unionid: [{}]", 
                    wechatInfo.getUnionid() != null ? wechatInfo.getUnionid().substring(0, Math.min(8, wechatInfo.getUnionid().length())) + "..." : "null");
                    
            } else {
                result.setCode("1");
                result.setMsg("获取微信信息失败: " + wechatInfo.getErrorDescription());
            }
            
        } catch (Exception e) {
            logger.error("❌ 获取UnionID信息失败", e);
            result.setCode("1");
            result.setMsg("获取UnionID信息失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取用户UnionID信息（授权登录）
     */
    @ApiOperation("获取用户UnionID信息（带用户资料）")
    @PostMapping("/getUnionInfoWithAuth")
    public ResponseEntity<Result> getUnionInfoWithAuth(@RequestBody Map<String, Object> request) {
        Result result = new Result();
        
        try {
            String code = (String) request.get("code");
            String encryptedData = (String) request.get("encrypted_data");
            String iv = (String) request.get("iv");
            String signature = (String) request.get("signature");
            String rawData = (String) request.get("raw_data");
            
            logger.info("📥 接收到授权登录获取UnionID请求 - code: [{}]", 
                code != null ? code.substring(0, Math.min(8, code.length())) + "..." : "null");
            
            // 先获取基本信息
            WeChatInfo wechatInfo = WeChatUtils.getWeChatInfo(code);
            
            if (!wechatInfo.hasValidInfo()) {
                result.setCode("1");
                result.setMsg("获取微信基本信息失败: " + wechatInfo.getErrorDescription());
                return ResponseEntity.ok(result);
            }
            
            // 解密用户详细信息
            String sessionKey = wechatInfo.getSessionKey();
            // 这里需要实现AES解密逻辑，解密 encryptedData
            // 暂时先返回基本信息
            
            Map<String, Object> data = new HashMap<>();
            data.put("openid", wechatInfo.getOpenid());
            data.put("sessionKey", wechatInfo.getSessionKey());
            data.put("unionid", wechatInfo.getUnionid());
            
            // TODO: 从解密的数据中提取用户详细信息
            // data.put("nickname", decryptedUserInfo.getNickname());
            // data.put("avatarUrl", decryptedUserInfo.getAvatarUrl());
            result.setData(data);
            result.setCode("0");
            result.setMsg("获取用户详细信息成功");
            
            logger.info("✅ 授权登录获取UnionID信息成功");
            
        } catch (Exception e) {
            logger.error("❌ 授权登录获取UnionID信息失败", e);
            result.setCode("1");
            result.setMsg("授权登录获取UnionID信息失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取管家权限列表
     */
    private String[] getManagerPermissions() {
        return new String[]{
            "appointment.query",        // 预约查询
            "appointment.audit",        // 预约审核
            "appointment.query.all",    // 查询所有预约
            "violation.manage",         // 违规管理
            "violation.view.all",       // 查看所有违规
            "audit.member",            // 审核会员申请
            "audit.appointment",       // 审核预约申请
            "owner.manage",            // 业主管理
            "manage.facility"          // 设施管理
        };
    }
    
    /**
     * 获取业主权限列表
     */
    private String[] getOwnerPermissions() {
        return new String[]{
            "appointment.create",       // 创建预约
            "appointment.query.own",    // 查询个人预约
            "appointment.cancel",       // 取消预约
            "violation.view.own",       // 查看个人违规
            "violation.report"          // 举报违规
        };
    }
    
    /**
     * 获取访客权限列表（最小权限，仅预约相关）
     */
    private String[] getVisitorPermissions() {
        return new String[]{
            "visitor.appointment",      // 访客预约（专用）
            "visitor.query",           // 访客查询（专用）
            "appointment.query.own"    // 查询个人预约（基础）
        };
    }
    
    /**
     * 检查用户权限
     */
    @ApiOperation("检查用户权限")
    @GetMapping("/checkPermission")
    public ResponseEntity<Result> checkPermission(
            @RequestParam String phoneNumber,
            @RequestParam String permission,
            @RequestParam(required = false) String openid,
            @RequestParam(required = false, defaultValue = "四季上东") String parkName) {
        Result result = new Result();
        
        try {
            Map<String, Object> userInfo = determineUserRole(phoneNumber, openid, null, parkName);
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) userInfo.get("permissions");
            
            boolean hasPermission = false;
            for (String p : permissions) {
                if (p.equals(permission)) {
                    hasPermission = true;
                    break;
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("hasPermission", hasPermission);
            data.put("userRole", userInfo.get("role"));
            data.put("roleText", userInfo.get("roleText"));
            data.put("source", userInfo.get("source"));
            
            result.setData(data);
            result.setCode("0");
            result.setMsg("检查完成");
            
        } catch (Exception e) {
            logger.error("❌ 权限检查失败", e);
            result.setCode("1");
            result.setMsg("权限检查失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取角色统计信息（调试用）
     */
    @ApiOperation("获取角色统计信息")
    @GetMapping("/roleStats")
    public ResponseEntity<Result> getRoleStats() {
        Result result = new Result();
        
        try {
            Map<String, Object> stats = ownerRoleVerificationService.getStatistics();
            
            // 添加本地数据统计
            long butlerCount = butlerService.count();
            long ownerCount = ownerinfoService.count();
            long memberCount = memberService.count();
            
            stats.put("butlerCount", butlerCount);
            stats.put("ownerCount", ownerCount);
            stats.put("memberCount", memberCount);
            
            result.setData(stats);
            result.setCode("0");
            result.setMsg("统计信息获取成功");
            
        } catch (Exception e) {
            logger.error("❌ 获取统计信息失败", e);
            result.setCode("1");
            result.setMsg("获取统计信息失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
} 