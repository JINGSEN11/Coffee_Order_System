package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vincent.entity.Member;
import com.vincent.entity.Orders;
import com.vincent.entity.PointsRecord;
import com.vincent.mapper.MemberMapper;
import com.vincent.mapper.PointsRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单维度的消费积分发放与回收（F-U20：消费 1 元 = 1 积分）。
 *
 * 发放基数取**实付金额**（券与积分抵扣掉的部分不再生积分），按 sys_config.points_rate 换算，
 * 向下取整 —— 与种子数据 gen_seed_data.mjs 的 Math.floor(payAmount) 口径一致。
 *
 * 两个方法都幂等：重复调用不会重复发、也不会重复扣。
 * 发放时点在**支付成功**（markPaid），而不是订单完成 ——
 * 因为管理端目前还没有「接单/出餐」入口，挂在完成上等于这功能永远不会触发。
 * 若以后要改成完成时发放，把调用点从 markPaid 挪到 OrderServiceImpl 的 status=3 分支即可。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MemberPointsService {

    /** 流水类型：1 消费获得 / 6 退款扣回（见 AppCalc#pointsTypeText） */
    private static final int TYPE_EARN = 1;
    private static final int TYPE_REVOKE = 6;

    private final MemberMapper memberMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final AppConfigHelper appConfigHelper;

    /**
     * 支付成功后按实付金额发放消费积分。
     *
     * @return 实际发放的积分数；已发过或算出来是 0 时返回 0
     */
    public int awardForPaidOrder(Orders order, LocalDateTime now) {
        if (order == null || order.getId() == null || order.getUserId() == null) {
            return 0;
        }
        // 幂等：同一订单只发一次（重复支付回调、状态来回改都不会重复发）
        Long already = pointsRecordMapper.selectCount(
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getOrderId, order.getId())
                        .eq(PointsRecord::getType, TYPE_EARN)
        );
        if (already != null && already > 0) {
            return 0;
        }

        int earnRate = appConfigHelper.intValue("points_rate", 100);
        int earned = AppCalc.amountToEarnPoints(AppCalc.payAmountOf(order), earnRate);
        if (earned <= 0) {
            return 0;
        }

        Member member = memberMapper.selectById(order.getUserId());
        if (member == null) {
            log.warn("订单 {} 的会员 {} 不存在，跳过积分发放", order.getOrderNo(), order.getUserId());
            return 0;
        }

        Member update = new Member();
        update.setId(member.getId());
        update.setPoints((member.getPoints() == null ? 0 : member.getPoints()) + earned);
        update.setUpdatedAt(now);
        memberMapper.updateById(update);

        PointsRecord record = new PointsRecord();
        record.setUserId(order.getUserId());
        record.setChangeValue(earned);
        record.setType(TYPE_EARN);
        record.setOrderId(order.getId());
        record.setCreatedAt(now);
        pointsRecordMapper.insert(record);

        log.info("订单 {} 实付 {}，会员 {} 获得 {} 积分",
                order.getOrderNo(), AppCalc.payAmountOf(order), order.getUserId(), earned);
        return earned;
    }

    /**
     * 退款时收回该订单已发放的消费积分。
     *
     * 积分可能已经被花掉，只扣到余额为 0 为止，流水的金额也按**实际扣回的数额**记 ——
     * 这样 member.points 与 SUM(points_record.change_value) 的等式仍然成立
     * （种子数据与 gen_seed_data.mjs 都依赖这条不变量）。
     *
     * @return 实际收回的积分数
     */
    public int revokeForRefundedOrder(Orders order, LocalDateTime now) {
        if (order == null || order.getId() == null) {
            return 0;
        }
        List<PointsRecord> earned = pointsRecordMapper.selectList(
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getOrderId, order.getId())
                        .eq(PointsRecord::getType, TYPE_EARN)
                        .orderByDesc(PointsRecord::getId)
                        .last("LIMIT 1")
        );
        if (earned.isEmpty() || earned.get(0).getChangeValue() == null
                || earned.get(0).getChangeValue() <= 0) {
            // 这单没发过积分（未支付就取消、或秒退），无需回收
            return 0;
        }
        // 幂等：已经扣过就不再扣（重复审核退款、状态来回改都不会重复扣）
        Long revoked = pointsRecordMapper.selectCount(
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getOrderId, order.getId())
                        .eq(PointsRecord::getType, TYPE_REVOKE)
        );
        if (revoked != null && revoked > 0) {
            return 0;
        }

        int entitled = earned.get(0).getChangeValue();
        Member member = memberMapper.selectById(order.getUserId());
        int balance = member == null || member.getPoints() == null ? 0 : member.getPoints();
        int deduct = Math.min(balance, entitled);

        if (member != null && deduct > 0) {
            Member update = new Member();
            update.setId(member.getId());
            update.setPoints(balance - deduct);
            update.setUpdatedAt(now);
            memberMapper.updateById(update);
        }
        if (deduct < entitled) {
            // 差额收不回来，留痕便于人工核对
            log.warn("订单 {} 退款应扣回 {} 积分，会员 {} 余额仅 {}，实扣 {}",
                    order.getOrderNo(), entitled, order.getUserId(), balance, deduct);
        }

        PointsRecord record = new PointsRecord();
        record.setUserId(order.getUserId());
        record.setChangeValue(-deduct);
        record.setType(TYPE_REVOKE);
        record.setOrderId(order.getId());
        record.setCreatedAt(now);
        pointsRecordMapper.insert(record);

        log.info("订单 {} 退款，收回会员 {} 的 {} 积分", order.getOrderNo(), order.getUserId(), deduct);
        return deduct;
    }

}
