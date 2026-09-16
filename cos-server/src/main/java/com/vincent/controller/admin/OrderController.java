package com.vincent.controller.admin;

import com.vincent.annotation.OpLog;
import com.vincent.annotation.RequirePerm;
import com.vincent.common.Result;
import com.vincent.dto.OrderQueryDTO;
import com.vincent.service.OrderService;
import com.vincent.vo.OrderBoardVO;
import com.vincent.vo.OrderVO;
import com.vincent.vo.PageVO;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@OpLog(module = "订单")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * 订单看板数据
     */
    @GetMapping("/board")
    @RequirePerm("order:board")
    public Result<OrderBoardVO> board() {
        return Result.success(orderService.board());
    }

    /**
     * 订单池（待处理/制作中的订单列表）
     */
    @GetMapping("/pool")
    @RequirePerm("order:pool")
    public Result<List<OrderVO>> pool() {
        return Result.success(orderService.pool());
    }

    /**
     * 订单详情
     */
    @GetMapping("/details/{id}")
    @RequirePerm("order:list")
    public Result<OrderVO> detail(@PathVariable Long id) {
        log.info("管理端查询订单详情，id：{}", id);
        OrderVO orderVO = orderService.detail(id);
        return Result.success(orderVO);
    }

    /**
     * 订单分页查询
     */
    @GetMapping("/page")
    @RequirePerm("order:list")
    public Result<PageVO<OrderVO>> page(OrderQueryDTO dto) {
        log.info("管理端分页查询订单：{}", dto);
        PageVO<OrderVO> pageVO = orderService.pageQuery(dto);
        return Result.success(pageVO);
    }

    /**
     * 更新订单状态
     */
    /**
     * 更新订单状态（接单 / 出餐 / 叫号等）。
     * 一个接口承担多种流转，故按「任一订单操作权限」放行：门店店员持有 accept/finish/call/pickup 四种。
     */
    @PutMapping("/{id}/status")
    @RequirePerm({"order:accept", "order:finish", "order:call", "order:pickup"})
    @OpLog(action = "推进订单状态")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        log.info("管理端更新订单状态，id：{}，status：{}", id, status);
        orderService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 取消订单
     */
    @DeleteMapping("/{id}")
    @RequirePerm("order:cancel")
    @OpLog(action = "取消订单")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        log.info("管理端取消订单，id：{}，原因：{}", id, reason);
        orderService.cancel(id, reason);
        return Result.success();
    }

}