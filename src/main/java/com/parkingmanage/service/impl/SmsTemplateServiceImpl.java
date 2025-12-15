package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.SmsTemplate;
import com.parkingmanage.mapper.SmsTemplateMapper;
import com.parkingmanage.service.SmsTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 短信模板服务实现类
 * </p>
 *
 * @author 系统管理员
 */
@Service
public class SmsTemplateServiceImpl extends ServiceImpl<SmsTemplateMapper, SmsTemplate> implements SmsTemplateService {

    @Override
    public boolean checkDuplicateTemplateCode(String templateCode, Integer id) {
        return baseMapper.checkDuplicateTemplateCode(templateCode, id) > 0;
    }

    @Override
    public List<SmsTemplate> getByTemplateType(Integer templateType) {
        return baseMapper.selectByTemplateType(templateType);
    }

    @Override
    public List<SmsTemplate> getByTemplateName(String templateName) {
        return baseMapper.selectByTemplateName(templateName);
    }

    @Override
    public List<SmsTemplate> queryListSmsTemplate(String templateName, Integer templateType) {
        LambdaQueryWrapper<SmsTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmsTemplate::getDeleted, 0);
        
        if (templateName != null && !templateName.trim().isEmpty()) {
            wrapper.like(SmsTemplate::getTemplateName, templateName);
        }
        
        if (templateType != null) {
            wrapper.eq(SmsTemplate::getTemplateType, templateType);
        }
        
        wrapper.orderByDesc(SmsTemplate::getGmtCreate);
        
        return baseMapper.selectList(wrapper);
    }
}

