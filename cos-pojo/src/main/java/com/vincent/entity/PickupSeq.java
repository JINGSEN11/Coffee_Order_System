package com.vincent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 取餐码发号序列表（pickup_seq）。
 * 主键为 shop_id + biz_date 联合主键，MyBatis-Plus 不支持联合主键，
 * 此处沿用项目内 RoleMenu 的写法：@TableId(type = IdType.INPUT) 标注其中一个列，
 * 因此**不要**使用 updateById / deleteById，发号与读取统一走 PickupSeqMapper 的自定义 SQL。
 */
@Data
@TableName("pickup_seq")
public class PickupSeq {
    @TableId(type = IdType.INPUT)
    private Long shopId;
    private LocalDate bizDate;
    /** 当前已发放到的取餐码序号 */
    private Integer currentNo;
    private LocalDateTime updatedAt;
}
