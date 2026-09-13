package com.vincent.vo;

import lombok.Data;

@Data
public class AlertVO {
    private Long id;
    private String type;
    private String message;
    private Integer level;
    private Object extra;
}