package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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
@ApiModel(value="YardInfo对象", description="")
public class YardInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "ID号")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "车场编码")
    private String yardCode;

    @ApiModelProperty(value = "车场名称")
    private String yardName;

    @ApiModelProperty(value = "车场编号")
    private Integer yardNo;

    @ApiModelProperty(value = "关联的短信模板ID")
    private Integer smsTemplateId;

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
