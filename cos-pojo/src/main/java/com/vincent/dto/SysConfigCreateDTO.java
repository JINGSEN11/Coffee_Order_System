package com.vincent.dto;

import lombok.Data;

@Data
public class SysConfigCreateDTO {
    private String configKey;
    private String configValue;
    private String description;
}