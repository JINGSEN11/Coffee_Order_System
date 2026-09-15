package com.vincent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品 ⇄ 规格组关联：声明某商品带哪些规格组。
 * optionIds 为该商品在改组下可用的选项 ID 列表（JSON 数组字符串），
 * 为 null 表示「全组可用」——例：冷萃咖啡瓶只要冰，则只放冰的选项 ID。
 */
@Data
@TableName("product_spec_group")
public class ProductSpecGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long groupId;
    /** 可用选项 ID 列表的 JSON 字符串，如 "[3]"；null 表示全组可用 */
    private String optionIds;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
