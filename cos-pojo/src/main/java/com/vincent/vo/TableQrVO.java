package com.vincent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TableQrVO {
    private Long id;
    private Long shopId;
    private String shopName;
    private String tableNo;
    private String area;
    private Integer seats;
    private Integer status;
    private String qrUrl;
    private LocalDateTime createdAt;
}