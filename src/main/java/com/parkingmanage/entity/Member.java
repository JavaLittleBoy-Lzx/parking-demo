package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author MLH
 * @since 2022-07-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Member对象", description="")
public class Member implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String memberno;
    private String nickname;
    private String avatarurl;
    private String userkind;
    private String userphone;
    private LocalDateTime applydate;

}
