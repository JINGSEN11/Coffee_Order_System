package com.vincent.dto;

import lombok.Data;

@Data
public class NoticeCreateDTO {
    private String title;
    private String content;
    private Integer status;
}