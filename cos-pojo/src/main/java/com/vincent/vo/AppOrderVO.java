package com.vincent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * C 端订单视图（契约 13.3 OrderVO）
 */
@Data
public class AppOrderVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long shopId;
    private String tableNo;
    /** 1:堂食 2:自取 */
    private Integer type;
    /** 1:桌码扫码 2:小程序首页 3:分享链接 4:再来一单 */
    private Integer source;
    private String expectedPickupTime;
    private BigDecimal amount;
    private BigDecimal discountAmount;
    /** max(0, amount - discount_amount)，实付金额 */
    private BigDecimal payAmount;
    private Long userCouponId;
    private BigDecimal couponAmount;
    private Integer pointsUsed;
    private BigDecimal pointsAmount;
    private Integer status;
    /** 由 pickup_no 派生 */
    private String pickupCode;
    private Integer pickupNo;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate pickupDate;
    /** 0:未发号 1:待制作 2:制作中 3:待取餐 4:已取餐 5:已作废 */
    private Integer pickupStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime calledAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime pickupTime;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime payDeadline;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime payTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime finishTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime cancelTime;
    private String cancelReason;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
    /** 明细数量合计 */
    private Integer totalQty;
    private List<AppOrderItemVO> items;
    private AppReviewVO review;
    private AppPayRecordVO payRecord;
    private AppRefundRecordVO refundRecord;
    /** 门店简要信息 {id, name, address} */
    private Map<String, Object> shop;
}
