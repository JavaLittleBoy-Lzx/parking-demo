package com.parkingmanage.service.impl;

import com.parkingmanage.entity.ReportCarIn;
import com.parkingmanage.entity.ReportCarInReservation;
import com.parkingmanage.entity.ReportCarOutReservation;
import com.parkingmanage.mapper.ReportCarInMapper;
import com.parkingmanage.service.ReportCarInService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lzx
 * @since 2024-04-27
 */
@Service
public class ReportCarInServiceImpl extends ServiceImpl<ReportCarInMapper, ReportCarIn> implements ReportCarInService {

    @Override
    public List<ReportCarIn> findByLicenseNumber(String parkingCode) {
        return baseMapper.findByLicenseNumber(parkingCode);
    }

    @Override
    public int countByDate(String startDate, String endDate, String yardName) {
        return baseMapper.countByDate(startDate,endDate,yardName);
    }

    @Override
    public int countByDateVIP(String startDate, String endDate, String yardName) {
        return baseMapper.countByDateVIP(startDate,endDate,yardName);
    }

    @Override
    public int updateByCarNumber(String carLicenseNumber, String preVipType) {
        return baseMapper.updateByCarNumber(carLicenseNumber,preVipType);
    }

    @Override
    public List<ReportCarInReservation> queryListReportOutExport(String startDate, String endDate, String yardName) {
        return baseMapper.queryListReportOutExport(startDate,endDate,yardName);
    }

    @Override
    public List<ReportCarInReservation> queryListReportCarOutExportLinShi(String startDate, String endDate, String yardName) {
        return baseMapper.queryListReportCarOutExportLinShi(startDate,endDate,yardName);
    }

    @Override
    public List<ReportCarIn> selectCarRecords(String carCode,String enterTime) {
        return baseMapper.selectCarRecords(carCode,enterTime);
    }

    @Override
    public List<ReportCarInReservation> queryListReportOutExportWan(String startDate, String endDate, String yardName, String channelName) {
        return baseMapper.queryListReportOutExportWan(startDate,endDate,yardName,channelName);
    }

    @Override
    public List<ReportCarInReservation> queryListReportCarOutExportLinShiWan(String startDate, String endDate, String yardName, String channelName) {
        return baseMapper.queryListReportCarOutExportLinShiWan(startDate,endDate,yardName,channelName);
    }

    @Override
    public int countByDateWan(String startDate, String endDate, String yardName, String channelName) {
        return baseMapper.countByDateWan(startDate,endDate,yardName,channelName);
    }

    @Override
    public int countByDateVIPWan(String startDate, String endDate, String yardName, String channelName) {
        return baseMapper.countByDateVIPWan(startDate,endDate,yardName,channelName);
    }
}
