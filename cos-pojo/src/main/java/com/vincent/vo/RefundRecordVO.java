package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundRecordVO {
    private Long id;
    private Long orderId;
    /** 订单号，从 orders 表带出，便于退款列表直接定位订单 */
    private String orderNo;
    private String refundNo;
    private BigDecimal amount;
    private String reason;
    private Integer status;
    private String operator;
    private LocalDateTime callbackTime;
    private LocalDateTime createdAt;
}
