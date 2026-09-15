package com.vincent.controller.user;

import com.vincent.common.BaseContext;
import com.vincent.common.Result;
import com.vincent.service.AppCouponService;
import com.vincent.vo.AppCouponPageVO;
import com.vincent.vo.AppUsableCouponListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/app/coupons")
@RequiredArgsConstructor
@Slf4j
public class AppCouponController {

    private final AppCouponService appCouponService;

    /** 领券中心（tab=center）｜我的券（tab=0/1/2） */
    @GetMapping
    public Result<AppCouponPageVO> list(@RequestParam(value = "tab", required = false) String tab) {
        return Result.success(appCouponService.list(BaseContext.getCurrentId(), tab));
    }

    /** 领取优惠券 */
    @PostMapping("/{id}/claim")
    public Result<Map<String, Object>> claim(@PathVariable Long id) {
        return Result.success(appCouponService.claim(BaseContext.getCurrentId(), id));
    }

    /** 下单可用券（含试算） */
    @GetMapping("/usable")
    public Result<AppUsableCouponListVO> usable(
            @RequestParam(value = "amount", required = false) BigDecimal amount) {
        return Result.success(appCouponService.usable(BaseContext.getCurrentId(), amount));
    }
}
