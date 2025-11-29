package com.parkingmanage.service;

import com.parkingmanage.entity.ReleaseReason;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 李子雄
 *
 */
public interface ReleaseReasonService extends IService<ReleaseReason> {

    int duplicate(ReleaseReason releaseReason);

    List<ReleaseReason> releaseReasonList();

    List<ReleaseReason> queryListReleaseReason(String releaseReason);
}
