package com.vincent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("role_menu")
public class RoleMenu {
    @TableId(type = IdType.INPUT)
    private Long roleId;
    private Long menuId;
}