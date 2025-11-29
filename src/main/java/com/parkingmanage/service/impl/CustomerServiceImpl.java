package com.parkingmanage.service.impl;

import com.parkingmanage.entity.Customer;
import com.parkingmanage.mapper.CustomerMapper;
import com.parkingmanage.service.CustomerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 <p>
 客户管理 服务实现类
 </p>

 @author yuli
 @since 2022-02-28
*/
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

}
