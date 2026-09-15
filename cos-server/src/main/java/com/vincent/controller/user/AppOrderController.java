package com.vincent.controller.user;

import com.vincent.common.BaseContext;
import com.vincent.common.Result;
import com.vincent.dto.AppOrderCancelDTO;
import com.vincent.dto.AppOrderCreateDTO;
import com.vincent.dto.AppReviewCreateDTO;
import com.vincent.service.AppOrderService;
import com.vincent.vo.AppOrderListVO;
import com.vincent.vo.AppOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
@Slf4j
public class AppOrderController {

    private final AppOrderService appOrderService;

    /** 创建订单（状态 0 待支付） */
    @PostMapping("/order")
    public Result<AppOrderVO> create(@RequestBody AppOrderCreateDTO dto) {
        return Result.success(appOrderService.create(BaseContext.getCurrentId(), dto));
    }

    /** 订单列表；status 为逗号分隔的状态集合，空或 all 表示全部 */
    @GetMapping("/orders")
    public Result<AppOrderListVO> list(@RequestParam(value = "status", required = false) String status) {
        return Result.success(appOrderService.list(BaseContext.getCurrentId(), status));
    }

    /** 订单详情；id 支持订单主键或 order_no */
    @GetMapping("/order/{id}")
    public Result<AppOrderVO> detail(@PathVariable String id) {
        return Result.success(appOrderService.detail(BaseContext.getCurrentId(), id));
    }

    /** 取消订单（待支付 0 / 待接单 1） */
    @PostMapping("/order/{id}/cancel")
    public Result<AppOrderVO> cancel(@PathVariable String id,
                                     @RequestBody(required = false) AppOrderCancelDTO dto) {
        return Result.success(appOrderService.cancel(BaseContext.getCurrentId(), id,
                dto == null ? null : dto.getReason()));
    }

    /** 再来一单 */
    @PostMapping("/order/{id}/again")
    public Result<Map<String, Object>> again(@PathVariable String id) {
        return Result.success(appOrderService.again(BaseContext.getCurrentId(), id));
    }

    /** 提交评价 */
    @PostMapping("/order/{id}/review")
    public Result<Map<String, Object>> review(@PathVariable String id,
                                              @RequestBody(required = false) AppReviewCreateDTO dto) {
        return Result.success(appOrderService.review(BaseContext.getCurrentId(), id, dto));
    }
}
