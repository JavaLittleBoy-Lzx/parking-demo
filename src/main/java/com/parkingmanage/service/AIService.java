package com.parkingmanage.service;

import com.parkingmanage.dto.ai.requests.*;
import com.parkingmanage.dto.ai.responses.*;

import java.util.List;

/**
 * AI智能服务接口
 * 定义所有AI相关的服务方法
 */
public interface AIService {

    /**
     * 智能客服对话
     * @param question 用户问题
     * @param context 上下文信息
     * @return AI回答
     */
    CustomerServiceResponse customerService(String question, String context);

    /**
     * 生成违规描述
     * @param request 违规数据请求
     * @return 生成的违规描述
     */
    ViolationDescriptionResponse generateViolationDescription(ViolationDescriptionRequest request);

    /**
     * 生成数据分析报告
     * @param statistics 统计数据
     * @param reportType 报告类型
     * @return 生成的分析报告
     */
    DataReportResponse generateDataReport(Object statistics, String reportType);

    /**
     * 生成通知文本
     * @param notificationType 通知类型
     * @param params 通知参数
     * @return 生成的通知文本
     */
    NotificationTextResponse generateNotificationText(String notificationType, Object params);

    /**
     * 用户行为分析
     * @param userActivities 用户活动日志
     * @param userId 用户ID
     * @return 分析结果
     */
    BehaviorAnalysisResponse analyzeUserBehavior(List<?> userActivities, String userId);

    /**
     * 智能问答助手
     * @param question 问题
     * @param documents 参考文档
     * @return 回答
     */
    IntelligentQAResponse intelligentQA(String question, List<?> documents);

    /**
     * 获取AI功能状态
     * @return AI功能启用状态
     */
    AIStatusResponse getAIStatus();

    /**
     * 获取AI使用统计
     * @return AI使用统计数据
     */
    AIUsageResponse getAIUsage();
}