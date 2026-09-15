package com.vincent.vo;

import lombok.Data;

/**
 * 管理端登录返回体，对应前端 stores/user.js：
 * <pre>
 *   const res = await authApi.login(form)
 *   setToken(res.token); this.userInfo = res.user
 * </pre>
 * 全局 Jackson 已配置 SNAKE_CASE，realName / roleId / roleName / shopId
 * 序列化后即为 real_name / role_id / role_name / shop_id。
 */
@Data
public class AuthLoginVO {

    private String token;

    private UserInfo user;

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String realName;
        private Long roleId;
        private String roleName;
        private Long shopId;
    }
}
