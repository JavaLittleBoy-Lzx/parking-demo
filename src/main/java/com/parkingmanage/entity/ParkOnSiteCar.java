package com.parkingmanage.entity;

import lombok.Data;

@Data
public class ParkOnSiteCar {
    private String carNo;
    private String enterTime;
    private String parkName;
    private String parkingDuration;
    private Long parkingDurationTimes;
}
