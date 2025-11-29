package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 @author LZX
 @since 2022-09-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Refusereason对象", description="")
public class Refusereason implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String reason;
    private Integer sortno;
}