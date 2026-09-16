package com.vincent.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * C 端修改会员资料入参（PUT /api/app/member）。
 *
 * 只开放这三项：昵称、头像、生日。
 * 手机号走独立的绑定流程（/auth/bind-phone，需微信授权码），不能在这里直接改，
 * 否则等于允许随便填一个别人的号码。
 */
@Data
public class AppMemberUpdateDTO {

    private String nickname;

    private String avatar;

    /**
     * 生日 yyyy-MM-dd；传空字符串或 null 表示清除。
     * 只用到月日，年份不参与生日判定。
     */
    private LocalDate birthday;

}
