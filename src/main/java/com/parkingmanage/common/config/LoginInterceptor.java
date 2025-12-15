package com.parkingmanage.common.config;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.parkingmanage.common.exception.CustomException;
import com.parkingmanage.entity.User;
import com.parkingmanage.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @PROJECT_NAME: parkingmanage
 * @PACKAGE_NAME: com.parkingmanage.common.config
 * @NAME: LoginInterceptor
 * @author:yuli
 * @DATE: 2022/1/18 17:56
 * @description: 登录拦截器
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private UserService userService;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)  {
        String token = request.getHeader("token");
        String url = request.getRequestURI();
        if (StrUtil.isBlank(token)) {
            token = request.getParameter("token");
        //  request.getHeader()
        }
        if (StrUtil.isBlank(token)) {
            throw new CustomException("401", "未获取到token, 请重新登录");
        }
        //获取token中的userId
        String userId;

        User user;
        try {
            userId = JWT.decode(token).getAudience().get(0);
            user = userService.getById(Integer.valueOf(userId));
        } catch (Exception e) {
            throw new CustomException("401", "获取token失败，请重新登录");
        }
        // 根据获取的userId在数据库中进行查询
        if (user == null) {
            throw new CustomException("404","用户不存在，请重新登录！");
        }
        try {
            //用户密码加签验证
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(user.getPassword())).build();
            jwtVerifier.verify(token);
        } catch (Exception e) {
            throw new CustomException("401", "验证token失败，请重新登录");
        }
        return true;
    }
}