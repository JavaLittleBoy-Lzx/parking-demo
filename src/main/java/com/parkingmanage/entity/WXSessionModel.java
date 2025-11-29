package com.parkingmanage.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="WXSessionModel", description="")
public class WXSessionModel {
	private String session_key;
	private String openid;
	//	public String getSession_key() {
//		return session_key;
//	}
//	public void setSession_key(String session_key) {
//		this.session_key = session_key;
//	}
//	public String getOpenid() {
//		return openid;
//	}
//	public void setOpenid(String openid) {
//		this.openid = openid;
//	}
}
