package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vincent.entity.Member;
import com.vincent.entity.Orders;
import com.vincent.entity.Product;
import com.vincent.entity.Review;
import com.vincent.mapper.MemberMapper;
import com.vincent.mapper.OrderMapper;
import com.vincent.mapper.ProductMapper;
import com.vincent.mapper.ReviewMapper;
import com.vincent.service.DashboardService;
import com.vincent.vo.DashboardVO;
import com.vincent.vo.OrderStatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final OrderMapper orderMapper;
    private final MemberMapper memberMapper;
    private final ProductMapper productMapper;
    private final ReviewMapper reviewMapper;

    @Override
    public DashboardVO stats() {
        // 今日起止时间
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        // 1. 今日订单数（所有非取消订单）
        Long todayOrderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<Orders>()
                        .ge(Orders::getCreatedAt, todayStart)
                        .le(Orders::getCreatedAt, todayEnd)
                        .ne(Orders::getStatus, 5) // 排除已取消
        );

        // 2. 今日营收（已完成的订单）
        List<Orders> todayCompleted = orderMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .ge(Orders::getCreatedAt, todayStart)
                        .le(Orders::getCreatedAt, todayEnd)
                        .eq(Orders::getStatus, 3) // 已完成
        );
        BigDecimal todayRevenue = todayCompleted.stream()
                .map(Orders::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 会员总数（正常状态）
        Long totalMemberCount = memberMapper.selectCount(
                new LambdaQueryWrapper<Member>()
                        .eq(Member::getStatus, 1)
        );

        // 4. 商品总数（上架状态）
        Long totalProductCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
        );

        // 5. 待处理订单（待支付 + 待接单/待处理）
        Long pendingOrderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<Orders>()
                        .in(Orders::getStatus, 0, 1)
        );

        // 6. 待审核评论
        Long pendingReviewCount = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getStatus, 0)
        );

        DashboardVO vo = new DashboardVO();
        vo.setTodayOrderCount(todayOrderCount);
        vo.setTodayRevenue(todayRevenue);
        vo.setTotalMemberCount(totalMemberCount);
        vo.setTotalProductCount(totalProductCount);
        vo.setPendingOrderCount(pendingOrderCount);
        vo.setPendingReviewCount(pendingReviewCount);

        log.info("数据看板概览：今日订单={}, 今日营收={}, 会员={}, 商品={}, 待处理={}, 待审核={}",
                todayOrderCount, todayRevenue, totalMemberCount, totalProductCount,
                pendingOrderCount, pendingReviewCount);

        return vo;
    }

    @Override
    public List<OrderStatVO> trend(LocalDate startDate, LocalDate endDate) {
        List<OrderStatVO> result = new ArrayList<>();

        // 按天遍历日期区间
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            // 当天订单数
            Long orderCount = orderMapper.selectCount(
                    new LambdaQueryWrapper<Orders>()
                            .ge(Orders::getCreatedAt, dayStart)
                            .le(Orders::getCreatedAt, dayEnd)
                            .ne(Orders::getStatus, 5)
            );

            // 当天营收（已完成订单）
            List<Orders> completed = orderMapper.selectList(
                    new LambdaQueryWrapper<Orders>()
                            .ge(Orders::getCreatedAt, dayStart)
                            .le(Orders::getCreatedAt, dayEnd)
                            .eq(Orders::getStatus, 3)
            );
            BigDecimal revenue = completed.stream()
                    .map(Orders::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 客单价
            BigDecimal avgAmount = orderCount > 0
                    ? revenue.divide(BigDecimal.valueOf(orderCount), 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            OrderStatVO stat = new OrderStatVO();
            stat.setDate(date.toString());
            stat.setOrderCount(orderCount);
            stat.setRevenue(revenue);
            stat.setAvgOrderAmount(avgAmount);

            result.add(stat);
        }

        return result;
    }

}