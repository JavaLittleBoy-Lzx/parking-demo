package com.parkingmanage.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.common.HttpClientUtil;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.SubscribeMessage;
import com.parkingmanage.entity.SubscribeMessageData;
import com.parkingmanage.entity.SubscribeMessageSon;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

/**
 * 小程序推送服务器地址
 */
@RestController
@RequestMapping(value = "/parking", method = {RequestMethod.GET, RequestMethod.POST})
public class SubscribeMessageController {

    private Logger logger = LoggerFactory.getLogger(Controller.class);


    @RequestMapping(value = "/getMessage", method = RequestMethod.GET)
    public void eventCallAuth(HttpServletRequest req, HttpServletResponse resp) {
        logger.info("进入验证微信公众号绑定");
        try {
            String signature = req.getParameter("signature");//签名
            String timestamp = req.getParameter("timestamp");//时间戳
            String nonce = req.getParameter("nonce");//随机数
            String echostr = req.getParameter("echostr");//随机字符串用于初次绑定服务器原样返回
            resp.getWriter().write(echostr);//用于直接返回
            logger.info("接口配置信息验证请求签名signature={}，时间戳timestamp={}，随机数nonce={}，随机字符串echostr={}", signature, timestamp, nonce, echostr);
        } catch (Exception e) {
            logger.error("验证微信公众号绑定失败，", e);
        }
    }

    @ApiOperation("调用微信接口查询Token")
    @RequestMapping(value = "/getToken", method = RequestMethod.GET)
    public ResponseEntity<Result> getToken(@RequestParam(required = false) String appid,
                                           @RequestParam(required = false) String secret) {
        HashMap<String, String> hashEmptyMap = new HashMap<>();
        hashEmptyMap.put("grant_type","client_credential");
        hashEmptyMap.put("appid", appid);
        // 此处的getEnterChannelCustomCode为通道自定义编码，且接口的value也必须为通道自定义编码
        hashEmptyMap.put("secret",secret);
        String getGet = HttpClientUtil.doPost("https://api.weixin.qq.com/cgi-bin/token", hashEmptyMap);
        JSONObject jsonObject = JSONObject.parseObject(getGet);
        String accessToken = (String) jsonObject.get("access_token");
        logger.info(getGet);
        logger.info(accessToken);
        Result result = new Result();
        result.setData(accessToken);
        result.setMsg("调用成功");
        result.setCode("0");
        return ResponseEntity.ok(result);
    }

    @ApiOperation("调用微信接口发送message")
    @RequestMapping(value = "/sendMessage/login", method = RequestMethod.GET)
    public ResponseEntity<Result> sendMessage(@RequestBody SubscribeMessage subscribeMessage) {
        //封装一个data的对象
        SubscribeMessageData subscribeMessageData = JSON.parseObject(subscribeMessage.getData(), SubscribeMessageData.class);
        HashMap<String, String> hashEmptyMap = new HashMap<>();
        SubscribeMessageSon subscribeMessageSon = new SubscribeMessageSon();
        subscribeMessageSon.setTemplate_id(subscribeMessage.getTemplate_id());
        subscribeMessageSon.setTouser(subscribeMessage.getTouser());
        subscribeMessageSon.setLang("zh_CN");
        subscribeMessageSon.setMiniprogram_state("trial");
        subscribeMessageSon.setPage("pages/index/index");
        subscribeMessageSon.setData(subscribeMessageData);
        String jsonBody = JSON.toJSONString(subscribeMessageSon);
        logger.info(jsonBody);
        String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + subscribeMessage.getAccess_token();
        String getGet = HttpClientUtil.doPostJson(url,jsonBody);
        logger.info(url);
        logger.info(getGet);
        Result result = new Result();
        logger.info(subscribeMessage.getData().toString());
        return ResponseEntity.ok(result);
    }

    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void checkAndSendNotifications() {
        // 每隔15分钟发送一次
    }

    /**
     * 停车超时提醒定时任务
     * 每5分钟执行一次
     */
//    @Scheduled(fixedRate = 300000) // 5分钟
//    @ApiOperation("停车超时提醒定时任务")
//    public void checkParkingTimeoutNotifications() {
////        logger.info("执行停车超时提醒定时任务");
//    }
}