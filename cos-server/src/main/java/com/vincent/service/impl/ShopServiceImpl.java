package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.ShopCreateDTO;
import com.vincent.entity.Shop;
import com.vincent.mapper.ShopMapper;
import com.vincent.service.ShopService;
import com.vincent.vo.PageVO;
import com.vincent.vo.ShopVO;
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
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {

    private final ShopMapper shopMapper;

    @Override
    public PageVO<ShopVO> pageQuery(String name, Integer status, Integer pageNum, Integer pageSize) {
        Page<Shop> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<Shop>()
                .like(StringUtils.hasText(name), Shop::getName, name)
                .eq(status != null, Shop::getStatus, status)
                .orderByDesc(Shop::getCreatedAt);

        Page<Shop> result = page(page, wrapper);

        List<ShopVO> voList = result.getRecords().stream().map(s -> {
            ShopVO vo = new ShopVO();
            BeanUtils.copyProperties(s, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageVO<>(result.getTotal(), pageNum, pageSize, voList);
    }

    @Override
    public ShopVO getShopDetail(Long id) {
        Shop shop = getById(id);
        if (shop == null) throw new ServiceException("门店不存在");
        ShopVO vo = new ShopVO();
        BeanUtils.copyProperties(shop, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createShop(ShopCreateDTO dto) {
        Shop shop = new Shop();
        BeanUtils.copyProperties(dto, shop);
        shop.setCreatedAt(LocalDateTime.now());
        shop.setUpdatedAt(LocalDateTime.now());
        save(shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShop(Long id, ShopCreateDTO dto) {
        Shop shop = getById(id);
        if (shop == null) throw new ServiceException("门店不存在");
        BeanUtils.copyProperties(dto, shop);
        shop.setId(id);
        shop.setUpdatedAt(LocalDateTime.now());
        updateById(shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShop(Long id) {
        if (getById(id) == null) throw new ServiceException("门店不存在");
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleAcceptOrder(Long id, Integer acceptOrder) {
        Shop shop = getById(id);
        if (shop == null) throw new ServiceException("门店不存在");
        shop.setAcceptOrder(acceptOrder);
        shop.setUpdatedAt(LocalDateTime.now());
        updateById(shop);
    }

}