package com.vincent.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class RefundQueryDTO extends PageDTO {
    private String orderNo;
    private Integer status;
    private LocalDate startDate;
    private LocalDate endDate;
}