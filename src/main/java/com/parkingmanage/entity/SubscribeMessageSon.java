package com.parkingmanage.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 <p>
接受小程序发送的推送数据
 </p>

 @author lzx
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "SubscribeMessage对象的子类", description = "接受小程序发送的推送数据")
public class SubscribeMessageSon {

    @ApiModelProperty(value = "template_id")
    private String template_id;
    @ApiModelProperty(value = "touser")
    private String touser;
    @ApiModelProperty(value = "page")
    private String page;
    @ApiModelProperty(value = "miniprogram_state")
    // 跳转小程序类型：developer为开发版；trial为体验版；formal为正式版；默认为正式版
    private String miniprogram_state;
    @ApiModelProperty(value = "lang")
    private String lang;
    @ApiModelProperty(value = "data")
    private SubscribeMessageData data;
}
