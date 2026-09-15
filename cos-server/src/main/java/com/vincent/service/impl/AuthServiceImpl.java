package com.vincent.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vincent.common.BaseContext;
import com.vincent.common.JwtUtil;
import com.vincent.common.exception.ServiceException;
import com.vincent.config.JwtProperties;
import com.vincent.dto.EmployeeLoginDTO;
import com.vincent.entity.Employee;
import com.vincent.entity.Menu;
import com.vincent.entity.Role;
import com.vincent.mapper.EmployeeMapper;
import com.vincent.mapper.MenuMapper;
import com.vincent.mapper.RoleMapper;
import com.vincent.service.AuthService;
import com.vincent.service.PermissionService;
import com.vincent.vo.AuthLoginVO;
import com.vincent.vo.LoginMenuVO;
import com.vincent.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端认证服务实现。
 * 约定：role_id = 1 为内置超级管理员，拥有全部菜单与通配权限 "*:*:*"。
 *
 * 权限数据的读取统一走 PermissionService，与 AdminAuthInterceptor 的接口鉴权同源，
 * 保证「登录后返回的菜单/权限码」和「接口实际放行的权限」不会各算各的。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    /** 菜单类型：3 = 按钮（不参与菜单树渲染，只提供权限码） */
    private static final int MENU_TYPE_BUTTON = 3;

    private final EmployeeMapper employeeMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final PermissionService permissionService;
    private final JwtProperties jwtProperties;

    @Override
    public AuthLoginVO login(EmployeeLoginDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
            throw new ServiceException("请输入账号和密码");
        }

        Employee employee = employeeMapper.selectOne(
                new LambdaQueryWrapper<Employee>().eq(Employee::getUsername, dto.getUsername())
        );
        if (employee == null) {
            throw new ServiceException("账号不存在");
        }
        if (employee.getStatus() == null || employee.getStatus() != 1) {
            throw new ServiceException("账号已被禁用，请联系管理员");
        }
        if (!passwordMatches(dto.getPassword(), employee.getPassword())) {
            throw new ServiceException("密码错误");
        }

        employee.setLastLoginAt(LocalDateTime.now());
        employeeMapper.updateById(employee);

        Role role = roleMapper.selectById(employee.getRoleId());
        String token = issueToken(employee);

        AuthLoginVO.UserInfo user = new AuthLoginVO.UserInfo();
        user.setId(employee.getId());
        user.setUsername(employee.getUsername());
        user.setRealName(employee.getRealName());
        user.setRoleId(employee.getRoleId());
        user.setRoleName(role == null ? null : role.getName());
        user.setShopId(employee.getShopId());

        AuthLoginVO vo = new AuthLoginVO();
        vo.setToken(token);
        vo.setUser(user);
        log.info("员工 {} 登录成功，角色：{}", employee.getUsername(), user.getRoleName());
        return vo;
    }

    @Override
    public LoginMenuVO menus(Long employeeId) {
        if (employeeId == null) {
            throw new ServiceException("登录状态无效，请重新登录");
        }
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new ServiceException("登录状态无效，请重新登录");
        }

        Long roleId = employee.getRoleId();
        Role role = roleId == null ? null : roleMapper.selectById(roleId);

        LoginMenuVO vo = new LoginMenuVO();
        vo.setMenus(menuTreeByRole(roleId));
        vo.setPerms(permsByRole(roleId));
        vo.setRoleName(role == null ? null : role.getName());
        return vo;
    }
    @Override
    public String refresh(String username) {
        Employee employee = employeeMapper.selectOne(
                new LambdaQueryWrapper<Employee>().eq(Employee::getUsername, username)
        );
        if (employee == null) {
            throw new ServiceException("账号不存在");
        }
        return issueToken(employee);
    }

    /* ---------------- 内部方法 ---------------- */

    private String issueToken(Employee employee) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", employee.getId());
        claims.put("username", employee.getUsername());
        // 身份域：AdminAuthInterceptor 会校验它必须为 admin。
        // 两个 token 的 id 分属 employee / member 两套主键，只有 scope 能区分，
        // 所以要判断"这是谁"必须先看 scope —— 否则员工 id 5 会被当成会员 id 5。
        claims.put("scope", BaseContext.SCOPE_ADMIN);
        return JwtUtil.createToken(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims
        );
    }

    /**
     * 密码校验：优先按 BCrypt 比对；若库中存的是非 BCrypt 值（历史/演示数据），回退明文比对。
     */
    private boolean passwordMatches(String raw, String stored) {
        if (!StringUtils.hasText(stored)) {
            return false;
        }
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            try {
                return BCrypt.checkpw(raw, stored);
            } catch (Exception e) {
                log.warn("BCrypt 校验异常：{}", e.getMessage());
                return false;
            }
        }
        return stored.equals(raw);
    }

    /** 按角色构建菜单树（排除按钮节点） */
    private List<MenuVO> menuTreeByRole(Long roleId) {
        Set<Long> owned = permissionService.ownedMenuIds(roleId);
        if (owned.isEmpty()) {
            return new ArrayList<>();
        }
        List<Menu> visible = menuMapper.selectList(
                        new LambdaQueryWrapper<Menu>()
                                .eq(Menu::getStatus, 1)
                                .orderByAsc(Menu::getSort)
                ).stream()
                .filter(m -> owned.contains(m.getId()))
                .filter(m -> m.getType() == null || m.getType() != MENU_TYPE_BUTTON)
                .collect(Collectors.toList());
        return buildTree(visible, 0L);
    }

    /** 按角色取权限码（与 AdminAuthInterceptor 的判定同源，见 PermissionService） */
    private List<String> permsByRole(Long roleId) {
        return new ArrayList<>(permissionService.permsOf(roleId));
    }

    private List<MenuVO> buildTree(List<Menu> menus, Long parentId) {
        List<MenuVO> tree = new ArrayList<>();
        for (Menu menu : menus) {
            if (Objects.equals(menu.getParentId(), parentId)) {
                MenuVO vo = new MenuVO();
                BeanUtils.copyProperties(menu, vo);
                vo.setChildren(buildTree(menus, menu.getId()));
                tree.add(vo);
            }
        }
        return tree;
    }
}
