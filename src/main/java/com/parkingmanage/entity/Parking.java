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
 * @since 2022-11-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Parking对象", description="")
public class Parking implements Serializable {

    private static final long serialVersionUID = 1L;

    private String province;

    private String city;

    private String district;

    private String community;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String parking;
    private String remark;

}
