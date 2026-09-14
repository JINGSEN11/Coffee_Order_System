package com.vincent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vincent.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 领券时原子占用一个发放名额：仅当不限量（total_count = 0）或仍有剩余时才自增。
     * 返回受影响行数，0 表示已被领完，用于并发下的兜底。
     */
    @Update("UPDATE coupon SET received_count = received_count + 1 "
            + "WHERE id = #{id} AND status = 1 AND (total_count = 0 OR received_count < total_count)")
    int incrementReceivedIfAvailable(@Param("id") Long id);
}
