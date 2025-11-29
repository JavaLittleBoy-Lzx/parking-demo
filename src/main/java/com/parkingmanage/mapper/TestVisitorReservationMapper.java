package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.TestVisitorReservation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 测试用访客预约记录Mapper接口
 * 
 * @author System
 */
public interface TestVisitorReservationMapper extends BaseMapper<TestVisitorReservation> {
    
    /**
     * 根据创建时间范围分页查询预约记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 预约记录列表
     */
    List<TestVisitorReservation> selectByCreateTimeRange(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("pageNum") Integer pageNum,
            @Param("pageSize") Integer pageSize
    );
    
    /**
     * 统计指定时间范围内的预约记录数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 记录数量
     */
    int countByCreateTimeRange(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime
    );
}

