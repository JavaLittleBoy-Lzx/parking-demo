package com.parkingmanage.entity;

import cn.hutool.core.annotation.Alias;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jdk.nashorn.internal.ir.annotations.Ignore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author lzx
 * @since 2023-12-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="BlackList对象", description="")
public class BlackList implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @Alias("车场名称")
    private String parkName;
    @Alias("有效期")
    private String blackListForeverFlag;
    @Alias("车牌号码")
    private String carCode;
    @Alias("车主姓名")
    private String owner;
    @Alias("黑名单原因")
    private String reason;
    @Alias("备注1")
    private String remark1;
    @Alias("备注2")
    private String remark2;
    @TableField(exist = false)
    private int specialCarTypeConfigId;
    @Alias("黑名单类型")
    private String specialCarTypeConfigName;
    
    /**
     * 黑名单类型编码
     */
    @ApiModelProperty(value = "黑名单类型编码")
    private String blacklistTypeCode;
    
    /**
     * 拉黑开始时间
     */
    @ApiModelProperty(value = "拉黑开始时间")
    private LocalDateTime blacklistStartTime;
    
    /**
     * 拉黑结束时间
     */
    @ApiModelProperty(value = "拉黑结束时间")
    private LocalDateTime blacklistEndTime;
    
    /**
     * 逻辑删除标志 0-未删除 1-已删除
     */
    @TableLogic
    @ApiModelProperty(value = "逻辑删除标志", notes = "0-未删除，1-已删除")
    private Integer deleted;
    
    /**
     * 删除时间
     */
    @ApiModelProperty(value = "删除时间")
    private LocalDateTime deleteTime;
    
    /**
     * 删除人
     */
    @ApiModelProperty(value = "删除人")
    private String deleteBy;

    // 重写toString方法
    @Override
    public String toString() {
        return "BlackList{" +
                "id=" + id +
                ", parkName=" + parkName +
                ", blackListForeverFlag=" + blackListForeverFlag +
                ", carCode=" + carCode +
                ", owner=" + owner +
                ", reason=" + reason +
                ", remark1=" + remark1 +
                ", remark2=" + remark2 +
                ", specialCarTypeConfigId="
                + specialCarTypeConfigId +
                ", specialCarTypeConfigName=" + specialCarTypeConfigName +
                ", blacklistTypeCode=" + blacklistTypeCode +
                ", blacklistStartTime=" + blacklistStartTime +
                ", blacklistEndTime=" + blacklistEndTime +
                ", deleted=" + deleted +
                ", deleteTime=" + deleteTime +
                ", deleteBy=" + deleteBy +
                "}";
    }
}
