package com.vincent.controller.user;

import com.vincent.common.BaseContext;
import com.vincent.common.Result;
import com.vincent.dto.AppAiChatDTO;
import com.vincent.service.AppAiService;
import com.vincent.vo.AppAiChatVO;
import com.vincent.vo.AppAiGreetingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端 AI 助手（小卡）：推荐商品 + 引导领券。
 *
 * 需要登录：推荐要结合「你已经领过哪些券」来判断还能领什么，
 * 所以本组接口不放进 WebMvcConfig 的免鉴权白名单。
 */
@RestController
@RequestMapping("/api/app/ai")
@RequiredArgsConstructor
@Slf4j
public class AppAiController {

    private final AppAiService appAiService;

    /** 开场白：空态文案 + 建议问法 */
    @GetMapping("/greeting")
    public Result<AppAiGreetingVO> greeting(@RequestParam(value = "shop_id", required = false) Long shopId) {
        return Result.success(appAiService.greeting(BaseContext.getCurrentId(), shopId));
    }

    /** 单轮对话：回复 + 商品卡 + 券卡 + 建议追问 */
    @PostMapping("/chat")
    public Result<AppAiChatVO> chat(@RequestBody AppAiChatDTO dto) {
        return Result.success(appAiService.chat(BaseContext.getCurrentId(), dto));
    }
}
