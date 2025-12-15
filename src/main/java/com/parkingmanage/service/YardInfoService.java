package com.parkingmanage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parkingmanage.entity.Community;
import com.parkingmanage.entity.YardInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 李子雄
 *
 */
public interface YardInfoService extends IService<YardInfo> {
    int duplicate(YardInfo yardInfo);
    List<YardInfo> queryYardInfo(QueryWrapper<YardInfo> wrapper);
    List<String> yardCode(String yardName);
    List<YardInfo>  yardNameList();

    List<YardInfo> queryListYardInfo(String yardName);

    List<YardInfo> expYardNameList();

    YardInfo yardByName(String parkName);

    String selectParkCode(String parkName);

    List<String>  selectByParkCode(String parkCode);
}
