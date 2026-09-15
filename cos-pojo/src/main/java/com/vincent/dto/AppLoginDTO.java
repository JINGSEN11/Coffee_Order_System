package com.vincent.dto;

import lombok.Data;

/**
 * 小程序端微信登录入参（POST /api/app/auth/login）
 */
@Data
public class AppLoginDTO {
    /** wx.login 返回的临时登录凭证 */
    private String code;
    /** getUserProfile 昵称，首次授权时传 */
    private String nickname;
    /** 头像 URL */
    private String avatar;
}
