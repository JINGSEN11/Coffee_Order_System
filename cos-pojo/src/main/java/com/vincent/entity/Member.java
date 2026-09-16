package com.vincent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("member")
public class Member {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private String nickname;
    private String avatar;
    private String phone;
    /** 生日，仅「月 + 日」参与生日双倍积分判定，年份不参与 */
    private LocalDate birthday;
    private Integer points;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}