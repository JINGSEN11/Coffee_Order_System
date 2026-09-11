package com.vincent.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MenuVO {
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String perms;
    private Integer type;
    private Integer sort;
    private Integer status;
    private LocalDateTime createdAt;
    private List<MenuVO> children;
}