package com.vincent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 领券中心 / 我的券响应（GET /api/app/coupons）
 */
@Data
public class AppCouponPageVO {
    private String tab;
    private List<AppCouponVO> list;
    /** 仅领券中心返回：当前用户已领取过的券模板 ID */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Long> claimedIds;
}
