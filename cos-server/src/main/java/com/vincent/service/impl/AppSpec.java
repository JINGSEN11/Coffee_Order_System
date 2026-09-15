package com.vincent.service.impl;

import com.vincent.vo.AppDetailItemVO;

import java.util.ArrayList;
import java.util.List;

/**
 * C 端取餐码、搜索热词等固定口径常量与纯派生逻辑。
 *
 * 注意：规格组（杯型/温度/糖度）与加料**不再是常量模板** —— 它们已落到
 * spec_group / spec_option / product_spec_group / product_addon 表里，见
 * sql/06_migrate_spec_addon.sql 与 AppCatalogServiceImpl#buildSpecGroups / #buildAddonGroup。
 * 原来这里硬编码的 specGroupTemplates / addonGroupTemplate 已删除。
 */
public final class AppSpec {

    /** 「非矩阵」商品的规格占位值：specs_json = {"规格":"标准"} */
    public static final String STANDARD_SPEC_KEY = "规格";
    public static final String STANDARD_SPEC_VALUE = "标准";

    /** 加料组的固定 key/label（加料商品本身由 product_addon 关联决定） */
    public static final String ADDON_GROUP_KEY = "addon";
    public static final String ADDON_GROUP_LABEL = "加料";

    /** 搜索页热门词（数据库无对应存储，与契约示例一致） */
    public static final List<String> HOT_KEYWORDS =
            List.of("拿铁", "美式", "生椰", "气泡", "可颂", "柠檬茶");

    private AppSpec() {
    }

    /** 商品参数表（契约标注为固定文案） */
    public static List<AppDetailItemVO> details(boolean hasMatrix) {
        List<AppDetailItemVO> details = new ArrayList<>();
        details.add(detail("主要原料", "阿拉比卡咖啡豆 / 鲜牛奶"));
        details.add(detail("规格", hasMatrix ? "杯型 · 温度 · 糖度 可选" : "标准规格"));
        details.add(detail("保存方式", "建议 30 分钟内饮用风味最佳"));
        details.add(detail("过敏原", "含乳制品，可能含坚果成分"));
        return details;
    }

    /**
     * 取餐码派生：字母按 99 循环 + 两位序号。
     * 1~99 → A-01~A-99，100~198 → B-01~B-99（用 99 而非 100 保证码宽统一）。
     */
    public static String pickupCodeOf(Integer no) {
        if (no == null || no < 1) {
            return null;
        }
        char letter = (char) ('A' + ((no - 1) / 99) % 26);
        return letter + "-" + String.format("%02d", (no - 1) % 99 + 1);
    }

    private static AppDetailItemVO detail(String label, String value) {
        AppDetailItemVO item = new AppDetailItemVO();
        item.setLabel(label);
        item.setValue(value);
        return item;
    }
}
