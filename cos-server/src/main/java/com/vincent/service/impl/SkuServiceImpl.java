package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.SkuCreateDTO;
import com.vincent.dto.SkuQueryDTO;
import com.vincent.entity.Product;
import com.vincent.entity.Sku;
import com.vincent.mapper.ProductMapper;
import com.vincent.mapper.SkuMapper;
import com.vincent.service.SkuService;
import com.vincent.vo.PageVO;
import com.vincent.vo.SkuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkuServiceImpl extends ServiceImpl<SkuMapper, Sku> implements SkuService {

    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;

    @Override
    public PageVO<SkuVO> warnPage(Integer pageNum, Integer pageSize) {
        Page<Sku> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Sku> wrapper = new LambdaQueryWrapper<Sku>()
                .apply("stock <= warn_stock")
                .orderByAsc(Sku::getStock);

        Page<Sku> result = page(page, wrapper);

        return new PageVO<>(result.getTotal(), pageNum, pageSize, toVoList(result.getRecords()));
    }

    @Override
    public PageVO<SkuVO> pageQuery(SkuQueryDTO dto) {
        Page<Sku> page = new Page<>(dto.getPage(), dto.getPageSize());

        LambdaQueryWrapper<Sku> wrapper = new LambdaQueryWrapper<Sku>()
                .eq(dto.getProductId() != null, Sku::getProductId, dto.getProductId())
                .orderByDesc(Sku::getCreatedAt);

        // 库存管理页支持按商品名称搜索：sku 表没有名称列，先按名称解析出商品 id 再过滤
        if (StringUtils.hasText(dto.getProductName())) {
            List<Long> productIds = productMapper.selectObjs(
                    new LambdaQueryWrapper<Product>()
                            .select(Product::getId)
                            .like(Product::getName, dto.getProductName())
            ).stream().map(id -> ((Number) id).longValue()).collect(Collectors.toList());

            if (productIds.isEmpty()) {
                return new PageVO<>(0L, dto.getPage(), dto.getPageSize(), List.of());
            }
            wrapper.in(Sku::getProductId, productIds);
        }

        Page<Sku> result = page(page, wrapper);

        return new PageVO<>(result.getTotal(), dto.getPage(), dto.getPageSize(), toVoList(result.getRecords()));
    }

    @Override
    public SkuVO getSkuDetail(Long id) {
        Sku sku = getById(id);
        if (sku == null) {
            throw new ServiceException("SKU不存在");
        }
        return toVoList(List.of(sku)).get(0);
    }

    /**
     * 转 VO 并补上所属商品名。按商品 id 批量取名称，避免逐行查库（N+1）。
     */
    private List<SkuVO> toVoList(List<Sku> skus) {
        if (skus == null || skus.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = skus.stream()
                .map(Sku::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> nameById = productIds.isEmpty() ? Map.of() : productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .select(Product::getId, Product::getName)
                        .in(Product::getId, productIds)
        ).stream().collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));

        return skus.stream().map(sku -> {
            SkuVO vo = new SkuVO();
            BeanUtils.copyProperties(sku, vo);
            vo.setProductName(nameById.get(sku.getProductId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSku(Long id, SkuCreateDTO dto) {
        Sku sku = getById(id);
        if (sku == null) {
            throw new ServiceException("SKU不存在");
        }
        BeanUtils.copyProperties(dto, sku);
        sku.setId(id);
        sku.setUpdatedAt(LocalDateTime.now());
        updateById(sku);
    }

}