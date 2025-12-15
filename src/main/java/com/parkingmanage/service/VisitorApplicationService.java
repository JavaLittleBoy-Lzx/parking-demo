package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.VisitorApplication;

import java.util.List;

/**
 * <p>
 * 访客申请 服务类
 * </p>
 *
 * @author System
 * @since 2024-01-15
 */
public interface VisitorApplicationService extends IService<VisitorApplication> {

    /**
     * 根据条件查询访客申请列表
     *
     * @param nickname  访客姓名
     * @param community 小区名称
     * @param applydate 申请日期
     * @return 访客申请列表
     */
    List<VisitorApplication> queryListVisitorApplication(String nickname, String community, String applydate);

    /**
     * 根据手机号查询访客申请
     *
     * @param phone 手机号
     * @return 访客申请信息
     */
    VisitorApplication getByPhone(String phone);

    /**
     * 根据手机号查询访客的所有申请记录
     *
     * @param phone 手机号
     * @return 访客申请记录列表
     */
    List<VisitorApplication> getRecordsByPhone(String phone);

    /**
     * 根据手机号和审核状态查询访客申请记录（用于获取地址信息）
     *
     * @param phone       手机号
     * @param auditstatus 审核状态
     * @return 访客申请记录列表
     */
    List<VisitorApplication> getApprovedApplicationsByPhone(String phone, String auditstatus);

    /**
     * 根据申请编号查询访客申请
     *
     * @param applicationNo 申请编号
     * @return 访客申请信息
     */
    VisitorApplication getByApplicationNo(String applicationNo);

    /**
     * 更新访客申请
     *
     * @param visitorApplication 访客申请信息
     * @return 更新结果
     */
    boolean updateVisitorApplication(VisitorApplication visitorApplication);

    /**
     * 生成申请编号
     *
     * @return 申请编号
     */
    String generateApplicationNo();
} 