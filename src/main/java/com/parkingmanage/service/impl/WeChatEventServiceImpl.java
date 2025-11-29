package com.parkingmanage.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.entity.UserMapping;
import com.parkingmanage.mapper.UserMappingMapper;
import com.parkingmanage.service.WeChatEventService;
import com.parkingmanage.service.UserMappingService;
import com.parkingmanage.service.WeChatCustomMessageService;
import com.parkingmanage.common.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信事件处理服务实现
 * 实现微信公众号事件的接收和处理，包括用户关注事件的实时处理
 * 
 * @author MLH
 * @since 2024-01-01
 */
@Slf4j
@Service
public class WeChatEventServiceImpl implements WeChatEventService {
    
    // 消息排重缓存，根据官方文档推荐使用FromUserName + CreateTime排重
    private static final ConcurrentHashMap<String, Long> messageCache = new ConcurrentHashMap<>();
    
    @Value("${wechat.public.appid}")
    private String appId;
    
    @Value("${wechat.public.secret}")
    private String secret;
    
    @Resource
    private UserMappingMapper userMappingMapper;
    
    @Resource
    private UserMappingService userMappingService;
    
    @Resource
    private WeChatCustomMessageService weChatCustomMessageService;
    
    @Override
    public String handleWeChatEvent(String xmlData) {
        try {
            // 解析XML数据
            Map<String, String> eventData = parseXmlData(xmlData);
            
            String msgType = eventData.get("MsgType");
            String event = eventData.get("Event");
            String openId = eventData.get("FromUserName");
            String eventTimeStr = eventData.get("CreateTime");
            
            log.info("🎯 处理微信事件 - 类型: {}, 事件: {}, openId: {}, 时间: {}", 
                msgType, event, openId, eventTimeStr);
            
            // 处理事件类型
            if ("event".equals(msgType)) {
                Long eventTime = Long.parseLong(eventTimeStr);
                
                switch (event) {
                    case "subscribe":
                        // 用户关注事件（根据官方文档）
                        String eventKey = eventData.get("EventKey");
                        if (StringUtils.hasText(eventKey) && eventKey.startsWith("qrscene_")) {
                            log.info("📱 用户通过扫描二维码关注 - openId: {}, eventKey: {}", openId, eventKey);
                        }
                        handleSubscribeEvent(openId, eventTime);
                        break;
                        
                    case "unsubscribe":
                        // 用户取消关注事件（根据官方文档要求删除用户信息保护隐私）
                        handleUnsubscribeEvent(openId, eventTime);
                        break;
                        
                    case "SCAN":
                        // 用户已关注时扫描带参数二维码事件（根据官方文档）
                        String scanEventKey = eventData.get("EventKey");
                        log.info("📱 已关注用户扫描二维码 - openId: {}, eventKey: {}", openId, scanEventKey);
                        // 这里可以根据业务需求处理扫码事件
                        break;
                        
                    case "LOCATION":
                        // 上报地理位置事件（根据官方文档）
                        String latitude = eventData.get("Latitude");
                        String longitude = eventData.get("Longitude");
                        String precision = eventData.get("Precision");
                        log.info("📍 用户上报地理位置 - openId: {}, 纬度: {}, 经度: {}, 精度: {}", 
                            openId, latitude, longitude, precision);
                        break;
                        
                    case "CLICK":
                        // 自定义菜单点击事件（根据官方文档）
                        String clickEventKey = eventData.get("EventKey");
                        log.info("🖱️ 用户点击自定义菜单 - openId: {}, eventKey: {}", openId, clickEventKey);
                        break;
                        
                    case "VIEW":
                        // 自定义菜单跳转链接事件（根据官方文档）
                        String viewEventKey = eventData.get("EventKey");
                        log.info("🔗 用户点击菜单链接 - openId: {}, url: {}", openId, viewEventKey);
                        break;
                        
                    default:
                        log.info("ℹ️ 未处理的事件类型: {}", event);
                        break;
                }
            }
            
            return "success";
            
        } catch (Exception e) {
            log.error("❌ 处理微信事件异常", e);
            return "error";
        }
    }
    
