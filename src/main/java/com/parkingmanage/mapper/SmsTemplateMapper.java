package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.SmsTemplate;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 短信模板 Mapper 接口
 * </p>
 *
 * @author 系统管理员
 */
public interface SmsTemplateMapper extends BaseMapper<SmsTemplate> {

    /**
     * 检查模板CODE是否重复
     */
    int checkDuplicateTemplateCode(@Param("templateCode") String templateCode, @Param("id") Integer id);

    /**
     * 根据模板类型查询模板列表
     */
    List<SmsTemplate> selectByTemplateType(@Param("templateType") Integer templateType);

    /**
     * 根据模板名称模糊查询
     */
    List<SmsTemplate> selectByTemplateName(@Param("templateName") String templateName);
}

