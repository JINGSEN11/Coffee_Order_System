package com.vincent.controller.user;

import com.vincent.common.BaseContext;
import com.vincent.common.Result;
import com.vincent.dto.AppPayPrepayDTO;
import com.vincent.service.AppPayService;
import com.vincent.vo.AppPayPrepayVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/app/pay")
@RequiredArgsConstructor
@Slf4j
public class AppPayController {

    private final AppPayService appPayService;

    /** 预下单（统一下单） */
    @PostMapping("/prepay")
    public Result<AppPayPrepayVO> prepay(@RequestBody AppPayPrepayDTO dto) {
        return Result.success(appPayService.prepay(BaseContext.getCurrentId(),
                dto == null ? null : dto.getOrderId()));
    }

    /**
     * 微信支付回调（免鉴权）。
     * 响应体非统一响应结构，按微信规范返回 {"code":"SUCCESS","message":"成功"}。
     */
    @PostMapping("/notify")
    public Map<String, String> notify(@RequestBody(required = false) String body) {
        log.info("收到微信支付回调");
        return appPayService.notify(body);
    }
}
