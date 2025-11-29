package com.parkingmanage.service.impl;

import com.parkingmanage.entity.Illegalregiste;
import com.parkingmanage.mapper.IllegalregisteMapper;
import com.parkingmanage.service.IllegalregisteService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 <p>
  服务实现类
 </p>

 @author MLH
 @since 2022-09-18
*/
@Service
public class IllegalregisteServiceImpl extends ServiceImpl<IllegalregisteMapper, Illegalregiste> implements IllegalregisteService {
    @Override
    public List<Illegalregiste> allManage(String community, String plateNumber, String operatordate){
        return baseMapper.allManage(community,plateNumber,operatordate);
    }
}
