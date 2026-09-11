package com.vincent.dto;

import lombok.Data;

@Data
public class CategoryCreateDTO {
    private String name;
    private Integer sort;
    private Integer status;
}