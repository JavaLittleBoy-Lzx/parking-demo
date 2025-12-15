package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.Result;
import com.parkingmanage.dto.PatrolDutyRequest;
import com.parkingmanage.entity.Patrol;
import com.parkingmanage.entity.UserMapping;
import com.parkingmanage.mapper.UserMappingMapper;
import com.parkingmanage.service.CommunityService;
import com.parkingmanage.service.PatrolService;
import com.parkingmanage.utils.PageUtils;
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
 * <p>
 * 前端控制器
 * </p>
 * @author MLH
 * @since 2023-02-11
 */
@RestController
@RequestMapping("/parking/patrol")
public class PatrolController {
    @Resource
    private PatrolService patrolService;
    @Resource
    private CommunityService communityService;
    @Resource
    private UserMappingMapper userMappingMapper;

    @ApiOperation("查询单条")
    @GetMapping("/{openid}")
    public ResponseEntity<Result> findByOpenid(@PathVariable String openid) {
        Patrol patrol = patrolService.getPatrolByOpenId(openid);
        Result result = new Result();
        result.setData(patrol);
        return ResponseEntity.ok(result);
    }
    @ApiOperation("查询单条")
    @GetMapping("/getById")
    public ResponseEntity<Result> getById(@RequestParam(required = false) String id) {
        Patrol patrol = patrolService.getById(id);
        Result result = new Result();
        result.setData(patrol);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertPatrol(@RequestBody Patrol patrol) {
        int num = patrolService.duplicate(patrol);
        Result result = new Result();
        if (num == 0) {
            patrol.setCreatedate(LocalDateTime.now());
            patrol.setStatus("待确认");
            patrolService.save(patrol);
        } else {
            result.setCode("1");
            result.setMsg("数据重复，增加失败！");
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Patrol patrol) {
        int num = patrolService.duplicate(patrol);
        Result result = new Result();
        if (num == 0) {
            patrolService.updateById(patrol);
        } else {
            result.setCode("1");
            result.setMsg("数据重复，修改失败！");
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return patrolService.removeById(id);
    }

    @ApiOperation("分页查询")
    @GetMapping("/querypage")
    public IPage<Patrol> queryPage(
            @RequestParam(required = false) String username,
            @RequestParam(required = false, value = "community") String community,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Patrol> ownerList = patrolService.queryListPatrol(username, community);
        //按照设备名和申请日期排序
        List<Patrol> asServices = ownerList.stream().sorted(Comparator.comparing(Patrol::getUsername).thenComparing(Patrol::getCommunity)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }

    /**
     * 确认巡检员状态 - 根据手机号查询user_mapper获取openid并更新状态
     * @param phone 巡检员手机号
     * @return 操作结果
     */
    @ApiOperation("确认巡检员状态")
    @PostMapping("/confirmStatus")
    public ResponseEntity<Result> confirmPatrolStatus(@RequestParam String phone) {
        Result result = new Result();
        
        try {
            // 参数验证
            if (phone == null || phone.trim().isEmpty()) {
                result.setCode("1");
                result.setMsg("手机号不能为空");
                return ResponseEntity.ok(result);
            }
            
            phone = phone.trim();
            System.out.println("🔍 [巡检员状态确认] 开始确认巡检员状态，手机号: " + phone);
            
            // 1. 查询巡检员信息
            Patrol patrol = patrolService.getPatrolByPhone(phone);
            if (patrol == null) {
                result.setCode("1");
                result.setMsg("未找到对应的巡检员信息");
                System.out.println("❌ [巡检员状态确认] 未找到巡检员记录");
                return ResponseEntity.ok(result);
            }
            
            System.out.println("✅ [巡检员状态确认] 找到巡检员记录: ID=" + patrol.getId() + ", 姓名=" + patrol.getUsername());
            
            // 2. 查询user_mapper获取openid
            List<UserMapping> userMappings = userMappingMapper.findByPhone(phone);
            if (userMappings == null || userMappings.isEmpty()) {
                result.setCode("1");
                result.setMsg("该手机号尚未在微信小程序中授权，请先在小程序中完成手机号授权");
                System.out.println("❌ [巡检员状态确认] 未找到user_mapper记录");
                return ResponseEntity.ok(result);
            }
            
            UserMapping userMapping = userMappings.get(0);
            String openid = userMapping.getOpenid();
            System.out.println("✅ [巡检员状态确认] 找到openid: " + openid);
            
            // 3. 更新巡检员信息
            patrol.setOpenid(openid);
            patrol.setStatus("已确定");
            patrol.setConfirmdate(LocalDateTime.now());
            
            boolean updated = patrolService.updateById(patrol);
            
            if (updated) {
                result.setCode("0");
                result.setMsg("巡检员状态确认成功！openid已关联，状态已更新为\"已确定\"");
                
                Map<String, Object> data = new HashMap<>();
                data.put("id", patrol.getId());
                data.put("username", patrol.getUsername());
                data.put("phone", patrol.getPhone());
                data.put("openid", openid);
                data.put("status", "已确定");
                data.put("confirmdate", patrol.getConfirmdate());
                
                result.setData(data);
                System.out.println("✅ [巡检员状态确认] 状态更新成功");
            } else {
                result.setCode("1");
                result.setMsg("状态更新失败，请稍后重试");
                System.out.println("❌ [巡检员状态确认] 数据库更新失败");
            }
            
        } catch (Exception e) {
            result.setCode("1");
            result.setMsg("确认状态时发生错误: " + e.getMessage());
            System.err.println("❌ [巡检员状态确认] 异常: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 切换巡检员消息通知状态（值班开关）
     * @param request 请求参数（包含openid和enabled）
     * @return 操作结果
     */
    @ApiOperation("切换值班状态")
    @PostMapping("/toggleNotification")
    public ResponseEntity<Result> toggleNotificationStatus(
            @RequestBody PatrolDutyRequest request) {
        
        Result result = new Result();
        
        try {
            String openid = request.getOpenid();
            Integer enabled = request.getEnabled();
            
            System.out.println("🔄 [值班状态切换] 请求参数: " + request);
            System.out.println("🔄 [值班状态切换] openid: " + openid + ", 目标状态: " + (enabled == 1 ? "值班中" : "离岗"));
            
            // 1. 根据openid查询巡检员
            Patrol patrol = patrolService.getPatrolByOpenId(openid);
            if (patrol == null) {
                result.setCode("1");
                result.setMsg("未找到巡检员信息");
                System.out.println("⚠️ [值班状态切换] 未找到巡检员 - openid: " + openid);
                return ResponseEntity.ok(result);
            }
            
            // 2. 更新通知接收状态
            patrol.setNotificationEnabled(enabled);
            patrol.setLastStatusChangeTime(LocalDateTime.now());
            boolean updated = patrolService.updateById(patrol);
            
            if (updated) {
                // 4. 返回成功结果
                String statusText = enabled == 1 ? "值班中" : "离岗";
                result.setCode("0");
                result.setMsg("状态已更新为：" + statusText);
                
                Map<String, Object> data = new HashMap<>();
                data.put("patrolName", patrol.getUsername());
                data.put("community", patrol.getCommunity());
                data.put("notificationEnabled", enabled);
                data.put("statusText", statusText);
                data.put("changeTime", LocalDateTime.now());
                result.setData(data);
                
                System.out.println("✅ [值班状态切换] 成功 - 巡检员: " + patrol.getUsername() + 
                    ", 小区: " + patrol.getCommunity() + ", 新状态: " + statusText);
                
            } else {
                result.setCode("1");
                result.setMsg("状态更新失败，请稍后重试");
                System.out.println("❌ [值班状态切换] 数据库更新失败 - openid: " + openid);
            }
            
        } catch (Exception e) {
            result.setCode("1");
            result.setMsg("系统错误: " + e.getMessage());
            System.err.println("❌ [值班状态切换] 异常");
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 查询巡检员当前值班状态
     * @param openid 巡检员openid
     * @return 值班状态信息
     */
    @ApiOperation("查询值班状态")
    @GetMapping("/getDutyStatus")
    public ResponseEntity<Result> getDutyStatus(@RequestParam(required = false) String openid) {
        Result result = new Result();
        
        try {
            Patrol patrol = patrolService.getPatrolByOpenId(openid);
            if (patrol == null) {
                result.setCode("1");
                result.setMsg("未找到巡检员信息");
                return ResponseEntity.ok(result);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("notificationEnabled", patrol.getNotificationEnabled() != null ? patrol.getNotificationEnabled() : 1);
            data.put("statusText", (patrol.getNotificationEnabled() != null && patrol.getNotificationEnabled() == 1) ? "值班中" : "离岗");
            data.put("lastChangeTime", patrol.getLastStatusChangeTime());
            data.put("patrolName", patrol.getUsername());
            data.put("community", patrol.getCommunity());
            
            result.setCode("0");
            result.setData(data);
            
        } catch (Exception e) {
            result.setCode("1");
            result.setMsg("查询失败: " + e.getMessage());
            System.err.println("❌ [查询值班状态] 异常");
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 根据姓名和手机号查询巡检员信息（用于获取openid）
     * @param username 巡检员姓名
     * @param phone 巡检员手机号
     * @return 巡检员信息（包含openid）
     */
    @ApiOperation("根据姓名和手机号查询巡检员")
    @GetMapping("/getPatrolByInfo")
    public ResponseEntity<Result> getPatrolByInfo(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phone) {
        
        Result result = new Result();
        
        try {
            System.out.println("🔍 [查询巡检员] 姓名: " + username + ", 手机: " + phone);
            
            if ((username == null || username.trim().isEmpty()) && 
                (phone == null || phone.trim().isEmpty())) {
                result.setCode("1");
                result.setMsg("请提供姓名或手机号");
                return ResponseEntity.ok(result);
            }
            
            // 根据姓名或手机号查询
            Patrol patrol = null;
            if (phone != null && !phone.trim().isEmpty()) {
                // 优先使用手机号查询（更精确）
                patrol = patrolService.getPatrolByPhone(phone);
            } else if (username != null && !username.trim().isEmpty()) {
                // 使用姓名查询
                List<Patrol> patrolList = patrolService.queryListPatrol(username, null);
                if (patrolList != null && !patrolList.isEmpty()) {
                    patrol = patrolList.get(0); // 取第一个匹配的
                    if (patrolList.size() > 1) {
                        System.out.println("⚠️ [查询巡检员] 找到多个同名巡检员: " + patrolList.size() + " 个");
                    }
                }
            }
            
            if (patrol == null) {
                result.setCode("1");
                result.setMsg("未找到巡检员信息");
                System.out.println("⚠️ [查询巡检员] 未找到匹配记录");
                return ResponseEntity.ok(result);
            }
            
            // 返回巡检员信息
            Map<String, Object> data = new HashMap<>();
            data.put("id", patrol.getId());
            data.put("username", patrol.getUsername());
            data.put("usercode", patrol.getUsercode());
            data.put("phone", patrol.getPhone());
            data.put("openid", patrol.getOpenid());
            data.put("community", patrol.getCommunity());
            data.put("notificationEnabled", patrol.getNotificationEnabled() != null ? patrol.getNotificationEnabled() : 1);
            
            result.setCode("0");
            result.setMsg("查询成功");
            result.setData(data);
            
            System.out.println("✅ [查询巡检员] 成功 - 姓名: " + patrol.getUsername() + 
                ", openid: " + (patrol.getOpenid() != null ? patrol.getOpenid().substring(0, Math.min(10, patrol.getOpenid().length())) + "..." : "null"));
            
        } catch (Exception e) {
            result.setCode("1");
            result.setMsg("查询失败: " + e.getMessage());
            System.err.println("❌ [查询巡检员] 异常");
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(result);
    }
}

