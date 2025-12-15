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
 * 部门管理
 * </p>
 *
 * @author yuli
 * @since 2022-02-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Department对象", description="部门管理")
public class Department implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键id")
    @TableId(value = "department_id", type = IdType.AUTO)
    private Integer departmentId;

    @ApiModelProperty(value = "部门名称")
    private String departmentName;

    @ApiModelProperty(value = "地址")
    private String departmentAddress;

    @ApiModelProperty(value = "负责人")
    private String leader;

    @ApiModelProperty(value = "联系电话")
    private String leaderPhone;

    @ApiModelProperty(value = "是否有效，1 有效，0 失效")
    private Integer deleted;
}