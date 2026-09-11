package com.vincent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Orders {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long shopId;
    private String tableNo;
    private Integer type;
    private BigDecimal amount;
    private BigDecimal discountAmount;
    private Long userCouponId;
    private BigDecimal couponAmount;
    private Integer pointsUsed;
    private BigDecimal pointsAmount;
    private Integer status;
    private String pickupCode;
    private String remark;
    private LocalDateTime payDeadline;
    private LocalDateTime payTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}