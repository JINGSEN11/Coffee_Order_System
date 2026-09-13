package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vincent.entity.Category;
import com.vincent.entity.Member;
import com.vincent.entity.Orders;
import com.vincent.entity.Product;
import com.vincent.entity.Review;
import com.vincent.entity.Sku;
import com.vincent.mapper.CategoryMapper;
import com.vincent.mapper.MemberMapper;
import com.vincent.mapper.OrderMapper;
import com.vincent.mapper.ProductMapper;
import com.vincent.mapper.ReviewMapper;
import com.vincent.mapper.SkuMapper;
import com.vincent.service.DashboardService;
import com.vincent.vo.AlertVO;
import com.vincent.vo.DashboardChartVO;
import com.vincent.vo.DashboardOverviewVO;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final OrderMapper orderMapper;
    private final MemberMapper memberMapper;
    private final ProductMapper productMapper;
    private final ReviewMapper reviewMapper;
    private final SkuMapper skuMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public DashboardVO stats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        Long todayOrderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<Orders>()
                        .ge(Orders::getCreatedAt, todayStart)
                        .le(Orders::getCreatedAt, todayEnd)
                        .ne(Orders::getStatus, 5)
        );

        List<Orders> todayCompleted = orderMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .ge(Orders::getCreatedAt, todayStart)
                        .le(Orders::getCreatedAt, todayEnd)
                        .eq(Orders::getStatus, 3)
        );
        BigDecimal todayRevenue = todayCompleted.stream()
                .map(Orders::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long totalMemberCount = memberMapper.selectCount(
                new LambdaQueryWrapper<Member>()
                        .eq(Member::getStatus, 1)
        );

        Long totalProductCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
        );

        Long pendingOrderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<Orders>()
                        .in(Orders::getStatus, 0, 1)
        );

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

        log.info("\u6570\u636e\u770b\u677f\u6982\u89c8\uff1a\u4eca\u65e5\u8ba2\u5355={}, \u4eca\u65e5\u8425\u6536={}, \u4f1a\u5458={}, \u5546\u54c1={}, \u5f85\u5904\u7406={}, \u5f85\u5ba1\u6838={}",
                todayOrderCount, todayRevenue, totalMemberCount, totalProductCount,
                pendingOrderCount, pendingReviewCount);

        return vo;
    }

    @Override
    public List<OrderStatVO> trend(LocalDate startDate, LocalDate endDate) {
        List<OrderStatVO> result = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            Long orderCount = orderMapper.selectCount(
                    new LambdaQueryWrapper<Orders>()
                            .ge(Orders::getCreatedAt, dayStart)
                            .le(Orders::getCreatedAt, dayEnd)
                            .ne(Orders::getStatus, 5)
            );

            List<Orders> completed = orderMapper.selectList(
                    new LambdaQueryWrapper<Orders>()
                            .ge(Orders::getCreatedAt, dayStart)
                            .le(Orders::getCreatedAt, dayEnd)
                            .eq(Orders::getStatus, 3)
            );
            BigDecimal revenue = completed.stream()
                    .map(Orders::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

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

    @Override
    public List<AlertVO> alerts() {
        List<AlertVO> alerts = new ArrayList<>();

        List<Sku> lowStockSkus = skuMapper.selectList(
                new LambdaQueryWrapper<Sku>()
                        .apply("stock <= warn_stock AND warn_stock > 0")
        );

        if (!lowStockSkus.isEmpty()) {
            Map<Long, Product> productMap = productMapper.selectList(
                    new LambdaQueryWrapper<Product>()
                            .in(Product::getId,
                                    lowStockSkus.stream().map(Sku::getProductId).collect(Collectors.toSet()))
            ).stream().collect(Collectors.toMap(Product::getId, Function.identity()));

            for (Sku sku : lowStockSkus) {
                Product product = productMap.get(sku.getProductId());
                String productName = product != null ? product.getName() : "unknown";
                AlertVO alert = new AlertVO();
                alert.setId(sku.getId());
                alert.setType("low_stock");
                alert.setLevel(2);
                alert.setMessage("Product '" + productName + "' is low on stock, current: " + sku.getStock());
                alert.setExtra(sku);
                alerts.add(alert);
            }
        }

        Long pendingOrderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<Orders>()
                        .in(Orders::getStatus, 0, 1)
        );
        if (pendingOrderCount > 0) {
            AlertVO alert = new AlertVO();
            alert.setId(0L);
            alert.setType("pending_order");
            alert.setLevel(2);
            alert.setMessage("There are " + pendingOrderCount + " pending orders to process");
            alert.setExtra(pendingOrderCount);
            alerts.add(alert);
        }

        Long pendingReviewCount = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getStatus, 0)
        );
        if (pendingReviewCount > 0) {
            AlertVO alert = new AlertVO();
            alert.setId(0L);
            alert.setType("pending_review");
            alert.setLevel(1);
            alert.setMessage("There are " + pendingReviewCount + " reviews pending approval");
            alert.setExtra(pendingReviewCount);
            alerts.add(alert);
        }

        log.info("Dashboard alerts query completed, total: {}", alerts.size());
        return alerts;
    }

    @Override
    public DashboardOverviewVO overview() {
        DashboardOverviewVO overview = new DashboardOverviewVO();
        overview.setStats(stats());
        overview.setAlerts(alerts());
        log.info("Management dashboard overview query completed");
        return overview;
    }

    @Override
    public DashboardChartVO charts() {
        DashboardChartVO chart = new DashboardChartVO();

        // 1. Order status distribution
        List<Orders> allOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Orders>().ne(Orders::getStatus, 5)
        );
        Map<Integer, Long> statusCounts = allOrders.stream()
                .collect(Collectors.groupingBy(Orders::getStatus, Collectors.counting()));
        List<DashboardChartVO.ChartItem> statusStats = new ArrayList<>();
        statusStats.add(new DashboardChartVO.ChartItem("Pending Payment", statusCounts.getOrDefault(0, 0L)));
        statusStats.add(new DashboardChartVO.ChartItem("Pending", statusCounts.getOrDefault(1, 0L)));
        statusStats.add(new DashboardChartVO.ChartItem("Preparing", statusCounts.getOrDefault(2, 0L)));
        statusStats.add(new DashboardChartVO.ChartItem("Completed", statusCounts.getOrDefault(3, 0L)));
        statusStats.add(new DashboardChartVO.ChartItem("Cancelled", statusCounts.getOrDefault(4, 0L)));
        chart.setOrderStatusStats(statusStats);

        // 2. Order type distribution
        Map<Integer, Long> typeCounts = allOrders.stream()
                .collect(Collectors.groupingBy(Orders::getType, Collectors.counting()));
        List<DashboardChartVO.ChartItem> typeStats = new ArrayList<>();
        typeStats.add(new DashboardChartVO.ChartItem("Dine-in", typeCounts.getOrDefault(0, 0L)));
        typeStats.add(new DashboardChartVO.ChartItem("Takeout", typeCounts.getOrDefault(1, 0L)));
        chart.setOrderTypeStats(typeStats);

        // 3. Category distribution
        List<Product> allProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1)
        );
        Map<Long, Long> categoryCounts = allProducts.stream()
                .collect(Collectors.groupingBy(Product::getCategoryId, Collectors.counting()));

        List<Category> categories = categoryMapper.selectList(null);
        Map<Long, String> categoryNames = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        List<DashboardChartVO.CategoryStat> categoryStats = new ArrayList<>();
        categoryCounts.forEach((catId, count) -> {
            String name = categoryNames.getOrDefault(catId, "Unknown");
            categoryStats.add(new DashboardChartVO.CategoryStat(name, count));
        });
        chart.setCategoryStats(categoryStats);

        // 4. Monthly trend (last 6 months)
        LocalDate now = LocalDate.now();
        List<OrderStatVO> monthlyTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            LocalDateTime monthStartTime = monthStart.atStartOfDay();
            LocalDateTime monthEndTime = monthEnd.atTime(LocalTime.MAX);

            Long orderCount = orderMapper.selectCount(
                    new LambdaQueryWrapper<Orders>()
                            .ge(Orders::getCreatedAt, monthStartTime)
                            .le(Orders::getCreatedAt, monthEndTime)
                            .ne(Orders::getStatus, 5)
            );

            List<Orders> completedInMonth = orderMapper.selectList(
                    new LambdaQueryWrapper<Orders>()
                            .ge(Orders::getCreatedAt, monthStartTime)
                            .le(Orders::getCreatedAt, monthEndTime)
                            .eq(Orders::getStatus, 3)
            );
            BigDecimal revenue = completedInMonth.stream()
                    .map(Orders::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            OrderStatVO stat = new OrderStatVO();
            stat.setDate(monthStart.toString());
            stat.setOrderCount(orderCount);
            stat.setRevenue(revenue);
            monthlyTrend.add(stat);
        }
        chart.setMonthlyTrend(monthlyTrend);

        log.info("Dashboard charts query completed");
        return chart;
    }
}