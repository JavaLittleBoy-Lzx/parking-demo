package com.parkingmanage.mapper;

import com.parkingmanage.entity.Member;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 <p>
  Mapper 接口
 </p>

 @author MLH
 @since 2022-07-13
*/
public interface MemberMapper extends BaseMapper<Member> {
    Member selectMemberByOpenId(String openid);
}
