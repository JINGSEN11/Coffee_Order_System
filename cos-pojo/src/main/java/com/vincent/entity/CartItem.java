package com.vincent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cart_item")
public class CartItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long skuId;
    private Integer qty;
    /** 加料明细，JSON 数组字符串 [{name,extra}]（见 sql/04_migrate_app_fields.sql） */
    private String addons;
    /** 加料签名 MD5(排序后 name:extra 以 | 拼接)，无加料为空串；参与唯一键 uk_user_sku_shop_addons */
    private String addonsHash;
    private Long shopId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}