package com.vincent.controller.admin;

import com.vincent.common.Result;
import com.vincent.dto.ReviewApproveDTO;
import com.vincent.service.ReviewService;
import com.vincent.vo.PageVO;
import com.vincent.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("adminReviewController")
@RequestMapping("/admin/review")
@RequiredArgsConstructor
@Slf4j
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