package com.parkingmanage.service.impl;

import com.parkingmanage.entity.Permission;
import com.parkingmanage.mapper.PermissionMapper;
import com.parkingmanage.service.PermissionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 <p>
 权限 服务实现类
 </p>

 @author yuli
 @since 2022-02-27
*/
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

}
