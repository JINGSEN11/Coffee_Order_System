# 咖啡订购系统 - 管理端 API 联调用例

> **基准 URL**：`http://localhost:8080/admin`
>
> **认证方式**：除登录/刷新外，所有接口需在 Header 携带 `Authorization: Bearer <token>`
>
> **统一响应格式**：`{ code: 0, message: "操作成功", data: ... }`，code=0 成功，非0失败

---

## 目录

1. [Auth 认证模块](#1-auth-认证模块)
2. [Dashboard 数据看板](#2-dashboard-数据看板)
3. [Shop 门店管理](#3-shop-门店管理)
4. [Banner 轮播图管理](#4-banner-轮播图管理)
5. [Category 分类管理](#5-category-分类管理)
6. [Config 系统配置](#6-config-系统配置)
7. [Coupon 优惠券管理](#7-coupon-优惠券管理)
8. [Employee 员工管理](#8-employee-员工管理)
9. [Log 操作日志](#9-log-操作日志)
10. [Member 会员管理](#10-member-会员管理)
11. [Menu 菜单管理](#11-menu-菜单管理)
12. [Notice 公告管理](#12-notice-公告管理)
13. [Order 订单管理](#13-order-订单管理)
14. [Points 积分流水](#14-points-积分流水)
15. [Product 商品管理](#15-product-商品管理)
16. [Refund 退款管理](#16-refund-退款管理)
17. [Review 评价管理](#17-review-评价管理)
18. [Role 角色管理](#18-role-角色管理)
19. [Sku SKU 管理](#19-sku-sku-管理)
20. [Stock 库存管理](#20-stock-库存管理)
21. [TableQr 桌码管理](#21-tableqr-桌码管理)

---

## 1. Auth 认证模块

### 1.1 员工登录

- **URL**：`POST /auth/login`
- **免鉴权**：是
- **请求体**：

```json
{
    "username": "admin",
    "password": "123456"
}
```

- **成功响应示例**：

```json
{
    "code": 0,
    "message": "操作成功",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiIs...",
        "userInfo": {
            "id": 1,
            "username": "admin",
            "realName": "管理员",
            "avatar": null
        }
    }
}
```

### 1.2 刷新 Token

- **URL**：`POST /auth/refresh`
- **免鉴权**：是
- **请求体**：

```json
{
    "username": "admin"
}
```

- **成功响应**：

```json
{
    "code": 0,
    "message": "操作成功",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiIs..."
    }
}
```

### 1.3 获取菜单与按钮权限

- **URL**：`GET /auth/menus`
- **鉴权**：是
- **成功响应**：

```json
{
    "code": 0,
    "message": "操作成功",
    "data": {
        "menus": [
            {
                "id": 1,
                "name": "工作台",
                "path": "/dashboard",
                "component": "dashboard/index",
                "icon": "Odometer",
                "children": [],
                "buttons": ["view"]
            }
        ]
    }
}
```

---

## 2. Dashboard 数据看板

### 2.1 看板统计

- **URL**：`GET /dashboard/stats`

- **成功响应**：

```json
{
    "code": 0,
    "data": {
        "todayOrderCount": 128,
        "todayOrderAmount": 4567.50,
        "todayMemberCount": 15,
        "pendingOrderCount": 8,
        "refundCount": 2,
        "lowStockCount": 5
    }
}
```

### 2.2 数据趋势

- **URL**：`GET /dashboard/trend?startDate=2026-09-07&endDate=2026-09-14`
- **查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| startDate | string | 否 | 开始日期，默认7天前 |
| endDate | string | 否 | 结束日期，默认今天 |

- **成功响应**：

```json
{
    "code": 0,
    "data": [
        { "date": "2026-09-07", "orderCount": 98, "orderAmount": 3200.00 },
        { "date": "2026-09-08", "orderCount": 112, "orderAmount": 3890.50 }
    ]
}
```

### 2.3 告警列表

- **URL**：`GET /dashboard/alerts`

- **成功响应**：

```json
{
    "code": 0,
    "data": [
        { "type": "low_stock", "message": "库存不足：拿铁咖啡", "level": "warning" },
        { "type": "refund", "message": "待处理退款：订单 NO20260914001", "level": "danger" }
    ]
}
```

### 2.4 总览数据

- **URL**：`GET /dashboard/overview`

- **成功响应**：

```json
{
    "code": 0,
    "data": {
        "totalOrders": 15820,
        "totalRevenue": 526800.00,
        "totalMembers": 3240,
        "totalProducts": 86
    }
}
```

### 2.5 图表数据

- **URL**：`GET /dashboard/charts`

- **成功响应**：

```json
{
    "code": 0,
    "data": {
        "categoryStats": [
            { "name": "咖啡", "value": 45 },
            { "name": "茶饮", "value": 30 }
        ],
        "chartItems": [
            { "date": "09-07", "amount": 3200 },
            { "date": "09-08", "amount": 3890 }
        ]
    }
}
```

---

## 3. Shop 门店管理

### 3.1 分页查询门店

- **URL**：`GET /shop/page?name=&status=&page=1&pageSize=10`
- **查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 否 | 门店名称模糊搜索 |
| status | int | 否 | 状态 0=禁用 1=启用 |
| page | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页条数，默认10 |

- **成功响应**：

```json
{
    "code": 0,
    "data": {
        "records": [
            {
                "id": 1,
                "name": "咖啡工坊（中山路店）",
                "address": "中山路100号",
                "phone": "0592-1234567",
                "businessHours": "08:00-22:00",
                "acceptOrder": 1,
                "status": 1
            }
        ],
        "total": 5,
        "page": 1,
        "pageSize": 10
    }
}
```

### 3.2 查询门店信息

- **URL**：`GET /shop/info`

### 3.3 更新门店信息

- **URL**：`PUT /shop/info`
- **请求体**：

```json
{
    "name": "咖啡工坊（中山路店）",
    "address": "中山路100号",
    "phone": "0592-1234567",
    "businessHours": "08:00-22:00",
    "acceptOrder": 1,
    "status": 1
}
```

### 3.4 门店详情

- **URL**：`GET /shop/{id}`
- **响应**：

```json
{
    "code": 0,
    "data": {
        "id": 1,
        "name": "咖啡工坊（中山路店）",
        "address": "中山路100号",
        "phone": "0592-1234567",
        "businessHours": "08:00-22:00",
        "acceptOrder": 1,
        "status": 1
    }
}
```

### 3.5 新增门店

- **URL**：`POST /shop`

```json
{
    "name": "咖啡工坊（集美店）",
    "address": "集美区银江路50号",
    "phone": "0592-7654321",
    "businessHours": "08:00-22:00",
    "acceptOrder": 1,
    "status": 1
}
```

### 3.6 更新门店

- **URL**：`PUT /shop/{id}`（请求体同新增）

### 3.7 删除门店

- **URL**：`DELETE /shop/{id}`

### 3.8 切换接单状态

- **URL**：`PUT /shop/{id}/accept-order?acceptOrder=0`
- **参数**：`acceptOrder` 0=停止接单 1=开始接单

---

## 4. Banner 轮播图管理

### 4.1 查询 Banner 列表

- **URL**：`GET /banner/list`

### 4.2 新增 Banner

- **URL**：`POST /banner`

```json
{
    "imageUrl": "https://example.com/banner1.jpg",
    "linkUrl": "https://example.com/promo",
    "sort": 1,
    "status": 1
}
```

### 4.3 修改 Banner

- **URL**：`PUT /banner/{id}`（请求体同新增）

### 4.4 删除 Banner

- **URL**：`DELETE /banner/{id}`

### 4.5 切换 Banner 状态

- **URL**：`PUT /banner/{id}/status?status=0`
- **参数**：`status` 0=禁用 1=启用

---

## 5. Category 分类管理

### 5.1 查询分类列表

- **URL**：`GET /category/list`

### 5.2 新增分类

- **URL**：`POST /category`

```json
{
    "name": "手冲咖啡",
    "sort": 1,
    "status": 1
}
```

### 5.3 修改分类

- **URL**：`PUT /category/{id}`（请求体同新增）

### 5.4 删除分类

- **URL**：`DELETE /category/{id}`

---

## 6. Config 系统配置

### 6.1 查询配置列表

- **URL**：`GET /config/list`

### 6.2 配置详情

- **URL**：`GET /config/{id}`

### 6.3 新增配置

- **URL**：`POST /config`

```json
{
    "configKey": "store_notice",
    "configValue": "欢迎光临",
    "description": "门店公告"
}
```

### 6.4 修改配置

- **URL**：`PUT /config/{id}`（请求体同新增）

### 6.5 删除配置

- **URL**：`DELETE /config/{id}`

---

## 7. Coupon 优惠券管理

### 7.1 优惠券列表

- **URL**：`GET /coupon/list`

### 7.2 优惠券分页

- **URL**：`GET /coupon/page?name=&status=&page=1&pageSize=10`
- **参数**：name(模糊), status(0/1), page, pageSize

### 7.3 优惠券详情

- **URL**：`GET /coupon/{id}`

### 7.4 新增优惠券

- **URL**：`POST /coupon`

```json
{
    "name": "新人专享5折券",
    "type": 2,
    "thresholdAmount": 0,
    "discountAmount": null,
    "discountRate": 0.5,
    "totalCount": 1000,
    "perUserLimit": 1,
    "validStartTime": "2026-09-14T00:00:00",
    "validEndTime": "2026-10-14T23:59:59",
    "status": 1
}
```

> **type 说明**：1=满减券 2=折扣券

### 7.5 修改优惠券

- **URL**：`PUT /coupon/{id}`（请求体同新增）

### 7.6 删除优惠券

- **URL**：`DELETE /coupon/{id}`

### 7.7 查看优惠券领取用户

- **URL**：`GET /coupon/{id}/issued-users`

---

## 8. Employee 员工管理

### 8.1 员工列表

- **URL**：`GET /employee/list`

### 8.2 员工分页

- **URL**：`GET /employee/page?keyword=&roleId=&shopId=&page=1&pageSize=10`
- **参数**：keyword, roleId, shopId, page, pageSize

### 8.3 当前登录员工信息

- **URL**：`GET /employee/current`

### 8.4 员工详情

- **URL**：`GET /employee/{id}`

### 8.5 新增员工

- **URL**：`POST /employee`

```json
{
    "username": "zhangsan",
    "password": "123456",
    "realName": "张三",
    "roleId": 2,
    "shopId": 1,
    "status": 1
}
```

### 8.6 修改员工

- **URL**：`PUT /employee/{id}`（请求体同新增）

### 8.7 删除员工

- **URL**：`DELETE /employee/{id}`

### 8.8 切换员工状态

- **URL**：`PUT /employee/{id}/status?status=0`

### 8.9 重置密码

- **URL**：`PUT /employee/{id}/reset-password?newPassword=654321`

---

## 9. Log 操作日志

### 9.1 分页查询操作日志

- **URL**：`GET /log/page?operator=&module=&startDate=&endDate=&page=1&pageSize=10`
- **参数**：operator, module, startDate, endDate, page, pageSize

- **成功响应**：

```json
{
    "code": 0,
    "data": {
        "records": [
            {
                "id": 1,
                "operator": "admin",
                "module": "商品管理",
                "action": "新增商品",
                "detail": "新增商品：经典拿铁",
                "ip": "127.0.0.1",
                "createTime": "2026-09-14 10:30:00"
            }
        ],
        "total": 1,
        "page": 1,
        "pageSize": 10
    }
}
```

---

## 10. Member 会员管理

### 10.1 会员列表

- **URL**：`GET /member/list`

### 10.2 会员分页

- **URL**：`GET /member/page?keyword=&status=&page=1&pageSize=10`
- **参数**：keyword, status(0/1), page, pageSize

### 10.3 当前会员信息

- **URL**：`GET /member/current`

### 10.4 会员详情

- **URL**：`GET /member/{id}`

### 10.5 修改会员

- **URL**：`PUT /member`

```json
{
    "id": 1,
    "nickname": "咖啡爱好者",
    "avatar": "https://example.com/avatar.jpg",
    "phone": "13800138000"
}
```

### 10.6 切换会员状态

- **URL**：`PUT /member/{id}/status?status=0`

---

## 11. Menu 菜单管理

### 11.1 获取菜单树

- **URL**：`GET /menu/tree`

### 11.2 新增菜单

- **URL**：`POST /menu`

```json
{
    "parentId": 0,
    "name": "订单管理",
    "path": "/order",
    "component": "order/index",
    "icon": "ShoppingCart",
    "sort": 1,
    "type": 2,
    "permission": null,
    "status": 1
}
```

> **type 说明**：1=目录 2=菜单 3=按钮

### 11.3 修改菜单

- **URL**：`PUT /menu`（请求体同新增）

### 11.4 删除菜单

- **URL**：`DELETE /menu/{id}`

---

## 12. Notice 公告管理

### 12.1 公告列表

- **URL**：`GET /notice/list`

### 12.2 公告分页

- **URL**：`GET /notice/page?title=&status=&page=1&pageSize=10`
- **参数**：title, status(0=草稿/1=发布), page, pageSize

### 12.3 公告详情

- **URL**：`GET /notice/{id}`

### 12.4 新增公告

- **URL**：`POST /notice`

```json
{
    "title": "国庆节营业通知",
    "content": "国庆期间正常营业，欢迎光临！",
    "status": 1
}
```

### 12.5 修改公告

- **URL**：`PUT /notice/{id}`（请求体同新增）

### 12.6 删除公告

- **URL**：`DELETE /notice/{id}`

---

## 13. Order 订单管理

### 13.1 订单看板

- **URL**：`GET /order/board`

### 13.2 订单池

- **URL**：`GET /order/pool`

### 13.3 订单详情

- **URL**：`GET /order/details/{id}`

### 13.4 订单分页

- **URL**：`GET /order/page?orderNo=&userId=&shopId=&status=&type=&startDate=&endDate=&page=1&pageSize=10`
- **参数**：orderNo, userId, shopId, status, type, startDate, endDate, page, pageSize

### 13.5 更新订单状态

- **URL**：`PUT /order/{id}/status?status=3`
- **参数**：status（目标状态码）

### 13.6 取消订单

- **URL**：`DELETE /order/{id}?reason=顾客要求取消`

---

## 14. Points 积分流水

### 14.1 分页查询积分流水

- **URL**：`GET /points/page?userId=&userNickname=&type=&startDate=&endDate=&page=1&pageSize=10`
- **参数**：userId, userNickname, type(1=获得/2=使用), startDate, endDate, page, pageSize

---

## 15. Product 商品管理

### 15.1 商品列表

- **URL**：`GET /product/list`

### 15.2 商品分页

- **URL**：`GET /product/page?name=&categoryId=&status=&page=1&pageSize=10`
- **参数**：name, categoryId, status(0=下架/1=上架), page, pageSize

### 15.3 商品详情

- **URL**：`GET /product/{id}`

### 15.4 新增商品

- **URL**：`POST /product`

```json
{
    "categoryId": 1,
    "name": "经典拿铁",
    "description": "浓缩咖啡与丝滑牛奶的完美融合",
    "image": "https://example.com/latte.jpg",
    "tags": "热销,经典",
    "status": 1
}
```

### 15.5 修改商品

- **URL**：`PUT /product/{id}`（请求体同新增）

### 15.6 删除商品

- **URL**：`DELETE /product/{id}`

### 15.7 批量创建 SKU

- **URL**：`POST /product/{id}/sku`

```json
[
    {
        "specsJson": "{\"size\":\"大杯\",\"temperature\":\"冰\"}",
        "price": 28.00,
        "stock": 100,
        "warnStock": 10
    },
    {
        "specsJson": "{\"size\":\"中杯\",\"temperature\":\"热\"}",
        "price": 24.00,
        "stock": 100,
        "warnStock": 10
    }
]
```

### 15.8 批量更新 SKU

- **URL**：`PUT /product/{id}/sku`（请求体同批量创建）

---

## 16. Refund 退款管理

### 16.1 退款记录分页

- **URL**：`GET /order/refund/page?orderNo=&status=&startDate=&endDate=&page=1&pageSize=10`
- **参数**：`orderNo` 匹配的是**订单号**（不是退款单号）；`status` 0=待处理/1=已退款/2=已拒绝
- **说明**：返回的每条记录都带 `orderNo`，可直接定位到订单

### 16.2 退款概览

- **URL**：`GET /order/refund/info`

### 16.3 退款记录详情

- **URL**：`GET /order/refund/{id}`

### 16.4 审核退款

- **URL**：`PUT /order/refund/status/{id}`
- **权限**：`order:refund:audit`

**通过**

```json
{
    "status": "approved",
    "received": true,
    "refundNo": "4200001234202609161234567890"
}
```

**拒绝**

```json
{
    "status": "rejected",
    "reason": "已开始制作，无法退款"
}
```

> **status 取值**：`approved`=通过, `rejected`=拒绝
>
> **`received` 必填且必须为 true**：当前微信支付是沙箱直通模式，后端发不出真实的退款请求，
> 钱需要商家在微信商户平台操作。不默认放行是为了避免「点一下通过、订单标成已退款、但钱没退」
> 这种和商户后台对不上账的情况。
> 若商户后台显示退款成功但这里没传 `received`，接口会返回业务异常。
>
> **`refundNo` 可选**：填微信退款单号便于对账；不传则按 `RF + yyyyMMdd + 5 位 id` 生成本地单号。
>
> **通过后的连锁动作**：订单置 `status=7 已退款`、`pickup_status=5 已作废`；
> 归还库存、退回抵扣积分（写 `points_record` type=3）、释放优惠券。
> 订单必须处于 `status=6 退款中` 才允许通过，否则报业务异常（防重复审核）。
>
> **拒绝后的连锁动作**：订单回到申请前的状态（出过餐回 `3 已完成`，否则回 `2 制作中`），
> 原因写入 `cancel_reason` 并追加到退款流水的 `reason` 里；**不回退**库存与积分。

---

## 17. Review 评价管理

### 17.1 评价分页

- **URL**：`GET /review/page?score=&status=&page=1&pageSize=10`
- **参数**：score(1~5), status(0=待审核/1=通过/2=驳回)

### 17.2 评价详情

- **URL**：`GET /review/{id}`

### 17.3 审核评价

- **URL**：`PUT /review/approve`

```json
{
    "id": 1,
    "status": 1
}
```

> **status**：1=通过, 2=驳回

---

## 18. Role 角色管理

### 18.1 角色列表

- **URL**：`GET /role/list`

### 18.2 角色详情

- **URL**：`GET /role/{id}`

### 18.3 新增角色

- **URL**：`POST /role`

```json
{
    "name": "店长",
    "description": "门店管理者，可管理门店所有事务",
    "status": 1,
    "menuIds": [1, 2, 3, 4, 5]
}
```

### 18.4 修改角色

- **URL**：`PUT /role/{id}`（请求体同新增）

### 18.5 删除角色

- **URL**：`DELETE /role/{id}`

### 18.6 分配菜单权限

- **URL**：`PUT /role/{id}/assign-menus`

```json
[1, 2, 3, 4, 5, 6, 7]
```

---

## 19. Sku SKU 管理

### 19.1 SKU 分页

- **URL**：`GET /sku/page?productId=&productName=&page=1&pageSize=10`

### 19.2 库存预警列表

- **URL**：`GET /sku/warn?page=1&pageSize=10`

### 19.3 SKU 详情

- **URL**：`GET /sku/{id}`

### 19.4 更新 SKU

- **URL**：`PUT /sku/{id}`

```json
{
    "price": 30.00,
    "stock": 200,
    "warnStock": 20
}
```

---

## 20. Stock 库存管理

### 20.1 库存列表

- **URL**：`GET /stock/list?productId=&productName=&page=1&pageSize=10`

### 20.2 入库操作

- **URL**：`POST /stock/in`
- **说明**：库存、售价、预警值都挂在 **SKU** 上（规格矩阵商品下有多个 SKU，各自库存独立），
  所以入参是 `skuId`；`PUT /sku/{id}` 同样用于按 SKU 改价

```json
{
    "skuId": 388,
    "quantity": 50
}
```

### 20.3 预警值设置

- **URL**：`PUT /stock/warning/{id}`

```json
{
    "warnStock": 30
}
```

---

## 21. TableQr 桌码管理

### 21.1 桌码分页

- **URL**：`GET /table/list?keyword=&page=1&pageSize=10`

### 21.2 全部桌码列表

- **URL**：`GET /table/all`

### 21.3 桌码详情

- **URL**：`GET /table/{id}`

### 21.4 新增桌码

- **URL**：`POST /table`

```json
{
    "tableNo": "A-01",
    "area": "大厅",
    "seats": 4
}
```

### 21.5 修改桌码

- **URL**：`PUT /table/{id}`（请求体同新增）

### 21.6 删除桌码

- **URL**：`DELETE /table/{id}`

---

## 附录

### A. 通用错误码说明

| code | 说明 |
|------|------|
| 0 | 操作成功 |
| 401 | 未认证 / token 过期 |
| 403 | 无权限 |
| 500 | 服务器内部错误 |

### B. 常用字段枚举说明

| 字段 | 枚举值 |
|------|--------|
| status（通用） | 0=禁用/下架/草稿, 1=启用/上架/发布 |
| acceptOrder（门店） | 0=停止接单, 1=开始接单 |
| coupon.type | 1=满减券, 2=折扣券 |
| menu.type | 1=目录, 2=菜单, 3=按钮 |
| points.type | 1=获得, 2=使用 |
| review.status | 0=待审核, 1=通过, 2=驳回 |
| refund.status | approved=通过, rejected=拒绝 |