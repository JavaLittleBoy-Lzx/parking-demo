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
 * 车场短信模板关联实体类
 * </p>
 *
 * @author 系统管理员
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="YardSmsTemplateRelation对象", description="车场短信模板关联")
public class YardSmsTemplateRelation implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "车场ID")
    private Integer yardId;

    @ApiModelProperty(value = "短信模板ID")
    private Integer smsTemplateId;

    @ApiModelProperty(value = "模板用途：1-违规提醒，2-停车超时，3-其他")
    private Integer templateUsage;

    @ApiModelProperty(value = "是否为默认模板：0-否，1-是")
    private Integer isDefault;

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

