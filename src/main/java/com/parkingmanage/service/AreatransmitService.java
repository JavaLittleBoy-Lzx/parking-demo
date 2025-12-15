package com.parkingmanage.service;

import com.parkingmanage.vo.AreaSimple;
import com.parkingmanage.entity.Areatransmit;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 <p>
  服务类
 </p>

 @author MLH
 @since 2022-09-08
*/
public interface AreatransmitService extends IService<Areatransmit> {
    void saveAreatransmit(AreaSimple areaSimple);
}
