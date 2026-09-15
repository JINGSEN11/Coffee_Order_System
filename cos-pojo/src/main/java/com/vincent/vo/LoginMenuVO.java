package com.vincent.vo;

import lombok.Data;

import java.util.List;

/**
 * 管理端动态菜单与权限，对应前端 stores/user.js 的 loadMenus()：
 * <pre>
 *   const res = await authApi.menus()
 *   this.menus = res.menus; this.perms = res.perms; this.roleName = res.role_name
 * </pre>
 */
@Data
public class LoginMenuVO {

    /** 该角色可见的菜单树（已排除 type=3 的按钮节点） */
    private List<MenuVO> menus;

    /** 该角色的按钮权限码集合，超级管理员为 ["*:*:*"] */
    private List<String> perms;

    /** 角色名称 */
    private String roleName;
}
