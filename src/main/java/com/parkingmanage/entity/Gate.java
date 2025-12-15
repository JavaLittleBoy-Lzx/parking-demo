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
 * @author Lzx
 * @since 2023-02-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Gate对象", description="")
public class Gate implements Serializable {

    private static final long serialVersionUID = 1L;

    private String province;
    private String city;
    private String district;
    private String community;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String gatename;
    private String parkingcode;
    private String parkingkey;
    private String parkingsecret;
}