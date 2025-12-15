package com.parkingmanage.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 测试消息请求参数
 * 
 * @author System
 */
@Data
@ApiModel(value="TestMessageRequest", description="测试消息请求参数")
public class TestMessageRequest {
    
    @ApiModelProperty(value = "微信OpenID")
    private String openid;
    
    @ApiModelProperty(value = "车牌号")
    private String plateNumber;
    
    @ApiModelProperty(value = "停车场名称")
    private String yardName;
} 