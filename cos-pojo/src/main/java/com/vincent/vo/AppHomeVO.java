package com.vincent.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 首页聚合（GET /api/app/home）
 */
@Data
public class AppHomeVO {
    /** 门店简要信息 {id, name, accept_order} */
    private Map<String, Object> shop;
    private List<AppBannerVO> banners;
    private List<AppNoticeVO> notices;
    private List<AppCategoryVO> categories;
    /** 按 sales 倒序取 6 条 */
    private List<AppProductVO> hot;
    /** tags 含「推荐」取 4 条 */
    private List<AppProductVO> recommend;
    /** 状态 0/1/2/3 的最近 2 条 */
    private List<AppOrderVO> ongoing;
    /** 会员摘要 {id, nickname, points, coupon_count} */
    private Map<String, Object> member;
}
