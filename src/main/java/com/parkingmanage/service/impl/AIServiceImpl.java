package com.parkingmanage.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkingmanage.config.BigModelConfig;
import com.parkingmanage.dto.ai.*;
import com.parkingmanage.dto.ai.requests.*;
import com.parkingmanage.dto.ai.responses.*;
import com.parkingmanage.service.AIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI智能服务实现类
 * 通过调用BigModel API提供AI功能
 */
@Service
@Slf4j
public class AIServiceImpl implements AIService {

    private final RestTemplate restTemplate;
    private final BigModelConfig bigModelConfig;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入
     */
    public AIServiceImpl(
            @Qualifier("bigModelRestTemplate") RestTemplate restTemplate,
            BigModelConfig bigModelConfig,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.bigModelConfig = bigModelConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    @Cacheable(value = "customerService", key = "#question + '_' + #context")
    public CustomerServiceResponse customerService(String question, String context) {
        try {
            String systemPrompt = buildCustomerServicePrompt(context);
            BigModelRequest request = buildChatRequest("glm-4.6", systemPrompt, question);

            BigModelResponse response = callBigModelAPI(request);

            String answer = response.getChoices().get(0).getMessage().getContent();
            return new CustomerServiceResponse(answer, System.currentTimeMillis());

        } catch (Exception e) {
            log.error("智能客服调用失败", e);
            throw new RuntimeException("智能客服暂时不可用", e);
        }
    }

    @Override
    @Cacheable(value = "violationDescription", key = "#request.licensePlate + '_' + #request.violationType + '_' + #request.location")
    public ViolationDescriptionResponse generateViolationDescription(ViolationDescriptionRequest request) {
        try {
            String prompt = buildViolationDescriptionPrompt(request);
            String systemPrompt = "你是一个专业的交通违规记录员，负责生成规范、准确的违规停车描述。";
            BigModelRequest bigModelRequest = buildChatRequest("glm-4.6", systemPrompt, prompt);

            BigModelResponse response = callBigModelAPI(bigModelRequest);

            String description = response.getChoices().get(0).getMessage().getContent();
            ViolationDescriptionResponse vResponse = new ViolationDescriptionResponse();
            vResponse.setDescription(description);
            vResponse.setGeneratedTime(System.currentTimeMillis());
            return vResponse;

        } catch (Exception e) {
            log.error("违规描述生成失败", e);
            throw new RuntimeException("违规描述生成失败", e);
        }
    }

    @Override
    @Cacheable(value = "dataReport", key = "#reportType + '_' + #statistics.hashCode()")
    public DataReportResponse generateDataReport(Object statistics, String reportType) {
        try {
            String prompt = buildDataReportPrompt(statistics, reportType);
            String systemPrompt = "你是一个专业的停车场管理分析师，擅长数据分析和报告撰写。";
            BigModelRequest request = buildChatRequest("glm-4.6", systemPrompt, prompt);

            BigModelResponse response = callBigModelAPI(request);

            String report = response.getChoices().get(0).getMessage().getContent();
            return new DataReportResponse(report, reportType, System.currentTimeMillis());

        } catch (Exception e) {
            log.error("数据报告生成失败", e);
            throw new RuntimeException("数据报告生成失败", e);
        }
    }

    @Override
    public NotificationTextResponse generateNotificationText(String notificationType, Object params) {
        try {
            String prompt = buildNotificationPrompt(notificationType, params);
            String systemPrompt = "你是一个专业的文案撰写助手，擅长撰写各类通知和公告。";
            BigModelRequest request = buildChatRequest("glm-4.6", systemPrompt, prompt);

            BigModelResponse response = callBigModelAPI(request);

            String content = response.getChoices().get(0).getMessage().getContent();

            return new NotificationTextResponse(content, notificationType, System.currentTimeMillis());

        } catch (Exception e) {
            log.error("通知文本生成失败", e);
            throw new RuntimeException("通知文本生成失败", e);
        }
    }

    @Override
    public BehaviorAnalysisResponse analyzeUserBehavior(List<?> userActivities, String userId) {
        try {
            String prompt = buildBehaviorAnalysisPrompt(userActivities, userId);
            String systemPrompt = "你是一个专业的信息安全分析师，擅长用户行为分析和异常检测。";
            BigModelRequest request = buildChatRequest("glm-4.6", systemPrompt, prompt);

            BigModelResponse response = callBigModelAPI(request);

            String analysisResult = response.getChoices().get(0).getMessage().getContent();

            // 解析分析结果，提取关键信息
            String riskLevel = extractRiskLevel(analysisResult);
            String recommendation = "";
            return new BehaviorAnalysisResponse(analysisResult, riskLevel, recommendation, System.currentTimeMillis());

        } catch (Exception e) {
            log.error("用户行为分析失败", e);
            throw new RuntimeException("用户行为分析失败", e);
        }
    }

    @Override
    public IntelligentQAResponse intelligentQA(String question, List<?> documents) {
        try {
            String prompt = buildQAPrompt(question, documents);
            String systemPrompt = buildQASystemPrompt(documents);
            BigModelRequest request = buildChatRequest("glm-4.6", systemPrompt, prompt);

            BigModelResponse response = callBigModelAPI(request);

            String answer = response.getChoices().get(0).getMessage().getContent();
            return new IntelligentQAResponse(answer, 0.95, null, System.currentTimeMillis());

        } catch (Exception e) {
            log.error("智能问答失败", e);
            throw new RuntimeException("智能问答失败", e);
        }
    }

    @Override
    public AIStatusResponse getAIStatus() {
        Map<String, Boolean> features = new HashMap<>();
        features.put("customerService", true);
        features.put("violationDescription", true);
        features.put("dataReport", true);
        features.put("notificationText", true);
        features.put("behaviorAnalysis", true);

        boolean apiKeyConfigured = bigModelConfig.getApiKey() != null && !bigModelConfig.getApiKey().isEmpty();
        boolean enabled = apiKeyConfigured;

        return new AIStatusResponse(enabled, apiKeyConfigured, features, "1.0", "AI服务运行正常");
    }

    @Override
    public AIUsageResponse getAIUsage() {
        // 这里可以从数据库或缓存中获取实际使用统计
        Map<String, Long> featureUsage = new HashMap<>();
        featureUsage.put("customerService", 50L);
        featureUsage.put("violationDescription", 30L);
        featureUsage.put("dataReport", 25L);
        featureUsage.put("notificationText", 15L);
        featureUsage.put("behaviorAnalysis", 5L);

        return new AIUsageResponse(3847L, 125L, featureUsage, 1500.0, 98.5, System.currentTimeMillis());
    }

    // ========== 私有方法 ==========

    /**
     * 调用BigModel API
     */
    private BigModelResponse callBigModelAPI(BigModelRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(bigModelConfig.getApiKey());

            HttpEntity<BigModelRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<BigModelResponse> response = restTemplate.exchange(
                bigModelConfig.getBaseUrl() + "/chat/completions",
                HttpMethod.POST,
                entity,
                BigModelResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("BigModel API调用失败: " + response.getStatusCode());
            }

            return response.getBody();

        } catch (Exception e) {
            log.error("BigModel API调用异常", e);
            throw new RuntimeException("BigModel API调用异常", e);
        }
    }

