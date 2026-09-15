package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 积分余额与流水（GET /api/app/points）
 */
@Data
public class AppPointsVO {
    private Integer points;
    /** 消费 1 元获得积分（sys_config.points_rate） */
    private Integer earnRate;
    /** 抵扣 1 元所需积分（sys_config.points_deduct_rate） */
    private Integer deductRate;
    /** 当前积分可抵扣的最大金额 */
    private BigDecimal maxDeductAmount;
    private List<AppPointsRecordVO> records;
}
