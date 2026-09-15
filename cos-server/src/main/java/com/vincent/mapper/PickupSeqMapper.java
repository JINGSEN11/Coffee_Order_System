package com.vincent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vincent.entity.PickupSeq;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
 * 取餐码发号序列表 Mapper。
 * 联合主键（shop_id, biz_date）无法走 MyBatis-Plus 的通用方法，故发号链路使用自定义 SQL：
 * 先 UPSERT 自增，再读取当前水位，两步须在同一事务内（行锁保证并发下不重号）。
 */
@Mapper
public interface PickupSeqMapper extends BaseMapper<PickupSeq> {

    /**
     * 发号水位 +1：不存在则插入 1，存在则 current_no = current_no + 1
     */
    @Insert("INSERT INTO pickup_seq (shop_id, biz_date, current_no) VALUES (#{shopId}, #{bizDate}, 1) "
            + "ON DUPLICATE KEY UPDATE current_no = current_no + 1")
    int upsertIncrement(@Param("shopId") Long shopId, @Param("bizDate") LocalDate bizDate);

    /**
     * 读取当前发号水位（须紧跟在 upsertIncrement 之后、同一事务内调用）
     */
    @Select("SELECT current_no FROM pickup_seq WHERE shop_id = #{shopId} AND biz_date = #{bizDate}")
    Integer selectCurrentNo(@Param("shopId") Long shopId, @Param("bizDate") LocalDate bizDate);
}
