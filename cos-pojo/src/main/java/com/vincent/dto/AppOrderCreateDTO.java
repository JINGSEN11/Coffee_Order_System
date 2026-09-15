package com.vincent.dto;

import lombok.Data;

import java.util.List;

/**
 * 下单入参（POST /api/app/order）
 */
@Data
public class AppOrderCreateDTO {
    /** 1:堂食 2:自取 */
    private Integer type;
    /** 堂食桌号（扫桌码带入） */
    private String tableNo;
    /** 自取时段，如 12:30-12:45 */
    private String expectedPickupTime;
    private String remark;
    /** 核销的持券 ID（user_coupon.id） */
    private Long userCouponId;
    /** 使用积分数量，服务端会夹到合法区间 */
    private Integer pointsUsed;
    /** 订单来源 1:桌码扫码 2:小程序首页 3:分享链接 4:再来一单，默认 2 */
    private Integer source;
    /** 参与结算的购物车行 ID 数组 */
    private List<Long> lines;
}
