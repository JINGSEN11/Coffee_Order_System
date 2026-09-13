package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.exception.ServiceException;
import com.vincent.entity.Menu;
import com.vincent.entity.RoleMenu;
import com.vincent.mapper.MenuMapper;
import com.vincent.mapper.RoleMenuMapper;
import com.vincent.service.MenuService;
import com.vincent.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;

    @Override
    public List<MenuVO> tree() {
        List<Menu> allMenus = menuMapper.selectList(
                new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSort)
        );
        return buildTree(allMenus, 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createMenu(Menu menu) {
        menu.setCreatedAt(LocalDateTime.now());
        menu.setUpdatedAt(LocalDateTime.now());
        save(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(Menu menu) {
        Menu exist = getById(menu.getId());
        if (exist == null) throw new ServiceException("菜单不存在");
        menu.setUpdatedAt(LocalDateTime.now());
        updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        Menu menu = getById(id);
        if (menu == null) throw new ServiceException("菜单不存在");

        // 检查是否有子菜单
        Long childCount = menuMapper.selectCount(
                new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id)
        );
        if (childCount > 0) throw new ServiceException("请先删除子菜单");

        // 清理角色-菜单关联
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getMenuId, id));

        removeById(id);
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        List<RoleMenu> list = roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId)
        );
        return list.stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
    }

    private List<MenuVO> buildTree(List<Menu> menus, Long parentId) {
        List<MenuVO> tree = new ArrayList<>();
        for (Menu menu : menus) {
            if (menu.getParentId() != null && menu.getParentId().equals(parentId)) {
                MenuVO vo = new MenuVO();
                BeanUtils.copyProperties(menu, vo);
                vo.setChildren(buildTree(menus, menu.getId()));
                tree.add(vo);
            }
        }
        return tree;
    }

}