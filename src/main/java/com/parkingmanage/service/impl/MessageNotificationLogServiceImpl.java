package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.MessageNotificationLog;
import com.parkingmanage.mapper.MessageNotificationLogMapper;
import com.parkingmanage.service.MessageNotificationLogService;
import org.springframework.stereotype.Service;

/**
 * 消息提醒记录Service实现类
 * 
 * @author System
 */
@Service
public class MessageNotificationLogServiceImpl extends ServiceImpl<MessageNotificationLogMapper, MessageNotificationLog> 
        implements MessageNotificationLogService {

} 