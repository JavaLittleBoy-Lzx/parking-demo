package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.UserWechatInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户微信信息Mapper接口
 */
@Mapper
public interface UserWechatInfoMapper extends BaseMapper<UserWechatInfo> {
    
    /**
     * 通过UnionID查询用户信息
     */
    @Select("SELECT * FROM user_wechat_info WHERE unionid = #{unionid} LIMIT 1")
    UserWechatInfo selectByUnionid(@Param("unionid") String unionid);
    
    /**
     * 通过小程序OpenID查询用户信息
     */
    @Select("SELECT * FROM user_wechat_info WHERE miniapp_openid = #{miniappOpenid} LIMIT 1")
    UserWechatInfo selectByMiniappOpenid(@Param("miniappOpenid") String miniappOpenid);
    
    /**
     * 通过公众号OpenID查询用户信息
     */
    @Select("SELECT * FROM user_wechat_info WHERE public_openid = #{publicOpenid} LIMIT 1")
    UserWechatInfo selectByPublicOpenid(@Param("publicOpenid") String publicOpenid);
    
    /**
     * 通过手机号查询用户信息
     */
    @Select("SELECT * FROM user_wechat_info WHERE phone = #{phone} LIMIT 1")
    UserWechatInfo selectByPhone(@Param("phone") String phone);
} 