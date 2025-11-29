package com.parkingmanage.service;

import com.parkingmanage.entity.Member;
import com.baomidou.mybatisplus.extension.service.IService;


import java.util.List;

/**
 <p>
  服务类
 </p>

 @author MLH
 @since 2022-07-13
*/
public interface MemberService extends IService<Member> {
    Member getMemberByOpenId(String openid);
    List<Member> queryListMember(String username, String community, String applydate);
    List<Member> manageListMember(String username, String community, String applydate);
    void updateMember(Member member);
}
