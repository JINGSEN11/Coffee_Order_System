package com.vincent.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OpLogVO {
    private Long id;
    private String operator;
    private String module;
    private String action;
    private String ip;
    private String detail;
    private LocalDateTime createdAt;
}