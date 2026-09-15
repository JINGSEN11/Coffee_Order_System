package com.vincent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券（领券中心条目 / 我的券条目，两个 tab 共用，按 tab 输出各自字段）
 */
@Data
public class AppCouponVO {
    private Long id;
    private String name;
    /** 1:满减 2:折扣 */
    private Integer type;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Integer totalCount;
    private Integer receivedCount;
    private Integer perUserLimit;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime validStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime validEndTime;
    private Integer status;

    /* ---- 领券中心（tab=center）字段 ---- */
    /** 剩余发放量，-1 表示不限量 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer remaining;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer claimedCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean canClaim;

    /* ---- 我的券（tab=0/1/2）字段 ---- */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long userCouponId;
    /** 持券状态 0:未使用 1:已使用 2:已过期（由 valid_end_time 实时判定） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer ucStatus;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String remainText;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime claimedAt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime usedAt;
}
