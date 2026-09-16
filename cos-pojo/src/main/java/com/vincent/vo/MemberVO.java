package com.vincent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MemberVO {
    private Long id;
    private String openid;
    private String nickname;
    private String avatar;
    private String phone;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
    private Integer points;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}