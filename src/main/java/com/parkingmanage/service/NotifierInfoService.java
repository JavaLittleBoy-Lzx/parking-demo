package com.parkingmanage.service;

import com.parkingmanage.entity.NotifierInfo;
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
public interface NotifierInfoService extends IService<NotifierInfo> {

    List<NotifierInfo> notifierNameList(String merchantName);

    List<NotifierInfo> merchantNameList();

    int duplicate(NotifierInfo notifierInfo);

    List<NotifierInfo> queryListNotifierInfo(String merchantName);
}
