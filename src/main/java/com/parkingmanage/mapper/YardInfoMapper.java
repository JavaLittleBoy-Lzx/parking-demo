package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parkingmanage.entity.YardInfo;
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
public interface YardInfoMapper extends BaseMapper<YardInfo> {

    int duplicate(YardInfo yardInfo);

    List<YardInfo> queryYardInfo(QueryWrapper<YardInfo> wrapper);

    List<YardInfo> yardCodeList();

    List<YardInfo> yardNameList();

    //TODO 根据车场编号查询车场通道
//    List<YardInfo> entrancePassageList(String yardCode, String yardName);

    List<YardInfo> expYardNameList();

    List<String> yardCode(String yardName);

    YardInfo yardByName(String parkName);

    String selectParkCode(String parkName);

    List<String>  selectByParkCode(String parkCode);

//    List<YardInfo> getChannelInfoList(String yardCode);
}
