package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkingmanage.entity.ReleaseReason;
import com.parkingmanage.mapper.ReleaseReasonMapper;
import com.parkingmanage.service.ReleaseReasonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 李子雄
 *
 */
@Service
public class ReleaseReasonServiceImpl extends ServiceImpl<ReleaseReasonMapper, ReleaseReason> implements ReleaseReasonService {
    @Resource
    private ReleaseReasonService releaseReasonService;
    @Override
    public int duplicate(ReleaseReason releaseReason) {
        return baseMapper.duplicate(releaseReason);
    }

    @Override
    public List<ReleaseReason> releaseReasonList() {
        return baseMapper.releaseReasonList();
    }

    @Override
    public List<ReleaseReason> queryListReleaseReason(String releaseReason) {
        LambdaQueryWrapper<ReleaseReason> queryWrapper = new LambdaQueryWrapper();

        if (StringUtils.hasLength(releaseReason)) {
            queryWrapper.like(ReleaseReason::getReleaseReason, releaseReason);
        }
        List<ReleaseReason> releaseReasonList = releaseReasonService.list(queryWrapper);
        return releaseReasonList;
    }
}
