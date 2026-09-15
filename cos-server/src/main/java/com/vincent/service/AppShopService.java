package com.vincent.service;

import com.vincent.vo.AppHomeVO;
import com.vincent.vo.AppShopVO;

/**
 * C 端门店与首页聚合服务
 */
public interface AppShopService {

    /** 门店信息 */
    AppShopVO shop(Long shopId);

    /** 首页聚合数据 */
    AppHomeVO home(Long shopId, Long userId);
}
