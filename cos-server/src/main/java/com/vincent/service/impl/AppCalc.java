package com.vincent.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.vincent.dto.AppAddonDTO;
import com.vincent.entity.Coupon;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * C 端金额与 JSON 计算工具。
 * 券门槛与积分抵扣规则与小程序 utils/price.js 的 couponAmountOf / calcAmount 同源，
 * 保证「小程序试算」与「服务端重算」结果一致。
 * JSON 解析使用 hutool（项目 Spring Boot 4 运行时为 Jackson 3，Jackson 2 的 databind 不在编译期依赖中）。
 */
@Slf4j
public final class AppCalc {

    /** 金额统一保留两位小数 */
    private static final int MONEY_SCALE = 2;

    private AppCalc() {
    }

    /** 金额规整：两位小数，四舍五入 */
    public static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 券可抵扣金额。
     * 满减：min(discount_amount, amount)；折扣：amount * (100 - discount_rate) / 100；未达门槛返回 0。
     */
    public static BigDecimal couponAmountOf(Coupon coupon, BigDecimal amount) {
        if (coupon == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal goods = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal threshold = coupon.getThresholdAmount() == null ? BigDecimal.ZERO : coupon.getThresholdAmount();
        if (goods.compareTo(threshold) < 0) {
            return BigDecimal.ZERO;
        }
        if (coupon.getType() != null && coupon.getType() == 2) {
            BigDecimal rate = coupon.getDiscountRate() == null ? BigDecimal.ZERO : coupon.getDiscountRate();
            return money(goods.multiply(BigDecimal.valueOf(100).subtract(rate))
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        }
        BigDecimal discount = coupon.getDiscountAmount() == null ? BigDecimal.ZERO : coupon.getDiscountAmount();
        return money(discount.min(goods));
    }

    /** 积分 → 金额（默认 100 积分 = 1 元） */
    public static BigDecimal pointsToAmount(Integer points, int deductRate) {
        int rate = deductRate <= 0 ? 100 : deductRate;
        return money(BigDecimal.valueOf(points == null ? 0 : points)
                .divide(BigDecimal.valueOf(rate), 4, RoundingMode.HALF_UP));
    }

    /** 金额 → 所需积分（向下取整到整数积分） */
    public static int amountToPoints(BigDecimal amount, int deductRate) {
        int rate = deductRate <= 0 ? 100 : deductRate;
        BigDecimal rest = amount == null ? BigDecimal.ZERO : amount;
        return rest.multiply(BigDecimal.valueOf(rate)).setScale(0, RoundingMode.FLOOR).intValue();
    }

    /**
     * 积分抵扣夹取：不超过用户可用积分，也不超过券后应付对应积分。
     * 与 price.js#calcAmount 一致，最终消耗积分向下取整到 deductRate 的整数倍。
     */
    public static int clampPoints(Integer requested, int userPoints, BigDecimal afterCoupon, int deductRate) {
        int rate = deductRate <= 0 ? 100 : deductRate;
        int maxByAmount = amountToPoints(afterCoupon, rate);
        int cap = Math.min(Math.max(userPoints, 0), maxByAmount);
        int want = Math.max(0, Math.min(requested == null ? 0 : requested, cap));
        return want / rate * rate;
    }

    /** 份数规整：null 或 < 1 一律视为 1 */
    public static int qtyOf(Integer qty) {
        return qty == null || qty < 1 ? 1 : qty;
    }

    /**
     * 加料签名：skuId:qty 排序后以 | 拼接；无加料返回空串（对应 cart_item.addons_hash）。
     * 加料是独立商品，签名只认 SKU 与份数——名称与价格随时可变，不能进签名。
     */
    public static String addonsHash(List<AppAddonDTO> addons) {
        if (addons == null || addons.isEmpty()) {
            return "";
        }
        String signature = addons.stream()
                .filter(java.util.Objects::nonNull)
                .filter(a -> a.getSkuId() != null)
                .map(a -> a.getSkuId() + ":" + qtyOf(a.getQty()))
                .sorted()
                .collect(Collectors.joining("|"));
        return signature.isEmpty() ? "" : DigestUtil.md5Hex(signature);
    }

    /**
     * 选项签名：组:值 排序后以 | 拼接；无选项返回空串（对应 cart_item.options_hash）。
     * 温度/糖度不影响价格，但决定购物车行的合并与拆分。
     */
    public static String optionsHash(Map<String, Object> options) {
        if (options == null || options.isEmpty()) {
            return "";
        }
        String signature = options.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .map(e -> e.getKey() + ":" + e.getValue())
                .sorted()
                .collect(Collectors.joining("|"));
        return signature.isEmpty() ? "" : DigestUtil.md5Hex(signature);
    }

    /** 规格快照文案：仅规格值以 / 拼接（如 中杯/冰/半糖） */
    public static String specText(Map<String, Object> specs) {
        if (specs == null || specs.isEmpty()) {
            return "";
        }
        return specs.values().stream().map(String::valueOf).collect(Collectors.joining("/"));
    }

    /** 规格 + 选项的展示文案：如 大杯/冰/半糖（任一侧为空则直接用另一侧） */
    public static String combineText(Map<String, Object> specs, Map<String, Object> options) {
        String a = specText(specs);
        String b = specText(options);
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        return a + "/" + b;
    }

    /** 解析 SKU 的 specs_json 字符串为有序 Map */
    public static Map<String, Object> parseSpecs(String specsJson) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (specsJson == null || specsJson.isBlank()) {
            return map;
        }
        try {
            JSONObject object = JSONUtil.parseObj(specsJson);
            for (String key : object.keySet()) {
                map.put(key, object.get(key));
            }
        } catch (Exception e) {
            log.warn("解析 specs_json 失败：{}", specsJson, e);
        }
        return map;
    }

    /** 解析 cart_item.options（类型与 specs_json 相同，都是键值对） */
    public static Map<String, Object> parseOptions(String options) {
        return parseSpecs(options);
    }

    /** 序列化下单选项，无选项时落库为 null */
    public static String writeOptions(Map<String, Object> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return JSONUtil.toJsonStr(options);
        } catch (Exception e) {
            log.warn("序列化 options 失败", e);
            return null;
        }
    }

