package com.vincent.controller.user;

import com.vincent.common.Result;
import com.vincent.service.AppShopService;
import com.vincent.vo.AppShopVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/shop")
@RequiredArgsConstructor
@Slf4j
public class AppShopController {

    private final AppShopService appShopService;

    /** 门店信息 */
    @GetMapping
    public Result<AppShopVO> shop(@RequestParam(value = "shop_id", required = false) Long shopId) {
        return Result.success(appShopService.shop(shopId));
    }
}
