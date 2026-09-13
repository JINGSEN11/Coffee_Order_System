package com.vincent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardChartVO {
    private List<ChartItem> orderStatusStats;
    private List<ChartItem> orderTypeStats;
    private List<CategoryStat> categoryStats;
    private List<OrderStatVO> monthlyTrend;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartItem {
        private String name;
        private Long value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStat {
        private String name;
        private Long value;
    }
}