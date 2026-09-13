package com.vincent.vo;

import lombok.Data;

import java.util.List;

@Data
public class DashboardOverviewVO {
    private DashboardVO stats;
    private List<AlertVO> alerts;
}