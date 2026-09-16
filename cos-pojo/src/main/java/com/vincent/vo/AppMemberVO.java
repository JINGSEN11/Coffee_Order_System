package com.vincent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会员信息（GET /api/app/member）
 */
@Data
public class AppMemberVO {
    private Long id;
    private String openid;
    private String nickname;
    private String avatar;
    private String phone;
    /** 生日 yyyy-MM-dd；未设置为 null */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
    /** 今天是不是该会员生日（会员日下单积分翻倍，前端据此提示） */
    private Boolean birthdayToday;
    /** 生日当天的积分倍数（sys_config.birthday_points_multiple，默认 2） */
    private Integer birthdayMultiple;
    private Integer points;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
    /** 有效订单数（排除已取消 4、支付失败 5） */
    private Long orderCount;
    /** 未使用且未过期的持券数 */
    private Long couponCount;
    /** 累计实付金额（状态 1/2/3/6/7） */
    private BigDecimal consumeTotal;
    private String level;
    /** 升级所需积分，已满级为 0 */
    private Integer nextLevelPoints;
}
