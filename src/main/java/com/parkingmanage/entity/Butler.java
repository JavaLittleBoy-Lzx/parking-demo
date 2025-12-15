package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author lzx
 * @since 2023-02-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Butler对象", description="")
public class Butler implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String usercode;

    private String username;

    private String phone;

    private String province;

    private String city;

    private String district;

    private String community;

    private LocalDateTime createdate;

    private String createman;

    private String status;

    private LocalDateTime auditdate;

    private String openid;

    private LocalDateTime confirmdate;
}
