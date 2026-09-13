package com.vincent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysConfigVO {
    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}