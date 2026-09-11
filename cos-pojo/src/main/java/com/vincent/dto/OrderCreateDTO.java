package com.vincent.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderCreateDTO {
    private Long shopId;
    private String tableNo;
    private Integer type;
    private String remark;
    private Long userCouponId;
    private Integer pointsUsed;
    private List<OrderItemDTO> items;
}