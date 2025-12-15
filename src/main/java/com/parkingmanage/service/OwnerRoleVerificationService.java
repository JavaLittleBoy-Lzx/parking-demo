package com.parkingmanage.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.common.HttpClientUtil;
import com.parkingmanage.common.config.AIKEConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 业主身份验证服务
 * 通过外部月票API验证用户是否为业主
 * 
 * @author parking-system
 * @since 2024
 */
@Slf4j
@Service
public class OwnerRoleVerificationService {

    @Autowired
    private AIKEConfig aikeConfig;

    // 停车场代码映射
    private static final Map<String, String> PARK_CODE_MAP = new HashMap<>();
    static {
        PARK_CODE_MAP.put("万象上东", "2KST9MNP");
        PARK_CODE_MAP.put("四季上东", "2KUG6XLU");
        PARK_CODE_MAP.put("欧洲新城", "2KPL6XFF");
    }

    // 默认查询的停车场代码
    private static final List<String> DEFAULT_PARK_CODES = Arrays.asList("2KST9MNP", "2KUG6XLU","2KPL6XFF");

    /**
     * 清除指定手机号的所有缓存
     * 
     * @param phoneNumber 手机号
     */
    @CacheEvict(value = {"ownerVerification", "ownerVerificationByPark", "ownerDetails"}, allEntries = true)
    public void clearCache(String phoneNumber) {
        log.info("🗑️ 清除手机号 [{}] 的所有缓存", phoneNumber);
    }
    
    /**
     * 清除所有缓存
     */
    @CacheEvict(value = {"ownerVerification", "ownerVerificationByPark", "ownerDetails"}, allEntries = true)
    public void clearAllCache() {
        log.info("🗑️ 清除所有业主验证缓存");
    }

    /**
     * 不使用缓存的业主验证方法（用于调试）
     * 
     * @param phoneNumber 手机号
     * @param parkName 停车场名称
     * @return true-找到业主身份，false-未找到
     */
    public boolean verifyOwnerWithoutCache(String phoneNumber, String parkName) {
        long startTime = System.currentTimeMillis();
        log.info("🔍 开始无缓存业主身份验证 - 手机号: [{}], 停车场: [{}]", phoneNumber, parkName);
        
        try {
            // 获取停车场代码
            String parkCode = PARK_CODE_MAP.get(parkName);
            if (parkCode == null) {
                log.warn("⚠️ 未知停车场名称: [{}]", parkName);
                return false;
            }
            
            // 直接调用搜索方法，不经过缓存
            boolean found = searchOwnerInPark(phoneNumber, parkCode);
            
            long endTime = System.currentTimeMillis();
            log.info("🚀 无缓存验证完成 - 手机号: [{}], 停车场: [{}], 结果: {}, 耗时: {}ms", 
                phoneNumber, parkName, found ? "✅业主" : "❌非业主", (endTime - startTime));
            
            return found;
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("❌ 无缓存验证失败，手机号: [{}], 停车场: [{}], 耗时: {}ms", 
                phoneNumber, parkName, (endTime - startTime), e);
            return false;
        }
    }

    /**
     * 检查手机号是否在月票系统中（主要方法）
     * 
     * @param phoneNumber 手机号
     * @return true-找到业主身份，false-未找到
     */
    @Cacheable(value = "ownerVerification", key = "#phoneNumber")
    public boolean isOwnerByPhoneNumber(String phoneNumber) {
        // 使用优化版本
        return isOwnerByPhoneNumberOptimized(phoneNumber);
    }
    
    /**
     * 检查手机号是否在指定停车场的月票系统中（性能最优版本）
     * 
     * @param phoneNumber 手机号
     * @param parkName 停车场名称
     * @return true-找到业主身份，false-未找到
     */
    @Cacheable(value = "ownerVerificationByPark", key = "#phoneNumber + '_' + #parkName")
    public boolean isOwnerByPhoneNumberInPark(String phoneNumber, String parkName) {
        long startTime = System.currentTimeMillis();
        log.info("🎯 开始验证业主身份(指定停车场) - 手机号: [{}], 停车场: [{}]", phoneNumber, parkName);
        
        try {
            // 获取停车场代码
            String parkCode = PARK_CODE_MAP.get(parkName);
            if (parkCode == null) {
                log.warn("⚠️ 未知停车场名称: [{}]，使用默认查询方式", parkName);
                return isOwnerByPhoneNumberOptimized(phoneNumber);
            }
            
            // 只在指定停车场中搜索
            boolean found = searchOwnerInPark(phoneNumber, parkCode);
            
            long endTime = System.currentTimeMillis();
            log.info("🚀 指定停车场验证完成 - 手机号: [{}], 停车场: [{}], 结果: {}, 耗时: {}ms", 
                phoneNumber, parkName, found ? "✅业主" : "❌非业主", (endTime - startTime));
            
            return found;
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("❌ 指定停车场验证失败，手机号: [{}], 停车场: [{}], 耗时: {}ms", 
                phoneNumber, parkName, (endTime - startTime), e);
            return false;
        }
    }
    
