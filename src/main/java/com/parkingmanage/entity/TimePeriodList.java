package com.parkingmanage.entity;

import lombok.Data;

/**
 * @program: ParkManage
 * @description: 请求艾科接口查询月票截止日期
 * @author: lzx
 * @create: 2024-06-01 14:35
 **/
@Data
public class TimePeriodList {
    private String endTime;
    private String startTime;
    @Override
    public String toString() {
        return startTime + "至" + endTime;
    }
}

