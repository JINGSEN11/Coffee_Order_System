package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.RoleCreateDTO;
import com.vincent.entity.Menu;
import com.vincent.entity.Role;
import com.vincent.entity.RoleMenu;
import com.vincent.mapper.MenuMapper;
import com.vincent.mapper.RoleMapper;
import com.vincent.mapper.RoleMenuMapper;
import com.vincent.service.RoleService;
import com.vincent.vo.MenuVO;
import com.vincent.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;

    @Override
    public List<RoleVO> listAll() {
        List<Role> roles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>().orderByDesc(Role::getCreatedAt)
        );
        return roles.stream().map(r -> {
            RoleVO vo = new RoleVO();
            BeanUtils.copyProperties(r, vo);
            vo.setMenuList(getMenuTreeByRoleId(r.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public RoleVO getRoleDetail(Long id) {
        Role role = getById(id);
        if (role == null) throw new ServiceException("角色不存在");
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(role, vo);
        vo.setMenuList(getMenuTreeByRoleId(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(RoleCreateDTO dto) {
        Role role = new Role();
        BeanUtils.copyProperties(dto, role);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        save(role);

        // 保存角色-菜单关联
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            assignMenus(role.getId(), dto.getMenuIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, RoleCreateDTO dto) {
        Role role = getById(id);
        if (role == null) throw new ServiceException("角色不存在");
        BeanUtils.copyProperties(dto, role);
        role.setId(id);
        role.setUpdatedAt(LocalDateTime.now());
        updateById(role);

        // 全量更新角色-菜单关联
        if (dto.getMenuIds() != null) {
            assignMenus(id, dto.getMenuIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        if (getById(id) == null) throw new ServiceException("角色不存在");
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id));
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        // 清空旧的关联
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));

        // 插入新的关联
        if (menuIds != null && !menuIds.isEmpty()) {
            List<RoleMenu> list = menuIds.stream().map(menuId -> {
                RoleMenu rm = new RoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                return rm;
            }).collect(Collectors.toList());
            list.forEach(roleMenuMapper::insert);
        }
    }

    private List<MenuVO> getMenuTreeByRoleId(Long roleId) {
        // 查出该角色关联的菜单ID
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId)
        );
        if (roleMenus.isEmpty()) return Collections.emptyList();

        List<Long> menuIds = roleMenus.stream().map(RoleMenu::getMenuId).collect(Collectors.toList());

        // 查出所有菜单并构建树
        List<Menu> allMenus = menuMapper.selectList(
                new LambdaQueryWrapper<Menu>().in(Menu::getId, menuIds).orderByAsc(Menu::getSort)
        );
        return buildMenuTree(allMenus, 0L);
    }

    private List<MenuVO> buildMenuTree(List<Menu> menus, Long parentId) {
        List<MenuVO> tree = new ArrayList<>();
        for (Menu menu : menus) {
            if (menu.getParentId() != null && menu.getParentId().equals(parentId)) {
                MenuVO vo = new MenuVO();
                BeanUtils.copyProperties(menu, vo);
                vo.setChildren(buildMenuTree(menus, menu.getId()));
                tree.add(vo);
            }
        }
        return tree;
    }

}