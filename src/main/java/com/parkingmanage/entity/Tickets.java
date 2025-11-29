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
 @since 2023-03-03
*/
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Tickets对象", description="")
public class Tickets implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer gateid;

    private String building;

    private LocalDateTime createdate;

    private String createman;

    private String ticketcode;

    private String ticketname;
}
