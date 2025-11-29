package com.parkingmanage.service.impl;

import com.parkingmanage.vo.AreaSimple;
import com.parkingmanage.entity.Areatransmit;
import com.parkingmanage.mapper.AreatransmitMapper;
import com.parkingmanage.service.AreatransmitService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author MLH
 * @since 2022-09-08
 */
@Service
public class AreatransmitServiceImpl extends ServiceImpl<AreatransmitMapper, Areatransmit> implements AreatransmitService {
    public  void saveAreatransmit(AreaSimple areaSimple){
        if (areaSimple != null && areaSimple.getOpenid() != null) {
            System.out.println("99999999999999999999999999999999");
            System.out.println(areaSimple.getEnddate());
            baseMapper.saveAreatransmit(areaSimple.getOpenid(),areaSimple.getSourceopenid(),areaSimple.getBegindate(),areaSimple.getEnddate()   );
        }

    }
}
