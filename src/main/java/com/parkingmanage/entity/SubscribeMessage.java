package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 <p>
接受小程序发送的推送数据
 </p>

 @author lzx
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "SubscribeMessage对象", description = "接受小程序发送的推送数据")
public class SubscribeMessage implements Serializable {

    @ApiModelProperty(value = "access_token")
    private String access_token;

    @ApiModelProperty(value = "template_id")
    private String template_id;

    @ApiModelProperty(value = "touser")
    private String touser;

    //此处根据模板进行修改
    @ApiModelProperty(value = "data")
    private String data;

}
