package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vincent.entity.Menu;
import com.vincent.entity.RoleMenu;
import com.vincent.mapper.MenuMapper;
import com.vincent.mapper.RoleMenuMapper;
import com.vincent.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限查询实现。
 *
 * 每次调用都回表，不缓存 —— 角色授权（/admin/role/{id}/assign-menus）改动后立即生效，
 * 不需要等缓存过期，也不会出现「刚收回权限但接口还能调」的窗口。
 * 管理端接口量级很小，两次主键查询的成本可以忽略。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;

    @Override
    public boolean isSuper(Long roleId) {
        return roleId != null && roleId == SUPER_ROLE_ID;
    }

    @Override
    public Set<Long> ownedMenuIds(Long roleId) {
        if (isSuper(roleId)) {
            return menuMapper.selectList(new LambdaQueryWrapper<Menu>().eq(Menu::getStatus, 1))
                    .stream().map(Menu::getId).collect(Collectors.toSet());
        }
        if (roleId == null) {
            return new HashSet<>();
        }
        List<RoleMenu> relations = roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId)
        );
        return relations.stream().map(RoleMenu::getMenuId).collect(Collectors.toSet());
    }

    @Override
    public Set<String> permsOf(Long roleId) {
        if (isSuper(roleId)) {
            return Set.of(PERM_ALL);
        }
        Set<Long> owned = ownedMenuIds(roleId);
        if (owned.isEmpty()) {
            return Set.of();
        }
        return menuMapper.selectList(
                        new LambdaQueryWrapper<Menu>()
                                .in(Menu::getId, owned)
                                .isNotNull(Menu::getPerms)
                ).stream()
                .map(Menu::getPerms)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public boolean hasAny(Long roleId, String... required) {
        if (required == null || required.length == 0) {
            return true;
        }
        Set<String> owned = permsOf(roleId);
        if (owned.contains(PERM_ALL)) {
            return true;
        }
        return Arrays.stream(required)
                .filter(StringUtils::hasText)
                .anyMatch(owned::contains);
    }

}
