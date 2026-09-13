package com.vincent.controller.admin;

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
@RequiredArgsConstructor
@Slf4j
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/page")
    public Result<PageVO<ShopVO>> page(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(shopService.pageQuery(name, status, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<ShopVO> detail(@PathVariable Long id) {
        return Result.success(shopService.getShopDetail(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody ShopCreateDTO dto) {
        shopService.createShop(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ShopCreateDTO dto) {
        shopService.updateShop(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        shopService.deleteShop(id);
        return Result.success();
    }

    @PutMapping("/{id}/accept-order")
    public Result<Void> toggleAcceptOrder(@PathVariable Long id, @RequestParam Integer acceptOrder) {
        shopService.toggleAcceptOrder(id, acceptOrder);
        return Result.success();
    }

}