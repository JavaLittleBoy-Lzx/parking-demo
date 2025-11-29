package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Role;
import com.parkingmanage.entity.Butler;
import com.parkingmanage.entity.Patrol;
import com.parkingmanage.service.RoleService;
import com.parkingmanage.service.ButlerService;
import com.parkingmanage.service.PatrolService;
import com.parkingmanage.vo.RolePermVo;
import com.parkingmanage.vo.RoleSidebarVo;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 <p>
 角色 前端控制器
 </p>

 @author yuli
 @since 2022-02-27
*/
@RestController
@RequestMapping("/parking/role")
public class RoleController {

    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    @Resource
    private RoleService roleService;

    @Resource
    private ButlerService butlerService;

    @Resource
    private PatrolService patrolService;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertRole(@RequestBody Role role) {
        roleService.insertRole(role);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("修改")
    @PutMapping
    public boolean update(@RequestBody Role role) {
        return roleService.updateById(role);
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return roleService.removeById(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/listAll")
    public List<Role> findAll() {
        return roleService.list();
    }

    @ApiOperation("查询单条")
    @GetMapping("/{id}")
    public Role findById(@PathVariable String id) {
        return roleService.getById(id);
    }

    @ApiOperation("保存角色权限")
    @PostMapping("/perm/{id}")
    public boolean updatePermById(@PathVariable Integer id, @RequestParam String permission) {
        return roleService.updatePermById(id, permission);
    }

    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<Role> findPage(@RequestParam(required = false) String name,
                                @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return roleService.page(new Page<>(pageNum, pageSize), Wrappers.<Role>lambdaQuery().like(Role::getName, name));
    }

    @ApiOperation("查询权限")
    @GetMapping("/perm/{roleId}")
    public List<RolePermVo> findPermById(@PathVariable Integer roleId) {
        return roleService.findPermById(roleId);
    }

    @ApiOperation("根据用户角色id查询导航")
    @GetMapping("/sidebar/querySidebarById")
        //todo 无法记录上一次保存记录
    public List<RoleSidebarVo> querySidebarById(@RequestParam(value = "id") Integer id) {
        List<RoleSidebarVo> roleSidebarVos = roleService.findSidebarById(id);
        return roleSidebarVos;
    }

    // @ApiOperation("查询导航")
    // @GetMapping("/sidebar/{id}")
    // public List<RoleSidebarVo> findSidebarById(@PathVariable Integer id) {
    //     return roleService.findSidebarById(id);
    // }

    @ApiOperation("查询所有角色权限分配下拉框")
    @GetMapping("/noAdmin")
    public List<Role> findAllNoAdmin() {
        return roleService.list();
    }

    /**
     * 🆕 新增：通过二维码验证用户角色
     * 用于管家和巡逻员的身份验证
     */
    @ApiOperation("通过二维码验证用户角色")
    @PostMapping("/verifyByQrCode")
    public ResponseEntity<Result<Map<String, Object>>> verifyRoleByQrCode(@RequestBody Map<String, Object> params) {
        Result<Map<String, Object>> result = new Result<>();

        try {
            String applyKind = (String) params.get("applyKind");
            String targetId = (String) params.get("targetId");
            String userPhone = (String) params.get("userPhone");

            logger.info("🔍 [角色验证] 接收验证请求: applyKind={}, targetId={}, userPhone={}",
                applyKind, targetId, userPhone != null ? userPhone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2") : "null");

            // 验证必要参数
            if (applyKind == null || targetId == null || userPhone == null) {
                result.setCode("1");
                result.setMsg("参数不完整：applyKind、targetId、userPhone都是必需的");
                logger.warn("⚠️ [角色验证] 参数不完整");
                return ResponseEntity.ok(result);
            }
            Map<String, Object> data = new HashMap<>();
            switch (applyKind) {
                case "3": // 管家验证
                    logger.info("👨‍💼 [管家验证] 开始验证管家身份: targetId={}", targetId);
                    Butler butler = butlerService.getById(targetId);

                    if (butler == null) {
                        // 🔧 管家不存在的情况
                        data.put("verified", false);
                        data.put("message", "二维码中的管家信息不存在，请确认二维码是否正确");
                        data.put("errorCode", "MANAGER_NOT_FOUND");
                        data.put("targetId", targetId);
                        result.setCode("1");
                        result.setMsg("管家不存在");
                        logger.warn("❌ [管家验证] 管家不存在: targetId={}", targetId);
                    } else if (!userPhone.equals(butler.getPhone())) {
                        // 🔧 手机号不匹配的情况
                        data.put("verified", false);
                        data.put("message", "您的手机号与该管家信息不匹配，请确认您是否为该管家");
                        data.put("errorCode", "PHONE_MISMATCH");
                        data.put("managerName", butler.getUsername());
                        data.put("expectedPhone", butler.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
                        data.put("actualPhone", userPhone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
                        result.setCode("1");
                        result.setMsg("手机号不匹配");
                        logger.warn("❌ [管家验证] 手机号不匹配: 管家={}, 期望手机号={}, 实际手机号={}", 
                            butler.getUsername(), 
                            butler.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"),
                            userPhone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
                    } else {
                        // ✅ 验证成功
                        data.put("verified", true);
                        data.put("role", "manager");
                        data.put("managerName", butler.getUsername());
                        data.put("managerData", butler);
                        result.setCode("0");
                        result.setMsg("管家身份验证成功");
                        logger.info("✅ [管家验证] 验证成功: 管家={}", butler.getUsername());
                    }
                    break;
                case "4": // 巡逻员验证
                    logger.info("👮‍♂️ [巡逻员验证] 开始验证巡逻员身份: targetId={}", targetId);
                    
                    // 🔧 首先检查目标巡逻员是否存在
                    Patrol targetPatrol = patrolService.getById(targetId);
                    if (targetPatrol == null) {
                        data.put("verified", false);
                        data.put("message", "二维码中的巡逻员信息不存在，请确认二维码是否正确");
                        data.put("errorCode", "PATROL_NOT_FOUND");
                        data.put("targetId", targetId);
                        result.setCode("1");
                        result.setMsg("巡逻员不存在");
                        logger.warn("❌ [巡逻员验证] 目标巡逻员不存在: targetId={}", targetId);
                        break;
                    }
                    
                    // 🔧 检查手机号是否匹配
                    if (userPhone.equals(targetPatrol.getPhone())) {
                        // ✅ 验证成功
                        data.put("verified", true);
                        data.put("role", "patrol");
                        data.put("patrolName", targetPatrol.getUsername());
                        data.put("patrolData", targetPatrol);
                        result.setCode("0");
                        result.setMsg("巡逻员身份验证成功");
                        logger.info("✅ [巡逻员验证] 验证成功: 巡逻员={}", targetPatrol.getUsername());
                    } else {
                        // ❌ 手机号不匹配 - 检查是否是其他巡逻员
                        List<Patrol> allPatrols = patrolService.list();
                        logger.info("🔍 [巡逻员验证] 数据库中共有{}个巡逻员", allPatrols.size());
                        
                        Patrol actualPatrol = null;
                        for (Patrol p : allPatrols) {
                            if (userPhone.equals(p.getPhone())) {
                                actualPatrol = p;
                                logger.info("🔍 [巡逻员验证] 找到匹配手机号的巡逻员: ID={}, 姓名={}", p.getId(), p.getUsername());
                                break;
                            }
                        }

                        if (actualPatrol != null) {
                            // 🔧 用户是巡逻员，但不是这个二维码对应的巡逻员
                            data.put("verified", false);
                            data.put("message", "您是巡逻员 " + actualPatrol.getUsername() + "，但此二维码是 " + targetPatrol.getUsername() + " 的，请使用正确的巡逻员二维码");
                            data.put("errorCode", "WRONG_PATROL_QR");
                            data.put("actualPatrolName", actualPatrol.getUsername());
                            data.put("targetPatrolName", targetPatrol.getUsername());
                            result.setCode("1");
                            result.setMsg("二维码不匹配");
                            logger.warn("❌ [巡逻员验证] 二维码不匹配: 用户是巡逻员{}，但扫描的是{}的二维码", 
                                actualPatrol.getUsername(), targetPatrol.getUsername());
                        } else {
                            // 🔧 用户不是任何巡逻员
                            data.put("verified", false);
                            data.put("message", "您不是巡逻员，无法使用巡逻员二维码。如果您认为这是错误，请联系管理员确认您的巡逻员身份");
                            data.put("errorCode", "NOT_PATROL");
                            data.put("targetPatrolName", targetPatrol.getUsername());
                            data.put("userPhone", userPhone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
                            result.setCode("1");
                            result.setMsg("非巡逻员用户");
                            logger.warn("❌ [巡逻员验证] 非巡逻员用户: 手机号{}不属于任何巡逻员", 
                                userPhone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
                        }
                    }
                    break;
                case "2": // 访客邀请（保持现有逻辑）
                    logger.info("👥 [访客验证] 访客邀请验证");
                    data.put("verified", true);
                    data.put("role", "visitor");
                    result.setCode("0");
                    result.setMsg("访客邀请验证成功");
                    break;
                default:
                    data.put("verified", false);
                    data.put("message", "未知的角色类型: " + applyKind);
                    result.setCode("1");
                    result.setMsg("未知的角色类型");
                    logger.warn("❓ [角色验证] 未知的applyKind: {}", applyKind);
                    break;
            }
            result.setData(data);
        } catch (Exception e) {
            logger.error("❌ [角色验证] 验证过程中发生异常", e);
            result.setCode("1");
            result.setMsg("角色验证失败: " + e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("verified", false);
            errorData.put("message", "系统异常，请稍后重试");
            result.setData(errorData);
        }
        return ResponseEntity.ok(result);
    }
}