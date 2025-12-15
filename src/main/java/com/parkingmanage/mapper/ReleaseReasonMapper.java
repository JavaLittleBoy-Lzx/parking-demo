package com.parkingmanage.mapper;

import com.parkingmanage.entity.ReleaseReason;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 李子雄
 *
 */
public interface ReleaseReasonMapper extends BaseMapper<ReleaseReason> {

    int duplicate(ReleaseReason releaseReason);
    List<ReleaseReason> releaseReasonList();

}
