package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.vincent.entity.Member;
import com.vincent.entity.OrderItem;
import com.vincent.entity.Orders;
import com.vincent.entity.PointsRecord;
import com.vincent.entity.Sku;
import com.vincent.entity.UserCoupon;
import com.vincent.mapper.MemberMapper;
import com.vincent.mapper.OrderItemMapper;
import com.vincent.mapper.OrderMapper;
import com.vincent.mapper.PointsRecordMapper;
import com.vincent.mapper.SkuMapper;
import com.vincent.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款结算：把一笔已支付订单回退到「已退款」，并归还它占用的全部资源。
 *
 * 三条路径都必须走这里，口径不能各写各的：
 * - C 端取消已支付订单；
 * - C 端申请退款且订单尚未接单（秒退）；
 * - 管理端审核通过退款申请。
 *
 * 回退四件事：释放锁定的优惠券、退回抵扣的积分、归还扣减的库存、作废取餐码。
 * 注意「扣减发生在支付成功时」——所以只有已支付订单才需要回退库存与积分；
 * 券则在下单时就锁定了，未支付取消也要释放（那条路径不走本类，见 AppOrderServiceImpl#cancel）。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderRefundSettlement {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SkuMapper skuMapper;
    private final MemberMapper memberMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final UserCouponMapper userCouponMapper;
    private final MemberPointsService memberPointsService;

    /**
     * 执行退款结算：回退资源并把订单置为已退款（status=7）。
     *
     * @param order 待退款的订单，调用方需保证它是已支付且未退款的状态
     * @param reason 退款原因，写入订单的 cancel_reason 便于对账追溯
     * @return 回退的实付金额（用于写入退款流水的金额核对）
     */
    public void settle(Orders order, String reason, LocalDateTime now) {
        releaseLockedCoupon(order);
        refundPoints(order, now);
        restoreStock(order);
        // 收回这单已发的消费积分：退了钱还留着积分就是一个可反复套利的漏洞
        memberPointsService.revokeForRefundedOrder(order, now);

        order.setStatus(7);
        order.setCancelTime(now);
        order.setCancelReason(reason);
        // 已发号的订单作废取餐码，号不回收（与 C 端取消口径一致）
        order.setPickupStatus(order.getPickupNo() != null ? 5 : 0);
        order.setUpdatedAt(now);
        orderMapper.updateById(order);

        log.info("订单 {} 退款结算完成，金额 {}，原因：{}",
                order.getOrderNo(), payAmountOf(order), reason);
    }

    /** 实付金额 = 商品总额 - 优惠总额，用于退款金额的默认值与校验上限 */
    public BigDecimal payAmountOf(Orders order) {
        return AppCalc.payAmountOf(order);
    }

    /* ---------------- 内部回退动作 ---------------- */

    /**
     * 释放订单锁定的持券：券回到「未使用」，清掉核销痕迹。
     * 未支付订单取消时也需调用 —— 券是下单那一刻锁的，不是支付时锁的。
     */
    public void releaseLockedCoupon(Orders order) {
        if (order.getUserCouponId() == null) {
            return;
        }
        // order_id / used_at 需要显式置 NULL，updateById 无法写空值，故用 setSql
        userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getId, order.getUserCouponId())
                .set(UserCoupon::getStatus, 0)
                .setSql("order_id = NULL, used_at = NULL"));
    }

    /** 退回下单时抵扣的积分，记流水 type=3（退款退回） */
    private void refundPoints(Orders order, LocalDateTime now) {
        int used = order.getPointsUsed() == null ? 0 : order.getPointsUsed();
        if (used <= 0) {
            return;
        }
        Member member = memberMapper.selectById(order.getUserId());
        if (member != null) {
            Member update = new Member();
            update.setId(member.getId());
            update.setPoints((member.getPoints() == null ? 0 : member.getPoints()) + used);
            update.setUpdatedAt(now);
            memberMapper.updateById(update);
        }
        PointsRecord record = new PointsRecord();
        record.setUserId(order.getUserId());
        record.setChangeValue(used);
        record.setType(3);
        record.setOrderId(order.getId());
        record.setCreatedAt(now);
        pointsRecordMapper.insert(record);
    }

    /** 归还支付时扣减的库存 */
    private void restoreStock(Orders order) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
        );
        for (OrderItem item : items) {
            Sku sku = item.getSkuId() == null ? null : skuMapper.selectById(item.getSkuId());
            if (sku == null) {
                continue;
            }
            Sku update = new Sku();
            update.setId(sku.getId());
            update.setStock((sku.getStock() == null ? 0 : sku.getStock())
                    + (item.getQty() == null ? 0 : item.getQty()));
            skuMapper.updateById(update);
        }
    }

}
