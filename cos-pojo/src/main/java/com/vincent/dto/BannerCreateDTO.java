package com.vincent.dto;

import lombok.Data;

@Data
public class BannerCreateDTO {
    private String imageUrl;
    private String linkUrl;
    private Integer sort;
    private Integer status;
}