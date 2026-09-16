package com.vincent.service;

import com.vincent.dto.AppOrderCreateDTO;
import com.vincent.dto.AppReviewCreateDTO;
import com.vincent.dto.ApplyRefundDTO;
import com.vincent.entity.Orders;
import com.vincent.vo.AppOrderListVO;
import com.vincent.vo.AppOrderVO;

import java.util.List;
import java.util.Map;

/**
 * C 端订单服务（下单 / 列表 / 详情 / 取消 / 再来一单 / 评价 / 支付成功后的订单处理）
 */
public interface AppOrderService {

    /** 创建订单（状态 0 待支付）：服务端重算金额、锁券锁积分，不扣库存不发号 */
    AppOrderVO create(Long userId, AppOrderCreateDTO dto);

    /** 订单列表（status 为逗号分隔的状态集合，空或 all 表示全部） */
    AppOrderListVO list(Long userId, String status);

    /** 订单详情，idOrNo 支持订单主键或 order_no；待支付且已超时则自动关单 */
    AppOrderVO detail(Long userId, String idOrNo);

    /** 取消订单 */
    AppOrderVO cancel(Long userId, String idOrNo, String reason);

    /**
     * 申请退款。
     * 未接单（status=1）按订单状态机秒退；制作中/已完成（2/3）落一条待审核流水并把订单置为退款中，
     * 等管理端审核通过后才真正回退库存与积分。
     */
    AppOrderVO applyRefund(Long userId, String idOrNo, ApplyRefundDTO dto);

    /** 再来一单：把历史订单明细写回购物车，返回 {added, cart} */
    Map<String, Object> again(Long userId, String idOrNo);

    /** 提交评价，返回 {reviewed, points_gained} */
    Map<String, Object> review(Long userId, String idOrNo, AppReviewCreateDTO dto);

    /** 装配订单视图（供首页进行中订单复用） */
    AppOrderVO buildOrderVO(Orders order);

    /** 首页「进行中订单」：状态 0/1/2/3 的最近若干条 */
    List<AppOrderVO> recentOngoing(Long userId, int limit);

    /** 待支付且已过 pay_deadline 时自动关单，返回处理后的订单 */
    Orders closeIfExpired(Orders order);

    /**
     * 支付成功后的全部处理：回填 pay_record、订单置待接单、发取餐码、扣库存、
     * 清理已下单购物车行、写积分流水（type=2 抵扣消耗）。
     * 已在 status != 0 时直接返回，保证回调幂等（不重复发号、不重复扣库存）。
     */
    void markPaid(Long orderId, String transactionNo);
}
