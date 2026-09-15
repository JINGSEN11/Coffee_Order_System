package com.vincent.controller.admin;

import com.vincent.annotation.RequirePerm;
import com.vincent.common.Result;
import com.vincent.dto.OpLogQueryDTO;
import com.vincent.service.OpLogService;
import com.vincent.vo.OpLogVO;
import com.vincent.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("adminLogController")
@RequestMapping("/admin/log")
@RequiredArgsConstructor
@Slf4j
public class LogController {

    private final OpLogService opLogService;

    @GetMapping("/page")
    @RequirePerm("system:log")
    public Result<PageVO<OpLogVO>> page(OpLogQueryDTO dto) {
        log.info("\u7BA1\u7406\u7AEF\u5206\u9875\u67E5\u8BE2\u64CD\u4F5C\u65E5\u5FD7\uFF1A{}", dto);
        return Result.success(opLogService.pageQuery(dto));
    }
}