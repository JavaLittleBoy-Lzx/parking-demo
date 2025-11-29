package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.YardSmsTemplateRelation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 车场短信模板关联 Mapper 接口
 * </p>
 *
 * @author 系统管理员
 */
public interface YardSmsTemplateRelationMapper extends BaseMapper<YardSmsTemplateRelation> {

    /**
     * 根据车场ID查询关联的模板ID列表
     */
    List<Integer> selectTemplateIdsByYardId(@Param("yardId") Integer yardId);

    /**
     * 删除车场的所有模板关联
     */
    int deleteByYardId(@Param("yardId") Integer yardId);

    /**
     * 批量插入关联关系
     */
    int batchInsert(@Param("relations") List<YardSmsTemplateRelation> relations);
}

