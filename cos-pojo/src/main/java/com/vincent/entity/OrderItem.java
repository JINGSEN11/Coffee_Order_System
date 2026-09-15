package com.vincent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_item")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long skuId;
    /** 明细类型 1:主商品 2:加料（分装小料，独立扣库存、独立算销量） */
    private Integer itemType;
    /** 加料行指向的主商品明细 ID（order_item.id），便于出品时知道这份小料是给哪杯的 */
    private Long parentItemId;
    private String productName;
    private String specs;
    private BigDecimal price;
    private Integer qty;
    private LocalDateTime createdAt;
}
