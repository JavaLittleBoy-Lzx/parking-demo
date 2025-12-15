package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.Whitelist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 白名单管理 Mapper 接口
 * </p>
 *
 * @author MLH
 * @since 2025-10-07
 */
@Mapper
public interface WhitelistMapper extends BaseMapper<Whitelist> {

    /**
     * 检查车牌是否在白名单中
     * @param plateNumber 车牌号
     * @param parkName 停车场名称
     * @return 白名单记录数量
     */
    int checkWhitelist(@Param("plateNumber") String plateNumber, @Param("parkName") String parkName);
}

