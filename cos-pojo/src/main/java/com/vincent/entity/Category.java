package com.vincent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer sort;
    private Integer status;
    /** C 端菜单是否展示 1是 0否（加料分类为 0：它只作为饮品的可选项出现） */
    private Integer showInApp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}