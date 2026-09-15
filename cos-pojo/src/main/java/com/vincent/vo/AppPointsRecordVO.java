package com.vincent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分流水条目
 */
@Data
public class AppPointsRecordVO {
    private Long id;
    private Integer changeValue;
    /** 1:消费获得 2:抵扣消耗 3:退款退回 4:管理员调整 */
    private Integer type;
    private String typeText;
    private Long orderId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
