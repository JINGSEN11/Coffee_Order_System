package com.vincent.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class OpLogQueryDTO extends PageDTO {
    private String operator;
    private String module;
    private LocalDate startDate;
    private LocalDate endDate;
}