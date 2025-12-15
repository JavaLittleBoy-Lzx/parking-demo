package com.parkingmanage.dto.ai.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 用户行为分析请求
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BehaviorAnalysisRequest {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户活动日志
     */
    private List<?> userActivities;
}