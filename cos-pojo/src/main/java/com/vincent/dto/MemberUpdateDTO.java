package com.vincent.dto;

import lombok.Data;

@Data
public class MemberUpdateDTO {
    private Long id;
    private String nickname;
    private String avatar;
    private String phone;
}