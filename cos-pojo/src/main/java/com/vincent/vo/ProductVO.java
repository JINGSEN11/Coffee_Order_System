package com.vincent.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductVO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String image;
    private String tags;
    private Integer sales;
    private Integer status;
    private LocalDateTime createdAt;
    private List<SkuVO> skuList;
}