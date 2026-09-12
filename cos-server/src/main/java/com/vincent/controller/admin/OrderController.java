package com.vincent.controller.admin;

import com.vincent.common.Result;
import com.vincent.dto.OrderQueryDTO;
import com.vincent.service.OrderService;
import com.vincent.vo.OrderVO;
import com.vincent.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * 订单详情
     */
    @GetMapping("/details/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        log.info("管理端查询订单详情，id：{}", id);
        OrderVO orderVO = orderService.detail(id);
        return Result.success(orderVO);
    }

    /**
     * 订单分页查询
     */
    @GetMapping("/page")
    public Result<PageVO<OrderVO>> page(OrderQueryDTO dto) {
        log.info("管理端分页查询订单：{}", dto);
        PageVO<OrderVO> pageVO = orderService.pageQuery(dto);
        return Result.success(pageVO);
    }

    /**
     * 更新订单状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        log.info("管理端更新订单状态，id：{}，status：{}", id, status);
        orderService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 取消订单
     */
    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        log.info("管理端取消订单，id：{}，原因：{}", id, reason);
        orderService.cancel(id, reason);
        return Result.success();
    }

}