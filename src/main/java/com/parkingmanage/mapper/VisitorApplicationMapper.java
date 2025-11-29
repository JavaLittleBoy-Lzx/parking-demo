package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.VisitorApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 访客申请 Mapper 接口
 * </p>
 *
 * @author System
 * @since 2024-01-15
 */
@Mapper
public interface VisitorApplicationMapper extends BaseMapper<VisitorApplication> {

    /**
     * 根据条件查询访客申请列表
     *
     * @param nickname  访客姓名
     * @param community 小区名称
     * @param applydate 申请日期
     * @return 访客申请列表
     */
    List<VisitorApplication> queryListVisitorApplication(
        @Param("nickname") String nickname,
        @Param("community") String community,
        @Param("applydate") String applydate
    );

    /**
     * 根据手机号查询访客申请
     *
     * @param phone 手机号
     * @return 访客申请信息
     */
    VisitorApplication getByPhone(@Param("phone") String phone);

    /**
     * 根据申请编号查询访客申请
     *
     * @param applicationNo 申请编号
     * @return 访客申请信息
     */
    VisitorApplication getByApplicationNo(@Param("applicationNo") String applicationNo);
} 