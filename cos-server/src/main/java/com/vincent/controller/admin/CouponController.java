package com.vincent.controller.admin;

import com.vincent.annotation.OpLog;
import com.vincent.annotation.RequirePerm;
import com.vincent.common.Result;
import com.vincent.dto.CouponCreateDTO;
import com.vincent.service.CouponService;
import com.vincent.vo.CouponVO;
import com.vincent.vo.PageVO;
import com.vincent.vo.UserCouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminCouponController")
@RequestMapping("/admin/coupon")
@OpLog(module = "优惠券")
@RequiredArgsConstructor
@Slf4j
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/list")
    @RequirePerm("marketing:coupon")
    public Result<List<CouponVO>> list() {
        return Result.success(couponService.listAll());
    }

    @GetMapping("/page")
    @RequirePerm("marketing:coupon")
    public Result<PageVO<CouponVO>> page(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("管理端分页查询优惠券：name={}, status={}, page={}", name, status, page);
        PageVO<CouponVO> pageVO = couponService.pageQuery(name, status, page, pageSize);
        return Result.success(pageVO);
    }

    @GetMapping("/{id}")
    @RequirePerm("marketing:coupon")
    public Result<CouponVO> detail(@PathVariable Long id) {
        log.info("管理端查询优惠券详情：id={}", id);
        return Result.success(couponService.getCouponDetail(id));
    }

    @PostMapping
    @RequirePerm("marketing:coupon:edit")
    public Result<Void> create(@RequestBody CouponCreateDTO dto) {
        log.info("管理端新增优惠券：{}", dto);
        couponService.createCoupon(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequirePerm("marketing:coupon:edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody CouponCreateDTO dto) {
        log.info("管理端修改优惠券：id={}, dto={}", id, dto);
        couponService.updateCoupon(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("marketing:coupon:edit")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("管理端删除优惠券：id={}", id);
        couponService.deleteCoupon(id);
        return Result.success();
    }

    @GetMapping("/{id}/issued-users")
    @RequirePerm("marketing:coupon")
    public Result<List<UserCouponVO>> issuedUsers(@PathVariable Long id) {
        log.info("管理端查看优惠券领取用户：couponId={}", id);
        return Result.success(couponService.listIssuedUsers(id));
    }

}