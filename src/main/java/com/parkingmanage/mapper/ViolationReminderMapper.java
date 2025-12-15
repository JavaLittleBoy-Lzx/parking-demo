package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.ViolationReminder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 违规提醒记录表 Mapper 接口
 * </p>
 *
 * @author parking-system
 * @since 2024-01-XX
 */
@Mapper
public interface ViolationReminderMapper extends BaseMapper<ViolationReminder> {

    /**
     * 根据车牌号查询未处理的违规提醒
     */
    @Select("SELECT * FROM violation_reminders WHERE plate_number = #{plateNumber} AND is_processed = 0 ORDER BY create_time DESC")
    List<ViolationReminder> selectUnprocessedByPlateNumber(@Param("plateNumber") String plateNumber);

    /**
     * 根据车牌号标记所有未处理的提醒为已处理
     */
    @Update("UPDATE violation_reminders SET is_processed = 1, processed_time = NOW(), processed_by = #{processedBy} WHERE plate_number = #{plateNumber} AND is_processed = 0")
    int markAllAsProcessedByPlateNumber(@Param("plateNumber") String plateNumber, @Param("processedBy") String processedBy);

    /**
     * 根据车牌号查询所有违规提醒记录
     */
    @Select("SELECT * FROM violation_reminders WHERE plate_number = #{plateNumber} ORDER BY create_time DESC")
    List<ViolationReminder> selectAllByPlateNumber(@Param("plateNumber") String plateNumber);

    /**
     * 查询指定时间范围内的违规提醒记录
     */
    @Select("SELECT * FROM violation_reminders WHERE create_time BETWEEN #{startTime} AND #{endTime} ORDER BY create_time DESC")
    List<ViolationReminder> selectByTimeRange(@Param("startTime") String startTime, @Param("endTime") String endTime);

    /**
     * 统计未处理的违规提醒数量
     */
    @Select("SELECT COUNT(*) FROM violation_reminders WHERE is_processed = 0")
    int countUnprocessedReminders();

    /**
     * 统计指定车牌的违规提醒次数
     */
    @Select("SELECT COUNT(*) FROM violation_reminders WHERE plate_number = #{plateNumber}")
    int countByPlateNumber(@Param("plateNumber") String plateNumber);

    /**
     * 违规记录与提醒发送关联分析
     */
    List<Map<String, Object>> selectCorrelationAnalysis(@Param("days") Integer days);
}
