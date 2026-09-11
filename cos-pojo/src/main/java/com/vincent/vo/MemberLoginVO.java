package com.vincent.vo;

import lombok.Data;

@Data
public class MemberLoginVO {
    private Long id;
    private String nickname;
    private String avatar;
    private String phone;
    private Integer points;
    private String token;
}