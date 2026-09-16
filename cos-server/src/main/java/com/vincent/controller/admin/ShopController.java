package com.vincent.controller.admin;

import com.vincent.annotation.OpLog;
import com.vincent.annotation.RequirePerm;
import com.vincent.common.Result;
import com.vincent.dto.ShopCreateDTO;
import com.vincent.service.ShopService;
import com.vincent.vo.PageVO;
import com.vincent.vo.ShopVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@OpLog(module = "门店")
@RequiredArgsConstructor
@Slf4j
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/page")
    @RequirePerm("shop:info")
    public Result<PageVO<ShopVO>> page(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(shopService.pageQuery(name, status, page, pageSize));
    }

    @GetMapping("/info")
    @RequirePerm("shop:info")
    public Result<ShopVO> info() {
        log.info("管理端查询门店信息");
        return Result.success(shopService.getShopInfo());
    }

    @PutMapping("/info")
    @RequirePerm("shop:info")
    public Result<Void> updateInfo(@RequestBody ShopCreateDTO dto) {
        log.info("管理端更新门店信息");
        shopService.updateShopInfo(dto);
        return Result.success();
    }

    @GetMapping("/{id}")
    @RequirePerm("shop:info")
    public Result<ShopVO> detail(@PathVariable Long id) {
        return Result.success(shopService.getShopDetail(id));
    }

    @PostMapping
    @RequirePerm("shop:info")
    public Result<Void> create(@RequestBody ShopCreateDTO dto) {
        shopService.createShop(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequirePerm("shop:info")
    public Result<Void> update(@PathVariable Long id, @RequestBody ShopCreateDTO dto) {
        shopService.updateShop(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("shop:info")
    public Result<Void> delete(@PathVariable Long id) {
        shopService.deleteShop(id);
        return Result.success();
    }

    @PutMapping("/{id}/accept-order")
    @RequirePerm("shop:accept-toggle")
    @OpLog(action = "切换接单开关")
    public Result<Void> toggleAcceptOrder(@PathVariable Long id, @RequestParam Integer acceptOrder) {
        shopService.toggleAcceptOrder(id, acceptOrder);
        return Result.success();
    }

}