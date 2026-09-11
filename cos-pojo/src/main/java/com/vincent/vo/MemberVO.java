package com.vincent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemberVO {
    private Long id;
    private String openid;
    private String nickname;
    private String avatar;
    private String phone;
    private Integer points;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}