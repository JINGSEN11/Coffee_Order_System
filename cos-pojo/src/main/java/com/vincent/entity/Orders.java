package com.vincent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    /** 自取时段（如 12:30-12:45），自取订单必填（见 sql/04_migrate_app_fields.sql） */
    private String expectedPickupTime;
    private Integer type;
    /** 订单来源 1:桌码扫码 2:小程序首页 3:分享链接 4:再来一单（见 sql/04_migrate_app_fields.sql） */
    private Integer source;
    private BigDecimal amount;
    private BigDecimal discountAmount;
    private Long userCouponId;
    private BigDecimal couponAmount;
    private Integer pointsUsed;
    private BigDecimal pointsAmount;
    private Integer status;
    private String pickupCode;
    /** 取餐顺序号（每门店每个自然日从 1 递增，支付成功时发号） */
    private Integer pickupNo;
    /** 取餐码业务日期（每日重置，配合 shop_id + pickup_no 构成 uk_pickup） */
    private LocalDate pickupDate;
    /** 取餐状态 0:未发号 1:待制作 2:制作中 3:待取餐 4:已取餐 5:已作废 */
    private Integer pickupStatus;
    /** 叫号时间（最近一次叫号，首次叫号亦写入） */
    private LocalDateTime calledAt;
    /** 顾客取餐完成时间 */
    private LocalDateTime pickupTime;
    private String remark;
    private LocalDateTime payDeadline;
    private LocalDateTime payTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}