package com.parkingmanage.service;

import com.parkingmanage.dto.MonthTicketVehicleDTO;
import com.parkingmanage.dto.SearchResult;
import com.parkingmanage.dto.SearchCondition;
import java.util.List;

/**
 * 月票车辆搜索服务接口
 */
public interface MonthTicketSearchService {
    
    /**
     * 智能搜索月票车辆
     * @param keyword 搜索关键词
     * @param parkName 车场名称
     * @param onlyInPark 是否只返回在场车辆
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    SearchResult<MonthTicketVehicleDTO> smartSearch(String keyword, String parkName, Boolean onlyInPark, Integer page, Integer size);
    
    /**
     * 根据搜索条件搜索
     * @param condition 搜索条件
     * @return 搜索结果
     */
    SearchResult<MonthTicketVehicleDTO> searchByCondition(SearchCondition condition);
    
    /**
     * 获取车辆详细信息
     * @param plateNumber 车牌号
     * @return 车辆详细信息
     */
    MonthTicketVehicleDTO getVehicleDetails(String plateNumber);
    
    /**
     * 获取车辆详细信息（包含在场状态）
     * 专门用于用户选择具体车牌后查询完整信息
     * @param plateNumber 车牌号
     * @param parkCode 车场代码
     * @return 车辆详细信息（含在场状态）
     */
    MonthTicketVehicleDTO getVehicleDetailsWithParkStatus(String plateNumber, String parkCode);
    
    /**
     * 获取车牌号建议列表
     * @param keyword 关键词
     * @param parkName 车场名称
     * @param limit 限制数量
     * @return 建议列表
     */
    List<MonthTicketVehicleDTO> getPlateSuggestions(String keyword, String parkName, Integer limit);
    
    /**
     * 检查车辆是否在场
     * @param plateNumber 车牌号
     * @param parkCode 车场代码
     * @return 是否在场
     */
    Boolean checkVehicleInPark(String plateNumber, String parkCode);
    
    /**
     * 获取车辆预约记录数
     * @param plateNumber 车牌号
     * @return 预约记录数
     */
    Integer getAppointmentCount(String plateNumber);
    
    /**
     * 获取车辆违规记录数
     * @param plateNumber 车牌号
     * @return 违规记录数
     */
    Integer getViolationCount(String plateNumber);
} 