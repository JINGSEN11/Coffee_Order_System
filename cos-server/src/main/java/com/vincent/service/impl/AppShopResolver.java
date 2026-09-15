package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vincent.entity.Shop;
import com.vincent.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 门店解析：契约里购物车/首页/菜单的 shop_id 均可缺省，统一取「库中第一条门店」为当前门店，
 * 与 ShopService#getShopInfo 的既有口径一致。
 */
@Component
@RequiredArgsConstructor
public class AppShopResolver {

    private final ShopMapper shopMapper;

    /** 指定门店优先，缺省时回退到第一条门店；无门店时返回 null */
    public Shop resolve(Long shopId) {
        if (shopId != null) {
            Shop shop = shopMapper.selectById(shopId);
            if (shop != null) {
                return shop;
            }
        }
        return shopMapper.selectOne(
                new LambdaQueryWrapper<Shop>().orderByAsc(Shop::getId).last("LIMIT 1")
        );
    }

    /** 缺省门店 ID，无门店时返回 null */
    public Long resolveId(Long shopId) {
        Shop shop = resolve(shopId);
        return shop == null ? null : shop.getId();
    }
}
