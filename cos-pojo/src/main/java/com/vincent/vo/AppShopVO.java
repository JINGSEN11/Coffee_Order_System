package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 门店信息（GET /api/app/shop）
 */
@Data
public class AppShopVO {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String businessHours;
    /** 接单开关 1:开启 0:关闭（关闭后菜单页置灰不可下单） */
    private Integer acceptOrder;
    /** 1:营业 0:休息 */
    private Integer status;
    /** 展示用派生值，无定位权限时为空串 */
    private String distanceText;
    private BigDecimal deliveryFee;
    /** 展示用派生值，无定位权限时为空串 */
    private String etaText;
}
