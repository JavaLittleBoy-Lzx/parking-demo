package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.dto.MonthTicketVehicleDTO;
import com.parkingmanage.dto.SearchResult;
import com.parkingmanage.dto.SearchCondition;
import com.parkingmanage.entity.MonthTick;
import com.parkingmanage.entity.Appointment;
import com.parkingmanage.entity.Violations;
import com.parkingmanage.entity.YardInfo;
import com.parkingmanage.mapper.MonthTicketMapper;
import com.parkingmanage.mapper.AppointmentMapper;
import com.parkingmanage.mapper.ViolationsMapper;
import com.parkingmanage.service.MonthTicketSearchService;
import com.parkingmanage.utils.PageUtils;
import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.common.config.AIKEConfig;
import com.parkingmanage.controller.MonthTicketController;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 月票车辆搜索服务实现类
 */
@Service
public class MonthTicketSearchServiceImpl implements MonthTicketSearchService {

    // 默认查询的停车场代码
    private static final List<String> DEFAULT_PARK_CODES = Arrays.asList("2KST9MNP", "2KUG6XLU");

    @Autowired
    private MonthTicketMapper monthTicketMapper;
    
    @Autowired
    private AppointmentMapper appointmentMapper;
    
    @Autowired
    private ViolationsMapper violationsMapper;
    
    @Autowired
    private AIKEConfig aikeConfig;
    
    @Autowired
    private MonthTicketController monthTicketController;
    
    @Autowired
    private com.parkingmanage.service.YardInfoService yardInfoService;

    // 车牌号正则表达式
    private static final Pattern PLATE_PATTERN = Pattern.compile("^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领A-Z]{1}[A-Z]{1}[A-Z0-9]{4}[A-Z0-9挂学警港澳]{1}$");
    
    // 手机号正则表达式
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    
    // 添加缓存相关字段
    private final Map<String, CacheEntry> apiDataCache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRE_TIME = 5 * 60 * 1000; // 5分钟缓存过期时间
    
    // 线程池用于并发API调用
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    // 缓存条目类
    private static class CacheEntry {
        private final List<MonthTick> data;
        private final long timestamp;
        
        public CacheEntry(List<MonthTick> data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRE_TIME;
        }
        
        public List<MonthTick> getData() {
            return data;
        }
    }

    @Override
    public SearchResult<MonthTicketVehicleDTO> smartSearch(String keyword, String parkName, Boolean onlyInPark, Integer page, Integer size) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 🚀 性能优化：限制单次查询的数据量
            int optimizedSize = Math.min(size != null ? size : 20, 100); // 最多返回100条
            System.out.println("🔧 [性能优化] 原始请求size: " + size + ", 优化后size: " + optimizedSize);
            
            // 1. 构建搜索条件
            SearchCondition condition = buildSearchCondition(keyword, parkName, onlyInPark, page, optimizedSize);
            
            // 🚀 快速模式：对于简单查询，直接使用本地数据库
            if (shouldUseFastMode(keyword, size)) {
                System.out.println("⚡ [快速模式] 使用本地数据库查询");
                SearchResult<MonthTicketVehicleDTO> result = searchByConditionFast(condition);
                result.setSearchTime(System.currentTimeMillis() - startTime);
                result.setKeyword(keyword);
                return result;
            }
            
            // 2. 执行搜索
            SearchResult<MonthTicketVehicleDTO> result = searchByCondition(condition);
            
