package com.vincent.service;

import com.vincent.dto.AppMemberUpdateDTO;
import com.vincent.vo.AppMemberVO;
import com.vincent.vo.AppPointsVO;

/**
 * C 端会员与积分服务
 */
public interface AppMemberService {

    /** 会员信息（含订单数 / 券数 / 累计消费 / 等级派生值） */
    AppMemberVO memberInfo(Long userId);

    /** 积分余额与流水 */
    AppPointsVO points(Long userId);

    /**
     * 修改本人资料（昵称 / 头像 / 生日），返回更新后的会员信息。
     * 生日用于生日双倍积分判定，只取月日。
     */
    AppMemberVO updateProfile(Long userId, AppMemberUpdateDTO dto);
}