    /**
     * 构建聊天请求
     */
    private BigModelRequest buildChatRequest(String model, String systemPrompt, String userPrompt) {
        return BigModelRequest.builder()
            .model(model)
            .temperature(0.7)
            .maxTokens(model.equals("glm-4") ? 2000 : 1000)
            .messages(Arrays.asList(
                BigModelRequest.Message.builder()
                    .role("system")
                    .content(systemPrompt)
                    .build(),
                BigModelRequest.Message.builder()
                    .role("user")
                    .content(userPrompt)
                    .build()
            ))
            .build();
    }

    /**
     * 构建智能客服提示词
     */
    private String buildCustomerServicePrompt(String context) {
        return String.format(
            "你是一个智能助手，主要为停车管理系统提供客服服务，但也可以回答一般性问题。\n\n" +
            "停车系统功能包括：\n" +
            "- 车辆入场出场管理\n" +
            "- 违规停车处理\n" +
            "- 月卡办理和续费\n" +
            "- 车位查询和预约\n" +
            "- 费用查询和缴纳\n" +
            "- 业主信息管理\n\n" +
            "回答要求：\n" +
            "1. 对于停车相关问题：专业、准确、详细，提供具体操作步骤\n" +
            "2. 对于一般性问题：友好、准确地回答，展现你的知识能力\n" +
            "3. 回答要清晰明了，根据问题复杂度调整回答长度\n" +
            "4. 保持专业和友好的语气\n\n" +
            "当前上下文：%s",
            context != null ? context : "无");
    }

    /**
     * 构建违规描述提示词
     */
    private String buildViolationDescriptionPrompt(ViolationDescriptionRequest request) {
        return String.format(
            "请根据以下违规停车信息，生成一个标准、规范的违规描述：\n\n" +
            "车牌号：%s\n" +
            "违规类型：%s\n" +
            "违规地点：%s\n" +
            "违规时间：%s\n" +
            "现场情况：%s\n" +
            "照片信息：%s\n" +
            "上报人：%s\n\n" +
            "请生成包含以下要素的违规描述：\n" +
            "1. 事实清晰、用词准确\n" +
            "2. 符合交通法规术语\n" +
            "3. 包含时间、地点、车牌等关键信息\n" +
            "4. 描述简洁，字数控制在100-200字之间\n" +
            "5. 语气客观、专业",
            request.getLicensePlate(),
            request.getViolationType(),
            request.getLocation(),
            request.getViolationTime(),
            request.getDescription() != null ? request.getDescription() : "无",
            request.getPhotos() != null && !request.getPhotos().isEmpty() ? "已拍摄照片" : "无照片",
            request.getReporter() != null ? request.getReporter() : "系统记录"
        );
    }

