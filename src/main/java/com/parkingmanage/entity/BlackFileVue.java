package com.parkingmanage.entity;

import lombok.Data;

@Data
public class BlackFileVue {
    String parkCode;
    String carCode;
    String carOwner;
    String isPermament;
    String timePeriod;
    String reason;
    String remark1;
    String remark2;
    String specialCarTypeId;
}