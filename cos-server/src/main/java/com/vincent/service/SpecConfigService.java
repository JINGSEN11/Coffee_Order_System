package com.vincent.service;

import com.vincent.dto.SpecConfigDTO;
import com.vincent.vo.SpecGroupAdminVO;

import java.util.List;

/**
 * 规格组字典与「商品 ⇄ 规格组 / 加料」配置（管理端）
 */
public interface SpecConfigService {

    /** 全部规格组（含选项），供管理端渲染配置面板 */
    List<SpecGroupAdminVO> groups();

    /** 某商品的规格与加料配置（optionIds 会展开成全选，便于前端回显） */
    SpecConfigDTO configOf(Long productId);

    /**
     * 保存某商品的规格与加料配置。
     * 顺带补齐「价格维度组」缺失的 SKU（按现有最低价 + 选项加价生成），
     * 但**绝不删除**已有 SKU —— 历史 order_item.sku_id 指向它们。
     */
    void saveConfig(Long productId, SpecConfigDTO dto);
}
