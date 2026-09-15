package com.vincent.service;

import com.vincent.vo.AppPayPrepayVO;

import java.util.Map;

/**
 * C 端支付服务
 */
public interface AppPayService {

    /** 预下单：首次生成 out_trade_no 写入 pay_record（幂等键）；未配置商户参数时进入沙箱直通模式 */
    AppPayPrepayVO prepay(Long userId, Long orderId);

    /** 微信支付回调（免鉴权），返回微信规范响应 {"code":"SUCCESS","message":"成功"} */
    Map<String, String> notify(String body);
}
