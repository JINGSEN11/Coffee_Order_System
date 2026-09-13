package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.ShopCreateDTO;
import com.vincent.entity.Shop;
import com.vincent.vo.PageVO;
import com.vincent.vo.ShopVO;

public interface ShopService extends IService<Shop> {

    PageVO<ShopVO> pageQuery(String name, Integer status, Integer page, Integer pageSize);

    ShopVO getShopDetail(Long id);

    void createShop(ShopCreateDTO dto);

    void updateShop(Long id, ShopCreateDTO dto);

    void deleteShop(Long id);

    void toggleAcceptOrder(Long id, Integer acceptOrder);
}