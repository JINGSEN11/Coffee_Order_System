package com.vincent.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * C 端申请退款入参（POST /api/app/order/{id}/refund）。
 */
@Data
public class ApplyRefundDTO {

    /** 申请退款金额；不传表示全额退实付 */
    private BigDecimal amount;

    /** 退款原因，必填（管理端审核要看到顾客的说法） */
    private String reason;

}
