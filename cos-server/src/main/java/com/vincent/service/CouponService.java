package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.CouponCreateDTO;
import com.vincent.entity.Coupon;
import com.vincent.vo.CouponVO;
import com.vincent.vo.PageVO;
import com.vincent.vo.UserCouponVO;

import java.util.List;

public interface CouponService extends IService<Coupon> {

    PageVO<CouponVO> pageQuery(String name, Integer status, Integer page, Integer pageSize);

    CouponVO getCouponDetail(Long id);

    void createCoupon(CouponCreateDTO dto);

    void updateCoupon(Long id, CouponCreateDTO dto);

    void deleteCoupon(Long id);

    List<UserCouponVO> listIssuedUsers(Long couponId);

}