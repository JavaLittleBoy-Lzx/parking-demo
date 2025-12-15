package com.parkingmanage.controller;


import cn.hutool.core.codec.Base64;
import com.baomidou.mybatisplus.extension.api.R;
import com.parkingmanage.common.HttpClientUtil;
import com.parkingmanage.common.JsonUtils;
import com.parkingmanage.entity.Sys;
import com.parkingmanage.entity.WXSessionModel;
import com.parkingmanage.query.DecrypQuery;
import com.parkingmanage.service.MemberService;
import com.parkingmanage.service.SysService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author MLH
 * @since 2022-07-13
 */
@RestController
@RequestMapping("/sys")
public class SysController {
    @Resource
    private SysService sysService;
    private MemberService memberService;

    @ApiOperation("查询所有")
    @GetMapping("/listSys")
    public List<Sys> findAll() {
        return sysService.list();
    }

    @ApiOperation("查询openid")
    @GetMapping("/getOpenid")
    public R<Map<String, Object>> getOpenid(String code) {
        System.out.println("code  :" + code);
        List<Sys> listSys = findAll();
        String url = "https://api.weixin.qq.com/sns/jscode2session";
        Map<String, String> param = new HashMap<>();
        param.put("appid", listSys.get(0).getAppid());//my
        param.put("secret", listSys.get(0).getAppsecret());//my
        System.out.println(listSys.get(0).getAppid());
        System.out.println(listSys.get(0).getAppsecret());
        param.put("js_code", code);
        param.put("grant_type", "authorization_code");
        String wxResult = HttpClientUtil.doGet(url, param);
        System.out.println("aaaaaaaaaaaaaa");
        System.out.println(wxResult);
        System.out.println("bbbbbbbbbb");
        WXSessionModel model = JsonUtils.jsonToPojo(wxResult, WXSessionModel.class);
        System.out.println(model);
        System.out.println("0000111");
        String openid = model.getOpenid();
        Map<String, Object> wxMap = new HashMap<>();
        wxMap.put("openid", model.getOpenid());
        wxMap.put("session_key", model.getSession_key());
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("data", wxMap);
        return R.ok(dataMap);
    }

    @ApiOperation("解密 phone")
    @GetMapping("/getPhone")
    public R<Map<String, Object>> miniGetPhone(DecrypQuery query) {
        String phone = deciphering(query.getEncryptedData(), query.getIv(), query.getSessionID());
        Map<String, Object> wxMap = new HashMap<>();
        wxMap.put("phone", phone);
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("data", wxMap);
        return R.ok(dataMap);
    }

    public static String deciphering(String encrypdata, String ivdata, String sessionkey) {
        byte[] encrypData = Base64.decode(encrypdata);
        byte[] ivData = Base64.decode(ivdata);
        byte[] sessionKey = Base64.decode(sessionkey);
        String str = "";
        try {
            str = decrypt(sessionKey, ivData, encrypData);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return str;
    }

    public static String decrypt(byte[] key, byte[] iv, byte[] encData) throws Exception {
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        return new String(cipher.doFinal(encData), "UTF-8");
    }
}

