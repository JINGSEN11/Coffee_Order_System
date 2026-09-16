package com.vincent.controller.admin;

import com.vincent.annotation.OpLog;
import com.vincent.annotation.RequirePerm;
import com.vincent.common.Result;
import com.vincent.dto.ProductCreateDTO;
import com.vincent.dto.SkuCreateDTO;
import com.vincent.dto.SpecConfigDTO;
import com.vincent.service.ProductService;
import com.vincent.service.SpecConfigService;
import com.vincent.vo.PageVO;
import com.vincent.vo.ProductVO;
import com.vincent.vo.SpecGroupAdminVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController("adminProductController")
@RequestMapping("/admin/product")
@OpLog(module = "商品")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final SpecConfigService specConfigService;

    @GetMapping("/list")
    @RequirePerm("product:list")
    public Result<List<ProductVO>> list() {
        return Result.success(productService.listAll());
    }

    @GetMapping("/page")
    @RequirePerm("product:list")
    public Result<PageVO<ProductVO>> page(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String lowStock) {
        // 管理端契约（管理端-API联调用例 15.2）与 admin-web 搜索框都用 name，
        // keyword 作为历史写法保留兼容。
        String searchName = StringUtils.hasText(name) ? name : keyword;
        return Result.success(productService.pageQuery(searchName, categoryId, status, lowStock, page, pageSize));
    }

    @GetMapping("/{id}")
    @RequirePerm("product:list")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.success(productService.getProductDetail(id));
    }

    @PostMapping
    @RequirePerm("product:add")
    public Result<Map<String, Long>> create(@RequestBody ProductCreateDTO dto) {
        Long id = productService.createProduct(dto);
        return Result.success(Collections.singletonMap("id", id));
    }

    @PutMapping("/{id}")
    @RequirePerm("product:edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody ProductCreateDTO dto) {
        productService.updateProduct(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("product:delete")
    public Result<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequirePerm("product:online")
    public Result<Void> setStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        productService.setStatus(id, body.getOrDefault("status", 0));
        return Result.success();
    }

    @PostMapping("/{id}/sku")
    @RequirePerm("product:edit")
    public Result<Void> batchCreateSku(@PathVariable Long id, @RequestBody List<SkuCreateDTO> skuList) {
        productService.batchCreateSku(id, skuList);
        return Result.success();
    }

    @PutMapping("/{id}/sku")
    @RequirePerm("product:edit")
    public Result<Void> batchUpdateSku(@PathVariable Long id, @RequestBody List<SkuCreateDTO> skuList) {
        productService.batchUpdateSku(id, skuList);
        return Result.success();
    }

    /* ---------------- 规格组与加料配置 ---------------- */

    /** 规格组字典（杯型/温度/糖度及其选项），供配置面板渲染 */
    @GetMapping("/spec-groups")
    @RequirePerm("product:list")
    public Result<List<SpecGroupAdminVO>> specGroups() {
        return Result.success(specConfigService.groups());
    }

    /** 某商品的规格与加料配置 */
    @GetMapping("/{id}/spec-config")
    @RequirePerm("product:list")
    public Result<SpecConfigDTO> specConfig(@PathVariable Long id) {
        return Result.success(specConfigService.configOf(id));
    }

    /** 保存某商品的规格与加料配置（会为价格维度组补齐缺失 SKU，但不删已有 SKU） */
    @PutMapping("/{id}/spec-config")
    @RequirePerm("product:edit")
    public Result<Void> saveSpecConfig(@PathVariable Long id, @RequestBody SpecConfigDTO dto) {
        specConfigService.saveConfig(id, dto);
        return Result.success();
    }

}