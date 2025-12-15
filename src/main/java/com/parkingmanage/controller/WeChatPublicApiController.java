package com.parkingmanage.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.common.HttpClientUtil;
import com.parkingmanage.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信公众号API控制器
 * 实现微信公众号基础接口调用
 * 
 * @author MLH
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/parking/wechat-public")
@CrossOrigin(origins = "*")  // 🆕 添加跨域支持，允许静态HTML文件访问
@Api(tags = "微信公众号API接口")
public class WeChatPublicApiController {
    
    private static final Logger logger = LoggerFactory.getLogger(WeChatPublicApiController.class);
    
    // 微信API基础URL
    private static final String WECHAT_API_BASE = "https://api.weixin.qq.com";
    
    // 可以通过配置文件注入，这里先硬编码用于演示
    @Value("${wechat.public.appid:}")
    private String defaultAppId;
    
    @Value("${wechat.public.secret:}")
    private String defaultSecret;
    
    /**
     * 获取接口调用凭据 (access_token)
     * 参考文档：https://developers.weixin.qq.com/doc/service/api/base/api_getaccesstoken.html
     */
    @ApiOperation("获取微信公众号access_token")
    @RequestMapping("/getAccessToken")
    public ResponseEntity<Result> getAccessToken(
            @ApiParam(value = "公众号的唯一凭证", required = false) @RequestParam(required = false) String appid,
            @ApiParam(value = "公众号的唯一凭证密钥", required = false) @RequestParam(required = false) String secret) {
        
        Result result = new Result();
        
        try {
            // 使用传入的参数，如果没有则使用配置的默认值
            String finalAppId = (appid != null && !appid.trim().isEmpty()) ? appid : defaultAppId;
            String finalSecret = (secret != null && !secret.trim().isEmpty()) ? secret : defaultSecret;
            
            // 参数验证
            if (finalAppId == null || finalAppId.trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("缺少必需参数：appid");
                return ResponseEntity.ok(result);
            }
            
            if (finalSecret == null || finalSecret.trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("缺少必需参数：secret");
                return ResponseEntity.ok(result);
            }
            
            logger.info("🔑 开始获取access_token - appid: [{}]", finalAppId);
            
            // 构建请求参数
            Map<String, String> params = new HashMap<>();
            params.put("grant_type", "client_credential");
            params.put("appid", finalAppId);
            params.put("secret", finalSecret);
            
            // 调用微信API
            String url = WECHAT_API_BASE + "/cgi-bin/token";
            String response = HttpClientUtil.doGet(url, params);
            
            logger.info("📥 微信API响应: {}", response);
            
            // 解析响应
            JSONObject jsonResponse = JSONObject.parseObject(response);
            
            if (jsonResponse.containsKey("access_token")) {
                // 成功获取access_token
                String accessToken = jsonResponse.getString("access_token");
                Integer expiresIn = jsonResponse.getInteger("expires_in");
                
                Map<String, Object> data = new HashMap<>();
                data.put("access_token", accessToken);
                data.put("expires_in", expiresIn);
                data.put("expires_time", System.currentTimeMillis() + (expiresIn * 1000L)); // 过期时间戳
                
                result.setData(data);
                result.setCode("0");
                result.setMsg("获取access_token成功");
                
                logger.info("✅ 成功获取access_token，有效期: {} 秒", expiresIn);
                
            } else {
                // API调用失败
                Integer errcode = jsonResponse.getInteger("errcode");
                String errmsg = jsonResponse.getString("errmsg");
                
                result.setCode("1");
                result.setMsg("获取access_token失败: [" + errcode + "] " + errmsg);
                
                logger.error("❌ 获取access_token失败 - 错误码: {}, 错误信息: {}", errcode, errmsg);
            }
            
        } catch (Exception e) {
            logger.error("❌ 获取access_token异常", e);
            result.setCode("1");
            result.setMsg("获取access_token异常: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 批量获取用户基本信息
     * 参考文档：https://developers.weixin.qq.com/doc/service/api/usermanage/userinfo/api_batchuserinfo.html
     */
    @ApiOperation("批量获取关注公众号的用户基本信息")
    @RequestMapping("/batchGetUserInfo")
    public ResponseEntity<Result> batchGetUserInfo(
            @ApiParam(value = "接口调用凭证", required = true) @RequestParam String accessToken,
            @ApiParam(value = "用户openid列表", required = true) @RequestBody Map<String, Object> requestBody) {
        
        Result result = new Result();
        
        try {
            // 获取openid列表
            @SuppressWarnings("unchecked")
            List<String> openidList = (List<String>) requestBody.get("openids");
            String lang = (String) requestBody.getOrDefault("lang", "zh_CN"); // 默认简体中文
            
            // 参数验证
            if (accessToken == null || accessToken.trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("缺少必需参数：access_token");
                return ResponseEntity.ok(result);
            }
            
            if (openidList == null || openidList.isEmpty()) {
                result.setCode("1");
                result.setMsg("缺少必需参数：openids列表不能为空");
                return ResponseEntity.ok(result);
            }
            
            if (openidList.size() > 100) {
                result.setCode("1");
                result.setMsg("openids列表长度不能超过100");
                return ResponseEntity.ok(result);
            }
            
            logger.info("📊 开始批量获取用户信息 - 用户数量: {}, 语言: {}", openidList.size(), lang);
            
            // 构建请求体
            JSONObject requestJson = new JSONObject();
            JSONArray userList = new JSONArray();
            
            for (String openid : openidList) {
                JSONObject userInfo = new JSONObject();
                userInfo.put("openid", openid);
                userInfo.put("lang", lang);
                userList.add(userInfo);
            }
            
            requestJson.put("user_list", userList);
            
            // 调用微信API
            String url = WECHAT_API_BASE + "/cgi-bin/user/info/batchget?access_token=" + accessToken;
            String response = HttpClientUtil.doPostJson(url, requestJson.toJSONString());
            
            logger.info("📥 微信API响应: {}", response);
            
            // 解析响应
            JSONObject jsonResponse = JSONObject.parseObject(response);
            
            if (jsonResponse.containsKey("user_info_list")) {
                // 成功获取用户信息
                JSONArray userInfoList = jsonResponse.getJSONArray("user_info_list");
                
                Map<String, Object> data = new HashMap<>();
                data.put("user_info_list", userInfoList);
                data.put("total_count", userInfoList.size());
                data.put("request_count", openidList.size());
                
                result.setData(data);
                result.setCode("0");
                result.setMsg("批量获取用户信息成功");
                
                logger.info("✅ 成功获取用户信息 - 返回数量: {}", userInfoList.size());
                
            } else {
                // API调用失败
                Integer errcode = jsonResponse.getInteger("errcode");
                String errmsg = jsonResponse.getString("errmsg");
                
                result.setCode("1");
                result.setMsg("批量获取用户信息失败: [" + errcode + "] " + errmsg);
                
                logger.error("❌ 批量获取用户信息失败 - 错误码: {}, 错误信息: {}", errcode, errmsg);
            }
            
        } catch (Exception e) {
            logger.error("❌ 批量获取用户信息异常", e);
            result.setCode("1");
            result.setMsg("批量获取用户信息异常: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取单个用户基本信息
     * 参考文档：https://developers.weixin.qq.com/doc/service/api/usermanage/userinfo/api_userinfo.html
     */
    @ApiOperation("获取单个用户基本信息")
    @RequestMapping("/getUserInfo")
    public ResponseEntity<Result> getUserInfo(
            @ApiParam(value = "接口调用凭证", required = true) @RequestParam String accessToken,
            @ApiParam(value = "用户openid", required = true) @RequestParam String openid,
            @ApiParam(value = "返回国家地区语言版本", required = false) @RequestParam(defaultValue = "zh_CN") String lang) {
        
        Result result = new Result();
        
        try {
            // 参数验证
            if (accessToken == null || accessToken.trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("缺少必需参数：access_token");
                return ResponseEntity.ok(result);
            }
            
            if (openid == null || openid.trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("缺少必需参数：openid");
                return ResponseEntity.ok(result);
            }
            
            logger.info("👤 开始获取用户信息 - openid: [{}], 语言: {}", 
                openid.length() > 8 ? openid.substring(0, 8) + "..." : openid, lang);
            
            // 构建请求参数
            Map<String, String> params = new HashMap<>();
            params.put("access_token", accessToken);
            params.put("openid", openid);
            params.put("lang", lang);
            
            // 调用微信API
            String url = WECHAT_API_BASE + "/cgi-bin/user/info";
            String response = HttpClientUtil.doGet(url, params);
            
            logger.info("📥 微信API响应: {}", response);
            
            // 解析响应
            JSONObject jsonResponse = JSONObject.parseObject(response);
            
            if (jsonResponse.containsKey("openid")) {
                // 成功获取用户信息
                result.setData(jsonResponse);
                result.setCode("0");
                result.setMsg("获取用户信息成功");
                
                logger.info("✅ 成功获取用户信息 - 用户: {}", jsonResponse.getString("nickname"));
                
            } else {
                // API调用失败
                Integer errcode = jsonResponse.getInteger("errcode");
                String errmsg = jsonResponse.getString("errmsg");
                
                result.setCode("1");
                result.setMsg("获取用户信息失败: [" + errcode + "] " + errmsg);
                
                logger.error("❌ 获取用户信息失败 - 错误码: {}, 错误信息: {}", errcode, errmsg);
            }
            
        } catch (Exception e) {
            logger.error("❌ 获取用户信息异常", e);
            result.setCode("1");
            result.setMsg("获取用户信息异常: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取关注用户列表
     * 参考文档：https://developers.weixin.qq.com/doc/service/api/usermanage/userinfo/api_getfans.html
     */
    @ApiOperation("获取关注用户列表")
    @RequestMapping("/getUserList")
    public ResponseEntity<Result> getUserList(
            @ApiParam(value = "接口调用凭证", required = true) @RequestParam String accessToken,
            @ApiParam(value = "拉取列表的第一个用户的OPENID，不填默认从头开始拉取", required = false) @RequestParam(required = false) String nextOpenid) {
        
        Result result = new Result();
        
        try {
            // 参数验证
            if (accessToken == null || accessToken.trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("缺少必需参数：access_token");
                return ResponseEntity.ok(result);
            }
            
            logger.info("📋 开始获取关注用户列表 - nextOpenid: {}", 
                nextOpenid != null ? (nextOpenid.length() > 8 ? nextOpenid.substring(0, 8) + "..." : nextOpenid) : "从头开始");
            
            // 构建请求参数
            Map<String, String> params = new HashMap<>();
            params.put("access_token", accessToken);
            if (nextOpenid != null && !nextOpenid.trim().isEmpty()) {
                params.put("next_openid", nextOpenid);
            }
            
            // 调用微信API
            String url = WECHAT_API_BASE + "/cgi-bin/user/get";
            String response = HttpClientUtil.doGet(url, params);
            
            logger.info("📥 微信API响应: {}", response);
            
            // 解析响应
            JSONObject jsonResponse = JSONObject.parseObject(response);
            
            if (jsonResponse.containsKey("total")) {
                // 成功获取用户列表
                result.setData(jsonResponse);
                result.setCode("0");
                result.setMsg("获取关注用户列表成功");
                
                Integer total = jsonResponse.getInteger("total");
                Integer count = jsonResponse.getInteger("count");
                logger.info("✅ 成功获取关注用户列表 - 总数: {}, 本次返回: {}", total, count);
                
            } else {
                // API调用失败
                Integer errcode = jsonResponse.getInteger("errcode");
                String errmsg = jsonResponse.getString("errmsg");
                
                result.setCode("1");
                result.setMsg("获取关注用户列表失败: [" + errcode + "] " + errmsg);
                
                logger.error("❌ 获取关注用户列表失败 - 错误码: {}, 错误信息: {}", errcode, errmsg);
            }
            
        } catch (Exception e) {
            logger.error("❌ 获取关注用户列表异常", e);
            result.setCode("1");
            result.setMsg("获取关注用户列表异常: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 检查access_token有效性
     */
    @ApiOperation("检查access_token有效性")
    @RequestMapping("/checkAccessToken")
    public ResponseEntity<Result> checkAccessToken(
            @ApiParam(value = "接口调用凭证", required = true) @RequestParam String accessToken) {
        
        Result result = new Result();
        
        try {
            logger.info("🔍 检查access_token有效性");
            
            // 通过调用一个简单的API来检查token有效性
            Map<String, String> params = new HashMap<>();
            params.put("access_token", accessToken);
            
            String url = WECHAT_API_BASE + "/cgi-bin/getcallbackip";
            String response = HttpClientUtil.doGet(url, params);
            
            JSONObject jsonResponse = JSONObject.parseObject(response);
            
            if (jsonResponse.containsKey("ip_list")) {
                result.setCode("0");
                result.setMsg("access_token有效");
                Map<String, Object> data = new HashMap<>();
                data.put("valid", true);
                data.put("response", jsonResponse);
                result.setData(data);
                logger.info("✅ access_token有效");
            } else {
                Integer errcode = jsonResponse.getInteger("errcode");
                String errmsg = jsonResponse.getString("errmsg");
                
                result.setCode("1");
                result.setMsg("access_token无效: [" + errcode + "] " + errmsg);
                Map<String, Object> data = new HashMap<>();
                data.put("valid", false);
                data.put("errcode", errcode);
                data.put("errmsg", errmsg);
                result.setData(data);
                logger.warn("⚠️ access_token无效 - 错误码: {}, 错误信息: {}", errcode, errmsg);
            }
            
        } catch (Exception e) {
            logger.error("❌ 检查access_token异常", e);
            result.setCode("1");
            result.setMsg("检查access_token异常: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取jsapi_ticket（用于JS-SDK）
     * 参考文档：https://developers.weixin.qq.com/doc/service/api/base/api_getticket.html
     */
    @ApiOperation("获取jsapi_ticket")
    @RequestMapping("/getJsapiTicket")
    public ResponseEntity<Result> getJsapiTicket(
            @ApiParam(value = "access_token", required = true) @RequestParam String accessToken) {
        
        Result result = new Result();
        
        try {
            logger.info("🎫 开始获取jsapi_ticket");
            
            // 构建请求参数
            Map<String, String> params = new HashMap<>();
            params.put("access_token", accessToken);
            params.put("type", "jsapi");
            
            // 调用微信API
            String url = WECHAT_API_BASE + "/cgi-bin/ticket/getticket";
            String response = HttpClientUtil.doGet(url, params);
            
            logger.info("📥 获取jsapi_ticket响应: {}", response);
            
            // 解析响应
            JSONObject jsonResponse = JSONObject.parseObject(response);
            
            if (jsonResponse.getInteger("errcode") == 0) {
                // 成功获取jsapi_ticket
                String ticket = jsonResponse.getString("ticket");
                Integer expiresIn = jsonResponse.getInteger("expires_in");
                
                Map<String, Object> data = new HashMap<>();
                data.put("ticket", ticket);
                data.put("expires_in", expiresIn);
                data.put("expires_time", System.currentTimeMillis() + (expiresIn * 1000L));
                
                result.setData(data);
                result.setCode("0");
                result.setMsg("获取jsapi_ticket成功");
                
                logger.info("✅ 成功获取jsapi_ticket，有效期: {} 秒", expiresIn);
                
            } else {
                // API调用失败
                Integer errcode = jsonResponse.getInteger("errcode");
                String errmsg = jsonResponse.getString("errmsg");
                
                result.setCode("1");
                result.setMsg("获取jsapi_ticket失败: [" + errcode + "] " + errmsg);
                
                logger.error("❌ 获取jsapi_ticket失败 - 错误码: {}, 错误信息: {}", errcode, errmsg);
            }
            
        } catch (Exception e) {
            logger.error("❌ 获取jsapi_ticket异常", e);
            result.setCode("1");
            result.setMsg("获取jsapi_ticket异常: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 生成JS-SDK配置签名
     * 参考文档：https://developers.weixin.qq.com/doc/service/api/base/api_jsconfig.html
     */
    @ApiOperation("生成JS-SDK配置签名")
    @PostMapping("/getJssdkSignature")
    public ResponseEntity<Result> getJssdkSignature(@RequestBody Map<String, String> requestBody) {
        
        Result result = new Result();
        
        try {
            String url = requestBody.get("url");
            String appid = requestBody.get("appid");
            String secret = requestBody.get("secret");
            
            // 参数验证
            if (url == null || url.trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("缺少必需参数：url");
                return ResponseEntity.ok(result);
            }
            
            // 使用传入的参数，如果没有则使用配置的默认值
            String finalAppId = (appid != null && !appid.trim().isEmpty()) ? appid : defaultAppId;
            String finalSecret = (secret != null && !secret.trim().isEmpty()) ? secret : defaultSecret;
            
            if (finalAppId == null || finalAppId.trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("缺少必需参数：appid");
                return ResponseEntity.ok(result);
            }
            
            if (finalSecret == null || finalSecret.trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("缺少必需参数：secret");
                return ResponseEntity.ok(result);
            }
            
            logger.info("🔐 开始生成JS-SDK签名 - url: [{}], appid: [{}]", url, finalAppId);
            
            // 1. 获取access_token
            String accessToken = getAccessTokenInternal(finalAppId, finalSecret);
            if (accessToken == null) {
                result.setCode("1");
                result.setMsg("获取access_token失败");
                return ResponseEntity.ok(result);
            }
            
            // 2. 获取jsapi_ticket
            String jsapiTicket = getJsapiTicketInternal(accessToken);
            if (jsapiTicket == null) {
                result.setCode("1");
                result.setMsg("获取jsapi_ticket失败");
                return ResponseEntity.ok(result);
            }
            
            // 3. 生成签名
            String nonceStr = generateNonceStr();
            long timestamp = System.currentTimeMillis() / 1000;
            String signature = generateSignature(jsapiTicket, nonceStr, timestamp, url);
            
            // 4. 返回JS-SDK配置
            Map<String, Object> data = new HashMap<>();
            data.put("appId", finalAppId);
            data.put("timestamp", timestamp);
            data.put("nonceStr", nonceStr);
            data.put("signature", signature);
            data.put("url", url);
            
            result.setData(data);
            result.setCode("0");
            result.setMsg("生成JS-SDK签名成功");
            
            logger.info("✅ 成功生成JS-SDK签名");
            
        } catch (Exception e) {
            logger.error("❌ 生成JS-SDK签名异常", e);
            result.setCode("1");
            result.setMsg("生成JS-SDK签名异常: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 内部方法：获取access_token
     */
    private String getAccessTokenInternal(String appid, String secret) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("grant_type", "client_credential");
            params.put("appid", appid);
            params.put("secret", secret);
            
            String url = WECHAT_API_BASE + "/cgi-bin/token";
            String response = HttpClientUtil.doGet(url, params);
            JSONObject jsonResponse = JSONObject.parseObject(response);
            
            if (jsonResponse.containsKey("access_token")) {
                return jsonResponse.getString("access_token");
            } else {
                logger.error("❌ 获取access_token失败: {}", response);
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ 获取access_token异常", e);
            return null;
        }
    }
    
    /**
     * 内部方法：获取jsapi_ticket
     */
    private String getJsapiTicketInternal(String accessToken) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("access_token", accessToken);
            params.put("type", "jsapi");
            
            String url = WECHAT_API_BASE + "/cgi-bin/ticket/getticket";
            String response = HttpClientUtil.doGet(url, params);
            JSONObject jsonResponse = JSONObject.parseObject(response);
            
            if (jsonResponse.getInteger("errcode") == 0) {
                return jsonResponse.getString("ticket");
            } else {
                logger.error("❌ 获取jsapi_ticket失败: {}", response);
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ 获取jsapi_ticket异常", e);
            return null;
        }
    }
    
    /**
     * 生成随机字符串
     */
    private String generateNonceStr() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            result.append(characters.charAt((int) (Math.random() * characters.length())));
        }
        return result.toString();
    }
    
    /**
     * 生成JS-SDK签名
     */
    private String generateSignature(String jsapiTicket, String nonceStr, long timestamp, String url) {
        try {
            // 1. 对所有待签名参数按照字段名的ASCII码从小到大排序（字典序）
            String string1 = "jsapi_ticket=" + jsapiTicket +
                           "&noncestr=" + nonceStr +
                           "&timestamp=" + timestamp +
                           "&url=" + url;
            
            logger.debug("🔗 待签名字符串: {}", string1);
            
            // 2. 使用SHA1算法对字符串加密
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(string1.getBytes("UTF-8"));
            
            // 3. 转换为16进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (Exception e) {
            logger.error("❌ 生成签名异常", e);
            throw new RuntimeException("生成签名失败", e);
        }
    }
} 