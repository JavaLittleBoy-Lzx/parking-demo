package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.dto.ai.requests.*;
import com.parkingmanage.dto.ai.responses.*;
import com.parkingmanage.service.AIService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI智能服务控制器
 * 统一处理所有AI相关请求，包括智能客服、违规描述生成、数据分析报告等
 */
@Api(tags = "AI智能服务", description = "BigModel AI接口")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final AIService aiService;

    /**
     * 智能客服对话
     */
    @ApiOperation(value = "智能客服对话", notes = "基于自然语言的用户问答服务")
    @PostMapping("/customer-service")
    public Result<CustomerServiceResponse> customerService(@RequestBody CustomerServiceRequest request) {
        try {
            log.info("智能客服请求 - 问题: {}, 上下文: {}", request.getQuestion(), request.getContext());

            CustomerServiceResponse response = aiService.customerService(
                request.getQuestion(),
                request.getContext()
            );

            log.info("智能客服响应成功");
            return Result.success(response);

        } catch (Exception e) {
            log.error("智能客服调用失败", e);
            return Result.error("智能客服暂时不可用：" + e.getMessage());
        }
    }

    /**
     * 生成违规描述
     */
    @ApiOperation(value = "生成违规描述", notes = "AI自动生成规范的违规停车描述")
    @PostMapping("/violation-description")
    public Result<ViolationDescriptionResponse> generateViolationDescription(
            @RequestBody ViolationDescriptionRequest request) {
        try {
            log.info("违规描述生成请求 - 车牌号: {}, 违规类型: {}",
                request.getLicensePlate(), request.getViolationType());

            ViolationDescriptionResponse response = aiService.generateViolationDescription(request);

            log.info("违规描述生成成功");
            return Result.success(response);

        } catch (Exception e) {
            log.error("违规描述生成失败", e);
            return Result.error("违规描述生成失败：" + e.getMessage());
        }
    }

    /**
     * 生成数据分析报告
     */
    @ApiOperation(value = "生成数据分析报告", notes = "智能生成停车数据分析报告")
    @PostMapping("/data-report")
    public Result<DataReportResponse> generateDataReport(@RequestBody DataReportRequest request) {
        try {
            log.info("数据报告生成请求 - 报告类型: {}, 数据统计: {}",
                request.getReportType(), request.getStatistics());

            DataReportResponse response = aiService.generateDataReport(
                request.getStatistics(),
                request.getReportType()
            );

            log.info("数据报告生成成功");
            return Result.success(response);

        } catch (Exception e) {
            log.error("数据报告生成失败", e);
            return Result.error("数据报告生成失败：" + e.getMessage());
        }
    }

    /**
     * 生成通知文本
     */
    @ApiOperation(value = "生成通知文本", notes = "自动生成各类通知和提醒文本")
    @PostMapping("/notification-text")
    public Result<NotificationTextResponse> generateNotificationText(
            @RequestBody NotificationTextRequest request) {
        try {
            log.info("通知文本生成请求 - 类型: {}, 参数: {}",
                request.getNotificationType(), request.getParams());

            NotificationTextResponse response = aiService.generateNotificationText(
                request.getNotificationType(),
                request.getParams()
            );

            log.info("通知文本生成成功");
            return Result.success(response);

        } catch (Exception e) {
            log.error("通知文本生成失败", e);
            return Result.error("通知文本生成失败：" + e.getMessage());
        }
    }

    /**
     * 用户行为分析
     */
    @ApiOperation(value = "用户行为分析", notes = "分析用户操作日志,识别异常行为")
    @PostMapping("/behavior-analysis")
    public Result<BehaviorAnalysisResponse> analyzeUserBehavior(
            @RequestBody BehaviorAnalysisRequest request) {
        try {
            log.info("用户行为分析请求 - 用户ID: {}", request.getUserId());

            BehaviorAnalysisResponse response = aiService.analyzeUserBehavior(
                request.getUserActivities(),
                request.getUserId()
            );

            log.info("用户行为分析成功");
            return Result.success(response);

        } catch (Exception e) {
            log.error("用户行为分析失败", e);
            return Result.error("用户行为分析失败：" + e.getMessage());
        }
    }

    /**
     * 智能问答助手
     */
    @ApiOperation(value = "智能问答助手", notes = "基于知识库的智能问答")
    @PostMapping("/intelligent-qa")
    public Result<IntelligentQAResponse> intelligentQA(@RequestBody IntelligentQARequest request) {
        try {
            log.info("智能问答请求 - 问题: {}", request.getQuestion());

            IntelligentQAResponse response = aiService.intelligentQA(
                request.getQuestion(),
                request.getDocuments()
            );

            log.info("智能问答成功");
            return Result.success(response);

        } catch (Exception e) {
            log.error("智能问答失败", e);
            return Result.error("智能问答失败：" + e.getMessage());
        }
    }

    /**
     * 获取AI功能状态
     */
    @ApiOperation(value = "获取AI功能状态", notes = "检查AI功能是否正常启用")
    @GetMapping("/status")
    public Result<AIStatusResponse> getAIStatus() {
        try {
            AIStatusResponse response = aiService.getAIStatus();
            log.info("AI状态查询成功");
            return Result.success(response);

        } catch (Exception e) {
            log.error("AI状态查询失败", e);
            return Result.error("AI状态查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取AI使用统计
     */
    @ApiOperation(value = "获取AI使用统计", notes = "获取AI服务使用统计数据")
    @GetMapping("/usage")
    public Result<AIUsageResponse> getAIUsage() {
        try {
            AIUsageResponse response = aiService.getAIUsage();
            log.info("AI使用统计查询成功");
            return Result.success(response);

        } catch (Exception e) {
            log.error("AI使用统计查询失败", e);
            return Result.error("AI使用统计查询失败：" + e.getMessage());
        }
    }
}