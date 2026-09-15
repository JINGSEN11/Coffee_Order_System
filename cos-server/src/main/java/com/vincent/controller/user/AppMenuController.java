package com.vincent.controller.user;

import com.vincent.common.Result;
import com.vincent.service.AppCatalogService;
import com.vincent.vo.AppMenuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/menu")
@RequiredArgsConstructor
@Slf4j
public class AppMenuController {

    private final AppCatalogService appCatalogService;

    /** 菜单：分类 + 商品 + 购物车 */
    @GetMapping
    public Result<AppMenuVO> menu(@RequestParam(value = "shop_id", required = false) Long shopId) {
        return Result.success(appCatalogService.menu(shopId));
    }
}
