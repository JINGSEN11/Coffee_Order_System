package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.ReviewApproveDTO;
import com.vincent.entity.Member;
import com.vincent.entity.Review;
import com.vincent.mapper.MemberMapper;
import com.vincent.mapper.ReviewMapper;
import com.vincent.service.ReviewService;
import com.vincent.vo.PageVO;
import com.vincent.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final MemberMapper memberMapper;

    @Override
    public PageVO<ReviewVO> pageQuery(Integer score, Integer status, Integer pageNum, Integer pageSize) {
        Page<Review> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>()
                .eq(score != null, Review::getScore, score)
                .eq(status != null, Review::getStatus, status)
                .orderByDesc(Review::getCreatedAt);

        Page<Review> result = page(page, wrapper);

        List<ReviewVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageVO<>(result.getTotal(), pageNum, pageSize, voList);
    }

    @Override
    public ReviewVO getReviewDetail(Long id) {
        Review review = getById(id);
        if (review == null) throw new ServiceException("评论不存在");
        return convertToVO(review);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(ReviewApproveDTO dto) {
        Review review = getById(dto.getId());
        if (review == null) throw new ServiceException("评论不存在");
        review.setStatus(dto.getStatus());
        updateById(review);
    }

    private ReviewVO convertToVO(Review review) {
        ReviewVO vo = new ReviewVO();
        BeanUtils.copyProperties(review, vo);

        // 填充用户信息
        Member member = memberMapper.selectById(review.getUserId());
        if (member != null) {
            vo.setUserNickname(member.getNickname());
            vo.setUserAvatar(member.getAvatar());
        }

        return vo;
    }

}