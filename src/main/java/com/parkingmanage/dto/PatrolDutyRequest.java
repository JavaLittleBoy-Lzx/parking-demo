package com.parkingmanage.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 巡检员值班状态请求DTO
 */
@ApiModel(description = "巡检员值班状态请求")
public class PatrolDutyRequest {
    
    @ApiModelProperty(value = "巡检员openid", required = true)
    private String openid;
    
    @ApiModelProperty(value = "是否接收通知：1=值班中，0=离岗", required = true)
    private Integer enabled;
    
    public PatrolDutyRequest() {
    }
    
    public PatrolDutyRequest(String openid, Integer enabled) {
        this.openid = openid;
        this.enabled = enabled;
    }
    
    public String getOpenid() {
        return openid;
    }
    
    public void setOpenid(String openid) {
        this.openid = openid;
    }
    
    public Integer getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        return "PatrolDutyRequest{" +
                "openid='" + openid + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
