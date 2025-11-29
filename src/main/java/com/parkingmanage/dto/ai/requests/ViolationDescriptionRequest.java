package com.parkingmanage.dto.ai.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 违规描述生成请求
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ViolationDescriptionRequest {

    /**
     * 违规记录ID
     */
    private String violationId;

    /**
     * 车牌号
     */
    private String licensePlate;

    /**
     * 违规类型
     */
    private String violationType;

    /**
     * 违规地点
     */
    private String location;

    /**
     * 违规时间
     */
    private String violationTime;

    /**
     * 现场情况描述
     */
    private String description;

    /**
     * 照片URL列表
     */
    private List<String> photos;

    /**
     * 上报人
     */
    private String reporter;
}