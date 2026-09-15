package com.vincent.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 订单列表响应（GET /api/app/orders）
 */
@Data
public class AppOrderListVO {
    private Long total;
    /** 当前用户全部订单按状态计数（不受 status 筛选影响），键为 "0".."7" */
    private Map<String, Long> counts;
    private List<AppOrderVO> list;
}
