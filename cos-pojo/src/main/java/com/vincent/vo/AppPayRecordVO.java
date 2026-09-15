package com.vincent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水（订单详情 pay_record 字段）
 */
@Data
public class AppPayRecordVO {
    private Long id;
    private Long orderId;
    private String outTradeNo;
    private String transactionNo;
    private BigDecimal amount;
    /** 1:微信 2:支付宝 3:余额 */
    private Integer channel;
    /** 0:待支付 1:支付成功 2:支付失败 */
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime callbackTime;
}
