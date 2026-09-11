package com.vincent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewVO {
    private Long id;
    private Long orderId;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private Integer score;
    private String content;
    private String images;
    private Integer status;
    private LocalDateTime createdAt;
}