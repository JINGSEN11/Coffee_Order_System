package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.RoleCreateDTO;
import com.vincent.entity.Role;
import com.vincent.vo.RoleVO;

import java.util.List;

public interface RoleService extends IService<Role> {

    List<RoleVO> listAll();

    RoleVO getRoleDetail(Long id);

    void createRole(RoleCreateDTO dto);

    void updateRole(Long id, RoleCreateDTO dto);

    void deleteRole(Long id);

    void assignMenus(Long roleId, List<Long> menuIds);
}