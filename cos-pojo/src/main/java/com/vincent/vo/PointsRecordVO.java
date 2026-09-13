package com.vincent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PointsRecordVO {
    private Long id;
    private Long userId;
    private String userNickname;
    private Integer changeValue;
    private Integer type;
    private Long orderId;
    private LocalDateTime createdAt;
}