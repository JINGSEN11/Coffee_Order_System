package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.BaseContext;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.RefundQueryDTO;
import com.vincent.dto.RefundStatusDTO;
import com.vincent.entity.Orders;
import com.vincent.entity.RefundRecord;
import com.vincent.mapper.OrderMapper;
import com.vincent.mapper.RefundRecordMapper;
import com.vincent.service.RefundRecordService;
import com.vincent.vo.PageVO;
import com.vincent.vo.RefundRecordVO;
import com.vincent.vo.RefundStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundRecordServiceImpl extends ServiceImpl<RefundRecordMapper, RefundRecord>
        implements RefundRecordService {

    /** 退款单号里的日期段 */
    private static final DateTimeFormatter REFUND_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RefundRecordMapper refundRecordMapper;
    private final OrderMapper orderMapper;
    private final OrderRefundSettlement refundSettlement;

    @Override
    public PageVO<RefundRecordVO> pageQuery(RefundQueryDTO dto) {
        Page<RefundRecord> page = new Page<>(dto.getPage(), dto.getPageSize());

        LocalDateTime startDateTime = dto.getStartDate() != null ? dto.getStartDate().atStartOfDay() : null;
        LocalDateTime endDateTime = dto.getEndDate() != null ? dto.getEndDate().plusDays(1).atStartOfDay() : null;

        LambdaQueryWrapper<RefundRecord> wrapper = new LambdaQueryWrapper<RefundRecord>()
                .eq(dto.getStatus() != null, RefundRecord::getStatus, dto.getStatus())
                .ge(dto.getStartDate() != null, RefundRecord::getCreatedAt, startDateTime)
                .le(dto.getEndDate() != null, RefundRecord::getCreatedAt, endDateTime)
                .orderByDesc(RefundRecord::getCreatedAt);

        // orderNo 过滤的是订单号，退款表里没有这一列，先按订单号捞出订单 ID。
        // （原先这里写的是 like(RefundRecord::getRefundNo, orderNo)，拿订单号去匹配退款单号，永远匹配不到。）
        if (StringUtils.hasText(dto.getOrderNo())) {
            List<Long> orderIds = orderMapper.selectList(
                    new LambdaQueryWrapper<Orders>().like(Orders::getOrderNo, dto.getOrderNo())
            ).stream().map(Orders::getId).collect(Collectors.toList());
            if (orderIds.isEmpty()) {
                return new PageVO<>(0L, dto.getPage(), dto.getPageSize(), Collections.emptyList());
            }
            wrapper.in(RefundRecord::getOrderId, orderIds);
        }

        Page<RefundRecord> result = page(page, wrapper);
        return new PageVO<>(result.getTotal(), dto.getPage(), dto.getPageSize(),
                toVOList(result.getRecords()));
    }

    @Override
    public RefundRecordVO getDetail(Long id) {
        RefundRecord record = getById(id);
        if (record == null) {
            throw new ServiceException("退款记录不存在");
        }
        return toVOList(List.of(record)).get(0);
    }

    /**
     * 审核退款。通过时执行完整结算：回退库存、退回抵扣积分、释放优惠券、订单置为已退款。
     *
     * 钱这一步后端做不了 —— 当前微信支付是沙箱直通模式（见 AppPayServiceImpl），
     * 真实退款需要商户证书调微信退款接口，本项目没有。所以要求操作人显式确认
     * 「已在微信商户平台完成退款」（dto.received=true）才放行，避免出现
     * 「点一下通过、订单标成已退款、但钱其实没退」这种和商户后台对不上账的情况。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundStatusVO updateStatus(Long id, RefundStatusDTO dto) {
        RefundRecord record = getById(id);
        if (record == null) {
            throw new ServiceException("退款记录不存在");
        }
        if (record.getStatus() != null && record.getStatus() != 0) {
            throw new ServiceException("该退款申请已处理，不能重复审核");
        }
        if (dto == null || !StringUtils.hasText(dto.getStatus())) {
            throw new ServiceException("请指定审核结果");
        }

        Orders order = orderMapper.selectById(record.getOrderId());
        if (order == null) {
            throw new ServiceException("退款对应的订单不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        String operator = currentOperator();
        int newStatus;
        String statusName;
        String rejectReason = null;

        if ("approved".equals(dto.getStatus())) {
            if (!Boolean.TRUE.equals(dto.getReceived())) {
                throw new ServiceException("请先在微信商户平台完成退款，再确认已到账");
            }
            if (order.getStatus() == null || order.getStatus() != 6) {
                throw new ServiceException("订单当前不是「退款中」，无法通过退款（当前状态 "
                        + order.getStatus() + "）");
            }

            refundSettlement.settle(order, record.getReason(), now);
            record.setStatus(1);
            record.setRefundNo(StringUtils.hasText(dto.getRefundNo())
                    ? dto.getRefundNo()
                    : defaultRefundNo(record, now));
            newStatus = 1;
            statusName = "已通过";
        } else if ("rejected".equals(dto.getStatus())) {
            if (!StringUtils.hasText(dto.getReason())) {
                throw new ServiceException("请填写拒绝原因");
            }
            rejectReason = dto.getReason();

            // 驳回后订单回到申请前的状态：出过餐的回到已完成，否则回到制作中。
            // refund_record 没有单独的「审核意见」列，把驳回原因追加到原因后面留存
            // （与 review 表把 tags 拼进 content 的处理口径一致）。
            int backStatus = order.getFinishTime() != null ? 3 : 2;
            order.setStatus(backStatus);
            order.setCancelReason("退款申请已驳回：" + rejectReason);
            order.setUpdatedAt(now);
            orderMapper.updateById(order);

            record.setReason((record.getReason() == null ? "" : record.getReason())
                    + " ｜ 驳回原因：" + rejectReason);
            record.setStatus(2);
            newStatus = 2;
            statusName = "已拒绝";
        } else {
            throw new ServiceException("无效的状态值");
        }

        record.setOperator(operator);
        record.setCallbackTime(now);
        updateById(record);

        log.info("员工 {} 审核退款 {}：{}，订单 {}，金额 {}",
                operator, record.getRefundNo(), statusName, order.getOrderNo(), record.getAmount());

        RefundStatusVO vo = new RefundStatusVO();
        vo.setId(id);
        vo.setStatus(newStatus);
        vo.setStatusName(statusName);
        vo.setReason(rejectReason);
        return vo;
    }

    /* ---------------- 内部方法 ---------------- */

    /** 操作人取当前登录员工的用户名，便于对账追溯 */
    private String currentOperator() {
        BaseContext.Principal principal = BaseContext.get();
        if (principal == null) {
            return "系统";
        }
        return StringUtils.hasText(principal.username())
                ? principal.username()
                : "员工#" + principal.id();
    }

    private String defaultRefundNo(RefundRecord record, LocalDateTime now) {
        return "RF" + now.format(REFUND_NO_DATE) + String.format("%05d", record.getId());
    }

    /** 批量装配 VO，顺带把订单号带出来（列表里只有 orderId 的话根本定位不到订单） */
    private List<RefundRecordVO> toVOList(List<RefundRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> orderIds = records.stream()
                .map(RefundRecord::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> orderNoMap = orderIds.isEmpty()
                ? Collections.emptyMap()
                : orderMapper.selectBatchIds(orderIds).stream()
                        .collect(Collectors.toMap(Orders::getId, Orders::getOrderNo, (a, b) -> a));

        return records.stream().map(record -> {
            RefundRecordVO vo = new RefundRecordVO();
            BeanUtils.copyProperties(record, vo);
            vo.setOrderNo(orderNoMap.get(record.getOrderId()));
            return vo;
        }).collect(Collectors.toList());
    }

}
