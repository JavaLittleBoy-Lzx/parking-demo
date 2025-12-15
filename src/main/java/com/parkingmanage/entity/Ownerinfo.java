package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author MLH
 * @since 2022-08-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Ownerinfo对象", description="")
public class    Ownerinfo implements Serializable {

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

    private String ownername;

    private String ownerphone;
    private String isaudit;
    private String permitverify;
    private String plates;
    private String parkingspaces;

    @ApiModelProperty(value = "信用分")
    private Integer creditScore;
}
