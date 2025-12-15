package com.parkingmanage.mapper;

import com.parkingmanage.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 <p>
  Mapper 接口
 </p>

 @author yuli
 @since 2022-02-27
*/
public interface UserMapper extends BaseMapper<User> {
    User selectByUserName (String userName);
}