    /**
     * 检查手机号是否在指定停车场代码的月票系统中
     * 
     * @param phoneNumber 手机号
     * @param parkCode 停车场代码
     * @return true-找到业主身份，false-未找到
     */
    public boolean isOwnerByPhoneNumberInParkCode(String phoneNumber, String parkCode) {
        long startTime = System.currentTimeMillis();
        log.info("🎯 开始验证业主身份(停车场代码) - 手机号: [{}], 停车场代码: [{}]", phoneNumber, parkCode);
        
        try {
            boolean found = searchOwnerInPark(phoneNumber, parkCode);
            
            long endTime = System.currentTimeMillis();
            log.info("🚀 停车场代码验证完成 - 手机号: [{}], 停车场代码: [{}], 结果: {}, 耗时: {}ms", 
                phoneNumber, parkCode, found ? "✅业主" : "❌非业主", (endTime - startTime));
            
            return found;
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("❌ 停车场代码验证失败，手机号: [{}], 停车场代码: [{}], 耗时: {}ms", 
                phoneNumber, parkCode, (endTime - startTime), e);
            return false;
        }
    }
    
    /**
     * 优化版本：并行查询业主身份，支持早期终止
     * 
     * @param phoneNumber 手机号
     * @return true-找到业主身份，false-未找到
     */
    public boolean isOwnerByPhoneNumberOptimized(String phoneNumber) {
        long startTime = System.currentTimeMillis();
        log.info("🚀 开始优化版业主身份验证，手机号: [{}]", phoneNumber);
        
        try {
            // 使用并行流处理多个停车场，找到第一个匹配就返回
            boolean found = DEFAULT_PARK_CODES.parallelStream()
                .anyMatch(parkCode -> {
                    log.info("🔍 并行查询停车场: [{}]", parkCode);
                    return searchOwnerInPark(phoneNumber, parkCode);
                });
            
            long endTime = System.currentTimeMillis();
            log.info("⚡ 优化版业主身份验证完成 - 手机号: [{}], 结果: {}, 耗时: {}ms", 
                phoneNumber, found ? "✅业主" : "❌非业主", (endTime - startTime));
            
            return found;
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("❌ 优化版业主身份验证失败，手机号: [{}], 耗时: {}ms", 
                phoneNumber, (endTime - startTime), e);
            return false;
        }
    }
    
