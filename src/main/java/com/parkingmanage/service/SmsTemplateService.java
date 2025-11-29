package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.SmsTemplate;

import java.util.List;

/**
 * <p>
 * 短信模板服务类
 * </p>
 *
 * @author 系统管理员
 */
public interface SmsTemplateService extends IService<SmsTemplate> {

    /**
     * 检查模板CODE是否重复
     */
    boolean checkDuplicateTemplateCode(String templateCode, Integer id);

    /**
     * 根据模板类型查询模板列表
     */
    List<SmsTemplate> getByTemplateType(Integer templateType);

    /**
     * 根据模板名称模糊查询
     */
    List<SmsTemplate> getByTemplateName(String templateName);

    /**
     * 分页查询短信模板
     */
    List<SmsTemplate> queryListSmsTemplate(String templateName, Integer templateType);
}

