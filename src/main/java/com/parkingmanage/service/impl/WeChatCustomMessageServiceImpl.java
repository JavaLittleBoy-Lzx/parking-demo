package com.parkingmanage.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.service.WeChatCustomMessageService;
import com.parkingmanage.service.WeChatTempMediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 微信客服消息服务实现类
 * 
 * @author System
 * @since 2024-01-01
 */
@Slf4j
@Service
public class WeChatCustomMessageServiceImpl implements WeChatCustomMessageService {
    
    @Value("${wechat.public.appid}")
    private String appId;
    
    @Value("${wechat.public.secret}")
    private String secret;
    
    @Value("${wechat.miniapp.appid:}")
    private String miniAppId;
    
    @Value("${wechat.welcome.text:欢迎关注！您可以通过小程序进行停车预约、查询等操作。}")
    private String welcomeText;
    
    @Resource
    private WeChatTempMediaService weChatTempMediaService;
    
    @Override
    public boolean sendTextMessage(String openId, String content) {
        try {
            log.info("📤 发送文本消息 - openId: {}, 内容: {}", openId, content);
            
            String accessToken = getAccessToken();
            if (accessToken == null) {
                return false;
            }
            
            String url = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=" + accessToken;
            
            JSONObject message = new JSONObject();
            message.put("touser", openId);
            message.put("msgtype", "text");
            
            JSONObject text = new JSONObject();
            text.put("content", content);
            message.put("text", text);
            
            JSONObject result = sendPostRequest(url, message.toJSONString());
            
            if (result.getInteger("errcode") == 0) {
                log.info("✅ 文本消息发送成功");
                return true;
            } else {
                log.error("❌ 文本消息发送失败 - errcode: {}, errmsg: {}", 
                    result.getInteger("errcode"), result.getString("errmsg"));
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ 发送文本消息异常", e);
            return false;
        }
    }
    
    @Override
    public boolean sendImageMessage(String openId, String mediaId) {
        try {
            log.info("📤 发送图片消息 - openId: {}, mediaId: {}", openId, mediaId);
            
            String accessToken = getAccessToken();
            if (accessToken == null) {
                return false;
            }
            
            String url = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=" + accessToken;
            
            JSONObject message = new JSONObject();
            message.put("touser", openId);
            message.put("msgtype", "image");
            
            JSONObject image = new JSONObject();
            image.put("media_id", mediaId);
            message.put("image", image);
            
            JSONObject result = sendPostRequest(url, message.toJSONString());
            
            if (result.getInteger("errcode") == 0) {
                log.info("✅ 图片消息发送成功");
                return true;
            } else {
                log.error("❌ 图片消息发送失败 - errcode: {}, errmsg: {}", 
                    result.getInteger("errcode"), result.getString("errmsg"));
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ 发送图片消息异常", e);
            return false;
        }
    }
    
    @Override
    public boolean sendMiniprogramCard(String openId, String title, String appId, 
                                      String pagePath, String thumbMediaId) {
        try {
            log.info("📤 发送小程序卡片 - openId: {}, title: {}", openId, title);
            
            String accessToken = getAccessToken();
            if (accessToken == null) {
                return false;
            }
            
            String url = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=" + accessToken;
            
            JSONObject message = new JSONObject();
            message.put("touser", openId);
            message.put("msgtype", "miniprogrampage");
            
            JSONObject miniprogrampage = new JSONObject();
            miniprogrampage.put("title", title);
            miniprogrampage.put("appid", appId);
            miniprogrampage.put("pagepath", pagePath);
            miniprogrampage.put("thumb_media_id", thumbMediaId);
            message.put("miniprogrampage", miniprogrampage);
            
            JSONObject result = sendPostRequest(url, message.toJSONString());
            
            if (result.getInteger("errcode") == 0) {
                log.info("✅ 小程序卡片发送成功");
                return true;
            } else {
                log.error("❌ 小程序卡片发送失败 - errcode: {}, errmsg: {}", 
                    result.getInteger("errcode"), result.getString("errmsg"));
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ 发送小程序卡片异常", e);
            return false;
        }
    }
    
    @Override
    public boolean sendWelcomeMessage(String openId) {
        try {
            log.info("👋 发送欢迎消息 - openId: {}", openId);
            log.info("📌 小程序配置 - miniAppId: {}, welcomeText: {}", miniAppId, welcomeText);
            
            // 1. 发送欢迎文本
            boolean textSent = sendTextMessage(openId, welcomeText);
            if (!textSent) {
                log.warn("⚠️ 欢迎文本发送失败");
                return false;
            }
            
            // 延迟800ms，避免消息发送过快
            Thread.sleep(800);
            
            // 2. 获取小程序二维码的media_id
            String qrcodeMediaId = weChatTempMediaService.getValidMediaId("小程序二维码");
            log.info("🔍 获取到的media_id: {}", qrcodeMediaId);
            
            if (qrcodeMediaId != null && miniAppId != null && !miniAppId.isEmpty()) {
                log.info("📤 准备发送小程序卡片 - appid: {}, pagepath: pages/auth/phone-auth", miniAppId);
                
                // 发送小程序卡片
                boolean cardSent = sendMiniprogramCard(
                    openId,
                    "雪人停车小程序",  // 卡片标题
                    miniAppId,
                    "pages/auth/phone-auth",  // 小程序页面路径
                    qrcodeMediaId  // 封面图media_id
                );
                
                if (cardSent) {
                    log.info("✅ 欢迎消息发送成功（文本+小程序卡片）");
                    return true;
                } else {
                    log.error("❌ 小程序卡片发送失败！");
                    log.error("❌ 请检查：1.小程序AppID是否正确 2.页面路径是否存在 3.封面图是否有效");
                    // 作为降级方案，发送图片消息
                    log.info("📤 降级方案：发送图片消息");
                    sendImageMessage(openId, qrcodeMediaId);
                    return true;
                }
            } else {
                if (qrcodeMediaId == null) {
                    log.warn("⚠️ 未找到小程序二维码素材！");
                    log.warn("⚠️ 请确保：1.已上传素材 2.素材用途为'小程序二维码' 3.素材未过期");
                }
                if (miniAppId == null || miniAppId.isEmpty()) {
                    log.error("❌ 小程序AppID未配置！请检查application.yml中的wechat.miniapp.appid配置");
                }
                log.warn("⚠️ 仅发送文本消息");
                return true;
            }
            
        } catch (Exception e) {
            log.error("❌ 发送欢迎消息异常", e);
            return false;
        }
    }
    
    /**
     * 发送POST请求
     */
    private JSONObject sendPostRequest(String urlStr, String jsonData) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        
        // 发送请求数据
        OutputStream os = conn.getOutputStream();
        os.write(jsonData.getBytes("UTF-8"));
        os.flush();
        os.close();
        
        // 读取响应
        int responseCode = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                responseCode == 200 ? conn.getInputStream() : conn.getErrorStream(), 
                "UTF-8"
            )
        );
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        log.info("📥 微信API响应: {}", response.toString());
        return JSONObject.parseObject(response.toString());
    }
    
    /**
     * 获取微信access_token
     */
    private String getAccessToken() {
        try {
            String url = String.format(
                "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                appId, secret
            );
            
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder responseStr = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseStr.append(line);
            }
            reader.close();
            JSONObject jsonResponse = JSONObject.parseObject(responseStr.toString());
            
            if (jsonResponse.containsKey("access_token")) {
                return jsonResponse.getString("access_token");
            } else {
                log.error("❌ 获取access_token失败: {}", responseStr);
                return null;
            }
            
        } catch (Exception e) {
            log.error("❌ 获取access_token异常", e);
            return null;
        }
    }
}
