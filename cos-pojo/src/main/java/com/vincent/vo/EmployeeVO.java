package com.vincent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmployeeVO {
    private Long id;
    private String username;
    private String realName;
    private Long roleId;
    private String roleName;
    private Long shopId;
    private String shopName;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}