package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private String userNickname;
    private Long shopId;
    private String shopName;
    private String tableNo;
    private Integer type;
    private BigDecimal amount;
    private BigDecimal discountAmount;
    private BigDecimal couponAmount;
    private Integer pointsUsed;
    private BigDecimal pointsAmount;
    private Integer status;
    private String pickupCode;
    private String remark;
    private LocalDateTime payTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private LocalDateTime createdAt;
    private List<OrderItemVO> items;
}