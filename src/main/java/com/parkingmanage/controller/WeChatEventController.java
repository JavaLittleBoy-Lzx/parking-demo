package com.parkingmanage.controller;

import com.parkingmanage.service.WeChatEventService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * 微信公众号事件推送接收控制器
 * 用于接收微信服务器推送的用户关注、取消关注等事件
 * 
 * @author MLH
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/wechat")
@Api(tags = "微信事件推送")
public class WeChatEventController {
    
    private static final Logger logger = LoggerFactory.getLogger(WeChatEventController.class);
    
    // 微信公众号配置的Token，用于验证请求来源
    @Value("${wechat.token}")
    private String wechatToken;
    
    @Resource
    private WeChatEventService weChatEventService;
    
    /**
     * 微信服务器验证接口
     * 微信服务器会发送GET请求验证URL的有效性
     */
    @GetMapping("/event")
    @ApiOperation("微信服务器URL验证")
    public String verifyWeChatServer(
            @RequestParam(value = "signature", required = false) String signature,
            @RequestParam(value = "timestamp", required = false) String timestamp, 
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "echostr", required = false) String echostr) {
        
        // 检查是否为微信验证请求
        if (signature == null || timestamp == null || nonce == null || echostr == null) {
            logger.info("📄 收到非微信验证请求，可能是浏览器直接访问");
            return "WeChat Event Service is Running! Please configure this URL in your WeChat Official Account.";
        }
        
        logger.info("📥 收到微信服务器验证请求 - signature: {}, timestamp: {}, nonce: {}", 
            signature, timestamp, nonce);
        
        try {
            // 验证请求是否来自微信服务器
            if (verifySignature(signature, timestamp, nonce)) {
                logger.info("✅ 微信服务器验证成功");
                return echostr;
            } else {
                logger.error("❌ 微信服务器验证失败");
                return "";
            }
        } catch (Exception e) {
            logger.error("❌ 微信服务器验证异常", e);
            return "";
        }
    }
    
    /**
     * 接收微信事件推送
     * 根据官方文档：微信服务器在五秒内收不到响应会断掉连接，并且重新发起请求，总共重试三次
     * 推荐使用FromUserName + CreateTime 排重
     * 如果服务器无法保证在五秒内处理并回复，可以直接回复空串，微信服务器不会对此作任何处理，并且不会发起重试
     */
    @PostMapping("/event")
    @ApiOperation("接收微信事件推送")
    public String receiveWeChatEvent(HttpServletRequest request, HttpServletResponse response) {
        logger.info("📥 收到微信事件推送");
        
        // 添加请求参数验证日志
        String signature = request.getParameter("signature");
        String timestamp = request.getParameter("timestamp");
        String nonce = request.getParameter("nonce");
        logger.info("📋 POST请求参数 - signature: {}, timestamp: {}, nonce: {}", signature, timestamp, nonce);
        
        // 验证POST请求签名（可选，但建议验证）
        if (signature != null && timestamp != null && nonce != null) {
            if (!verifySignature(signature, timestamp, nonce)) {
                logger.error("❌ POST请求签名验证失败");
                return "";
            }
            logger.info("✅ POST请求签名验证成功");
        }
        
        try {
            // 获取微信推送的XML数据
            String xmlData = getRequestBody(request);
            logger.info("📄 接收到XML数据: {}", xmlData);
            
            // 简单解析XML数据以便调试
            if (xmlData.contains("<MsgType><![CDATA[event]]></MsgType>")) {
                logger.info("🎯 确认收到事件类型消息");
                if (xmlData.contains("<Event><![CDATA[subscribe]]></Event>")) {
                    logger.info("👋 确认收到用户关注事件！");
                } else if (xmlData.contains("<Event><![CDATA[unsubscribe]]></Event>")) {
                    logger.info("👋 确认收到用户取消关注事件！");
                }
            }
            
            // 异步处理事件，确保在5秒内响应微信服务器
            weChatEventService.processEventAsync(xmlData);
            
            // 立即返回success给微信服务器，避免超时重试
            return "success";
            
        } catch (Exception e) {
            logger.error("❌ 处理微信事件推送异常", e);
            // 根据官方文档，异常时返回空串，避免微信重试
            return "";
        }
    }
    
    /**
     * 验证微信签名
     */
    private boolean verifySignature(String signature, String timestamp, String nonce) {
        try {
            // 1. 将token、timestamp、nonce三个参数进行字典序排序
            String[] params = {wechatToken, timestamp, nonce};
            Arrays.sort(params);
            
            // 2. 将三个参数字符串拼接成一个字符串进行sha1加密
            String str = params[0] + params[1] + params[2];
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(str.getBytes());
            
            // 3. 转换为16进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // 4. 与signature对比
            return hexString.toString().equals(signature);
            
        } catch (Exception e) {
            logger.error("❌ 验证微信签名异常", e);
            return false;
        }
    }
    
    /**
     * 获取请求体内容
     */
    private String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder xmlData = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        
        while ((line = reader.readLine()) != null) {
            xmlData.append(line);
        }
        
        return xmlData.toString();
    }
} 