            // 3. 设置搜索耗时
            long searchTime = System.currentTimeMillis() - startTime;
            result.setSearchTime(searchTime);
            result.setKeyword(keyword);
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new SearchResult<>(new ArrayList<>(), 0L, page, size, 
                System.currentTimeMillis() - startTime, keyword);
        }
    }
    
    /**
     * 🚀 判断是否应该使用快速模式
     */
    private boolean shouldUseFastMode(String keyword, Integer size) {
        // 当请求大量数据时，使用本地数据库以提高响应速度
        if (size != null && size > 50) {
            return true;
        }
        
        // 对于短关键词（可能匹配很多结果），使用快速模式
        if (keyword == null || keyword.trim().length() <= 2) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 🚀 快速搜索模式 - 主要使用本地数据库
     */
    private SearchResult<MonthTicketVehicleDTO> searchByConditionFast(SearchCondition condition) {
        System.out.println("⚡ [快速搜索] 开始本地数据库查询");
        
        // 直接使用本地数据库查询，避免外部API调用
        List<MonthTick> monthTickets = searchMonthTicketsFromDB(condition);
        System.out.println("📊 [快速搜索] 获取到月票记录数: " + monthTickets.size());
        
        // 2. 拆分为单个车辆记录
        List<MonthTicketVehicleDTO> vehicles = new ArrayList<>();
        int emptyCarNoCount = 0;
        int validCarNoCount = 0;
        
        for (MonthTick ticket : monthTickets) {
            if (!StringUtils.hasText(ticket.getCarNo())) {
                emptyCarNoCount++;
                continue;
            }
            validCarNoCount++;
            vehicles.addAll(splitMonthTicketToVehicles(ticket));
            
            // 🚀 性能优化：限制处理的记录数
            if (vehicles.size() >= condition.getSize() * 10) {
                System.out.println("⚡ [快速搜索] 已处理足够数据，停止转换");
                break;
            }
        }
        
        System.out.println("📈 [快速搜索] 统计信息:");
        System.out.println("  - 总月票记录数: " + monthTickets.size());
        System.out.println("  - 空车牌号记录数: " + emptyCarNoCount);
        System.out.println("  - 有效车牌号记录数: " + validCarNoCount);
        System.out.println("🚗 拆分后的车辆总数: " + vehicles.size());
        
        // 3. 过滤匹配的记录
        List<MonthTicketVehicleDTO> filteredVehicles = vehicles.stream()
            .filter(vehicle -> {
                boolean matches = matchesKeyword(vehicle, condition.getKeyword());
                return matches;
            })
            .collect(Collectors.toList());
            
        System.out.println("🔍 [快速搜索] 关键词: " + condition.getKeyword() + ", 过滤后车辆数: " + filteredVehicles.size());
        
        // 4. 按相关度排序
        filteredVehicles.sort(this::compareRelevance);
        
        // 5. 分页处理
        Page<MonthTicketVehicleDTO> page = PageUtils.getPage(filteredVehicles, condition.getPage(), condition.getSize());
        return new SearchResult<>(page.getRecords(), page.getTotal(), Math.toIntExact(page.getCurrent()), Math.toIntExact(page.getSize()));
    }

    @Override
    public SearchResult<MonthTicketVehicleDTO> searchByCondition(SearchCondition condition) {
        // 1. 执行多字段搜索
        List<MonthTick> monthTickets = searchMonthTickets(condition);
        System.out.println("📊 获取到月票记录数: " + monthTickets.size());
        
        // 2. 拆分为单个车辆记录
        List<MonthTicketVehicleDTO> vehicles = new ArrayList<>();
        int emptyCarNoCount = 0;
        int validCarNoCount = 0;
        
        for (MonthTick ticket : monthTickets) {
            if (!StringUtils.hasText(ticket.getCarNo())) {
                emptyCarNoCount++;
                continue;
            }
            validCarNoCount++;
            vehicles.addAll(splitMonthTicketToVehicles(ticket));
        }
        
        System.out.println("📈 统计信息:");
        System.out.println("  - 总月票记录数: " + monthTickets.size());
        System.out.println("  - 空车牌号记录数: " + emptyCarNoCount);
        System.out.println("  - 有效车牌号记录数: " + validCarNoCount);
        System.out.println("🚗 拆分后的车辆总数: " + vehicles.size());
        if (!vehicles.isEmpty()) {
            System.out.println("🚗 第一个车辆示例: " + vehicles.get(0).getPlateNumber() + " - " + vehicles.get(0).getOwnerName());
        }
        
        // 3. 过滤匹配的记录
        List<MonthTicketVehicleDTO> filteredVehicles = vehicles.stream()
            .filter(vehicle -> {
                boolean matches = matchesKeyword(vehicle, condition.getKeyword());
                if (matches) {
                    System.out.println("✅ 匹配车辆: " + vehicle.getPlateNumber() + " - " + vehicle.getOwnerName());
                }
                return matches;
            })
            .collect(Collectors.toList());
            
        System.out.println("🔍 关键词: " + condition.getKeyword() + ", 过滤后车辆数: " + filteredVehicles.size());
        
        // 4. 查询在场状态（如果需要）- 优化：不自动查询在场状态，避免大量API调用
        // 注释掉自动查询在场状态的逻辑，改为用户选择具体车牌后再查询
        // if (condition.getOnlyInPark() != null && condition.getOnlyInPark()) {
        //     filteredVehicles = filterInParkVehicles(filteredVehicles, condition.getParkCode());
        // }
        
        // 如果用户确实需要只看在场车辆，先返回所有车辆，让前端按需查询在场状态
        System.out.println("💡 优化提示: 已跳过批量在场状态查询，提高查询速度。用户可选择具体车牌后查询在场状态。");
        
        // 5. 按相关度排序
        filteredVehicles.sort(this::compareRelevance);
        
        // 6. 分页处理
        Page<MonthTicketVehicleDTO> page = PageUtils.getPage(filteredVehicles, condition.getPage(), condition.getSize());
        return new SearchResult<>(page.getRecords(), page.getTotal(), Math.toIntExact(page.getCurrent()), Math.toIntExact(page.getSize()));
    }

    @Override
    public MonthTicketVehicleDTO getVehicleDetails(String plateNumber) {
        if (!StringUtils.hasText(plateNumber)) {
            return null;
        }
        
        // 查询包含该车牌号的月票记录
        QueryWrapper<MonthTick> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("car_no", plateNumber);
        
        List<MonthTick> monthTickets = monthTicketMapper.selectList(queryWrapper);
        
        for (MonthTick ticket : monthTickets) {
            List<MonthTicketVehicleDTO> vehicles = splitMonthTicketToVehicles(ticket);
            for (MonthTicketVehicleDTO vehicle : vehicles) {
                if (plateNumber.equals(vehicle.getPlateNumber())) {
                    // 填充额外信息（不包括在场状态）
                    enrichVehicleInfo(vehicle);
                    return vehicle;
                }
            }
        }
        
        return null;
    }

    /**
     * 获取车辆详细信息（包含在场状态）
     * 这个方法专门用于用户选择具体车牌后查询完整信息
     */
    public MonthTicketVehicleDTO getVehicleDetailsWithParkStatus(String plateNumber, String parkCode) {
        System.out.println("🔍 查询车辆详细信息（含在场状态）: " + plateNumber);
        
        // 先获取基本信息
        MonthTicketVehicleDTO vehicle = getVehicleDetails(plateNumber);
        if (vehicle == null) {
            return null;
        }
        
        // 查询在场状态
        try {
            Boolean isInPark = checkVehicleInPark(plateNumber, parkCode);
            vehicle.setIsInPark(isInPark);
            System.out.println("✅ 车辆 " + plateNumber + " 在场状态: " + (isInPark ? "在场" : "不在场"));
        } catch (Exception e) {
            System.err.println("❌ 查询车辆在场状态失败: " + e.getMessage());
            vehicle.setIsInPark(null);
        }
        
        return vehicle;
    }

    @Override
    public List<MonthTicketVehicleDTO> getPlateSuggestions(String keyword, String parkName, Integer limit) {
        if (!StringUtils.hasText(keyword)) {
            return new ArrayList<>();
        }
        
        QueryWrapper<MonthTick> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("car_no", keyword);
        
        if (StringUtils.hasText(parkName)) {
            queryWrapper.eq("park_name", parkName);
        }
        
        queryWrapper.last("LIMIT " + (limit != null ? limit : 10));
        
        List<MonthTick> monthTickets = monthTicketMapper.selectList(queryWrapper);
        
        List<MonthTicketVehicleDTO> suggestions = new ArrayList<>();
        for (MonthTick ticket : monthTickets) {
            List<MonthTicketVehicleDTO> vehicles = splitMonthTicketToVehicles(ticket);
            suggestions.addAll(vehicles.stream()
                .filter(v -> v.getPlateNumber().contains(keyword))
                .collect(Collectors.toList()));
        }
        
        return suggestions.stream()
            .limit(limit != null ? limit : 10)
            .collect(Collectors.toList());
    }

    @Override
    public Boolean checkVehicleInPark(String plateNumber, String parkCode) {
        if (!StringUtils.hasText(plateNumber)) {
            return false;
        }
        
        try {
            // 构建请求参数，参考VehicleReservationController的参数格式
            HashMap<String, Object> params = new HashMap<>();
            if (StringUtils.hasText(parkCode)) {
                // 如果传入的是停车场名称，先转换为车场编码
                List<String> yardCodes = yardInfoService.yardCode(parkCode);
                if (yardCodes != null && !yardCodes.isEmpty()) {
                    // 找到对应的车场编码
                    params.put("parkCodeList", yardCodes);
                    System.out.println("信息：根据停车场名称 '" + parkCode + "' 找到车场编码: " + yardCodes);
                } else {
                    // 如果没找到，可能传入的就是车场编码，直接使用
                    params.put("parkCodeList", Arrays.asList(parkCode));
                    System.out.println("信息：直接使用传入的车场编码: " + parkCode);
                }
            } else {
                // 当没有指定停车场代码时，查询所有yardinfo表中的车场编码
                List<YardInfo> allYards = yardInfoService.yardNameList();
                List<String> allParkCodes = allYards.stream()
                    .map(YardInfo::getYardCode)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
                
                if (allParkCodes.isEmpty()) {
                    // 如果没有找到任何车场编码，使用默认的
                    params.put("parkCodeList", DEFAULT_PARK_CODES);
                    System.out.println("警告：未找到任何车场编码，使用默认停车场代码查询在场车辆 - 车牌号: " + plateNumber);
                } else {
                    params.put("parkCodeList", allParkCodes);
                    System.out.println("信息：未指定停车场代码，使用所有车场编码查询在场车辆 - 车牌号: " + plateNumber + ", 车场数量: " + allParkCodes.size());
                }
            }
            // 设置时间范围，查询今天的在场车辆（使用API要求的yyyyMMddHHmmss格式）
            String todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            
            params.put("enterTimeFrom", todayStart);
            params.put("enterTimeTo", currentTime);
            params.put("pageNum", 1);
            params.put("pageSize", 1000); // 设置较大的页面大小以获取所有在场车辆
            
            // 添加车牌号作为查询参数
            params.put("carNo", plateNumber);
            
            // 调用外部API查询在场车辆状态
            System.out.println("🔍 查询车辆在场状态 - 车牌号: " + plateNumber + ", 请求参数: " + params);
            JSONObject response = aikeConfig.downHandler(
                AIKEConfig.AK_URL, 
                AIKEConfig.AK_KEY, 
                AIKEConfig.AK_SECRET, 
                "getParkOnSiteCar", 
                params
            );
            
            if (response != null && response.containsKey("code") && "0".equals(response.getString("code"))) {
                // 解析响应数据
                JSONObject data = response.getJSONObject("data");
                if (data != null && data.containsKey("list")) {
                    List<Object> carList = data.getJSONArray("list").toJavaList(Object.class);
                    System.out.println("✅ API查询成功，返回 " + carList.size() + " 条在场车辆记录");
                    
                    // 检查车牌号是否在在场车辆列表中
                    for (Object car : carList) {
                        if (car instanceof Map) {
                            Map<String, Object> carInfo = (Map<String, Object>) car;
                            String carNo = (String) carInfo.get("carNo");
                            if (plateNumber.equals(carNo)) {
                                System.out.println("🎯 找到匹配车辆: " + plateNumber + " 在场");
                                return true;
                            }
                        }
                    }
                    System.out.println("❌ 车辆 " + plateNumber + " 不在场");
                } else {
                    System.out.println("⚠️ API返回数据中没有车辆列表");
                }
                return false;
            } else {
                // API调用失败或返回错误，记录详细日志并返回false
                String errorMsg = response != null ? response.getString("message") : "API响应为空";
                String errorCode = response != null ? response.getString("code") : "无";
                System.err.println("查询车辆在场状态失败: " + errorMsg + ", 错误代码: " + errorCode);
                System.err.println("请求参数: " + params);
                if (response != null) {
                    System.err.println("完整响应: " + response.toJSONString());
                }
                return false;
            }
        } catch (Exception e) {
            // 异常情况下返回false，避免影响主业务流程
            e.printStackTrace();
            System.err.println("查询车辆在场状态异常: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Integer getAppointmentCount(String plateNumber) {
        if (!StringUtils.hasText(plateNumber)) {
            return 0;
        }
        
        QueryWrapper<Appointment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("platenumber", plateNumber);
        
        return Math.toIntExact(appointmentMapper.selectCount(queryWrapper));
    }

    @Override
    public Integer getViolationCount(String plateNumber) {
        if (!StringUtils.hasText(plateNumber)) {
            return 0;
        }
        
        QueryWrapper<Violations> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("platenumber", plateNumber);
        
        return Math.toIntExact(violationsMapper.selectCount(queryWrapper));
    }

    /**
     * 构建搜索条件
     */
    private SearchCondition buildSearchCondition(String keyword, String parkName, Boolean onlyInPark, Integer page, Integer size) {
        SearchCondition condition = new SearchCondition();
        condition.setKeyword(keyword);
        condition.setParkName(parkName);
        condition.setOnlyInPark(onlyInPark);
        condition.setPage(page != null ? page : 1);
        condition.setSize(size != null ? size : 20);
        
        // 根据关键词类型设置不同的搜索策略
        if (isPlateNumber(keyword)) {
            condition.setSearchType(SearchCondition.SearchType.PLATE_NUMBER);
        } else if (isPhoneNumber(keyword)) {
            condition.setSearchType(SearchCondition.SearchType.PHONE_NUMBER);
        } else {
            // 对于部分车牌号或其他关键词，使用多字段搜索
            condition.setSearchType(SearchCondition.SearchType.MIXED);
        }
        
        System.out.println("🔍 搜索条件: keyword=" + keyword + ", searchType=" + condition.getSearchType());
        
        return condition;
    }

    /**
     * 搜索月票记录 - 调用外部API
     */
    private List<MonthTick> searchMonthTickets(SearchCondition condition) {
        try {
            // 🆕 优化策略：为了获取更多数据，统一使用并发查询
            System.out.println("🔍 搜索策略: 使用并发查询获取所有可用数据");
            
            // 先尝试从缓存获取
            List<MonthTick> cachedResult = getCachedMonthTickets(condition);
            if (cachedResult != null) {
                System.out.println("🚀 使用缓存数据，共 " + cachedResult.size() + " 条记录");
                return filterMonthTicketsByKeyword(cachedResult, condition);
            }
            
            // 缓存未命中，使用并发查询获取所有数据
            List<MonthTick> apiResult = getMonthTicketsWithConcurrency(condition);
            if (!apiResult.isEmpty()) {
                // 缓存结果
                cacheMonthTickets(condition, apiResult);
                return filterMonthTicketsByKeyword(apiResult, condition);
            }
            
            // API查询失败，回退到本地数据库
            System.out.println("⚠️ API查询失败，回退到本地数据库查询");
            return searchMonthTicketsFromDB(condition);
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("调用外部API失败，回退到本地数据库查询: " + e.getMessage());
            return searchMonthTicketsFromDB(condition);
        }
    }
    
    /**
     * 判断是否为精确查询（车牌号或手机号）
     */
    private boolean isSpecificSearch(SearchCondition condition) {
        String keyword = condition.getKeyword();
        return keyword != null && (isPlateNumber(keyword) || isPhoneNumber(keyword));
    }
    
    /**
     * 从缓存获取月票数据
     */
    private List<MonthTick> getCachedMonthTickets(SearchCondition condition) {
        String cacheKey = buildCacheKey(condition);
        CacheEntry entry = apiDataCache.get(cacheKey);
        
        if (entry != null && !entry.isExpired()) {
            return new ArrayList<>(entry.getData());
        }
        
        // 清理过期缓存
        if (entry != null && entry.isExpired()) {
            apiDataCache.remove(cacheKey);
        }
        
        return null;
    }
    
    /**
     * 缓存月票数据
     */
    private void cacheMonthTickets(SearchCondition condition, List<MonthTick> data) {
        String cacheKey = buildCacheKey(condition);
        apiDataCache.put(cacheKey, new CacheEntry(new ArrayList<>(data)));
        
        // 限制缓存大小，避免内存溢出
        if (apiDataCache.size() > 100) {
            cleanExpiredCache();
        }
    }
    
    /**
     * 构建缓存键
     */
    private String buildCacheKey(SearchCondition condition) {
        StringBuilder key = new StringBuilder();
        key.append(condition.getParkName() != null ? condition.getParkName() : "ALL");
        key.append("_").append(condition.getKeyword() != null ? condition.getKeyword() : "");
        return key.toString();
    }
    
    /**
     * 清理过期缓存
     */
    private void cleanExpiredCache() {
        int sizeBefore = apiDataCache.size();
        apiDataCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int sizeAfter = apiDataCache.size();
        if (sizeBefore > sizeAfter) {
            System.out.println("🧹 清理了 " + (sizeBefore - sizeAfter) + " 个过期缓存项，当前缓存大小: " + sizeAfter);
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", apiDataCache.size());
        stats.put("expiredCount", apiDataCache.entrySet().stream()
            .mapToLong(entry -> entry.getValue().isExpired() ? 1 : 0)
            .sum());
        return stats;
    }
    
    /**
     * 清空所有缓存
     */
    public void clearAllCache() {
        int size = apiDataCache.size();
        apiDataCache.clear();
        System.out.println("🗑️ 已清空所有缓存，清理了 " + size + " 个缓存项");
    }
    
    /**
     * 性能监控装饰器
     */
    private <T> T withPerformanceMonitoring(String operation, java.util.function.Supplier<T> supplier) {
        long startTime = System.currentTimeMillis();
        try {
            T result = supplier.get();
            long endTime = System.currentTimeMillis();
            System.out.println("⏱️ " + operation + " 耗时: " + (endTime - startTime) + "ms");
            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            System.err.println("❌ " + operation + " 失败，耗时: " + (endTime - startTime) + "ms, 错误: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 优化的API查询 - 用于精确查询（车牌号、手机号）
     */
    private List<MonthTick> getMonthTicketsFromAPIOptimized(SearchCondition condition) {
        // 对于精确查询，只需要查询第一页即可，因为结果通常很少
        List<MonthTick> result = new ArrayList<>();
        
        try {
            String parkCodeList = getParkCodeList(condition);
            String validStatus = "1";
            int pageSize = 100;
            
            System.out.println("🎯 执行优化的精确查询 - keyword: " + condition.getKeyword());
            
            ResponseEntity<?> response = monthTicketController.getOnlineMonthTicketList(
                parkCodeList, "1", String.valueOf(pageSize), validStatus);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject jsonResponse = (JSONObject) response.getBody();
                
                if (isAPIResponseSuccess(jsonResponse)) {
                    JSONObject data = jsonResponse.getJSONObject("data");
                    if (data != null && data.containsKey("recordList")) {
                        List<MonthTick> pageData = convertAPIDataToMonthTicks(data.getJSONArray("recordList"));
                        result.addAll(pageData);
                        System.out.println("✅ 精确查询获取到 " + pageData.size() + " 条数据");
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ 优化查询失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 并发查询API数据 - 用于大数据量查询
     */
    private List<MonthTick> getMonthTicketsWithConcurrency(SearchCondition condition) {
        List<MonthTick> allResults = new ArrayList<>();
        
        try {
            String parkCodeList = getParkCodeList(condition);
            String validStatus = "1";
            int pageSize = 100;
            
            System.out.println("🚀 开始并发查询API数据");
            
            // 先获取第一页，确定总页数
            ResponseEntity<?> firstPageResponse = monthTicketController.getOnlineMonthTicketList(
                parkCodeList, "1", String.valueOf(pageSize), validStatus);
            
            if (!firstPageResponse.getStatusCode().is2xxSuccessful() || firstPageResponse.getBody() == null) {
                return allResults;
            }
            
            JSONObject firstPageJson = (JSONObject) firstPageResponse.getBody();
            if (!isAPIResponseSuccess(firstPageJson)) {
                return allResults;
            }
            
            JSONObject firstPageData = firstPageJson.getJSONObject("data");
            if (firstPageData == null) {
                return allResults;
            }
            
            // 处理第一页数据
            List<MonthTick> firstPageResult = null;
            if (firstPageData.containsKey("recordList")) {
                firstPageResult = convertAPIDataToMonthTicks(firstPageData.getJSONArray("recordList"));
                allResults.addAll(firstPageResult);
            }
            
            // 计算总页数
            int totalRecords = firstPageData.getIntValue("total");
            int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
            
            System.out.println("📊 总记录数: " + totalRecords + ", 总页数: " + totalPages);
            System.out.println("🔍 外部API查询参数: parkCodeList=" + parkCodeList + ", pageSize=" + pageSize + ", validStatus=" + validStatus);
            System.out.println("📄 第一页返回记录数: " + (firstPageResult != null ? firstPageResult.size() : 0));
            
            if (totalPages <= 1) {
                return allResults;
            }
            
            // 🚀 限制最大页数，避免查询过多数据导致超时
            int maxPages = Math.min(totalPages, 10); // 最多查询10页，即1000条记录，提高响应速度
            
            // 并发查询剩余页面
            List<CompletableFuture<List<MonthTick>>> futures = new ArrayList<>();
            
            for (int page = 2; page <= maxPages; page++) {
                final int currentPage = page;
                CompletableFuture<List<MonthTick>> future = CompletableFuture.supplyAsync(() -> {
                    return fetchSinglePage(parkCodeList, currentPage, pageSize, validStatus);
                }, executorService);
                futures.add(future);
            }
            
            // 🚀 等待所有并发任务完成，设置超时时间
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            
            // 🚀 设置较短的超时时间，避免长时间等待（从30秒减少到15秒）
            allFutures.get(15, TimeUnit.SECONDS);
            
            // 收集所有结果
            for (CompletableFuture<List<MonthTick>> future : futures) {
                List<MonthTick> pageResult = future.get();
                if (pageResult != null && !pageResult.isEmpty()) {
                    allResults.addAll(pageResult);
                }
            }
            
            System.out.println("🎉 并发查询完成，总共获取到 " + allResults.size() + " 条数据");
            
        } catch (TimeoutException e) {
            System.err.println("⏰ 并发查询超时，返回已获取的数据: " + allResults.size() + " 条");
        } catch (Exception e) {
            System.err.println("❌ 并发查询失败: " + e.getMessage());
        }
        
        return allResults;
    }
    
    /**
     * 获取单页数据
     */
    private List<MonthTick> fetchSinglePage(String parkCodeList, int pageNum, int pageSize, String validStatus) {
        try {
            ResponseEntity<?> response = monthTicketController.getOnlineMonthTicketList(
                parkCodeList, String.valueOf(pageNum), String.valueOf(pageSize), validStatus);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject jsonResponse = (JSONObject) response.getBody();
                
                if (isAPIResponseSuccess(jsonResponse)) {
                    JSONObject data = jsonResponse.getJSONObject("data");
                    if (data != null && data.containsKey("recordList")) {
                        List<MonthTick> pageData = convertAPIDataToMonthTicks(data.getJSONArray("recordList"));
                        System.out.println("✅ 第 " + pageNum + " 页获取到 " + pageData.size() + " 条数据");
                        return pageData;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 第 " + pageNum + " 页查询失败: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 获取车场编码列表
     */
    private String getParkCodeList(SearchCondition condition) {
        if (StringUtils.hasText(condition.getParkName())) {
            List<String> yardCodes = yardInfoService.yardCode(condition.getParkName());
            if (yardCodes != null && !yardCodes.isEmpty()) {
                return yardCodes.get(0);
            }
        }
        return null;
    }
    
    /**
     * 判断API响应是否成功
     */
    private boolean isAPIResponseSuccess(JSONObject jsonResponse) {
        String code = jsonResponse.getString("code");
        String message = jsonResponse.getString("message");
        
        // 方式1: code为"0"
        if ("0".equals(code)) {
            return true;
        }
        // 方式2: code为null但message包含"成功"
        else if (code == null && message != null && message.contains("成功")) {
            return true;
        }
        // 方式3: 直接检查是否有data字段
        else if (jsonResponse.containsKey("data") && jsonResponse.getJSONObject("data") != null) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 从外部API获取月票数据（原始轮询方法，保留作为备用）
     */
    private List<MonthTick> getMonthTicketsFromAPI(SearchCondition condition) {
        List<MonthTick> allMonthTicks = new ArrayList<>();
        
        try {
            // 构建基础参数 - 修复参数处理，将车场名称转换为车场编码
            String parkCodeList = null;
            if (StringUtils.hasText(condition.getParkName())) {
                // 将车场名称转换为车场编码
                List<String> yardCodes = yardInfoService.yardCode(condition.getParkName());
                if (yardCodes != null && !yardCodes.isEmpty()) {
                    parkCodeList = yardCodes.get(0); // 取第一个匹配的车场编码
                    System.out.println("🔄 车场名称转换: " + condition.getParkName() + " -> " + parkCodeList);
                } else {
                    System.out.println("⚠️ 未找到车场编码，车场名称: " + condition.getParkName());
                }
            }
            
            String validStatus = "1"; // 查询有效状态的月票
            int pageSize = 100; // 每页固定100条
            int currentPage = 1;
            int totalPages = 1;
            int totalRecords = 0;
            
            System.out.println("🚀 开始轮询获取外部API月票数据 - parkCodeList: " + parkCodeList);
            
            // 轮询获取所有页面的数据
            do {
                String pageNum = String.valueOf(currentPage);
                String pageSizeStr = String.valueOf(pageSize);
                
                System.out.println("🔍 调用外部API第 " + currentPage + " 页 - parkCodeList: " + parkCodeList + 
                                 ", pageNum: " + pageNum + ", pageSize: " + pageSizeStr + ", validStatus: " + validStatus);
                
                // 调用MonthTicketController的接口
                ResponseEntity<?> response = monthTicketController.getOnlineMonthTicketList(
                    parkCodeList, pageNum, pageSizeStr, validStatus);
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JSONObject jsonResponse = (JSONObject) response.getBody();
                    
                    System.out.println("📥 外部API第 " + currentPage + " 页响应: " + jsonResponse.toJSONString());
                    
                    // 判断API调用是否成功 - 支持多种成功响应格式
                    boolean isSuccess = false;
                    String code = jsonResponse.getString("code");
                    String message = jsonResponse.getString("message");
                    
                    // 方式1: code为"0"
                    if ("0".equals(code)) {
                        isSuccess = true;
                    }
                    // 方式2: code为null但message包含"成功"
                    else if (code == null && message != null && message.contains("成功")) {
                        isSuccess = true;
                    }
                    // 方式3: 直接检查是否有data字段
                    else if (jsonResponse.containsKey("data") && jsonResponse.getJSONObject("data") != null) {
                        isSuccess = true;
                    }
                    
                    if (isSuccess) {
                        JSONObject data = jsonResponse.getJSONObject("data");
                        if (data != null) {
                            // 获取总记录数和总页数（如果API返回了这些信息）
                            if (data.containsKey("total")) {
                                totalRecords = data.getIntValue("total");
                                totalPages = (int) Math.ceil((double) totalRecords / pageSize);
                                System.out.println("📊 总记录数: " + totalRecords + ", 总页数: " + totalPages);
                            }
                            
                            // 处理当前页的数据 - API返回的字段是recordList
                            if (data.containsKey("recordList")) {
                                List<MonthTick> currentPageData = convertAPIDataToMonthTicks(data.getJSONArray("recordList"));
                                allMonthTicks.addAll(currentPageData);
                                
                                System.out.println("✅ 第 " + currentPage + " 页获取到 " + currentPageData.size() + " 条数据，累计: " + allMonthTicks.size() + " 条");
                                
                                // 如果当前页数据少于100条，说明已经是最后一页
                                if (currentPageData.size() < pageSize) {
                                    System.out.println("🏁 已获取所有数据，当前页数据不足 " + pageSize + " 条，结束轮询");
                                    break;
                                }
                            } else {
                                System.out.println("⚠️ 第 " + currentPage + " 页无recordList数据，结束轮询");
                                break;
                            }
                        }
                    } else {
                        // 记录API返回的错误信息
                        String errorCode = jsonResponse.getString("code");
                        String errorMessage = jsonResponse.getString("message");
                        System.err.println("❌ 外部API第 " + currentPage + " 页返回错误 - code: " + errorCode + ", message: " + errorMessage);
                        break; // 出错时停止轮询
                    }
                } else {
                    System.err.println("❌ HTTP请求失败，第 " + currentPage + " 页 - 状态码: " + response.getStatusCode());
                    break; // 出错时停止轮询
                }
                
                currentPage++;
                
                // 防止无限循环，设置最大页数限制
                if (currentPage > 1000) {
                    System.err.println("⚠️ 达到最大页数限制(1000页)，停止轮询");
                    break;
                }
                
                // 如果已知总页数，且当前页超过总页数，则停止
                if (totalPages > 1 && currentPage > totalPages) {
                    System.out.println("🏁 已获取所有页面数据，结束轮询");
                    break;
                }
                
                // 添加短暂延迟，避免对API造成过大压力
                Thread.sleep(100);
                
            } while (true);
            
            System.out.println("🎉 轮询完成，总共获取到 " + allMonthTicks.size() + " 条月票数据");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("调用外部API获取月票数据失败: " + e.getMessage());
        }
        
        return allMonthTicks;
    }

    /**
     * 将API返回的数据转换为MonthTick对象列表
     */
    private List<MonthTick> convertAPIDataToMonthTicks(com.alibaba.fastjson.JSONArray apiList) {
        List<MonthTick> monthTicks = new ArrayList<>();
        
        if (apiList != null) {
            System.out.println("🔍 开始转换API数据，共 " + apiList.size() + " 条记录");
            for (int index = 0; index < apiList.size(); index++) {
                Object item = apiList.get(index);
                if (item instanceof Map) {
                    Map<String, Object> apiData = (Map<String, Object>) item;
                    MonthTick monthTick = new MonthTick();
                    
                    // 打印前3条记录的详细信息用于调试
                    if (index < 3) {
                        System.out.println("📋 第" + (index + 1) + "条API数据字段: " + apiData.keySet());
                        System.out.println("📋 第" + (index + 1) + "条API数据内容: " + apiData);
                    }
                    
                    // 映射API数据到MonthTick对象
                    String carNo = (String) apiData.get("carNo");
                    monthTick.setCarNo(processCarNo(carNo));
                    monthTick.setUserName((String) apiData.get("userName"));
                    monthTick.setUserPhone((String) apiData.get("userPhone"));
                    monthTick.setTicketName((String) apiData.get("ticketName"));
                    monthTick.setParkName((String) apiData.get("parkName"));
                    
                    // 如果carNo为空，尝试其他可能的字段名
                    if (!StringUtils.hasText(carNo)) {
                        carNo = (String) apiData.get("plateNumber");
                        if (!StringUtils.hasText(carNo)) {
                            carNo = (String) apiData.get("car_no");
                        }
                        if (!StringUtils.hasText(carNo)) {
                            carNo = (String) apiData.get("vehicleNumber");
                        }
                        if (!StringUtils.hasText(carNo)) {
                            carNo = (String) apiData.get("licensePlate");
                        }
                        monthTick.setCarNo(processCarNo(carNo));
                        
                        if (index < 3) {
                            System.out.println("🚗 第" + (index + 1) + "条记录最终车牌号: " + carNo);
                        }
                    }
                    
                    // 处理validStatus，确保类型正确
                    Object validStatusObj = apiData.get("validStatus");
                    if (validStatusObj != null) {
                        if (validStatusObj instanceof Integer) {
                            monthTick.setValidStatus((Integer) validStatusObj);
                        } else if (validStatusObj instanceof String) {
                            try {
                                monthTick.setValidStatus(Integer.parseInt((String) validStatusObj));
                            } catch (NumberFormatException e) {
                                monthTick.setValidStatus(1); // 默认为有效
                            }
                        } else {
                            monthTick.setValidStatus(1); // 默认为有效
                        }
                    } else {
                        monthTick.setValidStatus(1); // 默认为有效
                    }
                    
                    // 设置其他可能的字段
                    if (apiData.containsKey("ticketCode")) {
                        monthTick.setTicketCode((String) apiData.get("ticketCode"));
                    }
                    if (apiData.containsKey("createTime")) {
                        monthTick.setCreateTime((String) apiData.get("createTime"));
                    }
                    if (apiData.containsKey("updateTime")) {
                        monthTick.setUpdateTime((String) apiData.get("updateTime"));
                    }
                    if (apiData.containsKey("remark1")) {
                        monthTick.setRemark1((String) apiData.get("remark1"));
                    }
                    if (apiData.containsKey("remark2")) {
                        monthTick.setRemark2((String) apiData.get("remark2"));
                    }
                    if (apiData.containsKey("remark3")) {
                        monthTick.setRemark3((String) apiData.get("remark3"));
                    }
                    if (apiData.containsKey("id")) {
                        Object idObj = apiData.get("id");
                        if (idObj instanceof Integer) {
                            monthTick.setId((Integer) idObj);
                        } else if (idObj instanceof Long) {
                            monthTick.setId(((Long) idObj).intValue());
                        } else if (idObj instanceof String) {
                            try {
                                monthTick.setId(Integer.parseInt((String) idObj));
                            } catch (NumberFormatException e) {
                                monthTick.setId(0);
                            }
                        }
                    }
                    if (apiData.containsKey("isFrozen")) {
                        Object frozenObj = apiData.get("isFrozen");
                        if (frozenObj instanceof Integer) {
                            monthTick.setIsFrozen((Integer) frozenObj);
                        } else if (frozenObj instanceof String) {
                            try {
                                monthTick.setIsFrozen(Integer.parseInt((String) frozenObj));
                            } catch (NumberFormatException e) {
                                monthTick.setIsFrozen(0);
                            }
                        }
                    }
                    
                    monthTicks.add(monthTick);
                }
            }
            System.out.println("✅ API数据转换完成，共转换 " + monthTicks.size() + " 条记录");
        }
        
        return monthTicks;
    }

    /**
     * 根据关键词过滤月票数据
     */
    private List<MonthTick> filterMonthTicketsByKeyword(List<MonthTick> monthTicks, SearchCondition condition) {
        String keyword = condition.getKeyword();
        if (!StringUtils.hasText(keyword)) {
            System.out.println("🔍 无关键词，返回所有 " + monthTicks.size() + " 条记录");
            return monthTicks;
        }
        
        System.out.println("🔍 开始过滤，输入 " + monthTicks.size() + " 条记录，关键词: " + keyword + ", 搜索类型: " + condition.getSearchType());
        
        List<MonthTick> filteredList = monthTicks.stream()
            .filter(monthTick -> {
                boolean matches = false;
                switch (condition.getSearchType()) {
                    case PLATE_NUMBER:
                        matches = monthTick.getCarNo() != null && monthTick.getCarNo().contains(keyword);
                        break;
                    case PHONE_NUMBER:
                        matches = monthTick.getUserPhone() != null && monthTick.getUserPhone().contains(keyword);
                        break;
                    case OWNER_NAME:
                        matches = monthTick.getUserName() != null && monthTick.getUserName().contains(keyword);
                        break;
                    case MIXED:
                    default:
                        // 多字段搜索
                        matches = (monthTick.getCarNo() != null && monthTick.getCarNo().contains(keyword)) ||
                               (monthTick.getUserName() != null && monthTick.getUserName().contains(keyword)) ||
                               (monthTick.getUserPhone() != null && monthTick.getUserPhone().contains(keyword)) ||
                               (monthTick.getTicketName() != null && monthTick.getTicketName().contains(keyword));
                        break;
                }
                return matches;
            })
            .collect(Collectors.toList());
            
        System.out.println("🔍 过滤完成，匹配 " + filteredList.size() + " 条记录");
        return filteredList;
    }

    /**
     * 从本地数据库搜索月票记录（作为备用方案）
     */
    private List<MonthTick> searchMonthTicketsFromDB(SearchCondition condition) {
        QueryWrapper<MonthTick> queryWrapper = new QueryWrapper<>();
        
        String keyword = condition.getKeyword();
        if (StringUtils.hasText(keyword)) {
            // 根据搜索类型优化查询
            switch (condition.getSearchType()) {
                case PLATE_NUMBER:
                    queryWrapper.like("car_no", keyword);
                    break;
                case PHONE_NUMBER:
                    queryWrapper.like("user_phone", keyword);
                    break;
                case OWNER_NAME:
                    queryWrapper.like("user_name", keyword);
                    break;
                default:
                    // 多字段搜索
                    queryWrapper.and(wrapper -> wrapper
                        .like("car_no", keyword)
                        .or().like("user_name", keyword)
                        .or().like("user_phone", keyword)
                        .or().like("ticket_name", keyword));
            }
        }
        
        if (StringUtils.hasText(condition.getParkName())) {
            queryWrapper.eq("park_name", condition.getParkName());
        }
        
        // 只查询有效的月票
        queryWrapper.in("valid_status", Arrays.asList(1, 4)); // 1-有效, 4-过期但仍显示
        
        return monthTicketMapper.selectList(queryWrapper);
    }

    /**
     * 将包含多个车牌的月票记录拆分为单个车牌记录
     */
    private List<MonthTicketVehicleDTO> splitMonthTicketToVehicles(MonthTick monthTicket) {
        List<MonthTicketVehicleDTO> vehicles = new ArrayList<>();
        
        if (monthTicket == null) {
            System.out.println("⚠️ 跳过null月票记录");
            return vehicles;
        }
        
        if (!StringUtils.hasText(monthTicket.getCarNo())) {
            System.out.println("⚠️ 跳过无车牌号的记录: id=" + monthTicket.getId() + 
                             ", userName=" + monthTicket.getUserName() + 
                             ", carNo='" + monthTicket.getCarNo() + "'");
            return vehicles;
        }
        
        System.out.println("🔄 处理月票: carNo=" + monthTicket.getCarNo() + 
                         ", id=" + monthTicket.getId() + 
                         ", userName=" + monthTicket.getUserName());
        
        // 按逗号分割车牌号
        String[] plateNumbers = monthTicket.getCarNo().split(",");
        System.out.println("🔍 拆分车牌号: " + Arrays.toString(plateNumbers));
        
        for (int i = 0; i < plateNumbers.length; i++) {
            String plateNumber = plateNumbers[i].trim();
            if (!StringUtils.hasText(plateNumber)) {
                System.out.println("⚠️ 跳过空车牌号: index=" + i + ", plateNumber='" + plateNumber + "'");
                continue;
            }
            
            System.out.println("✅ 创建车辆记录: " + plateNumber);
            
            MonthTicketVehicleDTO vehicle = new MonthTicketVehicleDTO();
            // 安全设置ID
            if (monthTicket.getId() != null) {
            vehicle.setMonthTicketId(monthTicket.getId().longValue());
            } else {
                vehicle.setMonthTicketId(0L); // 默认值
            }
            vehicle.setPlateNumber(plateNumber);
            vehicle.setTicketName(monthTicket.getTicketName());
            vehicle.setOwnerName(monthTicket.getUserName());
            vehicle.setOwnerPhone(monthTicket.getUserPhone());
            vehicle.setParkName(monthTicket.getParkName());
            vehicle.setValidStatus(monthTicket.getValidStatus());
            vehicle.setIsFrozen(monthTicket.getIsFrozen());
            vehicle.setStartTime(monthTicket.getCreateTime());
            vehicle.setEndTime(monthTicket.getUpdateTime());
            vehicle.setRemark1(monthTicket.getRemark1());
            vehicle.setRemark2(monthTicket.getRemark2());
            vehicle.setRemark3(monthTicket.getRemark3());
            
            // 智能分配车位信息（如果有多个车位）
            if (StringUtils.hasText(monthTicket.getRemark1()) && monthTicket.getRemark1().contains(",")) {
                String[] parkingSpots = monthTicket.getRemark1().split(",");
                if (i < parkingSpots.length) {
                    vehicle.setParkingSpot(parkingSpots[i].trim());
                }
            } else {
                vehicle.setParkingSpot(monthTicket.getRemark1());
            }
            
            vehicles.add(vehicle);
        }
        
        System.out.println("🚗 月票拆分完成，生成 " + vehicles.size() + " 个车辆记录");
        
        return vehicles;
    }

    /**
     * 多字段匹配
     */
    private boolean matchesKeyword(MonthTicketVehicleDTO vehicle, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        
        String lowerKeyword = keyword.toLowerCase();
        
        return (vehicle.getPlateNumber() != null && vehicle.getPlateNumber().toLowerCase().contains(lowerKeyword)) ||
               (vehicle.getOwnerName() != null && vehicle.getOwnerName().toLowerCase().contains(lowerKeyword)) ||
               (vehicle.getOwnerPhone() != null && vehicle.getOwnerPhone().contains(keyword)) ||
               (vehicle.getParkingSpot() != null && vehicle.getParkingSpot().toLowerCase().contains(lowerKeyword)) ||
               (vehicle.getTicketName() != null && vehicle.getTicketName().toLowerCase().contains(lowerKeyword));
    }

    /**
     * 查询在场状态
     */
    private List<MonthTicketVehicleDTO> filterInParkVehicles(List<MonthTicketVehicleDTO> vehicles, String parkCode) {
        return vehicles.stream()
            .peek(vehicle -> {
                // 调用停车场API查询在场状态
                Boolean isInPark = checkVehicleInPark(vehicle.getPlateNumber(), parkCode);
                vehicle.setIsInPark(isInPark);
            })
            .filter(vehicle -> Boolean.TRUE.equals(vehicle.getIsInPark()))
            .collect(Collectors.toList());
    }

    /**
     * 比较相关度
     */
    private int compareRelevance(MonthTicketVehicleDTO v1, MonthTicketVehicleDTO v2) {
        // 简单的相关度排序：有效状态 > 车牌号字典序
        int statusCompare = Integer.compare(v2.getValidStatus(), v1.getValidStatus());
        if (statusCompare != 0) {
            return statusCompare;
        }
        
        return v1.getPlateNumber().compareTo(v2.getPlateNumber());
    }

    /**
     * 丰富车辆信息
     */
    private void enrichVehicleInfo(MonthTicketVehicleDTO vehicle) {
        if (vehicle == null) {
            return;
        }
        
        // 查询预约记录数
        vehicle.setAppointmentCount(getAppointmentCount(vehicle.getPlateNumber()));
        
        // 查询违规记录数
        vehicle.setViolationCount(getViolationCount(vehicle.getPlateNumber()));
        
        // 查询在场状态 - 优化：不自动查询，改为按需查询
        // vehicle.setIsInPark(checkVehicleInPark(vehicle.getPlateNumber(), vehicle.getParkCode()));
        vehicle.setIsInPark(null); // 设置为null，表示未查询
        
        // 设置信用分数（示例）
        vehicle.setCreditScore(100 - (vehicle.getViolationCount() != null ? vehicle.getViolationCount() * 5 : 0));
    }

    /**
     * 判断是否为车牌号
     */
    private boolean isPlateNumber(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return false;
        }
        return PLATE_PATTERN.matcher(keyword.toUpperCase()).matches() || 
               keyword.length() >= 7 && keyword.matches(".*[A-Z0-9].*");
    }

    /**
     * 判断是否为手机号
     */
    private boolean isPhoneNumber(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return false;
        }
        return PHONE_PATTERN.matcher(keyword).matches() || 
               (keyword.length() >= 10 && keyword.matches("\\d+"));
    }

    /**
     * 处理车牌号码，移除特殊字符
     * @param carNo 原始车牌号码
     * @return 处理后的车牌号码
     */
    private String processCarNo(String carNo) {
        if (carNo == null || carNo.trim().isEmpty()) {
            return carNo;
        }
        // 移除车牌号码中的特殊字符，如 ●
        return carNo.replace("●", "").trim();
    }
} 