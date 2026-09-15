package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * SKU（specs_json 在契约中是对象，实体里是 JSON 字符串，此处转成 Map 输出）
 */
@Data
public class AppSkuVO {
    private Long id;
    private Long productId;
    private Map<String, Object> specsJson;
    private BigDecimal price;
    private Integer stock;
    private Integer warnStock;
}
