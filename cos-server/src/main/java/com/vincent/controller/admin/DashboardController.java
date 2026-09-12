package com.vincent.controller.admin;

import com.vincent.common.Result;
import com.vincent.service.DashboardService;
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

@RestController("adminDashboardController")
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
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

}