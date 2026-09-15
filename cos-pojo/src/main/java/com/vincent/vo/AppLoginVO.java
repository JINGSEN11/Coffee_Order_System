package com.vincent.vo;

import lombok.Data;

/**
 * 小程序端登录响应
 */
@Data
public class AppLoginVO {
    private String token;
    private AppMemberBriefVO member;
}
