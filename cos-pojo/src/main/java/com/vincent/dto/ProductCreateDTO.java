package com.vincent.dto;

import lombok.Data;

@Data
public class ProductCreateDTO {
    private Long categoryId;
    private String name;
    private String description;
    private String image;
    private String tags;
    private Integer status;
}