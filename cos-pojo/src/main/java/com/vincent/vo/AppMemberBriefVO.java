package com.vincent.vo;

import lombok.Data;

/**
 * 会员简要信息（登录响应 / 首页 member 聚合）
 */
@Data
public class AppMemberBriefVO {
    private Long id;
    private String nickname;
    private String avatar;
    private String phone;
    private Integer points;
    private Integer status;
}
