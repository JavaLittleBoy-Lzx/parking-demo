package com.parkingmanage.dto.ai.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 智能客服请求
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerServiceRequest {

    /**
     * 用户问题
     */
    private String question;

    /**
     * 上下文信息
     */
    private String context;
}