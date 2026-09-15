package com.vincent.service;

import java.util.Set;

/**
 * RBAC 权限查询。管理端登录（返回菜单与权限码）与接口鉴权（AdminAuthInterceptor）
 * 都从这里取数，保证「页面上能看到的」和「接口上能调的」用的是同一份口径。
 */
public interface PermissionService {

    /** 内置超级管理员角色 ID */
    long SUPER_ROLE_ID = 1L;

    /** 通配权限码 */
    String PERM_ALL = "*:*:*";

    /** 该角色是否为内置超级管理员 */
    boolean isSuper(Long roleId);

    /** 该角色拥有的菜单 ID 集合（超级管理员为全部启用菜单） */
    Set<Long> ownedMenuIds(Long roleId);

    /** 该角色的权限码集合（超级管理员为 ["*:*:*"]） */
    Set<String> permsOf(Long roleId);

    /** 该角色是否命中 required 中的任意一个权限码 */
    boolean hasAny(Long roleId, String... required);

}
