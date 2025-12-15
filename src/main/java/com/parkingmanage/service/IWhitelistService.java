package com.parkingmanage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.Whitelist;

import java.util.List;

/**
 * <p>
 * 白名单管理 服务类
 * </p>
 *
 * @author MLH
 * @since 2025-10-07
 */
public interface IWhitelistService extends IService<Whitelist> {

    /**
     * 分页查询白名单列表
     * @param page 页码
     * @param size 每页大小
     * @param plateNumber 车牌号（可选）
     * @param ownerName 车主姓名（可选）
     * @param ownerPhone 车主电话（可选）
     * @param parkName 停车场名称（可选）
     * @return 分页结果
     */
    IPage<Whitelist> getWhitelistPage(Integer page, Integer size, String plateNumber, 
                                       String ownerName, String ownerPhone, String parkName);

    /**
     * 创建白名单记录
     * @param whitelist 白名单数据
     * @return 是否成功
     */
    boolean createWhitelist(Whitelist whitelist);

    /**
     * 更新白名单记录
     * @param whitelist 白名单数据
     * @return 是否成功
     */
    boolean updateWhitelist(Whitelist whitelist);

    /**
     * 删除白名单记录
     * @param id 白名单ID
     * @return 是否成功
     */
    boolean deleteWhitelist(Long id);

    /**
     * 批量删除白名单记录
     * @param ids 白名单ID列表
     * @return 是否成功
     */
    boolean batchDeleteWhitelist(List<Long> ids);

    /**
     * 根据车牌号查询白名单记录
     * @param plateNumber 车牌号
     * @param parkName 停车场名称
     * @return 白名单记录
     */
    Whitelist getWhitelistByPlate(String plateNumber, String parkName);

    /**
     * 检查车牌是否在白名单中
     * @param plateNumber 车牌号
     * @param parkName 停车场名称
     * @return 是否在白名单中
     */
    boolean checkWhitelist(String plateNumber, String parkName);
}

