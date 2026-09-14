package com.vincent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理端分页响应体。
 *
 * 字段名对齐《管理端-API联调用例》与 admin-web 的读取口径：
 * admin-web 统一使用 res.data.records / res.data.total / res.data.pageSize，
 * 因此这里不能改名成 list/size，否则管理端所有列表页都会取不到数据。
 * C 端（小程序）分页请使用各自的 AppXxxVO（如 AppCouponPageVO / AppOrderListVO）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVO<T> {
    private Long total;
    private Integer page;
    private Integer pageSize;
    private List<T> records;
}
