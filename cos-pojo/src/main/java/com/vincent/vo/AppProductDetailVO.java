package com.vincent.vo;

import lombok.Data;

import java.util.List;

/**
 * C 端商品详情（商品列表项 + 规格矩阵 + 加料 + 参数 + 评价摘要）
 */
@Data
public class AppProductDetailVO extends AppProductVO {
    /** 是否为规格矩阵商品；false 时为单一标准规格，前端不弹规格层 */
    private Boolean hasMatrix;
    private List<AppSkuVO> skus;
    /** 必选规格组；非矩阵商品为空数组 */
    private List<AppSpecGroupVO> specGroups;
    /** 可选加料组 */
    private AppAddonGroupVO addonGroup;
    private List<AppDetailItemVO> details;
    private List<AppReviewVO> reviews;
}
