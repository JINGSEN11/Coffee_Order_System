package com.vincent.dto;

import lombok.Data;

@Data
public class EmployeeCreateDTO {
    private String username;
    private String password;
    private String realName;
    private Long roleId;
    private Long shopId;
    private Integer status;
}