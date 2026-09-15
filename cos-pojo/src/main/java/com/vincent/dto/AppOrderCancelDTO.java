package com.vincent.dto;

import lombok.Data;

/**
 * 取消订单入参（POST /api/app/order/{id}/cancel）
 */
@Data
public class AppOrderCancelDTO {
    /** 缺省为「顾客主动取消」 */
    private String reason;
}
