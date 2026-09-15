package com.vincent.dto;

import lombok.Data;

/**
 * 修改购物车行入参（PUT /api/app/cart/{id}），qty / checked 可同时传
 */
@Data
public class AppCartUpdateDTO {
    private Integer qty;
    private Boolean checked;
}
