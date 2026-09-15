package com.vincent.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 商品的规格与加料配置（管理端 PUT /admin/product/{id}/spec-config）
 */
@Data
public class SpecConfigDTO {
    /** 该商品启用的规格组 ID（杯型 / 温度 / 糖度） */
    private List<Long> groupIds;
    /**
     * groupId → 该商品可用的选项 ID 列表。
     * 不传或等于该组全部选项时视为「全组可用」，落库为 option_ids = NULL。
     */
    private Map<Long, List<Long>> optionIds;
    /** 该商品可以加的加料商品 ID（加料是独立分装商品，category.show_in_app=0） */
    private List<Long> addonProductIds;
}
