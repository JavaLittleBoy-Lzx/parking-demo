package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 小区基本信息实体类
 * 用于存储小区的基本信息和图片，避免在 community 表中冗余存储
 * 
 * @author system
 * @since 2024-12-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("community_info")
@ApiModel(value = "CommunityInfo对象", description = "小区基本信息表")
public class CommunityInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Integer id;

    @ApiModelProperty(value = "省份")
    private String province;

    @ApiModelProperty(value = "城市")
    private String city;

    @ApiModelProperty(value = "区县")
    private String district;

    @ApiModelProperty(value = "小区名称")
    private String community;

    @ApiModelProperty(value = "小区主图URL")
    private String mainImage;

    @ApiModelProperty(value = "小区图片列表，JSON格式")
    private String images;

    @ApiModelProperty(value = "小区描述")
    private String description;

    @ApiModelProperty(value = "详细地址")
    private String address;

    @ApiModelProperty(value = "创建时间")
    private String createdAt;

    @ApiModelProperty(value = "更新时间")
    private String updatedAt;
}
