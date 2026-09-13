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
    public PageVO<ProductVO> pageQuery(String name, Long categoryId, Integer status, Integer pageNum, Integer pageSize) {
        Page<Product> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .like(StringUtils.hasText(name), Product::getName, name)
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .eq(status != null, Product::getStatus, status)
                .orderByDesc(Product::getCreatedAt);

        Page<Product> result = page(page, wrapper);

        List<ProductVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageVO<>(result.getTotal(), pageNum, pageSize, voList);
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

        // 填充SKU列表
        List<Sku> skuList = skuMapper.selectList(
                new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, product.getId())
        );
        List<SkuVO> skuVOList = skuList.stream().map(sku -> {
            SkuVO skuVO = new SkuVO();
            BeanUtils.copyProperties(sku, skuVO);
            return skuVO;
        }).collect(Collectors.toList());
        vo.setSkuList(skuVOList);

        return vo;
    }

}