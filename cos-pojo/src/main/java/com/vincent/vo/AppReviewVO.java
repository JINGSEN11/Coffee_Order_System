package com.vincent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价（订单详情 review 字段 / 商品详情 reviews 列表）
 */
@Data
public class AppReviewVO {
    private Long id;
    private Integer score;
    private String content;
    private List<String> images;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
