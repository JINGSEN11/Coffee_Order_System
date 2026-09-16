package com.vincent.controller.admin;

import com.vincent.annotation.OpLog;
import com.vincent.annotation.RequirePerm;
import com.vincent.common.Result;
import com.vincent.dto.SkuCreateDTO;
import com.vincent.dto.SkuQueryDTO;
import com.vincent.dto.StockInDTO;
import com.vincent.service.SkuService;
import com.vincent.vo.PageVO;
import com.vincent.vo.SkuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("adminStockController")
@RequestMapping("/admin/stock")
@OpLog(module = "库存")
@RequiredArgsConstructor
@Slf4j
public class StockController {

    private final SkuService skuService;

    /**
     * 库存分页列表（按 SKU 维度，返回里带商品名）
     */
    @GetMapping("/list")
    @RequirePerm("product:stock")
    public Result<PageVO<SkuVO>> list(SkuQueryDTO dto) {
        log.info("管理端查询库存列表：{}", dto);
        PageVO<SkuVO> pageVO = skuService.pageQuery(dto);
        return Result.success(pageVO);
    }

    /**
     * 入库操作：给某一个 SKU 加库存
     */
    @PostMapping("/in")
    @RequirePerm("stock:adjust")
    @OpLog(action = "入库")
    public Result<Void> stockIn(@RequestBody StockInDTO dto) {
        log.info("管理端入库操作：skuId={}, quantity={}", dto.getSkuId(), dto.getQuantity());

        SkuVO sku = skuService.getSkuDetail(dto.getSkuId());
        int newStock = sku.getStock() + dto.getQuantity();

        SkuCreateDTO updateDTO = new SkuCreateDTO();
        updateDTO.setStock(newStock);

        skuService.updateSku(dto.getSkuId(), updateDTO);
        log.info("入库成功，SKU {} 库存更新为：{}", dto.getSkuId(), newStock);
        return Result.success();
    }

    /**
     * 预警值设置
     */
    @PutMapping("/warning/{id}")
    @RequirePerm("stock:adjust")
    public Result<Void> warning(@PathVariable Long id, @RequestBody SkuCreateDTO dto) {
        log.info("管理端设置预警库存：id={}, warnStock={}", id, dto.getWarnStock());

        SkuCreateDTO updateDTO = new SkuCreateDTO();
        updateDTO.setWarnStock(dto.getWarnStock());

        skuService.updateSku(id, updateDTO);
        log.info("预警设置成功，SKU {} warnStock={}", id, dto.getWarnStock());
        return Result.success();
    }
}
