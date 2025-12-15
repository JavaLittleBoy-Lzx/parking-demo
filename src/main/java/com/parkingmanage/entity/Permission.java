package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 权限
 * </p>
 *
 * @author yuli
 * @since 2022-02-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Permission对象", description="权限")
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
 
    @ApiModelProperty(value = "权限名")
    @TableField("NAME")
    private String name;

    @ApiModelProperty(value = "路径")
    @TableField("PATH")
    private String path;

    @ApiModelProperty(value = "逻辑删除")
    @TableField("DELETED")
    private Integer deleted;

    @ApiModelProperty(value = "父级ID")
    @TableField("PID")
    private Integer pid;


}
