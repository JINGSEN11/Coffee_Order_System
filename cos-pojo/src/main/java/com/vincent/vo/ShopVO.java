package com.vincent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShopVO {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String businessHours;
    private Integer acceptOrder;
    private Integer status;
    private LocalDateTime createdAt;
}