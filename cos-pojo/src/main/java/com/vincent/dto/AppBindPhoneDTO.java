package com.vincent.dto;

import lombok.Data;

/**
 * 绑定手机号入参（POST /api/app/auth/bind-phone）
 */
@Data
public class AppBindPhoneDTO {
    /** getPhoneNumber 返回的 code */
    private String code;
}
