package com.vincent.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderQueryDTO extends PageDTO {
    private String orderNo;
    private Long userId;
    private Long shopId;
    private Integer status;
    private Integer type;
    private LocalDate startDate;
    private LocalDate endDate;
}