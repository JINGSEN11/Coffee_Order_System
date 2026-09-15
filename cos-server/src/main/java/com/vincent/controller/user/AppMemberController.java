package com.vincent.controller.user;

import com.vincent.common.BaseContext;
import com.vincent.common.Result;
import com.vincent.service.AppMemberService;
import com.vincent.vo.AppPointsVO;
import com.vincent.vo.AppMemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
@Slf4j
public class AppMemberController {

    private final AppMemberService appMemberService;

    /** 积分余额与流水 */
    @GetMapping("/points")
    public Result<AppPointsVO> points() {
        return Result.success(appMemberService.points(BaseContext.getCurrentId()));
    }

    /** 会员信息 */
    @GetMapping("/member")
    public Result<AppMemberVO> member() {
        return Result.success(appMemberService.memberInfo(BaseContext.getCurrentId()));
    }
}
