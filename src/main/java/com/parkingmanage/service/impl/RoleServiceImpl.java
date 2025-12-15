package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.common.exception.CustomException;
import com.parkingmanage.entity.Permission;
import com.parkingmanage.entity.Role;
import com.parkingmanage.mapper.RoleMapper;
import com.parkingmanage.service.PermissionService;
import com.parkingmanage.service.RoleService;
import com.parkingmanage.vo.RolePermVo;
import com.parkingmanage.vo.RoleSidebarVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 <p>
 角色 服务实现类
 </p>

 @author yuli
 @since 2022-02-27
*/
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
    @Resource
    private PermissionService permissionService;

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private RoleService roleService;

    @Override
    public List<RolePermVo> findPermById(Integer id) {
        List<Permission> permissionList = permissionService.list();
        //过滤pid为空情况
        List<Permission> permissionParentList = permissionList.stream().filter(p -> p.getPid() == null).collect(Collectors.toList());
        Role role = getById(id);

        Map<Integer, RolePermVo> permDataMap;
        if (StringUtils.hasText(role.getPermission())) {
            try {
                //json字符串转对象 objectMapper是jackson的类，专用来解析json的  =>  最转为map 对象
                permDataMap = objectMapper.readValue(role.getPermission(), new TypeReference<List<RolePermVo>>() {
                }).stream().collect(Collectors.toMap(RolePermVo::getId, s -> s));

            } catch (JsonProcessingException e) {
                log.error("Json解析失败");
                return null;
            }
        } else {
            permDataMap = new HashMap<>();
        }
        List<RolePermVo> permVoList = new ArrayList<>();
        for (Permission permission : permissionParentList) {
            RolePermVo rolePermVo = new RolePermVo();
            rolePermVo.setTitle(permission.getName());
            rolePermVo.setId(permission.getId());
            if (permDataMap.containsKey(permission.getId())) {
                RolePermVo permVo = permDataMap.get(permission.getId());
                rolePermVo.setCheckAll(permVo.getCheckAll());
                rolePermVo.setIsIndeterminate(permVo.getIsIndeterminate());
                rolePermVo.setCheckedList(permVo.getCheckedList());
            } else {
                rolePermVo.setCheckAll(false);
                rolePermVo.setIsIndeterminate(false);
                rolePermVo.setCheckedList(new ArrayList<>());
            }
            // 替换subs里面为最新的权限页面数据
            List<RolePermVo.Sub> subList = new ArrayList<>();
            List<Permission> permissionList1 = permissionList.stream().filter(p -> rolePermVo.getId().equals(p.getPid())).collect(Collectors.toList());
            for (Permission permissionChild : permissionList1) {
                RolePermVo.Sub sub = new RolePermVo.Sub();
                sub.setId(permissionChild.getId());
                sub.setTitle(permissionChild.getName());
                sub.setIndex(permissionChild.getPath());
                subList.add(sub);
            }
            rolePermVo.setSubs(subList);
            permVoList.add(rolePermVo);
        }
        return permVoList;
    }

    @Override
    public List<RoleSidebarVo> findSidebarById(Integer id) {
        Role role = this.getById(id);
        List<RoleSidebarVo> sidebarVoList=new ArrayList<>();
        if (StringUtils.hasText(role.getPermission())) {
            try {
                sidebarVoList = objectMapper.readValue(role.getPermission(), new TypeReference<List<RoleSidebarVo>>() {
                }).stream().filter(s -> !s.getCheckedList().isEmpty()).collect(Collectors.toList());
            } catch (JsonProcessingException e) {
                log.error("Json解析失败");
                return null;
            }
            // 替换subs里面为有权限的页面
            for (RoleSidebarVo rolePermVo : sidebarVoList) {
                List<Permission> permissionList = permissionService.listByIds(rolePermVo.getCheckedList());
                List<RolePermVo.Sub> subList = new ArrayList<>();
                for (Permission permission : permissionList) {
                    RolePermVo.Sub sub = new RolePermVo.Sub();
                    sub.setId(permission.getId());
                    sub.setTitle(permission.getName());
                    sub.setIndex(permission.getPath());
                    subList.add(sub);
                }
                rolePermVo.setSubs(subList);
                rolePermVo.setIcon("el-icon-lx-cascades");
                rolePermVo.setIndex(rolePermVo.getId().toString());
            }
            return sidebarVoList;

        } else {
            sidebarVoList = new ArrayList<>();
        }
        return sidebarVoList;
    }

    @Override
    public boolean updatePermById(Integer id, String permission) {
        Role role = getById(id);
        if (ObjectUtils.isEmpty(role)) {
            return false;
        }
        role.setPermission(permission);
        updateById(role);
        return true;
    }


    @Override
    public void insertRole(Role role) {
        if (role!= null && StringUtils.hasLength( role.getName())) {
            Role selectOne = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getName, role.getName()) );
            if (selectOne != null) {
                throw new CustomException("18","此角色已经存在,请勿重复添加");
            }
            else {
                save(role);
            }
        }
    }
}
