package com.vincent.service;

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
}
