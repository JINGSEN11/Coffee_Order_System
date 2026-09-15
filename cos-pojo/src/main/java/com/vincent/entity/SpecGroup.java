package com.vincent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 规格组（字典）：杯型 / 温度 / 糖度。
 * is_price_dim = 1 的组参与 SKU 价格矩阵（每个组合一个 SKU，各自有价格与库存）；
 * is_price_dim = 0 的组只作为下单选项，不影响价格与库存。
 * 见 sql/06_migrate_spec_addon.sql。
 */
@Data
@TableName("spec_group")
public class SpecGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 组标识 cup/temp/sugar（列名 group_key，避开 MySQL 保留字 key） */
    private String groupKey;
    private String name;
    /** 是否必选 1是 0否 */
    private Integer required;
    /** 是否多选 1是 0否 */
    private Integer multi;
    /** 是否参与 SKU 价格矩阵 1是 0否 */
    private Integer isPriceDim;
    private Integer sort;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
