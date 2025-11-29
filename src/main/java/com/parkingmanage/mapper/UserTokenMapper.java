package com.parkingmanage.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户Token Mapper接口
 * 
 * @author parking-system
 * @version 1.0
 */
@Mapper
public interface UserTokenMapper {

    /**
     * 插入token记录
     */
    @Insert("INSERT INTO user_tokens (user_id, token_hash, expires_at, created_at, is_active) " +
            "VALUES (#{user_id}, #{token_hash}, #{expires_at}, #{created_at}, #{is_active})")
    int insert(Map<String, Object> tokenRecord);

    /**
     * 根据token hash查询记录
     */
    @Select("SELECT * FROM user_tokens WHERE token_hash = #{tokenHash} AND is_active = 1")
    Map<String, Object> selectByTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * 使token失效
     */
    @Update("UPDATE user_tokens SET is_active = 0 WHERE token_hash = #{tokenHash}")
    int invalidateToken(@Param("tokenHash") String tokenHash);

    /**
     * 使用户的所有token失效
     */
    @Update("UPDATE user_tokens SET is_active = 0 WHERE user_id = #{userId}")
    int invalidateUserTokens(@Param("userId") Integer userId);

    /**
     * 清理过期的token
     */
    @Update("UPDATE user_tokens SET is_active = 0 WHERE expires_at < #{now}")
    int cleanExpiredTokens(@Param("now") LocalDateTime now);
} 