package com.vincent.controller.admin;

import com.vincent.common.Result;
import com.vincent.dto.NoticeCreateDTO;
import com.vincent.service.NoticeService;
import com.vincent.vo.NoticeVO;
import com.vincent.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("adminNoticeController")
@RequestMapping("/admin/notice")
@RequiredArgsConstructor
@Slf4j
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/page")
    public Result<PageVO<NoticeVO>> page(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("管理端分页查询公告：title={}, status={}, page={}", title, status, page);
        PageVO<NoticeVO> pageVO = noticeService.pageQuery(title, status, page, pageSize);
        return Result.success(pageVO);
    }

    @GetMapping("/{id}")
    public Result<NoticeVO> detail(@PathVariable Long id) {
        log.info("管理端查询公告详情：id={}", id);
        return Result.success(noticeService.getNoticeDetail(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody NoticeCreateDTO dto) {
        log.info("管理端新增公告：{}", dto);
        noticeService.createNotice(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody NoticeCreateDTO dto) {
        log.info("管理端修改公告：id={}, dto={}", id, dto);
        noticeService.updateNotice(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("管理端删除公告：id={}", id);
        noticeService.deleteNotice(id);
        return Result.success();
    }

}