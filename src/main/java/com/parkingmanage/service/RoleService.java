package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.Role;
import com.parkingmanage.vo.RolePermVo;
import com.parkingmanage.vo.RoleSidebarVo;

import java.util.List;

/**
 <p>
 角色 服务类
 </p>

 @author yuli
 @since 2022-02-27
*/
public interface RoleService extends IService<Role> {
    /**
     查询权限
     @param id
     @return
    */

    List<RolePermVo> findPermById(Integer id);

    /**
     查询导航
     @param id
     @return
    */

    List<RoleSidebarVo> findSidebarById(Integer id);

    /**
     保存角色权限
     @param id
     @param permission
     @return
    */


    boolean updatePermById(Integer id, String permission);

    /**
     新增角色
     @param role
    */
    void  insertRole(Role role);

}
