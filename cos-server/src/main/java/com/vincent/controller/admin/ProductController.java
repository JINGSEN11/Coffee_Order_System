package com.vincent.controller.admin;

import com.vincent.common.Result;
import com.vincent.dto.ProductCreateDTO;
import com.vincent.dto.SkuCreateDTO;
import com.vincent.service.ProductService;
import com.vincent.vo.PageVO;
import com.vincent.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminProductController")
@RequestMapping("/admin/product")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @GetMapping("/page")
    public Result<PageVO<ProductVO>> page(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(productService.pageQuery(name, categoryId, status, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.success(productService.getProductDetail(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody ProductCreateDTO dto) {
        productService.createProduct(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ProductCreateDTO dto) {
        productService.updateProduct(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }

    @PostMapping("/{id}/sku")
    public Result<Void> batchCreateSku(@PathVariable Long id, @RequestBody List<SkuCreateDTO> skuList) {
        productService.batchCreateSku(id, skuList);
        return Result.success();
    }

    @PutMapping("/{id}/sku")
    public Result<Void> batchUpdateSku(@PathVariable Long id, @RequestBody List<SkuCreateDTO> skuList) {
        productService.batchUpdateSku(id, skuList);
        return Result.success();
    }

}