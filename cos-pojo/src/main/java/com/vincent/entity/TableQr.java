package com.vincent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("table_qr")
public class TableQr {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shopId;
    private String tableNo;
    private String qrUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}