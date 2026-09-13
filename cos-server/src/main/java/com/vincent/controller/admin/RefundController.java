package com.vincent.controller.admin;

import com.vincent.common.Result;
import com.vincent.dto.RefundQueryDTO;
import com.vincent.dto.RefundStatusDTO;
import com.vincent.service.RefundRecordService;
import com.vincent.vo.PageVO;
import com.vincent.vo.RefundRecordVO;
import com.vincent.vo.RefundStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("adminRefundController")
@RequestMapping("/admin/order/refund")
@RequiredArgsConstructor
@Slf4j
public class RefundController {

    private final RefundRecordService refundRecordService;

    /**
     * 退款记录分页查询
     */
    @GetMapping("/page")
    public Result<PageVO<RefundRecordVO>> page(RefundQueryDTO dto) {
        log.info("管理端分页查询退款记录：{}", dto);
        PageVO<RefundRecordVO> pageVO = refundRecordService.pageQuery(dto);
        return Result.success(pageVO);
    }

    /**
     * 退款统计概览
     */
    @GetMapping("/info")
    public Result<RefundRecordVO> info() {
        log.info("管理端查询退款概览");
        return Result.success(null);
    }

    /**
     * 退款记录详情
     */
    @GetMapping("/{id}")
    public Result<RefundRecordVO> detail(@PathVariable Long id) {
        log.info("管理端查询退款记录详情：id={}", id);
        return Result.success(refundRecordService.getDetail(id));
    }

    /**
     * 审核退款（通过/拒绝）
     */
    @PutMapping("/status/{id}")
    public Result<RefundStatusVO> updateStatus(@PathVariable Long id, @RequestBody RefundStatusDTO dto) {
        log.info("管理端审核退款：id={}, status={}, reason={}", id, dto.getStatus(), dto.getReason());
        return Result.success(refundRecordService.updateStatus(id, dto));
    }

}