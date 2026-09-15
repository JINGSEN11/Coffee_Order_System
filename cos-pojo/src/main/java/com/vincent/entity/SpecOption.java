package com.vincent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 规格选项（字典）：中杯/大杯、热/冰/去冰、无糖/半糖/全糖。
 * extra 只对 is_price_dim = 1 的组有意义（温度/糖度加价恒为 0）。
 */
@Data
@TableName("spec_option")
public class SpecOption {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private String name;
    /** 加价，仅价格维度组有意义 */
    private BigDecimal extra;
    private Integer sort;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
