package com.parkingmanage.service.impl;

import com.parkingmanage.entity.Department;
import com.parkingmanage.mapper.DepartmentMapper;
import com.parkingmanage.service.DepartmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 <p>
 部门管理 服务实现类
 </p>

 @author lzx
 @since 2024-02-27
*/
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

}
