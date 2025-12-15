package com.parkingmanage.service;

import com.parkingmanage.entity.Appointment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.Member;
import com.parkingmanage.query.VehicleQuery;

import java.util.List;

/**
 <p>
  服务类
 </p>

 @author MLH
 @since 2022-07-13
*/
public interface AppointmentService extends IService<Appointment> {
    Appointment getAppointmentByOrderNumber(String orderNumber);
    List<Appointment> visitorList(String openid);
    List<Appointment> managerList(String openid);
    List<Appointment> vehicleQueryList(String openid,String platenumber,String leavedate);
    List<Appointment> subAppointQueryList(String openid, String platenumber, String visitorphone, String visitdateBegin,  String visitdateEnd,String recorddateBegin, String recorddateEnd);
    List<Appointment> auditQueryList(String openid, String platenumber, String visitorphone, String visitdateBegin,  String visitdateEnd,String recorddateBegin, String recorddateEnd);
    List<Appointment> allpage(String community,String plateNumber,String  visitdate,String auditstatus);
    List<Appointment> venuepage(String community,String plateNumber,String  arrivedate,String leavedate,String venuestatus);
    List<Appointment> listAppointNoAudit(String community,String ownername,String  recorddate) ;

    Appointment getByQuery(String enterCarLicenseNumber);

    int updateByCarNumber(String enterCarLicenseNumber, String enterTime);

    int updateLeaveTimeByCarNumber(String enterCarLicenseNumber, String enterTime, String leaveTime);

    List<Appointment> getAppointmentPlateNumber(String plateNumber);

    List<Appointment> subAppointQueryListDuration(String openid, String platenumber, String visitorphone, String visitdateBegin, String recorddateBegin, String visitdateEnd, String recorddateEnd);

    List<Appointment> listByPhone(String phone);

    List<Appointment> listByAddress(String community, String building, String units, String floor, String room);

    Appointment getByQueryInfo(String enterCarLicenseNumber, String yardName);
    
    // 🆕 新增方法：根据预约记录更新访客微信姓名
    int updateVisitorNameByAppointment(Integer appointmentId, String visitorName);
    
    // 🆕 新增方法：根据手机号更新最新的预约记录的访客姓名
    int updateVisitorNameByPhone(String visitorPhone, String visitorName);
    
    // 🆕 新增方法：根据openid更新最新的预约记录的访客姓名
    int updateVisitorNameByOpenid(String openid, String visitorName);
    
    // 🆕 新增方法：查询2小时内活跃的预约记录
    List<Appointment> getRecentActiveAppointments(java.time.LocalDateTime twoHoursAgo);
    
    // 🆕 新增方法：统计2小时内活跃的预约数量
    int countRecentActiveAppointments(java.time.LocalDateTime twoHoursAgo);
    
    /**
     * 🆕 获取即将超时的车辆（还剩15分钟到达2小时）
     * @param timeThreshold 时间阈值（如还剩15分钟）
     * @return 即将超时的预约记录列表
     */
    List<Appointment> getAlmostTimeoutAppointments(java.time.LocalDateTime timeThreshold);
    
    /**
     * 🔥 【优化】根据进场时间范围精准查询车辆
     * 用于定时任务精准查询特定时间点的车辆，避免查询所有2小时内的车辆
     * 
     * @param startTime 进场开始时间
     * @param endTime 进场结束时间
     * @return 在该时间段内进场的活跃车辆列表
     */
    List<Appointment> getActiveAppointmentsByTimeRange(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime);
}
