package com.parkingmanage.service;

import com.parkingmanage.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 <p>
  服务类
 </p>

 @author yuli
 @since 2022-02-27
*/
public interface UserService extends IService<User> {
    /**
          用户登录
     @param loginName
     @param password
     @return
    */
    User login(String loginName, String password);
    
    /**
     * 验证用户密码
     * @param user 用户对象
     * @param password 密码
     * @return 是否匹配
     */
    boolean verifyPassword(User user, String password);
}
