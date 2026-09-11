package com.vincent.vo;

import lombok.Data;

@Data
public class EmployeeLoginVO {
    private Long id;
    private String username;
    private String realName;
    private String token;
    private Long roleId;
    private String roleName;
}