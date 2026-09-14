package com.vincent.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductCreateDTO {
    private Long categoryId;
    private String name;
    private String description;
    private String image;
    private String tags;
    private Integer status;
    /** 编辑时前端 inline 传入的 SKU 列表 */
    private List<SkuCreateDTO> skus;
}