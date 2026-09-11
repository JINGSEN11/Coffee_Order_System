package com.vincent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sku")
public class Sku {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String specsJson;
    private BigDecimal price;
    private Integer stock;
    private Integer warnStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}