package com.parkingmanage.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.parkingmanage.entity.Gate;
import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.Ownerinfo;

import java.util.List;

/**
 <p>
  服务类
 </p>

 @author Lzx
 @since 2023-02-18
*/
public interface GateService extends IService<Gate> {
    List<Gate> queryGate(Wrapper<Gate> wrapper);
    List<Gate> queryListGate(String gatename, String community);
    int duplicate(Gate gate);
}
