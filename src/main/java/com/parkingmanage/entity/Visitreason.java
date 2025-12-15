package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 <p>

 </p>

 @author MLH
 @since 2022-08-27
*/
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="VisitReason对象", description="")
public class Visitreason implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @ApiModelProperty(value = "来访原因")
    private String reason;
    private Integer sortno;
}
