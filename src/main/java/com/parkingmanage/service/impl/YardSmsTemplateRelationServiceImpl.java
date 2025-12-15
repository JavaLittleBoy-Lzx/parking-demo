package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.SmsTemplate;
import com.parkingmanage.entity.YardSmsTemplateRelation;
import com.parkingmanage.mapper.SmsTemplateMapper;
import com.parkingmanage.mapper.YardSmsTemplateRelationMapper;
import com.parkingmanage.service.YardSmsTemplateRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 车场短信模板关联服务实现类
 * </p>
 *
 * @author 系统管理员
 */
@Service
public class YardSmsTemplateRelationServiceImpl extends ServiceImpl<YardSmsTemplateRelationMapper, YardSmsTemplateRelation> 
        implements YardSmsTemplateRelationService {

    @Autowired
    private SmsTemplateMapper smsTemplateMapper;

    @Override
    public List<Integer> getTemplateIdsByYardId(Integer yardId) {
        return baseMapper.selectTemplateIdsByYardId(yardId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateYardTemplates(Integer yardId, List<Integer> templateIds) {
        // 1. 先物理删除该车场的所有模板关联（避免重复）
        baseMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<YardSmsTemplateRelation>()
                .eq("yard_id", yardId));
        
        // 2. 如果模板ID列表为空，则直接返回
        if (templateIds == null || templateIds.isEmpty()) {
            return true;
        }
        
        // 3. 去重处理（防止传入重复的模板ID）
        List<Integer> uniqueTemplateIds = new java.util.ArrayList<>(new java.util.LinkedHashSet<>(templateIds));
        
        // 4. 批量插入新的关联关系
        List<YardSmsTemplateRelation> relations = new ArrayList<>();
        for (int i = 0; i < uniqueTemplateIds.size(); i++) {
            YardSmsTemplateRelation relation = new YardSmsTemplateRelation();
            relation.setYardId(yardId);
            relation.setSmsTemplateId(uniqueTemplateIds.get(i));
            // 第一个模板设为默认
            relation.setIsDefault(i == 0 ? 1 : 0);
            relation.setDeleted(0);
            relations.add(relation);
        }
        
        return baseMapper.batchInsert(relations) > 0;
    }

    @Override
    public boolean deleteByYardId(Integer yardId) {
        return baseMapper.deleteByYardId(yardId) > 0;
    }
    
    @Override
    public List<SmsTemplate> getSmsTemplatesByYardId(Integer yardId) {
        // 1. 查询车场关联的模板ID列表
        List<Integer> templateIds = getTemplateIdsByYardId(yardId);
        
        // 2. 如果没有关联模板，返回空列表
        if (templateIds == null || templateIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 3. 批量查询短信模板完整信息
        return templateIds.stream()
                .map(templateId -> smsTemplateMapper.selectById(templateId))
                .filter(template -> template != null && template.getDeleted() == 0)
                .collect(Collectors.toList());
    }
}

