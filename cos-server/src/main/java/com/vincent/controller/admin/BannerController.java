package com.vincent.controller.admin;

import com.vincent.common.Result;
import com.vincent.dto.BannerCreateDTO;
import com.vincent.service.BannerService;
import com.vincent.vo.BannerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminBannerController")
@RequestMapping("/admin/banner")
@RequiredArgsConstructor
@Slf4j
public class BannerController {

    private final BannerService bannerService;

    @GetMapping("/list")
    public Result<List<BannerVO>> list() {
        log.info("管理端查询 Banner 列表");
        return Result.success(bannerService.listAll());
    }

    @PostMapping
    public Result<Void> create(@RequestBody BannerCreateDTO dto) {
        log.info("管理端新增 Banner：{}", dto);
        bannerService.createBanner(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BannerCreateDTO dto) {
        log.info("管理端修改 Banner：id={}, dto={}", id, dto);
        bannerService.updateBanner(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("管理端删除 Banner：id={}", id);
        bannerService.deleteBanner(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        log.info("管理端切换 Banner 状态：id={}, status={}", id, status);
        bannerService.toggleStatus(id, status);
        return Result.success();
    }

}