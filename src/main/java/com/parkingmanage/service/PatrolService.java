package com.parkingmanage.service;

import com.parkingmanage.entity.Butler;
import com.parkingmanage.entity.Ownerinfo;
import com.parkingmanage.entity.Patrol;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 <p>
  服务类
 </p>

 @author MLH
 @since 2023-02-11
*/
public interface PatrolService extends IService<Patrol> {
    Patrol getPatrolByOpenId(String openid);
    List<Patrol> queryListPatrol(String username, String community);
    int duplicate(Patrol patrol);
}
