package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.entity.BlackList;
import com.parkingmanage.entity.Violations;
import com.parkingmanage.mapper.BlackListMapper;
import com.parkingmanage.mapper.ViolationsMapper;
import com.parkingmanage.service.ViolationsService;
import com.parkingmanage.service.AcmsVipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 违规记录处理功能服务实现
 * 提供手动处理、批量处理和自动拉黑功能
 *
 * @author MLH
 * @since 2025-01-31
 */
@Slf4j
@Component
public class ViolationProcessServiceImpl {

    @Resource
    private ViolationsMapper violationsMapper;

    @Resource
    private ViolationsService violationsService;

    @Resource
    private AcmsVipService acmsVipService;

    @Resource
    private BlackListMapper blackListMapper;

    /**
     * 🆕 手动处理单条违规记录
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean manualProcessViolation(Long violationId, String operatorName, String processRemark) {
        log.info("👨‍💼 [手动处理] 开始处理违规记录 - ID: {}, 操作员: {}", violationId, operatorName);

        try {
            // 1. 查询违规记录
            Violations violation = violationsMapper.selectById(violationId);
            if (violation == null) {
                log.warn("⚠️ [手动处理] 违规记录不存在 - ID: {}", violationId);
                return false;
            }

            // 2. 检查是否已处理
            if ("processed".equals(violation.getProcessStatus())) {
                log.warn("⚠️ [手动处理] 违规记录已被处理 - ID: {}, 处理方式: {}",
                        violationId, violation.getProcessType());
                return false;
            }

            // 3. 更新处理状态
            violation.setProcessStatus("processed");
            violation.setProcessType("manual");
            violation.setProcessedAt(LocalDateTime.now());
            violation.setProcessedBy(operatorName);
            violation.setProcessRemark(processRemark != null && !processRemark.trim().isEmpty()
                    ? processRemark : "手动处理");

            int updated = violationsMapper.updateById(violation);

            if (updated > 0) {
                log.info("✅ [手动处理] 处理成功 - ID: {}, 车牌: {}", violationId, violation.getPlateNumber());
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("❌ [手动处理] 处理失败 - ID: {}, 错误: {}", violationId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 🆕 手动批量处理违规记录
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchProcessViolations(List<Long> violationIds, String operatorName, String processRemark) {
        log.info("📋 [批量处理] 开始批量处理 - 数量: {}, 操作员: {}", violationIds.size(), operatorName);

        int successCount = 0;
        int failCount = 0;

        for (Long violationId : violationIds) {
            try {
                boolean success = manualProcessViolation(violationId, operatorName, processRemark);
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                log.error("❌ [批量处理] 处理单条记录失败 - ID: {}, 错误: {}", violationId, e.getMessage());
                failCount++;
            }
        }

        log.info("✅ [批量处理] 处理完成 - 成功: {}, 失败: {}", successCount, failCount);
        return successCount;
    }

    /**
     * 🆕 检查并执行自动拉黑
     * 核心逻辑：第N次违规时触发自动拉黑
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean checkAndAutoBlacklist(String plateNumber, String parkCode) {
        log.info("🔍 [自动拉黑检查] 开始检查 - 车牌: {}, 停车场: {}", plateNumber, parkCode);

        try {
            // 1. 统计该车牌的未处理违规次数（包括刚创建的这条）
            int unprocessedCount = countUnprocessedViolations(plateNumber);
            log.info("📊 [违规统计] 车牌 {} 当前未处理违规次数: {}", plateNumber, unprocessedCount);

            // 2. 获取配置的违规次数阈值
            Map<String, Object> config = violationsService.getMonthlyTicketTimeoutConfig(parkCode);
            Integer maxViolationCount = (Integer) config.get("maxViolationCount");

            if (maxViolationCount == null) {
                maxViolationCount = 3; // 默认3次
                log.warn("⚠️ [配置缺失] 未找到违规次数配置，使用默认值: {}", maxViolationCount);
            }

            log.info("⚙️ [配置信息] 停车场 {} 配置的违规次数阈值: {}", parkCode, maxViolationCount);

            // 3. 判断是否达到拉黑条件
            if (unprocessedCount >= maxViolationCount) {
                log.warn("🚫 [触发拉黑] 车牌 {} 违规次数 {} 已达到阈值 {}，开始执行自动拉黑",
                        plateNumber, unprocessedCount, maxViolationCount);

                // 4. 执行拉黑操作
                boolean blacklisted = executeAutoBlacklist(plateNumber, unprocessedCount);

                if (blacklisted) {
                    log.info("✅ [自动拉黑] 车牌 {} 已成功加入黑名单", plateNumber);
                    return true;
                } else {
                    log.error("❌ [自动拉黑] 车牌 {} 加入黑名单失败", plateNumber);
                    return false;
                }
            } else {
                log.info("ℹ️ [未达阈值] 车牌 {} 违规次数 {} 未达到阈值 {}，暂不拉黑",
                        plateNumber, unprocessedCount, maxViolationCount);
                return false;
            }

        } catch (Exception e) {
            log.error("❌ [自动拉黑检查] 检查失败 - 车牌: {}, 错误: {}", plateNumber, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 🔧 执行自动拉黑操作（内部方法）
     */
    private boolean executeAutoBlacklist(String plateNumber, int violationCount) {
        try {
            // 1. 添加到黑名单表
            String blacklistReason = String.format("累计违规%d次，系统自动拉黑", violationCount);
            boolean addedToBlacklist = violationsService.addToBlacklist(
                    plateNumber,
                    "系统停车场", // 使用默认停车场名称
                    blacklistReason,
                    "SYSTEM"
            );

            if (!addedToBlacklist) {
                log.error("❌ [自动拉黑] 添加到黑名单失败 - 车牌: {}", plateNumber);
                return false;
            }

            log.info("✅ [黑名单添加] 车牌 {} 已添加到黑名单", plateNumber);

            // 2. 批量标记该车牌的所有未处理违规记录为已处理
            int processedCount = violationsMapper.batchUpdateProcessStatusByPlate(
                    plateNumber,
                    "processed",
                    "auto_blacklist",
                    LocalDateTime.now(),
                    "SYSTEM",
                    blacklistReason
            );

            log.info("✅ [批量标记] 车牌 {} 的 {} 条违规记录已标记为已处理", plateNumber, processedCount);

            return true;

        } catch (Exception e) {
            log.error("❌ [自动拉黑] 执行失败 - 车牌: {}, 错误: {}", plateNumber, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 🆕 统计指定车牌的未处理违规次数
     */
    public int countUnprocessedViolations(String plateNumber) {
        return violationsMapper.countUnprocessedByPlate(plateNumber);
    }

    /**
     * 🆕 分页查询违规记录（支持处理状态筛选）
     */
    public IPage<Map<String, Object>> getViolationsWithProcess(
            Page<Map<String, Object>> page,
            String plateNumber,
            String status,
            String violationType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String createdByFilter,
            String communityFilter,
            String processStatus,
            String processType,
            Boolean onlyUnprocessed
    ) {
        return violationsMapper.selectViolationsDirectQueryWithProcess(
                page, plateNumber, status, violationType,
                startDate, endDate, createdByFilter, communityFilter,
                processStatus, processType, onlyUnprocessed
        );
    }

    /**
     * 🆕 手动加入黑名单（支持ACMS接口调用）
     * 
     * @param violationId 违规记录ID
     * @param operatorName 操作员姓名
     * @param blacklistType 黑名单类型（格式：code|name，例如："local_violation|违规黑名单"）
     * @param blacklistReason 拉黑原因
     * @param isPermanent 是否永久拉黑
     * @param blacklistStartTime 拉黑开始时间（格式：yyyy-MM-dd）
     * @param blacklistEndTime 拉黑结束时间（格式：yyyy-MM-dd）
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean addToBlacklist(
            Long violationId,
            String operatorName,
            String blacklistType,
            String blacklistReason,
            Boolean isPermanent,
            String blacklistStartTime,
            String blacklistEndTime
    ) {
        log.info("🚫 [手动拉黑] 开始处理 - 违规ID: {}, 操作员: {}, 类型: {}, 永久: {}", 
                violationId, operatorName, blacklistType, isPermanent);

        try {
            // 1. 查询违规记录获取完整信息
            Violations violation = violationsMapper.selectById(violationId);
            if (violation == null) {
                log.warn("⚠️ [手动拉黑] 违规记录不存在 - ID: {}", violationId);
                return false;
            }

            String plateNumber = violation.getPlateNumber();
            String parkName = violation.getParkName() != null ? violation.getParkName() : "系统停车场";
            String ownerName = violation.getOwnerName() != null ? violation.getOwnerName() : "未知车主";

            // 2. 解析黑名单类型（格式：code|name）
            String blacklistTypeCode = null;
            String blacklistTypeName = null;
            if (blacklistType != null && blacklistType.contains("|")) {
                String[] parts = blacklistType.split("\\|");
                blacklistTypeCode = parts[0];
                blacklistTypeName = parts.length > 1 ? parts[1] : parts[0];
            } else {
                blacklistTypeCode = blacklistType;
                blacklistTypeName = blacklistType;
            }

            log.info("🏷️ [黑名单类型] code: {}, name: {}", blacklistTypeCode, blacklistTypeName);

            // 3. 构建ACMS黑名单添加请求
            AcmsVipService.AddBlacklistRequest acmsRequest = new AcmsVipService.AddBlacklistRequest();
            acmsRequest.setParkName(parkName);
            acmsRequest.setVipTypeCode(blacklistTypeCode);
            acmsRequest.setVipTypeName(blacklistTypeName);
            acmsRequest.setCarCode(plateNumber);
            acmsRequest.setCarOwner(ownerName);
            acmsRequest.setReason(blacklistReason != null ? blacklistReason : "手动拉黑");
            
            // 设置拉黑时长类型
            String durationType = (isPermanent != null && isPermanent) ? "permanent" : "temporary";
            acmsRequest.setDurationType(durationType);
            
            // 如果是临时拉黑，设置时间段
            if ("temporary".equals(durationType) && blacklistStartTime != null && blacklistEndTime != null) {
                acmsRequest.setStartTime(blacklistStartTime + " 00:00:00");
                acmsRequest.setEndTime(blacklistEndTime + " 23:59:59");
                log.info("⏰ [临时拉黑] 时间段: {} 至 {}", acmsRequest.getStartTime(), acmsRequest.getEndTime());
            }
            
            acmsRequest.setRemark1("管理后台手动拉黑");
            acmsRequest.setRemark2("违规记录处理");
            acmsRequest.setOperator(operatorName != null ? operatorName : "系统");
            acmsRequest.setOperateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // 4. 调用ACMS接口添加黑名单
            log.info("📤 [ACMS拉黑] 开始调用ACMS接口 - 车牌: {}", plateNumber);
            boolean acmsSuccess = acmsVipService.addBlacklistToAcms(acmsRequest);

            if (!acmsSuccess) {
                log.error("❌ [ACMS拉黑失败] 车牌: {}, ACMS接口返回失败", plateNumber);
                return false;
            }

            log.info("✅ [ACMS拉黑成功] 车牌: {}", plateNumber);

            // 5. 添加到本地黑名单表（作为备份）- 参考小程序实现
            try {
                log.info("💾 [本地黑名单] 准备保存/更新到black_list表: plateNumber={}", plateNumber);
                
                // 查询是否已存在该车牌的黑名单记录
                LambdaQueryWrapper<BlackList> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(BlackList::getCarCode, plateNumber)
                           .eq(BlackList::getParkName, parkName)
                           .eq(BlackList::getDeleted, 0);  // 🔑 只查询未删除的记录
                
                BlackList existingBlackList = blackListMapper.selectOne(queryWrapper);
                
                if (existingBlackList != null) {
                    // ✏️ 更新已有记录
                    log.info("🔄 [黑名单已存在] blacklistId={}, plateNumber={}, 执行更新操作", 
                            existingBlackList.getId(), plateNumber);
                    
                    existingBlackList.setOwner(ownerName);  // 🔑 设置车主姓名
                    existingBlackList.setReason(blacklistReason != null ? blacklistReason : "手动拉黑");
                    existingBlackList.setSpecialCarTypeConfigName(blacklistTypeName);
                    existingBlackList.setBlacklistTypeCode(blacklistTypeCode);
                    
                    // 🔑 设置拉黑时长标志（黑名单状态）
                    if (isPermanent != null && isPermanent) {
                        existingBlackList.setBlackListForeverFlag("永久");  // 🔑 黑名单状态
                        existingBlackList.setBlacklistStartTime(null);
                        existingBlackList.setBlacklistEndTime(null);
                    } else {
                        existingBlackList.setBlackListForeverFlag("临时");  // 🔑 黑名单状态
                        // 🔑 设置拉黑时间（转换为 LocalDateTime）
                        if (blacklistStartTime != null && blacklistEndTime != null) {
                            existingBlackList.setBlacklistStartTime(
                                LocalDateTime.parse(blacklistStartTime + " 00:00:00", 
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            );
                            existingBlackList.setBlacklistEndTime(
                                LocalDateTime.parse(blacklistEndTime + " 23:59:59", 
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            );
                        }
                    }
                    
                    // 更新备注信息
                    existingBlackList.setRemark1("违规记录ID: " + violationId);
                    existingBlackList.setRemark2("管理后台手动拉黑，操作人: " + operatorName);
                    
                    int updateResult = blackListMapper.updateById(existingBlackList);
                    
                    if (updateResult > 0) {
                        log.info("✅ [黑名单更新成功] blacklistId={}, plateNumber={}, owner={}, type={}, duration={}, startTime={}, endTime={}", 
                                existingBlackList.getId(), existingBlackList.getCarCode(), 
                                existingBlackList.getOwner(),  // 🔑 输出车主姓名
                                existingBlackList.getSpecialCarTypeConfigName(), 
                                existingBlackList.getBlackListForeverFlag(),  // 🔑 输出黑名单状态
                                existingBlackList.getBlacklistStartTime(),  // 🔑 输出拉黑时间
                                existingBlackList.getBlacklistEndTime());
                    } else {
                        log.error("❌ [黑名单更新失败] blacklistId={}, plateNumber={}", 
                                existingBlackList.getId(), plateNumber);
                    }
                    
                } else {
                    // ➕ 新增记录
                    log.info("➕ [黑名单不存在] plateNumber={}, 执行新增操作", plateNumber);
                    
                    BlackList blackList = new BlackList();
                    blackList.setParkName(parkName);
                    blackList.setCarCode(plateNumber);
                    blackList.setOwner(ownerName);  // 🔑 设置车主姓名
                    blackList.setReason(blacklistReason != null ? blacklistReason : "手动拉黑");
                    
                    // 设置黑名单类型
                    blackList.setSpecialCarTypeConfigName(blacklistTypeName);
                    blackList.setBlacklistTypeCode(blacklistTypeCode);
                    
                    // 🔑 设置拉黑时长标志（黑名单状态）
                    if (isPermanent != null && isPermanent) {
                        blackList.setBlackListForeverFlag("永久");  // 🔑 黑名单状态
                    } else {
                        blackList.setBlackListForeverFlag("临时");  // 🔑 黑名单状态
                        // 🔑 设置拉黑时间（转换为 LocalDateTime）
                        if (blacklistStartTime != null && blacklistEndTime != null) {
                            blackList.setBlacklistStartTime(
                                LocalDateTime.parse(blacklistStartTime + " 00:00:00", 
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            );
                            blackList.setBlacklistEndTime(
                                LocalDateTime.parse(blacklistEndTime + " 23:59:59", 
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            );
                        }
                    }
                    
                    // 设置备注信息
                    blackList.setRemark1("违规记录ID: " + violationId);
                    blackList.setRemark2("管理后台手动拉黑，操作人: " + operatorName);
                    
                    int insertResult = blackListMapper.insert(blackList);
                    
                    if (insertResult > 0) {
                        log.info("✅ [黑名单新增成功] blacklistId={}, plateNumber={}, owner={}, type={}, duration={}, startTime={}, endTime={}", 
                                blackList.getId(), blackList.getCarCode(), 
                                blackList.getOwner(),  // 🔑 输出车主姓名
                                blackList.getSpecialCarTypeConfigName(), 
                                blackList.getBlackListForeverFlag(),  // 🔑 输出黑名单状态
                                blackList.getBlacklistStartTime(),  // 🔑 输出拉黑时间
                                blackList.getBlacklistEndTime());
                    } else {
                        log.error("❌ [黑名单新增失败] plateNumber={}", plateNumber);
                    }
                }
            } catch (Exception e) {
                log.error("❌ [本地黑名单保存异常] plateNumber={}, error={}", 
                        plateNumber, e.getMessage(), e);
                // 本地黑名单保存失败不影响整体结果（因为ACMS已经成功）
            }
            log.info("✅ [手动拉黑] 处理完成 - 车牌: {}, 类型: {}, 永久: {}", 
                    plateNumber, blacklistTypeName, isPermanent);
            return true;
        } catch (Exception e) {
            log.error("❌ [手动拉黑] 处理失败 - 违规ID: {}, 错误: {}", violationId, e.getMessage(), e);
            throw e;
        }
    }
}

