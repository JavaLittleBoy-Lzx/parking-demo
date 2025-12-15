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
    Patrol getPatrolByPhone(String phone);
    List<Patrol> queryListPatrol(String username, String community);
    int duplicate(Patrol patrol);
    
    /**
     * 查询指定小区所有值班中的巡检员
     * @param community 小区名称
     * @return 值班中的巡检员列表
     */
    List<Patrol> getOnDutyPatrolsByCommunity(String community);
}
