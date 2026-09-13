package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.ReviewApproveDTO;
import com.vincent.entity.Review;
import com.vincent.vo.PageVO;
import com.vincent.vo.ReviewVO;

public interface ReviewService extends IService<Review> {

    PageVO<ReviewVO> pageQuery(Integer score, Integer status, Integer page, Integer pageSize);

    ReviewVO getReviewDetail(Long id);

    void approve(ReviewApproveDTO dto);
}