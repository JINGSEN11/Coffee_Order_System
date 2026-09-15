package com.vincent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vincent.entity.OpLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OpLogMapper extends BaseMapper<OpLog> {
}