package com.parkingmanage.mapper;

import com.parkingmanage.entity.NotifierInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 李子雄
 *
 */
public interface NotifierInfoMapper extends BaseMapper<NotifierInfo> {

    List<NotifierInfo> merchantNameList();

    List<NotifierInfo> notifierNameList(String merchantName);

    int duplicate(NotifierInfo notifierInfo);

}
