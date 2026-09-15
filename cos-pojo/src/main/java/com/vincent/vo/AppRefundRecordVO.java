package com.vincent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款流水（订单详情 refund_record 字段）
 */
@Data
public class AppRefundRecordVO {
    private Long id;
    private Long orderId;
    private String refundNo;
    private BigDecimal amount;
    private String reason;
    /** 0:待处理 1:退款成功 2:退款失败 */
    private Integer status;
    private String operator;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime callbackTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
