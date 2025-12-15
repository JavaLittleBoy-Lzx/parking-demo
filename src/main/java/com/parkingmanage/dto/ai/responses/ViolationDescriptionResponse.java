package com.parkingmanage.dto.ai.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 违规描述生成响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationDescriptionResponse {

    /**
     * 生成的违规描述
     */
    private String description;

    /**
     * 生成时间
     */
    private Long generatedTime;

    /**
     * 违规记录ID
     */
    private String violationId;
}