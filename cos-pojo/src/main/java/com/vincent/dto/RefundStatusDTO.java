package com.vincent.dto;

import lombok.Data;

/**
 * 管理端审核退款入参（PUT /admin/order/refund/status/{id}）。
 */
@Data
public class RefundStatusDTO {

    /** approved / rejected */
    private String status;

    /** 拒绝原因；拒绝时必填 */
    private String reason;

    /**
     * 商家是否已在微信商户平台完成实际退款打款。
     *
     * 本项目当前的微信支付是沙箱直通模式（见 AppPayServiceImpl），后端发不出真实退款请求，
     * 所以钱这一步只能由商家在商户平台操作。通过审核时必须显式带上 true ——
     * 不默认放行，是为了避免「点一下就把订单标成已退款、但钱其实没退」这种对不上账的情况。
     */
    private Boolean received;

    /** 微信退款单号；不传则按日期+订单号生成一个便于对账的本地单号 */
    private String refundNo;

}
