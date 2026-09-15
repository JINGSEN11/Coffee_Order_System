package com.vincent.dto;

import lombok.Data;

/**
 * 预下单入参（POST /api/app/pay/prepay）
 */
@Data
public class AppPayPrepayDTO {
    private Long orderId;
}