    /**
     * 在指定停车场中搜索业主，支持早期终止
     * 
     * @param phoneNumber 手机号
     * @param parkCode 停车场代码
     * @return true-找到，false-未找到
     */
    private boolean searchOwnerInPark(String phoneNumber, String parkCode) {
        int pageNum = 1;
        int pageSize = 100; // 外部API最大限制是100
        int maxPages = 50;   // 增加最大页数，因为单页数据更少了
        long searchStartTime = System.currentTimeMillis();
        
        log.info("🔍 开始在停车场 [{}] 中搜索手机号: [{}]", parkCode, phoneNumber);
        
        while (pageNum <= maxPages) {
            try {
                // 检查超时（单个停车场最多查询5秒）
                long currentTime = System.currentTimeMillis();
                if (currentTime - searchStartTime > 5000) {
                    log.warn("⏰ 停车场 [{}] 查询超时(5秒)，终止搜索", parkCode);
                    break;
                }
                
                log.debug("📄 查询停车场 [{}] 第 {} 页", parkCode, pageNum);
                
                // 构建请求参数
                HashMap<String, Object> params = new HashMap<>();
                params.put("parkCodeList", Arrays.asList(parkCode));
                params.put("pageNum", pageNum);
                params.put("pageSize", pageSize);
                params.put("validStatus", 1);
                
                // 调用外部API，设置较短超时
                JSONObject response = aikeConfig.downHandler(
                    AIKEConfig.AK_URL, 
                    AIKEConfig.AK_KEY, 
                    AIKEConfig.AK_SECRET, 
                    "getOnlineMonthTicketList", 
                    params
                );
                
                // 检查API响应
                if (response != null && response.containsKey("code") && !response.getString("code").equals("0")) {
                    log.warn("⚠️ 停车场 [{}] API返回错误，跳过: {}", parkCode, response.getString("message"));
                    break;
                }
                
                // 解析当前页数据
                List<Map<String, Object>> pageTickets = parseTicketsFromResponse(response);
                
                if (pageTickets == null || pageTickets.isEmpty()) {
                    log.debug("📄 停车场 [{}] 第 {} 页无数据，结束查询", parkCode, pageNum);
                    break;
                }
                
                // 在当前页中搜索手机号
                for (Map<String, Object> ticket : pageTickets) {
                    String ticketPhone = (String) ticket.get("userPhone");
                    if (phoneNumber.equals(ticketPhone)) {
                        long searchEndTime = System.currentTimeMillis();
                        log.info("🎯 在停车场 [{}] 第 {} 页找到匹配记录！手机号: [{}], 姓名: [{}], 车牌: [{}], 耗时: {}ms", 
                            parkCode, pageNum, ticketPhone, ticket.get("userName"), ticket.get("carNo"),
                            (searchEndTime - searchStartTime));
                        return true; // 找到就立即返回
                    }
                }
                
                log.debug("📄 停车场 [{}] 第 {} 页查询完成，本页 {} 条记录，未找到匹配", 
                    parkCode, pageNum, pageTickets.size());
                
                // 如果当前页数据少于分页大小，说明是最后一页
                if (pageTickets.size() < pageSize) {
                    log.debug("📄 停车场 [{}] 已到最后一页，结束查询", parkCode);
                    break;
                }
                
                pageNum++;
                
                // 短暂休眠，避免过于频繁的API调用
                Thread.sleep(20);
                
            } catch (InterruptedException e) {
                log.warn("⚠️ 停车场 [{}] 查询被中断", parkCode);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("❌ 停车场 [{}] 第 {} 页查询失败: {}", parkCode, pageNum, e.getMessage());
                break;
            }
        }
        
        long searchEndTime = System.currentTimeMillis();
        log.info("❌ 停车场 [{}] 查询完成，未找到匹配记录，耗时: {}ms", parkCode, (searchEndTime - searchStartTime));
        return false;
    }

    /**
     * 获取业主详细信息
     * 
     * @param phoneNumber 手机号
     * @return 业主信息Map，如果未找到返回null
     */
    @Cacheable(value = "ownerDetails", key = "#phoneNumber")
    public Map<String, Object> getOwnerDetailsByPhone(String phoneNumber) {
        // 使用优化版本
        return getOwnerDetailsByPhoneOptimized(phoneNumber);
    }
    
    /**
     * 优化版本：获取业主详细信息，支持早期终止
     */
    public Map<String, Object> getOwnerDetailsByPhoneOptimized(String phoneNumber) {
        long startTime = System.currentTimeMillis();
        log.info("🚀 开始优化版获取业主详细信息，手机号: [{}]", phoneNumber);
        
        try {
            // 使用并行流查找业主信息
            Optional<Map<String, Object>> ownerInfo = DEFAULT_PARK_CODES.parallelStream()
                .map(parkCode -> searchOwnerDetailsInPark(phoneNumber, parkCode))
                .filter(Objects::nonNull)
                .findFirst();
            
            long endTime = System.currentTimeMillis();
            
            if (ownerInfo.isPresent()) {
                Map<String, Object> result = ownerInfo.get();
                log.info("✅ 优化版获取业主信息成功 - 姓名: [{}], 车牌: [{}], 耗时: {}ms", 
                    result.get("ownername"), result.get("carno"), (endTime - startTime));
                return result;
            } else {
                log.info("❌ 优化版未找到业主信息，手机号: [{}], 耗时: {}ms", phoneNumber, (endTime - startTime));
                return null;
            }
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("❌ 优化版获取业主信息失败，手机号: [{}], 耗时: {}ms", 
                phoneNumber, (endTime - startTime), e);
            return null;
        }
    }
    
