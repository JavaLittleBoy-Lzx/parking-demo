package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.VisitorApplication;
import com.parkingmanage.mapper.VisitorApplicationMapper;
import com.parkingmanage.service.VisitorApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * <p>
 * 访客申请 服务实现类
 * </p>
 *
 * @author System
 * @since 2024-01-15
 */
@Service
public class VisitorApplicationServiceImpl extends ServiceImpl<VisitorApplicationMapper, VisitorApplication> implements VisitorApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(VisitorApplicationServiceImpl.class);

    @Override
    public List<VisitorApplication> queryListVisitorApplication(String nickname, String community, String applydate) {
        logger.info("📋 查询访客申请列表: nickname={}, community={}, applydate={}", nickname, community, applydate);
        try {
            List<VisitorApplication> applications = baseMapper.queryListVisitorApplication(nickname, community, applydate);
            logger.info("✅ 查询到 {} 条访客申请记录", applications.size());
            return applications;
        } catch (Exception e) {
            logger.error("❌ 查询访客申请列表失败", e);
            throw e;
        }
    }

    @Override
    public VisitorApplication getByPhone(String phone) {
        logger.info("📱 根据手机号查询访客申请: phone={}", phone);
        try {
            VisitorApplication application = baseMapper.getByPhone(phone);
            if (application != null) {
                logger.info("✅ 找到访客申请: applicationNo={}", application.getApplicationNo());
            } else {
                logger.info("ℹ️ 未找到手机号为 {} 的访客申请", phone);
            }
            return application;
        } catch (Exception e) {
            logger.error("❌ 根据手机号查询访客申请失败", e);
            throw e;
        }
    }

    @Override
    public List<VisitorApplication> getRecordsByPhone(String phone) {
        logger.info("📋 根据手机号查询所有访客申请记录: phone={}", phone);
        try {
            LambdaQueryWrapper<VisitorApplication> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(VisitorApplication::getPhone, phone);
            wrapper.orderByDesc(VisitorApplication::getApplydate);
            
            List<VisitorApplication> records = list(wrapper);
            logger.info("✅ 找到 {} 条访客申请记录", records.size());
            return records;
        } catch (Exception e) {
            logger.error("❌ 根据手机号查询所有访客申请记录失败", e);
            throw e;
        }
    }

    @Override
    public List<VisitorApplication> getApprovedApplicationsByPhone(String phone, String auditstatus) {
        logger.info("🔍 根据手机号和审核状态查询访客申请记录: phone={}, auditstatus={}", phone, auditstatus);
        try {
            LambdaQueryWrapper<VisitorApplication> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(VisitorApplication::getPhone, phone);
            wrapper.eq(VisitorApplication::getAuditstatus, auditstatus);
            wrapper.orderByDesc(VisitorApplication::getApplydate); // 按申请时间倒序，最新的在前面
            
            List<VisitorApplication> records = list(wrapper);
            logger.info("✅ 找到 {} 条审核状态为 '{}' 的访客申请记录", records.size(), auditstatus);
            
            // 记录查询到的地址信息用于调试
            for (int i = 0; i < Math.min(records.size(), 3); i++) {
                VisitorApplication record = records.get(i);
                logger.info("📍 记录{}: applicationNo={}, province={}, city={}, district={}, community={}, building={}, units={}, floor={}, roomnumber={}, fullAddress={}",
                    i + 1, record.getApplicationNo(), record.getProvince(), record.getCity(), record.getDistrict(), 
                    record.getCommunity(), record.getBuilding(), record.getUnits(), record.getFloor(), 
                    record.getRoomnumber(), record.getFullAddress());
            }
            
            return records;
        } catch (Exception e) {
            logger.error("❌ 根据手机号和审核状态查询访客申请记录失败", e);
            throw e;
        }
    }

    @Override
    public VisitorApplication getByApplicationNo(String applicationNo) {
        logger.info("🔍 根据申请编号查询访客申请: applicationNo={}", applicationNo);
        try {
            VisitorApplication application = baseMapper.getByApplicationNo(applicationNo);
            if (application != null) {
                logger.info("✅ 找到访客申请: phone={}", application.getPhone());
            } else {
                logger.info("ℹ️ 未找到申请编号为 {} 的访客申请", applicationNo);
            }
            return application;
        } catch (Exception e) {
            logger.error("❌ 根据申请编号查询访客申请失败", e);
            throw e;
        }
    }

    @Override
    public boolean updateVisitorApplication(VisitorApplication visitorApplication) {
        logger.info("🔄 更新访客申请: id={}, status={}", visitorApplication.getId(), visitorApplication.getAuditstatus());
        try {
            visitorApplication.setUpdateTime(LocalDateTime.now());
            boolean result = updateById(visitorApplication);
            if (result) {
                logger.info("✅ 访客申请更新成功");
            } else {
                logger.warn("⚠️ 访客申请更新失败");
            }
            return result;
        } catch (Exception e) {
            logger.error("❌ 更新访客申请失败", e);
            throw e;
        }
    }

    @Override
    public String generateApplicationNo() {
        String prefix = "VA";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.valueOf((int) (Math.random() * 1000));
        String applicationNo = prefix + timestamp + String.format("%03d", Integer.parseInt(random));
        logger.info("🆔 生成申请编号: {}", applicationNo);
        return applicationNo;
    }
} 