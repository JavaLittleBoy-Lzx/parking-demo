package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 <p>

 </p>

 @author MLH
 @since 2022-09-18
*/
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Illegalregiste对象", description="")
public class Illegalregiste implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String province;

    private String city;

    private String district;
    private String community;
    private String building;
    private String units;

    private String cartype;

    @TableField("plateNumber")
    private String platenumber;

    private String location;

    private String openid;

    private String operatorcode;

    private LocalDateTime operatordate;

    private String imgurl;


}
