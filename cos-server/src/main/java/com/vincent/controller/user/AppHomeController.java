package com.vincent.controller.user;

import com.vincent.common.BaseContext;
import com.vincent.common.Result;
import com.vincent.service.AppShopService;
import com.vincent.vo.AppHomeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/home")
@RequiredArgsConstructor
@Slf4j
public class AppHomeController {

    private final AppShopService appShopService;

    /** 首页聚合：门店 + 轮播 + 公告 + 分类 + 热销 + 推荐 + 进行中订单 + 会员摘要 */
    @GetMapping
    public Result<AppHomeVO> home(@RequestParam(value = "shop_id", required = false) Long shopId) {
        return Result.success(appShopService.home(shopId, BaseContext.getCurrentId()));
    }
}
