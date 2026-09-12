package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.OrderCreateDTO;
import com.vincent.dto.OrderQueryDTO;
import com.vincent.entity.Orders;
import com.vincent.vo.OrderVO;
import com.vincent.vo.PageVO;

public interface OrderService extends IService<Orders> {

    OrderVO detail(Long id);

    PageVO<OrderVO> pageQuery(OrderQueryDTO dto);

    OrderVO create(OrderCreateDTO dto, Long userId);

    void cancel(Long id, String reason);

    void updateStatus(Long id, Integer status);

}