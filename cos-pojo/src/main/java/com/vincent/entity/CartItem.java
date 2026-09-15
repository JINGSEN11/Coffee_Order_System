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
    /**
     * 下单选项（温度/糖度等不参与价格矩阵的规格），JSON 字符串 {"温度":"冰","糖度":"半糖"}。
     * 它们不影响价格、不占库存，但必须区分购物车行：大杯/冰/半糖 与 大杯/热/全糖 是两行。
     */
    private String options;
    /** 选项签名 MD5(排序后 组:值 拼接)，无选项为空串；参与唯一键 uk_cart_line */
    private String optionsHash;
    /** 加料明细，JSON 数组字符串 [{skuId,qty}]，价格与名称下单时按加料 SKU 实时取 */
    private String addons;
    /** 加料签名 MD5(排序后 skuId:qty 拼接)，无加料为空串；参与唯一键 uk_cart_line */
    private String addonsHash;
    private Long shopId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