    @Override
    public void handleSubscribeEvent(String openId, Long eventTime) {
        log.info("👋 处理用户关注事件 - openId: {}, 时间: {}", openId, eventTime);
        
        try {
            // 1. 检查用户是否已存在
            UserMapping existingUser = userMappingService.getByOpenid(openId);
            
            if (existingUser != null) {
                // 用户已存在，更新关注状态
                UserMapping updatedUser = userMappingService.updateFollowStatus(openId, 1);
                log.info("✅ 更新已存在用户关注状态 - openId: {}, 关注时间: {}", 
                    openId, updatedUser != null ? updatedUser.getFollowTime() : null);
            } else {
                // 2. 获取用户详细信息
                Map<String, Object> userInfo = getUserInfoFromWeChat(openId);
                
                if (userInfo != null) {
                    // 3. 创建新的用户映射记录
                    UserMapping newUser = new UserMapping();
                    newUser.setOpenid(openId);
                    newUser.setNickname((String) userInfo.get("nickname"));
                    newUser.setAvatarUrl((String) userInfo.get("headimgurl"));
                    newUser.setGender((Integer) userInfo.get("sex"));
                    newUser.setIsFollowed(1);
                    newUser.setFollowTime(new Date());
                    newUser.setCreateTime(new Date());
                    newUser.setUpdateTime(new Date());
                    
                    UserMapping insertedUser = userMappingService.insertUserMapping(newUser);
                    
                    log.info("✅ 创建新用户记录 - openId: {}, 昵称: {}, ID: {}", 
                        openId, userInfo.get("nickname"), insertedUser.getId());
                } else {
                    log.error("❌ 获取用户信息失败 - openId: {}", openId);
                }
            }
            
            // 4. 发送欢迎消息（包含引导语和小程序二维码）
            try {
                log.info("👋 准备发送欢迎消息 - openId: {}", openId);
                boolean welcomeSent = weChatCustomMessageService.sendWelcomeMessage(openId);
                if (welcomeSent) {
                    log.info("✅ 欢迎消息发送成功 - openId: {}", openId);
                } else {
                    log.warn("⚠️ 欢迎消息发送失败 - openId: {}", openId);
                }
            } catch (Exception e) {
                log.error("❌ 发送欢迎消息异常 - openId: {}", openId, e);
                // 不抛出异常，不影响用户关注流程
            }
            
        } catch (Exception e) {
            log.error("❌ 处理用户关注事件异常 - openId: {}", openId, e);
        }
    }
    
    @Override
    public void handleUnsubscribeEvent(String openId, Long eventTime) {
        log.info("👋 处理用户取消关注事件 - openId: {}, 时间: {}", openId, eventTime);
        
        try {
            // 根据官方文档：为保护用户数据隐私，开发者收到用户取消关注事件时需要删除该用户的所有信息
            UserMapping existingUser = userMappingService.getByOpenid(openId);
            
            if (existingUser != null) {
                // 选择1：完全删除用户记录（符合官方要求）
                // userMappingMapper.deleteByOpenid(openId);
                // log.info("✅ 已删除用户所有信息 - openId: {}", openId);
                
                // 选择2：保留基本记录但清空敏感信息（推荐方案，便于数据统计）
                existingUser.setIsFollowed(0);
                existingUser.setUnfollowTime(new Date());
                existingUser.setUpdateTime(new Date());
                // 清空用户敏感信息以保护隐私
                existingUser.setNickname("已取消关注用户");
                existingUser.setAvatarUrl(null);
                UserMapping updatedUser = userMappingService.updateUserMapping(existingUser);
                
                log.info("✅ 已清空用户敏感信息并更新取消关注状态 - openId: {}, 取消关注时间: {}", 
                    openId, updatedUser != null ? updatedUser.getUnfollowTime() : null);
            } else {
                log.warn("⚠️ 未找到要取消关注的用户记录 - openId: {}", openId);
            }
            
        } catch (Exception e) {
            log.error("❌ 处理用户取消关注事件异常 - openId: {}", openId, e);
        }
    }
    
