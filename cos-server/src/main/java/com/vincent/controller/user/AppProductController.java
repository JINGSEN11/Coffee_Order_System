package com.vincent.controller.user;

import com.vincent.common.Result;
import com.vincent.service.AppCatalogService;
import com.vincent.vo.AppProductDetailVO;
import com.vincent.vo.AppSearchVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
@Slf4j
public class AppProductController {

    private final AppCatalogService appCatalogService;

    /** 商品详情：SPU + SKU 矩阵 + 规格模板 + 加料 + 参数 + 评价摘要 */
    @GetMapping("/product/{id}")
    public Result<AppProductDetailVO> detail(@PathVariable Long id) {
        return Result.success(appCatalogService.productDetail(id));
    }

    /** 商品搜索；kw 为空串时 list 返回空数组，hot_keywords 照常返回 */
    @GetMapping("/search")
    public Result<AppSearchVO> search(@RequestParam(value = "kw", required = false) String kw) {
        return Result.success(appCatalogService.search(kw));
    }
}
