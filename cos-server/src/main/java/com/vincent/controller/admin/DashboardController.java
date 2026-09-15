package com.vincent.controller.admin;

import com.vincent.annotation.RequirePerm;
import com.vincent.common.Result;
import com.vincent.service.DashboardService;
import com.vincent.vo.AlertVO;
import com.vincent.vo.DashboardChartVO;
import com.vincent.vo.DashboardOverviewVO;
import com.vincent.vo.DashboardVO;
import com.vincent.vo.OrderStatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 数据看板。
 * 类级 @RequirePerm 不带权限码：只要求是已登录的管理员。
 * 看板对应菜单「工作台」，menu 表里它是目录节点（type=1）没有 perms，
 * 而门店店员（role 3）也需要看板，所以这里不挂具体权限码。
 */
@RestController("adminDashboardController")
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
@RequirePerm
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<DashboardVO> stats() {
        log.info("管理端查询数据看板概览");
        return Result.success(dashboardService.stats());
    }

    @GetMapping("/trend")
    public Result<List<OrderStatVO>> trend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("管理端查询数据趋势：startDate={}, endDate={}", startDate, endDate);

        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(6);

        return Result.success(dashboardService.trend(start, end));
    }

    @GetMapping("/alerts")
    public Result<List<AlertVO>> alerts() {
        log.info("管理端查询数据看板告警");
        return Result.success(dashboardService.alerts());
    }

    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview() {
        log.info("管理端查询数据看板总览");
        return Result.success(dashboardService.overview());
    }

    @GetMapping("/charts")
    public Result<DashboardChartVO> charts() {
        log.info("管理端查询数据看板图表");
        return Result.success(dashboardService.charts());
    }

}