package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 <p>

 </p>

 @author MLH
 @since 2023-02-06
*/
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Room对象", description="")
public class Room implements Serializable {

    private static final long serialVersionUID = 1L;

    private String province;

    private String city;

    private String district;

    private String community;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String building;

    private String units;

    private String floor;

    private String roomnumber;

    private String parking;


}
