package com.vincent.service;

import com.vincent.dto.EmployeeLoginDTO;
import com.vincent.vo.AuthLoginVO;
import com.vincent.vo.LoginMenuVO;

/**
 * 管理端认证服务
 */
public interface AuthService {

    /** 员工登录：校验账号密码（BCrypt），签发 JWT，返回用户信息 */
    AuthLoginVO login(EmployeeLoginDTO dto);

    /** 按当前登录员工查询其角色的菜单树与按钮权限 */
    LoginMenuVO menus(Long employeeId);

    /** 重新签发 token */
    String refresh(String username);
}
