package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.QrCodeUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 二维码使用记录Mapper接口
 */
@Mapper
public interface QrCodeUsageMapper extends BaseMapper<QrCodeUsage> {

    /**
     * 根据二维码ID查询记录
     * @param qrId 二维码ID
     * @return 二维码使用记录
     */
    @Select("SELECT * FROM qr_code_usage WHERE qr_id = #{qrId}")
    QrCodeUsage findByQrId(@Param("qrId") String qrId);

    /**
     * 根据访客openid查询二维码使用记录（最近使用的一条）
     * @param visitorOpenid 访客openid
     * @return 二维码使用记录
     */
    @Select("SELECT * FROM qr_code_usage WHERE visitor_openid = #{visitorOpenid} AND is_used = 1 ORDER BY used_time DESC LIMIT 1")
    QrCodeUsage findByVisitorOpenid(@Param("visitorOpenid") String visitorOpenid);

    /**
     * 标记二维码为已使用
     * @param qrId 二维码ID
     * @param visitorOpenid 访客openid
     * @param visitorPhone 访客手机号
     * @return 更新行数
     */
    @Update("UPDATE qr_code_usage SET is_used = 1, used_time = NOW(), visitor_openid = #{visitorOpenid}, visitor_phone = #{visitorPhone} WHERE qr_id = #{qrId}")
    int markAsUsed(@Param("qrId") String qrId, @Param("visitorOpenid") String visitorOpenid, @Param("visitorPhone") String visitorPhone);

    /**
     * 清理过期的二维码记录
     * @return 清理的记录数
     */
    @Update("UPDATE qr_code_usage SET is_used = 1, remark = '已过期' WHERE expire_time < NOW() AND is_used = 0")
    int cleanExpiredQrCodes();
}
