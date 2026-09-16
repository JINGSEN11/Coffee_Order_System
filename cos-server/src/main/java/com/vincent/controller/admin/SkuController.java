package com.vincent.controller.admin;

import com.vincent.annotation.OpLog;
import com.vincent.annotation.RequirePerm;
import com.vincent.common.Result;
import com.vincent.dto.SkuCreateDTO;
import com.vincent.dto.SkuQueryDTO;
import com.vincent.service.SkuService;
import com.vincent.vo.PageVO;
import com.vincent.vo.SkuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("adminSkuController")
@RequestMapping("/admin/sku")
@OpLog(module = "SKU")
@RequiredArgsConstructor
@Slf4j
public class SkuController {

    private final SkuService skuService;

    /**
     * SKU分页查询
     */
    @GetMapping("/page")
    @RequirePerm("product:stock")
    public Result<PageVO<SkuVO>> page(SkuQueryDTO dto) {
        log.info("管理端分页查询SKU：{}", dto);
        PageVO<SkuVO> pageVO = skuService.pageQuery(dto);
        return Result.success(pageVO);
    }

    /**
     * 库存预警列表（stock <= warn_stock）
     */
    @GetMapping("/warn")
    @RequirePerm("product:stock")
    public Result<PageVO<SkuVO>> warn(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("管理端查询预警SKU：page={}, pageSize={}", page, pageSize);
        PageVO<SkuVO> pageVO = skuService.warnPage(page, pageSize);
        return Result.success(pageVO);
    }

    /**
     * SKU详情
     */
    @GetMapping("/{id}")
    @RequirePerm("product:stock")
    public Result<SkuVO> detail(@PathVariable Long id) {
        log.info("管理端查询SKU详情：id={}", id);
        return Result.success(skuService.getSkuDetail(id));
    }

    /**
     * 更新SKU
     */
    @PutMapping("/{id}")
    @RequirePerm("product:edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody SkuCreateDTO dto) {
        log.info("管理端更新SKU：id={}, dto={}", id, dto);
        skuService.updateSku(id, dto);
        return Result.success();
    }

}