    /**
     * 在指定停车场中搜索业主详细信息
     */
    private Map<String, Object> searchOwnerDetailsInPark(String phoneNumber, String parkCode) {
        // 复用搜索逻辑，但返回详细信息
        int pageNum = 1;
        int pageSize = 100; // 外部API最大限制是100
        int maxPages = 50;
        long searchStartTime = System.currentTimeMillis();
        
        while (pageNum <= maxPages) {
            try {
                // 检查超时
                if (System.currentTimeMillis() - searchStartTime > 5000) {
                    break;
                }
                
                HashMap<String, Object> params = new HashMap<>();
                params.put("parkCodeList", Arrays.asList(parkCode));
                params.put("pageNum", pageNum);
                params.put("pageSize", pageSize);
                params.put("validStatus", 1);
                
                JSONObject response = aikeConfig.downHandler(
                    AIKEConfig.AK_URL, 
                    AIKEConfig.AK_KEY, 
                    AIKEConfig.AK_SECRET, 
                    "getOnlineMonthTicketList", 
                    params
                );
                
                if (response != null && response.containsKey("code") && !response.getString("code").equals("0")) {
                    break;
                }
                
                List<Map<String, Object>> pageTickets = parseTicketsFromResponse(response);
                
                if (pageTickets == null || pageTickets.isEmpty()) {
                    break;
                }
                
                // 搜索匹配的业主
                for (Map<String, Object> ticket : pageTickets) {
                    String ticketPhone = (String) ticket.get("userPhone");
                    if (phoneNumber.equals(ticketPhone)) {
                        // 找到匹配，构建业主信息并返回
                        Map<String, Object> ownerInfo = buildOwnerInfo(ticket);
                        ownerInfo.put("parkCode", parkCode);
                        return ownerInfo;
                    }
                }
                
                if (pageTickets.size() < pageSize) {
                    break;
                }
                
                pageNum++;
                Thread.sleep(20);
                
            } catch (Exception e) {
                log.error("❌ 停车场 [{}] 详细信息查询失败: {}", parkCode, e.getMessage());
                break;
            }
        }
        
        return null;
    }

