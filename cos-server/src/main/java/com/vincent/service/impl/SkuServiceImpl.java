package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.SkuCreateDTO;
import com.vincent.dto.SkuQueryDTO;
import com.vincent.entity.Sku;
import com.vincent.mapper.SkuMapper;
import com.vincent.service.SkuService;
import com.vincent.vo.PageVO;
import com.vincent.vo.SkuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkuServiceImpl extends ServiceImpl<SkuMapper, Sku> implements SkuService {

    private final SkuMapper skuMapper;

    @Override
    public PageVO<SkuVO> warnPage(Integer pageNum, Integer pageSize) {
        Page<Sku> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Sku> wrapper = new LambdaQueryWrapper<Sku>()
                .apply("stock <= warn_stock")
                .orderByAsc(Sku::getStock);

        Page<Sku> result = page(page, wrapper);

        List<SkuVO> voList = result.getRecords().stream().map(sku -> {
            SkuVO vo = new SkuVO();
            BeanUtils.copyProperties(sku, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageVO<>(result.getTotal(), pageNum, pageSize, voList);
    }

    @Override
    public PageVO<SkuVO> pageQuery(SkuQueryDTO dto) {
        Page<Sku> page = new Page<>(dto.getPage(), dto.getPageSize());

        LambdaQueryWrapper<Sku> wrapper = new LambdaQueryWrapper<Sku>()
                .eq(dto.getProductId() != null, Sku::getProductId, dto.getProductId())
                .orderByDesc(Sku::getCreatedAt);

        Page<Sku> result = page(page, wrapper);

        List<SkuVO> voList = result.getRecords().stream().map(sku -> {
            SkuVO vo = new SkuVO();
            BeanUtils.copyProperties(sku, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageVO<>(result.getTotal(), dto.getPage(), dto.getPageSize(), voList);
    }

    @Override
    public SkuVO getSkuDetail(Long id) {
        Sku sku = getById(id);
        if (sku == null) {
            throw new ServiceException("SKU不存在");
        }
        SkuVO vo = new SkuVO();
        BeanUtils.copyProperties(sku, vo);
        return vo;
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