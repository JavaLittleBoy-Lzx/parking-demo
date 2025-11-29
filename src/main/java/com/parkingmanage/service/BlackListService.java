package com.parkingmanage.service;

import com.parkingmanage.entity.BlackList;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lzx
 * @since 2023-12-21
 */
public interface BlackListService extends IService<BlackList> {

    List<BlackList> queryInfoOnly(String parkName, String carCode, String specialCarTypeConfigName, String userName, String blackReason, String remark1, String remark2);

    List<BlackList> findOne(BlackList blackList);

    /**
     * 逻辑删除黑名单记录
     * @param ids 要删除的ID列表
     * @param deleteBy 删除人
     * @return 删除是否成功
     */
    boolean logicDeleteByIds(Collection<Integer> ids, String deleteBy);

    /**
     * 逻辑删除单个黑名单记录
     * @param id 要删除的ID
     * @param deleteBy 删除人
     * @return 删除是否成功
     */
    boolean logicDeleteById(Integer id, String deleteBy);

    /**
     * 查询已删除的黑名单记录
     * @param parkName 车场名称
     * @param carCode 车牌号码
     * @param specialCarTypeConfigName 黑名单类型
     * @param userName 车主姓名
     * @param blackReason 黑名单原因
     * @param remark1 备注1
     * @param remark2 备注2
     * @return 已删除的记录列表
     */
    List<BlackList> queryDeletedInfoOnly(String parkName, String carCode, String specialCarTypeConfigName, String userName, String blackReason, String remark1, String remark2);

    /**
     * 恢复已删除的黑名单记录
     * @param id 要恢复的记录ID
     * @return 恢复是否成功
     */
    boolean restoreById(Integer id);

    /**
     * 判断指定车场是否已存在该车牌的黑名单记录（未删除）
     * @param parkName 车场名称
     * @param carCode 车牌号码
     * @return 是否存在
     */
    boolean existsByParkAndCar(String parkName, String carCode);

}
