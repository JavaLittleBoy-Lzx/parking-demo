package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.SmsTemplate;
import com.parkingmanage.entity.YardSmsTemplateRelation;

import java.util.List;

/**
 * <p>
 * 车场短信模板关联服务类
 * </p>
 *
 * @author 系统管理员
 */
public interface YardSmsTemplateRelationService extends IService<YardSmsTemplateRelation> {

    /**
     * 根据车场ID查询关联的模板ID列表
     */
    List<Integer> getTemplateIdsByYardId(Integer yardId);

    /**
     * 更新车场的模板关联
     * @param yardId 车场ID
     * @param templateIds 模板ID列表
     */
    boolean updateYardTemplates(Integer yardId, List<Integer> templateIds);

    /**
     * 删除车场的所有模板关联
     */
    boolean deleteByYardId(Integer yardId);
    
    /**
     * 根据车场ID查询关联的短信模板完整信息
     * @param yardId 车场ID
     * @return 短信模板列表（包含code、signName等完整信息）
     */
    List<SmsTemplate> getSmsTemplatesByYardId(Integer yardId);
}

