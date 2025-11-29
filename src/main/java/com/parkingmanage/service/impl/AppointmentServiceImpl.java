package com.parkingmanage.service.impl;

import com.parkingmanage.entity.Appointment;
import com.parkingmanage.mapper.AppointmentMapper;
import com.parkingmanage.query.VehicleQuery;
import com.parkingmanage.service.AppointmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author MLH
 * @since 2022-07-13
 */
@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements AppointmentService {
    @Override
    public Appointment getAppointmentByOrderNumber(String orderNumber){
        return baseMapper.selectAppointmentByOrderNumber(orderNumber);
    }
    @Override
    public  List<Appointment> listAppointNoAudit(String community,String ownername,String  recorddate){
        return baseMapper.listAppointNoAudit(community,ownername,recorddate);
    }

    @Override
    public Appointment getByQuery(String enterCarLicenseNumber) {
        return baseMapper.getByQuery(enterCarLicenseNumber);
    }

    @Override
    public int updateByCarNumber(String enterCarLicenseNumber, String enterTime) {
        return baseMapper.updateByCarNumber(enterCarLicenseNumber,enterTime);
    }

    @Override
    public int updateLeaveTimeByCarNumber(String enterCarLicenseNumber, String enterTime, String leaveTime) {
        return baseMapper.updateLeaveTimeByCarNumber(enterCarLicenseNumber,enterTime,leaveTime);
    }

    @Override
    public List<Appointment> getAppointmentPlateNumber(String plateNumber) {
        return baseMapper.getAppointmentPlateNumber(plateNumber);
    }

    @Override
    public List<Appointment> subAppointQueryListDuration(String openid, String platenumber, String visitorphone, String visitdateBegin, String recorddateBegin, String visitdateEnd, String recorddateEnd) {
        return baseMapper.subAppointQueryListDuration(openid,platenumber,visitdateBegin,visitdateEnd,recorddateBegin,recorddateEnd,visitorphone);
    }

    @Override
    public List<Appointment> visitorList(String openid){
        return baseMapper.visitorList(openid);
    }
    @Override
    public List<Appointment> managerList(String openid){
        return baseMapper.managerList(openid);
    }
    @Override
    public List<Appointment> vehicleQueryList(String openid,String platenumber,String leavedate){
        return baseMapper.vehicleQueryList(openid,platenumber,leavedate);
    }
    @Override
    public List<Appointment> subAppointQueryList(String openid, String platenumber, String visitorphone, String visitdateBegin, String visitdateEnd, String recorddateBegin,String recorddateEnd){
        return baseMapper.subAppointQueryList(openid,platenumber,visitorphone,visitdateBegin,visitdateEnd,recorddateBegin,recorddateEnd);
    }
    @Override
    public List<Appointment> auditQueryList(String openid, String platenumber, String visitorphone, String visitdateBegin,  String visitdateEnd,String recorddateBegin, String recorddateEnd){
        return baseMapper.auditQueryList(openid,platenumber,visitorphone,visitdateBegin,visitdateEnd,recorddateBegin,recorddateEnd);
    }
    @Override
    public  List<Appointment> allpage(String community,String plateNumber,String  visitdate,String auditstatus){
        return baseMapper.allpage(community,plateNumber,visitdate,auditstatus);
    }
    @Override
    public  List<Appointment> venuepage(String community,String plateNumber,String  arrivedate,String leavedate,String venuestatus){
        return baseMapper.venuepage(community,plateNumber,arrivedate,leavedate,venuestatus);
    }
    
    @Override
    public List<Appointment> listByPhone(String phone) {
        return baseMapper.listByPhone(phone);
    }

    @Override
    public List<Appointment> listByAddress(String community, String building, String units, String floor, String room) {
        return baseMapper.listByAddress(community, building, units, floor, room);
    }

    @Override
    public Appointment getByQueryInfo(String enterCarLicenseNumber, String yardName) {
        return baseMapper.getByQueryInfo(enterCarLicenseNumber,yardName);
    }

    // 🆕 实现根据预约记录ID更新访客微信姓名
    @Override
    public int updateVisitorNameByAppointment(Integer appointmentId, String visitorName) {
        return baseMapper.updateVisitorNameByAppointment(appointmentId, visitorName);
    }
    
    // 🆕 实现根据手机号更新最新的预约记录的访客姓名
    @Override
    public int updateVisitorNameByPhone(String visitorPhone, String visitorName) {
        return baseMapper.updateVisitorNameByPhone(visitorPhone, visitorName);
    }
    
    // 🆕 实现根据openid更新最新的预约记录的访客姓名
    @Override
    public int updateVisitorNameByOpenid(String openid, String visitorName) {
        return baseMapper.updateVisitorNameByOpenid(openid, visitorName);
    }
    
    @Override
    public List<Appointment> getAlmostTimeoutAppointments(LocalDateTime timeThreshold) {
        return baseMapper.getAlmostTimeoutAppointments(timeThreshold);
    }
    
    // 🆕 实现查询2小时内活跃的预约记录
    @Override
    public List<Appointment> getRecentActiveAppointments(LocalDateTime twoHoursAgo) {
        // 将LocalDateTime转换为字符串格式进行比较（arrivedate是String类型）
        // 使用与数据库一致的格式：yyyy-MM-dd HH:mm:ss
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String twoHoursAgoStr = twoHoursAgo.format(formatter);
        System.out.println("twoHoursAgoStr = " + twoHoursAgoStr);
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNotNull(Appointment::getArrivedate)  // 进场时间不为空
                   .ge(Appointment::getArrivedate, twoHoursAgoStr)  // 进场时间在2小时内
                   .eq(Appointment::getVenuestatus, "已入场")    // 场地状态为已入场
                   .orderByAsc(Appointment::getArrivedate); // 按进场时间升序
        
        return this.list(queryWrapper);
    }
    
    // 🆕 实现统计2小时内活跃的预约数量
    @Override
    public int countRecentActiveAppointments(LocalDateTime twoHoursAgo) {
        // 将LocalDateTime转换为字符串格式进行比较（arrivedate是String类型）
        // 使用与数据库一致的格式：yyyy-MM-dd HH:mm:ss
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String twoHoursAgoStr = twoHoursAgo.format(formatter);
        
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNotNull(Appointment::getArrivedate)  // 进场时间不为空
                   .isNull(Appointment::getLeavedate)      // 离场时间为空
                   .ge(Appointment::getArrivedate, twoHoursAgoStr)  // 进场时间在2小时内
                   .eq(Appointment::getVenuestatus, "已入场");   // 场地状态为已入场
        
        return (int) this.count(queryWrapper);
    }

}
