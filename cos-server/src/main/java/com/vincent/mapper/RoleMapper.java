package com.vincent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vincent.entity.Role;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}