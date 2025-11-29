package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Member;
import com.parkingmanage.service.MemberService;
import com.parkingmanage.utils.PageUtils;
import com.parkingmanage.utils.ResourceUtil;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 <p>member
  前端控制器
 </p>

 @author MLH
 @since 2022-07-13
*/
@RestController
@RequestMapping("parking/member")
public class MemberController {
    @Resource
    private MemberService memberService;

    @ApiOperation("审核通过")
    @PutMapping("/auditMember")
    public ResponseEntity<Result> auditMember(@RequestBody Member member) {
        System.out.println(member);
        memberService.updateMember(member);
        return ResponseEntity.ok(new Result());
    }
    @ApiOperation("查询单条")
    @GetMapping("/{openid}")
    public R<Map<String,Object>> findByOpenid(@PathVariable String openid) {
        Member member=memberService.getMemberByOpenId(openid);
        Map<String,Object>  wxMap=new HashMap<>();
        if (member == null) {
            wxMap.put("count",0);
            wxMap.put("nickname","");
            wxMap.put("avata·hone","");
            wxMap.put("memberno","");
        }else {
            wxMap.put("count",1);
            wxMap.put("nickname",member.getNickname());
            wxMap.put("avatarurl",member.getAvatarurl());
            wxMap.put("userphone",member.getUserphone());
            wxMap.put("memberno",member.getMemberno());
        }

        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("data",wxMap);
        return R.ok(dataMap);

    }

    @ApiOperation("添加")
    @PostMapping("/addMember")
    public R<Object> addMember(@RequestBody Member member) {
        LocalDateTime time= LocalDateTime.now();
        member.setApplydate(time);
        return ResourceUtil.buildR(memberService.save(member));
    }
    @ApiOperation("分页查询")
    @GetMapping("/mypage")
    public IPage<Member> myFindPage(
            @RequestParam(required = false) String username,
            @RequestParam(required = false, value = "community") String community,
            @RequestParam(required = false, value = "applydate") String applydate,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Member> memberList = memberService.queryListMember(username, community, applydate);

        //按照设备名和申请日期排序
        List<Member> asServices = memberList.stream().sorted(Comparator.comparing(Member::getNickname).thenComparing(Member::getApplydate)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }
    @ApiOperation("分页查询")
    @GetMapping("/managepage")
    public IPage<Member> managePage(
            @RequestParam(required = false) String username,
            @RequestParam(required = false, value = "community") String community,
            @RequestParam(required = false, value = "applydate") String applydate,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Member> memberList = memberService.manageListMember(username, community, applydate);

        //按照设备名和申请日期排序
        List<Member> asServices = memberList.stream().sorted(Comparator.comparing(Member::getNickname).thenComparing(Member::getApplydate)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }
}

