package com.vincent.controller.admin;

import com.vincent.annotation.RequirePerm;
import com.vincent.common.BaseContext;
import com.vincent.common.Result;
import com.vincent.dto.EmployeeLoginDTO;
import com.vincent.service.AuthService;
import com.vincent.vo.AuthLoginVO;
import com.vincent.vo.LoginMenuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端认证接口。
 * 前端 baseURL=/api/admin，请求 /auth/login，经 vite 代理 rewrite 去掉 /api 前缀后，
 * 实际到达后端为 /admin/auth/login。
 */
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /** 员工登录（免鉴权） */
    @PostMapping("/login")
    public Result<AuthLoginVO> login(@RequestBody EmployeeLoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /** 刷新 token（免鉴权） */
    @PostMapping("/refresh")
    public Result<Map<String, String>> refresh(@RequestBody(required = false) Map<String, Object> body) {
        Object username = body == null ? null : body.get("username");
        String token = authService.refresh(username == null ? null : String.valueOf(username));
        return Result.success(Map.of("token", token));
    }

    /** 当前登录员工的菜单与按钮权限（需鉴权） */
    @GetMapping("/menus")
    @RequirePerm
    public Result<LoginMenuVO> menus() {
        return Result.success(authService.menus(BaseContext.getCurrentId()));
    }
}
