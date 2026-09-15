package com.vincent.vo;

import lombok.Data;

import java.util.Map;

/**
 * 预下单响应（POST /api/app/pay/prepay）
 */
@Data
public class AppPayPrepayVO {
    /** 商户支付单号，幂等键，首次预下单生成后不再变更 */
    private String outTradeNo;
    /** 是否处于支付沙箱直通模式 */
    private Boolean sandbox;
    /** 沙箱模式下为 true，表示订单已完成支付 */
    private Boolean paid;
    /** 微信 wx.requestPayment 所需参数；sandbox=true 时无需调起收银台 */
    private Map<String, Object> payload;
}
