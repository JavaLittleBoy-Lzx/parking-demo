package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.BlackList;
import com.parkingmanage.entity.Butler;
import com.parkingmanage.entity.MonthTick;
import com.parkingmanage.mapper.BlackListMapper;
import com.parkingmanage.mapper.MonthTicketMapper;
import com.parkingmanage.service.BlackListService;
import com.parkingmanage.service.MonthTicketService;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author lzx
 * @since 2023-12-21
 */
@Service
public class MonthTicketServiceImpl extends ServiceImpl<MonthTicketMapper, MonthTick> implements MonthTicketService {

    @Resource
    private MonthTicketService monthTicketService;
    @Autowired
    private MonthTicketMapper monthTicketMapper;

    @Override
    public List<MonthTick> queryInfoOnly(String parkName, String carNo, String ticketName, String userName, Integer timeDays, String timePeriodList, String userPhone, String remark1, String remark2, String remark3,Integer isFrozen,Integer isValid) {
        QueryWrapper<MonthTick> queryWrapper = new QueryWrapper<>();
        // 模糊搜索部分
        queryWrapper.like(StringUtils.isNotBlank(carNo), "car_no", carNo);
        queryWrapper.like(StringUtils.isNotBlank(parkName), "park_name", parkName);
        queryWrapper.like(StringUtils.isNotBlank(ticketName), "ticket_name", ticketName);
        queryWrapper.like(StringUtils.isNotBlank(userName), "user_name", userName);
        queryWrapper.like(StringUtils.isNotBlank(userPhone), "user_phone", userPhone);
        queryWrapper.like(StringUtils.isNotBlank(remark1), "remark1", remark1);
        queryWrapper.like(StringUtils.isNotBlank(remark2), "remark2", remark2);
        queryWrapper.like(StringUtils.isNotBlank(remark3), "remark3", remark3);
        if (isValid != null) {
            queryWrapper.eq("valid_status", isValid);
        }
        if (isFrozen != null) {
            queryWrapper.eq("is_frozen", isFrozen);
        }
        if (StringUtils.isNotBlank(timePeriodList)) {
            // 获取数据库中的全部的数据
            List<MonthTick> monthTickList = monthTicketMapper.selectAll(parkName);
            ArrayList<Integer> monthTicksIds = new ArrayList<>();
            int n = 1;
            for (MonthTick monthTick : monthTickList) {
                String timePeriodList1 = monthTick.getTimePeriodList();
                String[] dataArray = timePeriodList1.split(",");
                for (int i = 0; i < dataArray.length; i++) {
                    String data = dataArray[i];
                    // 接着将data按照"至"字进行拆分
                    String[] split = data.split("至");
                    if (split.length > 1) { // 确保拆分后有至少两个部分
                        String endDate = split[1];
                        if (endDate.contains(timePeriodList)) { // 检查拆分后的第二部分是否包含当前数据的年月日部分
                            if (i == 0) { // 如果位置是第一个
                                monthTicksIds.add(monthTick.getId());
                            }
                        }
                    }
                }
            }
            if (monthTicksIds.size() > 0) {
                queryWrapper.in("id", monthTicksIds);
            } else {
                return new ArrayList<>();
            }
        }
        ArrayList<MonthTick> objects = new ArrayList<>();
        // 获取当前日期
        LocalDate currentDate = LocalDate.now();
        // 当前日期加上5天
        if (timeDays != null) {
            QueryWrapper<MonthTick> queryWrapper1 = new QueryWrapper<>();
            LocalDate newDate = currentDate.plusDays(timeDays);
            // 格式化新日期为字符串
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String formattedDate = newDate.format(formatter);
            // 获取数据库中的全部的数据
            List<MonthTick> monthTickList = monthTicketMapper.selectAll(parkName);
            ArrayList<Integer> monthTicksIdsTime = new ArrayList<>();
            System.out.println("formattedDate = " + formattedDate);
            int n = 1;
            for (MonthTick monthTick : monthTickList) {
                String timePeriodList1 = monthTick.getTimePeriodList();
                String[] dataArray = timePeriodList1.split(",");
                for (int i = 0; i < dataArray.length; i++) {
                    String data = dataArray[i];
                    // 接着将data按照"至"字进行拆分
                    String[] split = data.split("至");
                    if (split.length > 1) { // 确保拆分后有至少两个部分
                        String endDate = split[1];
                        if (endDate.contains(formattedDate)) { // 检查拆分后的第二部分是否包含当前数据的年月日部分
                            if (i == 0) { // 如果位置是第一个
                                monthTicksIdsTime.add(monthTick.getId());
                            }
                        }
                    }
                }
            }
            if (monthTicksIdsTime.size() > 0) {
                queryWrapper.in("id", monthTicksIdsTime);
            } else {
                return objects;
            }
        }
        // 时间比较部分
        return monthTicketMapper.selectList(queryWrapper);
    }

    @Override
    public List<MonthTick> findOne(MonthTick monthTick) {
        LambdaQueryWrapper<MonthTick> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(MonthTick::getCarNo, monthTick.getCarNo()).eq(MonthTick::getTicketName, monthTick.getTicketName())
                .eq(MonthTick::getParkName, monthTick.getParkName()).eq(MonthTick::getRemark1, monthTick.getRemark1()).eq(MonthTick::getRemark2, monthTick.getRemark2()).eq(MonthTick::getRemark3, monthTick.getRemark3())
                .eq(MonthTick::getUserPhone, monthTick.getUserPhone()).eq(MonthTick::getUserName, monthTick.getUserName()).eq(MonthTick::getCreateTime, monthTick.getCreateTime());
        return monthTicketMapper.selectList(queryWrapper);
    }

}