    /**
     * 解析微信推送的XML数据
     */
    private Map<String, String> parseXmlData(String xmlData) throws Exception {
        Map<String, String> result = new HashMap<>();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlData.getBytes("UTF-8")));
        
        Element root = document.getDocumentElement();
        
        // 提取常用字段
        result.put("ToUserName", getElementText(root, "ToUserName"));
        result.put("FromUserName", getElementText(root, "FromUserName"));
        result.put("CreateTime", getElementText(root, "CreateTime"));
        result.put("MsgType", getElementText(root, "MsgType"));
        result.put("Event", getElementText(root, "Event"));
        result.put("EventKey", getElementText(root, "EventKey"));
        
        return result;
    }
    
    /**
     * 获取XML元素的文本内容
     */
    private String getElementText(Element parent, String tagName) {
        try {
            return parent.getElementsByTagName(tagName).item(0).getTextContent();
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 从微信API获取用户详细信息
     */
    private Map<String, Object> getUserInfoFromWeChat(String openId) {
        try {
            // 1. 获取access_token
            String accessToken = getAccessToken();
            if (StringUtils.isEmpty(accessToken)) {
                log.error("❌ 获取access_token失败");
                return null;
            }
            
            // 2. 调用用户信息接口
            String url = "https://api.weixin.qq.com/cgi-bin/user/info";
            Map<String, String> params = new HashMap<>();
            params.put("access_token", accessToken);
            params.put("openid", openId);
            params.put("lang", "zh_CN");
            
            String response = HttpClientUtil.doGet(url, params);
            log.info("📥 获取用户信息响应: {}", response);
            
            // 3. 解析响应
            JSONObject jsonResponse = JSONObject.parseObject(response);
            
            if (jsonResponse.containsKey("errcode")) {
                Integer errcode = jsonResponse.getInteger("errcode");
                String errmsg = jsonResponse.getString("errmsg");
                log.error("❌ 获取用户信息失败 - 错误码: {}, 错误信息: {}", errcode, errmsg);
                return null;
            }
            
            // 4. 返回用户信息
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("openid", jsonResponse.getString("openid"));
            userInfo.put("unionid", jsonResponse.getString("unionid"));
            userInfo.put("nickname", jsonResponse.getString("nickname"));
            userInfo.put("headimgurl", jsonResponse.getString("headimgurl"));
            userInfo.put("sex", jsonResponse.getInteger("sex"));
            userInfo.put("city", jsonResponse.getString("city"));
            userInfo.put("province", jsonResponse.getString("province"));
            userInfo.put("country", jsonResponse.getString("country"));
            userInfo.put("subscribe_time", jsonResponse.getLong("subscribe_time"));
            
            return userInfo;
            
        } catch (Exception e) {
            log.error("❌ 获取用户信息异常 - openId: {}", openId, e);
            return null;
        }
    }
    
    /**
     * 获取微信access_token
     */
    private String getAccessToken() {
        try {
            if (StringUtils.isEmpty(appId) || StringUtils.isEmpty(secret)) {
                log.error("❌ 微信公众号配置不完整");
                return null;
            }
            
            String url = "https://api.weixin.qq.com/cgi-bin/token";
            Map<String, String> params = new HashMap<>();
            params.put("grant_type", "client_credential");
            params.put("appid", appId);
            params.put("secret", secret);
            
            String response = HttpClientUtil.doGet(url, params);
            JSONObject jsonResponse = JSONObject.parseObject(response);
            
            if (jsonResponse.containsKey("access_token")) {
                return jsonResponse.getString("access_token");
            } else {
                log.error("❌ 获取access_token失败: {}", response);
                return null;
            }
            
        } catch (Exception e) {
            log.error("❌ 获取access_token异常", e);
            return null;
        }
    }
    
    /**
     * 异步处理微信事件推送
     * 根据官方文档，为避免5秒超时，使用异步方式处理
     * 推荐使用FromUserName + CreateTime排重
     */
    @Override
    @Async("wechatEventExecutor")
    public void processEventAsync(String xmlData) {
        try {
            log.info("🔄 开始异步处理微信事件");
            
            // 解析XML数据
            Map<String, String> eventData = parseXmlData(xmlData);
            String openId = eventData.get("FromUserName");
            String createTime = eventData.get("CreateTime");
            
            // 消息排重，根据官方文档推荐使用FromUserName + CreateTime
            String messageKey = openId + "_" + createTime;
            if (messageCache.containsKey(messageKey)) {
                log.info("🔄 消息已处理，跳过重复处理 - messageKey: {}", messageKey);
                return;
            }
            
            // 记录消息已处理（缓存1小时）
            messageCache.put(messageKey, System.currentTimeMillis());
            
            // 清理过期缓存（1小时前的消息）
            long expireTime = System.currentTimeMillis() - 3600000; // 1小时
            messageCache.entrySet().removeIf(entry -> entry.getValue() < expireTime);
            
            // 处理事件
            handleWeChatEvent(xmlData);
            
            log.info("✅ 异步处理微信事件完成 - messageKey: {}", messageKey);
            
        } catch (Exception e) {
            log.error("❌ 异步处理微信事件异常", e);
        }
    }
} 