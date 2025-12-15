package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * @program: ParkManage
 * @description: 请求艾科接口查询哪个月票合理
 * @author: lzx
 * @create: 2024-06-01 14:35
 **/
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="MonthTick", description="")
public class MonthTick  implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String parkName;
    private String carNo;
    private int carNoNum;
    private int configStatus;
    private String createBy;
    private String createTime;
    private int dynamicCarportNumber;
    private int isFrozen;
    private long monthTicketConfigId;
    private String remark1;
    private String remark2;
    private String remark3;
    private String ticketCode;
    private String ticketName;
    private String timePeriodList;
    private String updateBy;
    private String updateTime;
    private String userName;
    private String userPhone;
    private int validStatus;
}