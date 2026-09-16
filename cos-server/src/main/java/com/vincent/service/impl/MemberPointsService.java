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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

/**
 * 订单维度的消费积分发放与回收（F-U20：消费 1 元 = 1 积分，生日双倍）。
 *
 * 发放基数取**实付金额**（券与积分抵扣掉的部分不再生积分），按 sys_config.points_rate 换算，
 * 向下取整 —— 与种子数据 gen_seed_data.mjs 的 Math.floor(payAmount) 口径一致。
 *
 * 生日当天按 sys_config.birthday_points_multiple 加倍（默认 2）。加成部分单独记一条
 * type=7「生日双倍加成」流水，base 仍记 type=1 —— 会员的积分明细里能看出多出来的积分从哪来。
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

    /** 流水类型：1 消费获得 / 6 退款扣回 / 7 生日双倍加成（见 AppCalc#pointsTypeText） */
    private static final int TYPE_EARN = 1;
    private static final int TYPE_BONUS = 7;
    /** 退款时要一并收回的「发放类」流水类型 */
    private static final List<Integer> EARN_TYPES = List.of(TYPE_EARN, TYPE_BONUS);
    private static final int TYPE_REVOKE = 6;

    private final MemberMapper memberMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final AppConfigHelper appConfigHelper;

    /**
     * 支付成功后按实付金额发放消费积分；会员生日当天按倍数加成。
     *
     * @return 实际发放的积分总数（含生日加成）；已发过或算出来是 0 时返回 0
     */
    public int awardForPaidOrder(Orders order, LocalDateTime now) {
        if (order == null || order.getId() == null || order.getUserId() == null) {
            return 0;
        }
        // 幂等：同一订单只发一次（重复支付回调、状态来回改都不会重复发）
        Long already = pointsRecordMapper.selectCount(
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getOrderId, order.getId())
                        .in(PointsRecord::getType, EARN_TYPES)
        );
        if (already != null && already > 0) {
            return 0;
        }

        int earnRate = appConfigHelper.intValue("points_rate", 100);
        int base = AppCalc.amountToEarnPoints(AppCalc.payAmountOf(order), earnRate);
        if (base <= 0) {
            return 0;
        }

        Member member = memberMapper.selectById(order.getUserId());
        if (member == null) {
            log.warn("订单 {} 的会员 {} 不存在，跳过积分发放", order.getOrderNo(), order.getUserId());
            return 0;
        }

        // 生日加成：倍数取自配置，1 表示不平（等于关闭）
        int multiple = appConfigHelper.intValue("birthday_points_multiple", 2);
        boolean birthday = multiple > 1 && isBirthday(member.getBirthday(), now.toLocalDate());
        int bonus = birthday ? base * (multiple - 1) : 0;
        int total = base + bonus;

        Member update = new Member();
        update.setId(member.getId());
        update.setPoints((member.getPoints() == null ? 0 : member.getPoints()) + total);
        update.setUpdatedAt(now);
        memberMapper.updateById(update);

        insertRecord(order.getUserId(), base, TYPE_EARN, order.getId(), now);
        if (bonus > 0) {
            insertRecord(order.getUserId(), bonus, TYPE_BONUS, order.getId(), now);
            log.info("订单 {} 命中会员 {} 生日，积分 ×{}：基础 {} + 加成 {} = {}",
                    order.getOrderNo(), order.getUserId(), multiple, base, bonus, total);
        }

        log.info("订单 {} 实付 {}，会员 {} 获得 {} 积分",
                order.getOrderNo(), AppCalc.payAmountOf(order), order.getUserId(), total);
        return total;
    }

    /**
     * 退款时收回该订单已发放的消费积分（含生日加成）。
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
        // 发放可能分成两条（基础 + 生日加成），这里一并汇总
        List<PointsRecord> earned = pointsRecordMapper.selectList(
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getOrderId, order.getId())
                        .in(PointsRecord::getType, EARN_TYPES)
        );
        int entitled = earned.stream()
                .map(PointsRecord::getChangeValue)
                .filter(v -> v != null && v > 0)
                .mapToInt(Integer::intValue)
                .sum();
        if (entitled <= 0) {
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

        insertRecord(order.getUserId(), -deduct, TYPE_REVOKE, order.getId(), now);
        log.info("订单 {} 退款，收回会员 {} 的 {} 积分", order.getOrderNo(), order.getUserId(), deduct);
        return deduct;
    }

    /**
     * 今天是不是该会员的生日。只比月日，年份不参与。
     *
     * 2 月 29 日出生的会员在平年按 2 月 28 日判定 —— 否则只有闰年才触发一次，
     * 对 2/29 生日的人等于四年过一次。
     */
    public static boolean isBirthday(LocalDate birthday, LocalDate today) {
        if (birthday == null || today == null) {
            return false;
        }
        int bm = birthday.getMonthValue();
        int bd = birthday.getDayOfMonth();
        if (bm == today.getMonthValue() && bd == today.getDayOfMonth()) {
            return true;
        }
        return bm == 2 && bd == 29
                && today.getMonthValue() == 2 && today.getDayOfMonth() == 28
                && !Year.isLeap(today.getYear());
    }

    private void insertRecord(Long userId, int change, int type, Long orderId, LocalDateTime now) {
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setChangeValue(change);
        record.setType(type);
        record.setOrderId(orderId);
        record.setCreatedAt(now);
        pointsRecordMapper.insert(record);
    }

}