    /**
     * 构建数据报告提示词
     */
    private String buildDataReportPrompt(Object statistics, String reportType) {
        return String.format(
            "请根据以下停车数据，生成一份%s分析报告：\n\n" +
            "数据统计：%s\n\n" +
            "请生成包含以下内容的分析报告：\n" +
            "1. 数据概况和关键指标\n" +
            "2. 违规趋势分析\n" +
            "3. 车位使用情况分析\n" +
            "4. 存在的问题和风险点\n" +
            "5. 改进建议和管理措施\n" +
            "6. 下阶段工作重点\n\n" +
            "报告要求：\n" +
            "- 逻辑清晰，层次分明\n" +
            "- 数据准确，分析深入\n" +
            "- 建议具体可行\n" +
            "- 字数控制在800-1200字",
            reportType, statistics.toString());
    }

    /**
     * 构建通知提示词
     */
    private String buildNotificationPrompt(String notificationType, Object params) {
        Map<String, String> typeNames = new HashMap<>();
        typeNames.put("violation", "违规停车通知");
        typeNames.put("payment", "缴费提醒通知");
        typeNames.put("maintenance", "系统维护通知");
        typeNames.put("appointment", "预约成功通知");
        typeNames.put("emergency", "紧急情况通知");

        return String.format(
            "请生成一份%s，包含以下信息：\n\n" +
            "%s\n\n" +
            "通知要求：\n" +
            "1. 语气正式、礼貌\n" +
            "2. 信息完整、准确\n" +
            "3. 包含必要的联系方式\n" +
            "4. 字数控制在150-300字\n" +
            "5. 格式清晰，易于阅读",
            typeNames.getOrDefault(notificationType, "通知"), params.toString());
    }

    /**
     * 构建行为分析提示词
     */
    private String buildBehaviorAnalysisPrompt(List<?> userActivities, String userId) {
        return String.format(
            "请分析以下用户行为数据，识别潜在的异常行为：\n\n" +
            "用户ID：%s\n" +
            "活动记录：%s\n\n" +
            "请从以下维度进行分析：\n" +
            "1. 操作频率和时间分布\n" +
            "2. 功能使用偏好\n" +
            "3. 潜在的异常操作模式\n" +
            "4. 安全风险评估\n" +
            "5. 用户行为建议\n\n" +
            "分析要求：\n" +
            "- 重点关注异常模式\n" +
            "- 提供具体的分析结论\n" +
            "- 给出风险评估等级（low/medium/high）\n" +
            "- 建议相应的处理措施",
            userId, userActivities.toString());
    }

    /**
     * 构建问答提示词
     */
    private String buildQAPrompt(String question, List<?> documents) {
        return String.format(
            "%s\n\n" +
            "用户问题：%s",
            buildQASystemPrompt(documents), question);
    }

    /**
     * 构建问答系统提示词
     */
    private String buildQASystemPrompt(List<?> documents) {
        return String.format(
            "你是停车管理系统的智能问答助手。基于提供的参考文档回答用户问题。\n\n" +
            "回答原则：\n" +
            "1. 基于提供的文档信息进行回答\n" +
            "2. 如果文档中没有相关信息，明确说明\n" +
            "3. 回答要准确、专业、易懂\n" +
            "4. 可以适当扩展相关信息\n" +
            "5. 保持客观中立的立场\n\n" +
            "参考文档：%s",
            documents != null ? documents.toString() : "无参考文档");
    }

    // ========== 辅助方法 ==========

    private String buildNotificationTitle(String notificationType) {
        Map<String, String> titles = new HashMap<>();
        titles.put("violation", "违规停车处理通知");
        titles.put("payment", "缴费提醒通知");
        titles.put("maintenance", "系统维护通知");
        titles.put("appointment", "预约成功通知");
        titles.put("emergency", "紧急情况通知");
        return titles.getOrDefault(notificationType, "通知");
    }

    private String buildSMSContent(String notificationType, String content) {
        return String.format("【停车管理处】%s", content.length() > 50 ? content.substring(0, 47) + "..." : content);
    }

    private String extractRiskLevel(String analysisResult) {
        if (analysisResult.contains("高风险") || analysisResult.contains("high")) {
            return "high";
        } else if (analysisResult.contains("中风险") || analysisResult.contains("medium")) {
            return "medium";
        } else {
            return "low";
        }
    }

    private String extractBehaviorPattern(String analysisResult) {
        if (analysisResult.contains("正常") || analysisResult.contains("常规")) {
            return "正常办公时间操作";
        } else if (analysisResult.contains("异常")) {
            return "异常操作模式";
        } else {
            return "需进一步分析";
        }
    }

    private List<String> extractAnomalies(String analysisResult) {
        List<String> anomalies = new ArrayList<>();
        if (analysisResult.contains("频繁登录")) {
            anomalies.add("频繁登录操作");
        }
        if (analysisResult.contains("非常规时间")) {
            anomalies.add("非常规时间操作");
        }
        return anomalies;
    }

    private List<String> extractRecommendations(String analysisResult) {
        List<String> recommendations = new ArrayList<>();
        if (analysisResult.contains("监控")) {
            recommendations.add("加强监控");
        }
        if (analysisResult.contains("限制")) {
            recommendations.add("限制权限");
        }
        return recommendations;
    }
}
