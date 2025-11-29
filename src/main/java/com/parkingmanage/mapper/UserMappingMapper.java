package com.parkingmanage.mapper;

import com.parkingmanage.entity.UserMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

/**
 * <p>
 * 用户映射表 Mapper 接口
 * </p>
 *
 * @author system
 * @since 2025-01-28
 */
@Mapper
public interface UserMappingMapper {

    /**
     * 根据昵称统计记录数
     */
    @Select("SELECT COUNT(*) FROM user_mapping WHERE nickname = #{nickname}")
    int countByNickname(@Param("nickname") String nickname);

    /**
     * 根据昵称查询用户映射记录
     */
    @Select("SELECT * FROM user_mapping WHERE nickname = #{nickname}")
    List<UserMapping> findByNickname(@Param("nickname") String nickname);

    /**
     * 根据OpenID查询用户映射记录
     */
    @Select("SELECT * FROM user_mapping WHERE openid = #{openid}")
    UserMapping findByOpenid(@Param("openid") String openid);

    /**
     * 根据手机号查询用户映射记录
     */
    @Select("SELECT * FROM user_mapping WHERE phone = #{phone}")
    List<UserMapping> findByPhone(@Param("phone") String phone);

    /**
     * 插入用户映射记录
     */
    @Insert("INSERT INTO user_mapping (nickname, openid, phone, avatar_url, gender, " +
            "is_followed, follow_time, created_at, updated_at, deleted) " +
            "VALUES (#{nickname}, #{openid}, #{phone}, #{avatarUrl}, #{gender}, " +
            "#{isFollowed}, #{followTime}, #{createTime}, #{updateTime}, 0)")
    int insertUserMapping(UserMapping userMapping);

    /**
     * 更新用户映射记录
     */
    @Update("UPDATE user_mapping SET nickname = #{nickname}, phone = #{phone}, " +
            "avatar_url = #{avatarUrl}, gender = #{gender}, " +
            "is_followed = #{isFollowed}, follow_time = #{followTime}, " +
            "unfollow_time = #{unfollowTime}, updated_at = #{updateTime} WHERE openid = #{openid}")
    int updateUserMapping(UserMapping userMapping);

    /**
     * 根据OpenID删除用户映射记录（逻辑删除）
     */
    @Update("UPDATE user_mapping SET deleted = 1, updated_at = NOW() WHERE openid = #{openid}")
    int deleteByOpenid(@Param("openid") String openid);

    /**
     * 根据昵称删除用户映射记录（逻辑删除）
     */
    @Update("UPDATE user_mapping SET deleted = 1, updated_at = NOW() WHERE nickname = #{nickname}")
    int deleteByNickname(@Param("nickname") String nickname);

    /**
     * 根据多个条件查询用户关注状态
     * 优先级：openid > phone > nickname
     */
    @Select("SELECT * FROM user_mapping WHERE " +
            "(#{openid} IS NOT NULL AND openid = #{openid}) OR " +
            "(#{openid} IS NULL AND #{phone} IS NOT NULL AND phone = #{phone}) OR " +
            "(#{openid} IS NULL AND #{phone} IS NULL AND #{nickname} IS NOT NULL AND nickname = #{nickname}) " +
            "ORDER BY CASE " +
            "WHEN openid = #{openid} THEN 1 " +
            "WHEN phone = #{phone} THEN 2 " +
            "WHEN nickname = #{nickname} THEN 3 " +
            "END LIMIT 1")
    UserMapping findByMultipleConditions(@Param("openid") String openid, 
                                        @Param("phone") String phone, 
                                        @Param("nickname") String nickname);

} 