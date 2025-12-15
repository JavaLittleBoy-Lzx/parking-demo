package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 拉黑原因模板实体类
 * @author system
 * @date 2025-01-31
 */
@Data
@TableName("blacklist_reasons")
public class BlacklistReason {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 拉黑原因内容
     */
    private String reasonText;
    
    /**
     * 原因分类（violation-违规，security-安全，other-其他）
     */
    private String reasonCategory;
    
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

