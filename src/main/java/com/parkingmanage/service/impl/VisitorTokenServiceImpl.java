package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.VisitorToken;
import com.parkingmanage.mapper.VisitorTokenMapper;
import com.parkingmanage.service.VisitorTokenService;
import org.springframework.stereotype.Service;

/**
 * 访客Token服务实现类
 * @author System
 * @since 2025-11-23
 */
@Service
public class VisitorTokenServiceImpl extends ServiceImpl<VisitorTokenMapper, VisitorToken> 
    implements VisitorTokenService {
    
}
