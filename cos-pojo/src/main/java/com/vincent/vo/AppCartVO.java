package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车完整视图（契约 6.1 CartVO）
 */
@Data
public class AppCartVO {
    private List<AppCartItemVO> list;
    /** 已勾选行的商品合计 */
    private BigDecimal goodsAmount;
    private BigDecimal couponAmount;
    private Integer pointsUsed;
    private BigDecimal pointsAmount;
    /** 已勾选行的应付金额（仅商品合计，券与积分在结算页试算） */
    private BigDecimal payable;
    /** 全部行的数量合计（含未勾选） */
    private Integer totalQty;
}
