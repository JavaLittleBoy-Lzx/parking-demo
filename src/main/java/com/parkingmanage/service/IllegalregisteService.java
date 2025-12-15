package com.parkingmanage.service;

import com.parkingmanage.entity.Community;
import com.parkingmanage.entity.Illegalregiste;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 <p>
  服务类
 </p>

 @author MLH
 @since 2022-09-18
*/
public interface IllegalregisteService extends IService<Illegalregiste> {
    List<Illegalregiste> allManage(String community,String plateNumber,String operatordate);
}
