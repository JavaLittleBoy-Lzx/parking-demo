package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 违规描述模板实体类
 * @author system
 * @date 2025-01-31
 */
@Data
@TableName("violation_descriptions")
public class ViolationDescription {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 违规描述内容
     */
    private String descriptionText;
    
    /**
     * 关联的违规类型代码（为空表示通用）
     */
    private String violationTypeCode;
    
    /**
     * 所属车场名称（为空表示通用）
     */
    private String parkName;
    
    /**
     * 使用次数
     */
    private Integer usageCount;
    
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

