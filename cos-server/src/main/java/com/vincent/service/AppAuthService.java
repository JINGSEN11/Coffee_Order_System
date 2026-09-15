package com.vincent.service;

import com.vincent.dto.AppBindPhoneDTO;
import com.vincent.dto.AppLoginDTO;
import com.vincent.vo.AppLoginVO;

/**
 * 小程序端认证服务
 */
public interface AppAuthService {

    /** 微信登录：code → openid → 静默注册 → 签发用户端 JWT */
    AppLoginVO login(AppLoginDTO dto);

    /** 绑定手机号，返回绑定后的手机号 */
    String bindPhone(AppBindPhoneDTO dto);
}
