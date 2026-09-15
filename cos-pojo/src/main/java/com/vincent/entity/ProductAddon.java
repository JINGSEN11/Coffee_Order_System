package com.vincent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 ⇄ 加料商品关联：声明「哪些饮品可以加哪些料」。
 * 加料本身是一个真实商品（有自己的分类/价格/库存，可单独售卖），
 * 此表只负责把它挂到饮品上。
 */
@Data
@TableName("product_addon")
public class ProductAddon {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 主商品（饮品）ID */
    private Long productId;
    /** 加料商品 ID（分装小料） */
    private Long addonProductId;
    /** 加料价覆盖值，null 表示用加料商品自身售价 */
    private BigDecimal price;
    /** 单杯最多加几份 */
    private Integer maxQty;
    private Integer sort;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
