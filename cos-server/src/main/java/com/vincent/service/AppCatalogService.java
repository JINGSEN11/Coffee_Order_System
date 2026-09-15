package com.vincent.service;

import com.vincent.entity.Product;
import com.vincent.vo.AppMenuVO;
import com.vincent.vo.AppProductDetailVO;
import com.vincent.vo.AppProductVO;
import com.vincent.vo.AppSearchVO;

import java.util.List;

/**
 * C 端商品目录服务（菜单 / 商品详情 / 搜索），并对外提供商品列表项的统一装配能力。
 */
public interface AppCatalogService {

    /** 菜单：分类 + 商品 + 购物车 */
    AppMenuVO menu(Long shopId);

    /** 商品详情：SPU + SKU 矩阵 + 规格模板 + 加料 + 参数 + 评价摘要 */
    AppProductDetailVO productDetail(Long productId);

    /** 商品搜索 */
    AppSearchVO search(String kw);

    /** 把 SPU 列表装配成 C 端商品列表项（含价格区间/售罄/低库存/评价均分） */
    List<AppProductVO> buildProductVOs(List<Product> products);
}
