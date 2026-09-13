package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.OrderCreateDTO;
import com.vincent.dto.OrderQueryDTO;
import com.vincent.entity.Orders;
import com.vincent.vo.OrderBoardVO;
import com.vincent.vo.OrderVO;
import com.vincent.vo.PageVO;

import java.util.List;

public interface OrderService extends IService<Orders> {

    OrderBoardVO board();

    List<OrderVO> pool();

    OrderVO detail(Long id);

    PageVO<OrderVO> pageQuery(OrderQueryDTO dto);

    OrderVO create(OrderCreateDTO dto, Long userId);

    void cancel(Long id, String reason);

    void updateStatus(Long id, Integer status);

}