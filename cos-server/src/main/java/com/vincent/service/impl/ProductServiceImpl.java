package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.ProductCreateDTO;
import com.vincent.dto.SkuCreateDTO;
import com.vincent.entity.Category;
import com.vincent.entity.Product;
import com.vincent.entity.Sku;
import com.vincent.mapper.CategoryMapper;
import com.vincent.mapper.ProductMapper;
import com.vincent.mapper.SkuMapper;
import com.vincent.service.ProductService;
import com.vincent.vo.PageVO;
import com.vincent.vo.ProductVO;
import com.vincent.vo.SkuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public List<ProductVO> listAll() {
        List<Product> list = productMapper.selectList(
                new LambdaQueryWrapper<Product>().orderByDesc(Product::getCreatedAt)
        );
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public PageVO<ProductVO> pageQuery(String keyword, Long categoryId, Integer status, String lowStock, Integer pageNum, Integer pageSize) {
        Page<Product> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .like(StringUtils.hasText(keyword), Product::getName, keyword)
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .eq(status != null, Product::getStatus, status)
                .orderByDesc(Product::getCreatedAt);

        // lowStock 筛选：只查询有库存预警的商品，需在内存中进一步过滤
        Page<Product> result = page(page, wrapper);

        // 先转 VO（含聚合计算），再按 lowStock 过滤
        List<ProductVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        if ("1".equals(lowStock)) {
            voList = voList.stream()
                    .filter(vo -> Boolean.TRUE.equals(vo.getLowStock()))
                    .collect(Collectors.toList());
        }

        long total = "1".equals(lowStock) ? voList.size() : result.getTotal();
        return new PageVO<>(total, pageNum, pageSize, voList);
    }

    @Override
    public ProductVO getProductDetail(Long id) {
        Product product = getById(id);
        if (product == null) throw new ServiceException("商品不存在");
        return convertToVO(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProduct(ProductCreateDTO dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setSales(0);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        save(product);

        // 前端 inline 传入的 SKU
        if (dto.getSkus() != null && !dto.getSkus().isEmpty()) {
            batchCreateSku(product.getId(), dto.getSkus());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Long id, ProductCreateDTO dto) {
        Product product = getById(id);
        if (product == null) throw new ServiceException("商品不存在");
        BeanUtils.copyProperties(dto, product);
        product.setId(id);
        product.setUpdatedAt(LocalDateTime.now());
        updateById(product);

        // 前端 inline 传入的 SKU（全量覆盖）
        if (dto.getSkus() != null && !dto.getSkus().isEmpty()) {
            batchCreateSku(id, dto.getSkus());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        if (getById(id) == null) throw new ServiceException("商品不存在");
        // 同时删除关联SKU
        skuMapper.delete(new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, id));
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCreateSku(Long productId, List<SkuCreateDTO> skuList) {
        // 先清理旧的SKU
        skuMapper.delete(new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, productId));

        List<Sku> list = skuList.stream().map(dto -> {
            Sku sku = new Sku();
            BeanUtils.copyProperties(dto, sku);
            sku.setProductId(productId);
            return sku;
        }).collect(Collectors.toList());

        list.forEach(skuMapper::insert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateSku(Long productId, List<SkuCreateDTO> skuList) {
        // 全量覆盖：删旧插新
        batchCreateSku(productId, skuList);
    }

    private ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);

        // 填充分类名
        Category category = categoryMapper.selectById(product.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }

        // 填充 SKU 列表，并计算聚合字段（管理端列表页所需）
        List<Sku> skuList = skuMapper.selectList(
                new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, product.getId())
        );
        List<SkuVO> skuVOList = skuList.stream().map(sku -> {
            SkuVO skuVO = new SkuVO();
            BeanUtils.copyProperties(sku, skuVO);
            return skuVO;
        }).collect(Collectors.toList());
        vo.setSkus(skuVOList);
        vo.setSkuCount(skuList.size());
        if (skuList.isEmpty()) {
            vo.setMinPrice(null);
            vo.setMaxPrice(null);
            vo.setStock(0);
            vo.setLowStock(false);
        } else {
            java.math.BigDecimal min = skuList.get(0).getPrice();
            java.math.BigDecimal max = skuList.get(0).getPrice();
            int totalStock = 0;
            boolean low = false;
            for (Sku sku : skuList) {
                if (sku.getPrice().compareTo(min) < 0) min = sku.getPrice();
                if (sku.getPrice().compareTo(max) > 0) max = sku.getPrice();
                totalStock += sku.getStock() != null ? sku.getStock() : 0;
                if (sku.getWarnStock() != null && sku.getStock() != null
                        && sku.getStock() <= sku.getWarnStock()) {
                    low = true;
                }
            }
            vo.setMinPrice(min);
            vo.setMaxPrice(max);
            vo.setStock(totalStock);
            vo.setLowStock(low);
        }

        return vo;
    }

}