    /** 解析 cart_item.addons 字符串（[{skuId,qty}]） */
    public static List<AppAddonDTO> parseAddons(String addons) {
        List<AppAddonDTO> list = new ArrayList<>();
        if (addons == null || addons.isBlank()) {
            return list;
        }
        try {
            JSONArray array = JSONUtil.parseArray(addons);
            for (Object element : array) {
                if (!(element instanceof JSONObject object)) {
                    continue;
                }
                AppAddonDTO dto = new AppAddonDTO();
                dto.setSkuId(object.getLong("skuId"));
                dto.setQty(qtyOf(object.getInt("qty")));
                if (dto.getSkuId() != null) {
                    list.add(dto);
                }
            }
        } catch (Exception e) {
            log.warn("解析 cart_item.addons 失败：{}", addons, e);
        }
        return list;
    }

    /** 序列化加料明细，无加料时落库为 [] */
    public static String writeAddons(List<AppAddonDTO> addons) {
        try {
            return JSONUtil.toJsonStr(addons == null ? new ArrayList<>() : addons);
        } catch (Exception e) {
            log.warn("序列化 addons 失败", e);
            return "[]";
        }
    }

    /** 解析 review.images JSON 数组 */
    public static List<String> parseImages(String images) {
        List<String> list = new ArrayList<>();
        if (images == null || images.isBlank()) {
            return list;
        }
        try {
            JSONArray array = JSONUtil.parseArray(images);
            for (Object element : array) {
                if (element != null) {
                    list.add(String.valueOf(element));
                }
            }
        } catch (Exception e) {
            log.warn("解析 review.images 失败：{}", images, e);
        }
        return list;
    }

    /** 积分流水类型文案（枚举字典） */
    public static String pointsTypeText(Integer type) {
        if (type == null) {
            return "其他";
        }
        return switch (type) {
            case 1 -> "消费获得";
            case 2 -> "抵扣消耗";
            case 3 -> "退款退回";
            case 4 -> "管理员调整";
            default -> "其他";
        };
    }

    /** 商户支付单号（幂等键）：OT + yyyyMMddHHmmss + 6 位订单 ID */
    public static String outTradeNo(Long orderId) {
        String suffix = orderId == null ? "000000" : String.format("%06d", orderId);
        return "OT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + suffix;
    }
}
