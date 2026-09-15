package com.vincent.controller.user;

import com.vincent.common.Result;
import com.vincent.dto.AppBindPhoneDTO;
import com.vincent.dto.AppLoginDTO;
import com.vincent.service.AppAuthService;
import com.vincent.vo.AppLoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 小程序端认证接口。
 * 前端 PREFIX=/api/app，故这里直接声明 /api/app/auth/**（登录免鉴权，见 WebMvcConfig）。
 */
@RestController
@RequestMapping("/api/app/auth")
@RequiredArgsConstructor
@Slf4j
public class AppAuthController {

    private final AppAuthService appAuthService;

    /** 微信登录（免鉴权）：code → openid → 静默注册 → 签发 user-secret-key 的 JWT */
    @PostMapping("/login")
    public Result<AppLoginVO> login(@RequestBody AppLoginDTO dto) {
        return Result.success(appAuthService.login(dto));
    }

    /** 绑定手机号 */
    @PostMapping("/bind-phone")
    public Result<Map<String, Object>> bindPhone(@RequestBody AppBindPhoneDTO dto) {
        String phone = appAuthService.bindPhone(dto);
        Map<String, Object> data = new HashMap<>();
        data.put("phone", phone);
        return Result.success(data);
    }
}
