package com.vincent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_record")
public class RefundRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String refundNo;
    private BigDecimal amount;
    private String reason;
    private Integer status;
    private String operator;
    private LocalDateTime callbackTime;
    private LocalDateTime createdAt;
}