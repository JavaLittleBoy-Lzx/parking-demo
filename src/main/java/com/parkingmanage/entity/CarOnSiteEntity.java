package com.parkingmanage.entity;

import lombok.Data;

import java.util.ArrayList;

@Data
public class CarOnSiteEntity {
    private ArrayList<String> parkCodeList;
    private Integer timeOutInterval;
}
