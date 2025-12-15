package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.Member;
import com.parkingmanage.mapper.MemberMapper;
import com.parkingmanage.service.MemberService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 <p>
  服务实现类
 </p>

 @author MLH
 @since 2022-07-13
*/

@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {
    @Resource
    private MemberService memberService;

    @Override
    public void updateMember(Member member) {
        if (member.getId() != null ) {
            Member member1 = memberService.getById(member.getId());
            if (ObjectUtils.isNotEmpty(member1)) {
                member=member1;
            }
        }
        memberService.updateById(member);
    }

    @Override
    public Member getMemberByOpenId(String openid) {
        return baseMapper.selectMemberByOpenId(openid);
    }
    @Override
    public List<Member> queryListMember(String username, String community, String applydate) {
        LambdaQueryWrapper<Member> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.apply("userkind!='访客'");
        if (StringUtils.hasLength(username)) {
            queryWrapper.like(Member::getNickname, username);
        }
        if (StringUtils.hasLength(applydate)) {
            queryWrapper.apply("DATE_FORMAT(applydate,'%Y-%m-%d %H:%i:%s')='" + applydate + "' ");
        }
        List<Member> members = memberService.list(queryWrapper);
        return members;
    }
    @Override
    public List<Member> manageListMember(String username, String community, String applydate) {

        LambdaQueryWrapper<Member> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.apply("userkind!='访客' and auditstatus='已通过' ");
        if (StringUtils.hasLength(username)) {
            queryWrapper.like(Member::getNickname, username);
        }
        if (StringUtils.hasLength(applydate)) {
            queryWrapper.apply("DATE_FORMAT(applydate,'%Y-%m-%d %H:%i:%s')='" + applydate + "' ");
        }
        List<Member> members = memberService.list(queryWrapper);
        return members;
    }
}
