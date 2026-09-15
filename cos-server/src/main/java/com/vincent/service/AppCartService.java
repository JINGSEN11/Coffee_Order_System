package com.vincent.service;

import com.vincent.dto.AppCartAddDTO;
import com.vincent.dto.AppCartUpdateDTO;
import com.vincent.entity.OrderItem;
import com.vincent.vo.AppCartItemVO;
import com.vincent.vo.AppCartVO;

import java.util.List;

/**
 * C 端购物车服务。
 * 合并规则：同一 user_id + sku_id + shop_id + addons_hash 才合并数量，加料不同视为两行。
 */
public interface AppCartService {

    /** 购物车完整视图 */
    AppCartVO buildCart(Long userId);

    /** 加入购物车（含合并），返回完整 CartVO */
    AppCartVO add(Long userId, AppCartAddDTO dto);

    /** 修改购物车行（qty / checked 可同时传），返回完整 CartVO */
    AppCartVO update(Long userId, Long lineId, AppCartUpdateDTO dto);

    /** 删除指定行；lineId 为空表示清空当前用户购物车 */
    AppCartVO delete(Long userId, Long lineId);

    /**
     * 取参与结算的购物车行。
     * 显式传入 lineIds 时只结算这些行（空数组视为「未选中任何商品」）；
     * 完全不传（null）时才回退到购物车勾选态 —— 与小程序 utils/mock.js 的既有语义一致。
     */
    List<AppCartItemVO> settleLines(Long userId, List<Long> lineIds);

    /** 再来一单：把历史订单明细写回购物车，返回实际加入的行数 */
    int addBack(Long userId, Long shopId, List<OrderItem> items);

    /** 支付成功后删除已下单项对应的购物车行 */
    void removeOrdered(Long userId, Long shopId, List<Long> skuIds);
}
