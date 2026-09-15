package com.vincent.vo;

import lombok.Data;

@Data
public class RefundStatusVO {
    private Long id;
    private Integer status;
    private String statusName;
    private String reason;
}