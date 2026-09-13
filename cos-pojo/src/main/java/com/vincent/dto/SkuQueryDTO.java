package com.vincent.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SkuQueryDTO extends PageDTO {
    private Long productId;
    private String productName;
}