    /**
     * 获取指定停车场的业主信息
     * 
     * @param phoneNumber 手机号
     * @param parkName 停车场名称
     * @return 业主信息
     */
    public Map<String, Object> getOwnerDetailsByPark(String phoneNumber, String parkName) {
        String parkCode = PARK_CODE_MAP.get(parkName);
        if (parkCode == null) {
            log.warn("⚠️ 未知停车场名称: [{}]", parkName);
            return null;
        }
        
        try {
            List<Map<String, Object>> tickets = getOnlineMonthTicketsByPark(parkCode);
            
            Optional<Map<String, Object>> ownerTicket = tickets.stream()
                .filter(ticket -> phoneNumber.equals(ticket.get("userPhone")))
                .findFirst();
                
            if (ownerTicket.isPresent()) {
                Map<String, Object> ownerInfo = buildOwnerInfo(ownerTicket.get());
                ownerInfo.put("parkName", parkName);
                ownerInfo.put("parkCode", parkCode);
                return ownerInfo;
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("❌ 获取指定停车场业主信息失败，手机号: [{}], 停车场: [{}]", phoneNumber, parkName, e);
            return null;
        }
    }

    /**
     * 获取所有停车场的月票数据
     * 复用现有的分页逻辑
     */
    private List<Map<String, Object>> getAllOnlineMonthTickets() {
        List<Map<String, Object>> allTickets = new ArrayList<>();
        log.info("🏢 开始获取所有停车场的月票数据，停车场数量: {}", DEFAULT_PARK_CODES.size());
        log.info("📋 停车场列表: {}", DEFAULT_PARK_CODES);
        
        // 遍历所有停车场
        for (int i = 0; i < DEFAULT_PARK_CODES.size(); i++) {
            String parkCode = DEFAULT_PARK_CODES.get(i);
            
            try {
                log.info("🔄 正在处理第 {}/{} 个停车场: [{}]", (i + 1), DEFAULT_PARK_CODES.size(), parkCode);
                
                List<Map<String, Object>> parkTickets = getOnlineMonthTicketsByPark(parkCode);
                allTickets.addAll(parkTickets);
                
                log.info("✅ 停车场 [{}] 获取完成，本场 {} 条数据，总计 {} 条数据", 
                    parkCode, parkTickets.size(), allTickets.size());
                
            } catch (Exception e) {
                log.error("❌ 获取停车场 [{}] 月票数据失败，错误类型: {}, 错误信息: {}", 
                    parkCode, e.getClass().getSimpleName(), e.getMessage(), e);
                // 继续处理其他停车场，不中断整个流程
            }
        }
        
        log.info("🎯 所有停车场月票数据获取完成，总共获取 {} 条月票数据", allTickets.size());
        return allTickets;
    }

    /**
     * 获取指定停车场的月票数据
     * 基于现有Controller的分页逻辑
     */
    private List<Map<String, Object>> getOnlineMonthTicketsByPark(String parkCode) {
        List<Map<String, Object>> allTickets = new ArrayList<>();
        int pageNum = 1;
        int pageSize = 100;
        boolean hasMore = true;
        int maxPages = 50; // 防止无限循环
        
        log.info("🔄 开始获取停车场 [{}] 的月票数据", parkCode);
        
        while (hasMore && pageNum <= maxPages) {
            try {
                log.info("📄 正在获取停车场 [{}] 第 {} 页数据", parkCode, pageNum);
                
                // 构建请求参数（复用现有逻辑）
                HashMap<String, Object> params = new HashMap<>();
                params.put("parkCodeList", Arrays.asList(parkCode));
                params.put("pageNum", pageNum);
                params.put("pageSize", pageSize);
                params.put("validStatus", 1); // 1-生效状态
                
                log.debug("🔧 请求参数: {}", params);
                
                // 调用外部API（复用现有的aikeConfig）
                log.info("🌐 调用外部API获取月票列表 - 停车场: {}, 页码: {}", parkCode, pageNum);
                JSONObject response = aikeConfig.downHandler(
                    AIKEConfig.AK_URL, 
                    AIKEConfig.AK_KEY, 
                    AIKEConfig.AK_SECRET, 
                    "getOnlineMonthTicketList", 
                    params
                );
                
                log.info("📥 外部API响应 - 停车场: {}, 页码: {}, 响应: {}", parkCode, pageNum, response != null ? "成功" : "失败");
                
                // 检查API响应是否有错误
                if (response != null && response.containsKey("code") && !response.getString("code").equals("0")) {
                    log.error("❌ 外部API返回错误 - 停车场: {}, 页码: {}, 错误码: {}, 错误信息: {}", 
                        parkCode, pageNum, response.getString("code"), response.getString("message"));
                    hasMore = false;
                    continue;
                }
                
                // 解析响应数据
                List<Map<String, Object>> pageTickets = parseTicketsFromResponse(response);
                
                if (pageTickets != null && !pageTickets.isEmpty()) {
                    allTickets.addAll(pageTickets);
                    log.info("✅ 成功获取停车场 [{}] 第 {} 页数据，本页 {} 条，累计 {} 条", 
                        parkCode, pageNum, pageTickets.size(), allTickets.size());
                    hasMore = pageTickets.size() >= pageSize;
                    pageNum++;
                } else {
                    log.info("📄 停车场 [{}] 第 {} 页无数据，结束分页查询", parkCode, pageNum);
                    hasMore = false;
                }
                
                // 避免过于频繁的API调用
                Thread.sleep(50);
                
            } catch (InterruptedException e) {
                log.warn("⚠️ 线程中断，停止获取数据");
                Thread.currentThread().interrupt();
                hasMore = false;
            } catch (Exception e) {
                log.error("❌ 获取停车场 [{}] 第 {} 页数据失败，错误类型: {}, 错误信息: {}", 
                    parkCode, pageNum, e.getClass().getSimpleName(), e.getMessage(), e);
                hasMore = false;
            }
        }
        
        log.info("✅ 停车场 [{}] 月票数据获取完成，总共获取 {} 条数据", parkCode, allTickets.size());
        return allTickets;
    }

    /**
     * 解析API响应数据
     * 基于现有Controller的解析逻辑
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseTicketsFromResponse(JSONObject response) {
        try {
            if (response == null) {
                log.warn("⚠️ API响应为null，返回空列表");
                return new ArrayList<>();
            }
            
            log.debug("🔧 开始解析API响应数据...");
            
            // 根据现有响应结构解析
            Object dataObj = response.get("data");
            if (dataObj instanceof JSONObject) {
                JSONObject data = (JSONObject) dataObj;
                Object recordListObj = data.get("recordList");
                
                if (recordListObj instanceof JSONArray) {
                    JSONArray recordList = (JSONArray) recordListObj;
                    
                    List<Map<String, Object>> tickets = new ArrayList<>();
                    for (int i = 0; i < recordList.size(); i++) {
                        JSONObject record = recordList.getJSONObject(i);
                        tickets.add(record);
                    }
                    
                    log.debug("✅ 成功解析API响应，获得 {} 条记录", tickets.size());
                    
                    // 打印几条样本数据用于调试
                    if (!tickets.isEmpty() && tickets.size() <= 3) {
                        log.debug("📄 解析的记录样本: {}", tickets.get(0));
                    } else if (tickets.size() > 3) {
                        log.debug("📄 解析的记录样本(前3条): {}", tickets.subList(0, 3));
                    }
                    
                    return tickets;
                } else {
                    log.warn("⚠️ API响应中recordList不是JSONArray类型: {}", recordListObj != null ? recordListObj.getClass().getSimpleName() : "null");
                }
            } else {
                log.warn("⚠️ API响应中data不是JSONObject类型: {}", dataObj != null ? dataObj.getClass().getSimpleName() : "null");
            }
            
            log.warn("⚠️ API响应结构不符合预期，返回空列表。响应内容: {}", response);
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("❌ 解析API响应数据失败，错误类型: {}, 错误信息: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建业主信息对象
     * 基于现有的字段映射逻辑
     */
    private Map<String, Object> buildOwnerInfo(Map<String, Object> ticket) {
        Map<String, Object> ownerInfo = new HashMap<>();
        
        // 🎯 优先保存userName字段（用于显示业主姓名）
        String userName = (String) ticket.get("userName");
        ownerInfo.put("userName", userName);  // 明确保存userName字段
        ownerInfo.put("ownername", userName); // 兼容现有字段
        
        // 基本信息
        ownerInfo.put("ownerphone", ticket.get("userPhone"));
        ownerInfo.put("carno", ticket.get("carNo"));
        ownerInfo.put("userPhone", ticket.get("userPhone")); // 保持原字段名
        ownerInfo.put("carNo", ticket.get("carNo"));         // 保持原字段名
        
        // 月票信息
        ownerInfo.put("ticketName", ticket.get("ticketName"));
        ownerInfo.put("validStatus", ticket.get("validStatus"));
        ownerInfo.put("createTime", ticket.get("createTime"));
        ownerInfo.put("createBy", ticket.get("createBy"));
        
        // 时间相关字段
        ownerInfo.put("startTime", ticket.get("startTime"));
        ownerInfo.put("endTime", ticket.get("endTime"));
        ownerInfo.put("updateTime", ticket.get("updateTime"));
        ownerInfo.put("updateBy", ticket.get("updateBy"));
        
        // 标记数据来源
        ownerInfo.put("source", "external_api");
        ownerInfo.put("needSync", true);
        
        // 保存原始数据（包含所有字段）
        ownerInfo.put("originalData", ticket);
        
        // 🔍 添加调试日志，确认userName字段
        log.debug("🏗️ 构建业主信息 - userName: [{}], 原始数据keys: {}", 
            userName, ticket.keySet());
        
        return ownerInfo;
    }

    /**
     * 批量验证多个手机号
     * 用于批量处理场景
     */
    public Map<String, Boolean> batchVerifyOwners(List<String> phoneNumbers) {
        log.info("🔍 批量验证业主身份，数量: {}", phoneNumbers.size());
        
        Map<String, Boolean> results = new HashMap<>();
        
        try {
            // 一次性获取所有月票数据
            List<Map<String, Object>> allTickets = getAllOnlineMonthTickets();
            Set<String> ownerPhones = allTickets.stream()
                .map(ticket -> (String) ticket.get("userPhone"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            // 批量判断
            for (String phone : phoneNumbers) {
                results.put(phone, ownerPhones.contains(phone));
            }
            
            log.info("✅ 批量验证完成，验证数量: {}, 业主数量: {}", 
                phoneNumbers.size(), 
                results.values().stream().mapToInt(b -> b ? 1 : 0).sum());
                
        } catch (Exception e) {
            log.error("❌ 批量验证失败", e);
            // 返回所有false的结果
            for (String phone : phoneNumbers) {
                results.put(phone, false);
            }
        }
        
        return results;
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        try {
            List<Map<String, Object>> allTickets = getAllOnlineMonthTickets();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTickets", allTickets.size());
            stats.put("uniqueOwners", allTickets.stream()
                .map(ticket -> ticket.get("userPhone"))
                .filter(Objects::nonNull)
                .distinct()
                .count());
            stats.put("updateTime", new Date());
            
            return stats;
            
        } catch (Exception e) {
            log.error("❌ 获取统计信息失败", e);
            return new HashMap<>();
        }
    }

} 