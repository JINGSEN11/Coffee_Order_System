package com.vincent.dto;

import lombok.Data;

@Data
public class ShopCreateDTO {
    private String name;
    private String address;
    private String phone;
    private String businessHours;
    private Integer acceptOrder;
    private Integer status;
}