package com.parkingmanage.dto.ai.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 智能问答请求
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntelligentQARequest {

    /**
     * 问题
     */
    private String question;

    /**
     * 参考文档
     */
    private List<Object> documents;
}