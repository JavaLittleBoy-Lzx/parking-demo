package com.parkingmanage.mapper;

import com.parkingmanage.entity.Appointment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 <p>
  Mapper 接口
 </p>

 @author MLH
 @since 2022-07-13
*/
public interface AppointmentMapper extends BaseMapper<Appointment> {
    Appointment selectAppointmentByOrderNumber(String owneropenid);
    List<Appointment> visitorList(String openid);
    List<Appointment> managerList(String openid);
    List<Appointment> vehicleQueryList(String openid,String platenumber,String leavedate);
    List<Appointment> subAppointQueryList(String openid, String platenumber, String visitorphone, String visitdateBegin,  String visitdateEnd, String recorddateBegin,String recorddateEnd);
    List<Appointment> auditQueryList(String openid, String platenumber, String visitorphone, String visitdateBegin,  String visitdateEnd,String recorddateBegin, String recorddateEnd);
    List<Appointment> allpage(String community,String plateNumber,String  visitdate,String auditstatus);
    List<Appointment> venuepage(String community,String plateNumber,String arrivedate,String leavedate,String venuestatus);
    List<Appointment> listAppointNoAudit(String community,String ownername,String  recorddate);
    Appointment getByQuery(String enterCarLicenseNumber);
    int updateByCarNumber(String enterCarLicenseNumber, String enterTime);
    int updateLeaveTimeByCarNumber(String enterCarLicenseNumber, String enterTime, String leaveTime);
    List<Appointment> getAppointmentPlateNumber(String plateNumber);
    List<Appointment> subAppointQueryListDuration(String openid, String platenumber, String visitdateBegin, String visitdateEnd, String recorddateBegin, String recorddateEnd, String visitorphone);
    List<Appointment> listByPhone(String phone);
    List<Appointment> listByAddress(String community, String building, String units, String floor, String room);
    Appointment getByQueryInfo(String enterCarLicenseNumber, String yardName);
    
    // 🆕 新增方法：根据预约记录ID更新访客微信姓名
    int updateVisitorNameByAppointment(@Param("appointmentId") Integer appointmentId, @Param("visitorName") String visitorName);
    
    // 🆕 新增方法：根据手机号更新最新的预约记录的访客姓名
    int updateVisitorNameByPhone(@Param("visitorPhone") String visitorPhone, @Param("visitorName") String visitorName);
    
    // 🆕 新增方法：根据openid更新最新的预约记录的访客姓名
    int updateVisitorNameByOpenid(@Param("openid") String openid, @Param("visitorName") String visitorName);
    
    /**
     * 🆕 获取即将超时的车辆（还剩15分钟到达2小时）
     * @param timeThreshold 时间阈值（如还剩15分钟前的时间点）
     * @return 即将超时的预约记录列表
     */
    List<Appointment> getAlmostTimeoutAppointments(@Param("timeThreshold") LocalDateTime timeThreshold);
}
