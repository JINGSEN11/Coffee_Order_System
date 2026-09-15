package com.vincent.controller.admin;

import com.vincent.annotation.RequirePerm;
import com.vincent.common.Result;
import com.vincent.dto.ReviewApproveDTO;
import com.vincent.service.ReviewService;
import com.vincent.vo.PageVO;
import com.vincent.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 评价审核。
 * menu 表里没有评价管理节点（该模块也没有管理端页面），
 * 因此没有权限码可挂，只要求是已登录的管理员。
 */
@RestController("adminReviewController")
@RequestMapping("/admin/review")
@RequiredArgsConstructor
@Slf4j
@RequirePerm
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/page")
    public Result<PageVO<ReviewVO>> page(
            @RequestParam(required = false) Integer score,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(reviewService.pageQuery(score, status, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<ReviewVO> detail(@PathVariable Long id) {
        return Result.success(reviewService.getReviewDetail(id));
    }

    @PutMapping("/approve")
    public Result<Void> approve(@RequestBody ReviewApproveDTO dto) {
        reviewService.approve(dto);
        return Result.success();
    }

}