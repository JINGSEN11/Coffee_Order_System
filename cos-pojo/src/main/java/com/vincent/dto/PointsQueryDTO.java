package com.vincent.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class PointsQueryDTO extends PageDTO {
    private Long userId;
    private String userNickname;
    private Integer type;
    private LocalDate startDate;
    private LocalDate endDate;
}