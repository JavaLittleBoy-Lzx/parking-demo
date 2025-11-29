package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.ViolationReminder;
import com.parkingmanage.mapper.ViolationReminderMapper;
import com.parkingmanage.service.ViolationReminderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 违规提醒记录表 服务实现类
 * </p>
 *
 * @author parking-system
 * @since 2024-01-XX
 */
@Slf4j
@Service
public class ViolationReminderServiceImpl extends ServiceImpl<ViolationReminderMapper, ViolationReminder> implements ViolationReminderService {

    @Autowired
    private ViolationReminderMapper violationReminderMapper;

    @Override
    public List<ViolationReminder> getUnprocessedByPlateNumber(String plateNumber) {
        return violationReminderMapper.selectUnprocessedByPlateNumber(plateNumber);
    }

    @Override
    public boolean markAllAsProcessedByPlateNumber(String plateNumber, String processedBy) {
        int result = violationReminderMapper.markAllAsProcessedByPlateNumber(plateNumber, processedBy);
        return result > 0;
    }

    @Override
    public List<ViolationReminder> getAllByPlateNumber(String plateNumber) {
        return violationReminderMapper.selectAllByPlateNumber(plateNumber);
    }

    @Override
    public List<ViolationReminder> getByTimeRange(String startTime, String endTime) {
        return violationReminderMapper.selectByTimeRange(startTime, endTime);
    }

    @Override
    public int countUnprocessedReminders() {
        return violationReminderMapper.countUnprocessedReminders();
    }

    @Override
    public int countByPlateNumber(String plateNumber) {
        return violationReminderMapper.countByPlateNumber(plateNumber);
    }

    @Override
    public boolean createViolationReminder(ViolationReminder reminder) {
        reminder.setCreateTime(LocalDateTime.now());
        reminder.setUpdateTime(LocalDateTime.now());
        reminder.setIsProcessed(0); // 默认未处理
        return save(reminder);
    }

    @Override
    public boolean shouldSendReminder(String plateNumber) {
        // 检查是否是第一次违规（没有违规记录）
        int count = countByPlateNumber(plateNumber);
        return count == 0;
    }

    @Override
    public boolean shouldSendViolationSms(String plateNumber) {
        // 检查是否是第二次及以后违规（有违规记录）
        int count = countByPlateNumber(plateNumber);
        return count > 0;
    }

    @Override
    public boolean processViolationReminder(Long id, String processedBy) {
        ViolationReminder reminder = getById(id);
        if (reminder != null) {
            reminder.setIsProcessed(1);
            reminder.setProcessedTime(LocalDateTime.now());
            reminder.setProcessedBy(processedBy);
            reminder.setUpdateTime(LocalDateTime.now());
            return updateById(reminder);
        }
        return false;
    }

    // ==================== 📊 统计分析实现 ====================

    @Override
    public List<Map<String, Object>> getCorrelationAnalysis(Integer days) {
        log.info("📊 查询违规记录与提醒发送关联分析, 近{}天", days);
        return violationReminderMapper.selectCorrelationAnalysis(days);
    }
}
