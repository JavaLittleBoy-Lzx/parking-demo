package com.parkingmanage.dto.ai.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能客服响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerServiceResponse {

    /**
     * AI回答
     */
    private String answer;

    /**
     * 时间戳
     */
    private Long timestamp;
}