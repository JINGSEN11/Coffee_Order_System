package com.vincent.service;

import com.vincent.vo.AlertVO;
import com.vincent.vo.DashboardVO;
import com.vincent.vo.OrderStatVO;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {

    DashboardVO stats();

    List<OrderStatVO> trend(LocalDate startDate, LocalDate endDate);

    List<AlertVO> alerts();

}