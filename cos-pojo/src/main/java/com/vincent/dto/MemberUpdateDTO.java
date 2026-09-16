package com.vincent.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MemberUpdateDTO {
    private Long id;
    private String nickname;
    private String avatar;
    private String phone;
    /** 生日 yyyy-MM-dd；传 null 表示不改动，传空字符串则由前端置为 null 清除 */
    private LocalDate birthday;
}