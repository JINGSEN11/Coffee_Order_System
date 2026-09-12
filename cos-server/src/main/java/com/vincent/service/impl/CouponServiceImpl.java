package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.CouponCreateDTO;
import com.vincent.entity.Coupon;
import com.vincent.entity.UserCoupon;
import com.vincent.mapper.CouponMapper;
import com.vincent.mapper.UserCouponMapper;
import com.vincent.service.CouponService;
import com.vincent.vo.CouponVO;
import com.vincent.vo.PageVO;
import com.vincent.vo.UserCouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    public PageVO<CouponVO> pageQuery(String name, Integer status, Integer pageNum, Integer pageSize) {
        Page<Coupon> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<Coupon>()
                .like(StringUtils.hasText(name), Coupon::getName, name)
                .eq(status != null, Coupon::getStatus, status)
                .orderByDesc(Coupon::getCreatedAt);

        Page<Coupon> result = page(page, wrapper);

        List<CouponVO> voList = result.getRecords().stream().map(coupon -> {
            CouponVO vo = new CouponVO();
            BeanUtils.copyProperties(coupon, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageVO<>(result.getTotal(), pageNum, pageSize, voList);
    }

    @Override
    public CouponVO getCouponDetail(Long id) {
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new ServiceException("优惠券不存在");
        }
        CouponVO vo = new CouponVO();
        BeanUtils.copyProperties(coupon, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCoupon(CouponCreateDTO dto) {
        Coupon coupon = new Coupon();
        BeanUtils.copyProperties(dto, coupon);
        coupon.setReceivedCount(0);
        coupon.setCreatedAt(LocalDateTime.now());
        coupon.setUpdatedAt(LocalDateTime.now());
        save(coupon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCoupon(Long id, CouponCreateDTO dto) {
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new ServiceException("优惠券不存在");
        }
        BeanUtils.copyProperties(dto, coupon);
        coupon.setId(id);
        coupon.setUpdatedAt(LocalDateTime.now());
        updateById(coupon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCoupon(Long id) {
        if (getById(id) == null) {
            throw new ServiceException("优惠券不存在");
        }
        removeById(id);
    }

    @Override
    public List<UserCouponVO> listIssuedUsers(Long couponId) {
        List<UserCoupon> list = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getCouponId, couponId)
                        .orderByDesc(UserCoupon::getClaimedAt)
        );

        return list.stream().map(uc -> {
            UserCouponVO vo = new UserCouponVO();
            BeanUtils.copyProperties(uc, vo);
            return vo;
        }).collect(Collectors.toList());
    }

}