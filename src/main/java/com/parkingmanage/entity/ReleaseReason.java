package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author 李子雄
 *
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="ReleaseReason对象", description="")
public class ReleaseReason implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "ID号")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "放行原因")
    private String releaseReason;

    @ApiModelProperty(value = "序号")
    private Integer releaseNo;

//    @TableLogic(value="0",delval="1")
    @ApiModelProperty(value = "逻辑删除标识：0：未删除，1：已删除")
    private Integer deleted;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date gmtCreate;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date gmtModified;


}
