package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.Patrol;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 巡逻员表 Mapper 接口
 * </p>
 *
 * @author MLH
 * @since 2025-01-31
 */
@Mapper
public interface PatrolMapper extends BaseMapper<Patrol> {

    /**
     * 检查巡逻员是否重复
     */
    int duplicate(Patrol patrol);

    /**
     * 根据OpenId获取巡逻员信息
     */
    Patrol getPatrolByOpenId(String openid);

}
