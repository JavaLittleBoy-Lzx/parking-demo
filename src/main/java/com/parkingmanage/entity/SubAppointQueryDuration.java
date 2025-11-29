package com.parkingmanage.entity;

import lombok.Data;

@Data
public class SubAppointQueryDuration {
    private String openid;
    private  String platenumber;
    private String visitorphone;
    private String recorddateBegin;
    private String visitdateBegin;
    private String visitdateEnd;
    private String recorddateEnd;
}
