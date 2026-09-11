package com.vincent.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleCreateDTO {
    private String name;
    private String description;
    private Integer status;
    private List<Long> menuIds;
}