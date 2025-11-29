package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 违规类型配置实体类
 * @author system
 * @date 2025-01-31
 */
@Data
@TableName("violation_types")
public class ViolationType {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 违规类型名称
     */
    private String typeName;
    
    /**
     * 违规类型代码
     */
    private String typeCode;
    
    /**
     * 所属车场名称（为空表示通用）
     */
    private String parkName;
    
    /**
     * 严重程度（mild-轻微，moderate-中等，severe-严重）
     */
    private String severityLevel;
    
    /**
     * 图标名称
     */
    private String icon;
    
    /**
     * 类型描述
     */
    private String description;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder;
    
    /**
     * 是否启用（0-禁用，1-启用）
     */
    private Boolean isEnabled;
    
    /**
     * 创建人
     */
    private String createdBy;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

