package com.parkingmanage.entity;

import lombok.Data;

import java.util.List;

@Data
public class getCarInData {
    private String carLicenseNumber;
    private int confidence;
    private int enterCarLicenseColor;
    private String enterCarLicenseNumber;
    private String enterChannelName;
    private String enterCustomVipName;
    private List<EnterImageArray> enterImageArray;
    private int enterNovipCode;
    private int enterRecognitionConfidence;
    private String enterTime;
    private int enterType;
    private int enterVipType;
    private int isCorrect;
    private String parkCode;
    private String parkName;
    private String parkingCode;
    private int recordType;
}