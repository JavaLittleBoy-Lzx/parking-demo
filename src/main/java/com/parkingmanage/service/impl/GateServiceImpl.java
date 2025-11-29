package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkingmanage.entity.Gate;
import com.parkingmanage.mapper.GateMapper;
import com.parkingmanage.service.GateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 <p>
  服务实现类
 </p>

 @author MLH
 @since 2023-02-18
*/
@Service
public class GateServiceImpl extends ServiceImpl<GateMapper, Gate> implements GateService {
    @Resource
    private GateService gateService;
    @Override
    public int duplicate(Gate gate) {
        return baseMapper.duplicate(gate);
    }
    @Override
    public List<Gate> queryGate(Wrapper<Gate> wrapper) {
        return baseMapper.queryGate(wrapper);
    }
    @Override
    public List<Gate> queryListGate(String gatename, String community){
        LambdaQueryWrapper<Gate> queryWrapper = new LambdaQueryWrapper();
        if (StringUtils.hasLength(gatename)) {
            queryWrapper.like(Gate::getGatename, gatename);
        }
        if (StringUtils.hasLength(community)) {
            queryWrapper.like(Gate::getCommunity, community);
        }
        List<Gate> gate = gateService.list(queryWrapper);
        return gate;
    }
}
