package com.vincent.controller.admin;

import com.vincent.common.Result;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController("adminDictController")
@RequestMapping("/admin/dict")
public class DictController {

    /** 规格模板（含必选/可选组），供商品编辑页 SKU 矩阵渲染使用 */
    @GetMapping("/spec-templates")
    public Result<List<SpecTemplateVO>> specTemplates() {
        return Result.success(SPEC_TEMPLATES);
    }

    // ==================== 数据 ====================

    private static final List<SpecTemplateVO> SPEC_TEMPLATES = Arrays.asList(
        new SpecTemplateVO("cup", "杯型", true, Arrays.asList(
            new SpecOptionVO("中杯", 0),
            new SpecOptionVO("大杯", 3)
        )),
        new SpecTemplateVO("temp", "温度", true, Arrays.asList(
            new SpecOptionVO("热", 0),
            new SpecOptionVO("冰", 0),
            new SpecOptionVO("去冰", 0)
        )),
        new SpecTemplateVO("sugar", "糖度", true, Arrays.asList(
            new SpecOptionVO("无糖", 0),
            new SpecOptionVO("半糖", 0),
            new SpecOptionVO("全糖", 0)
        )),
        new SpecTemplateVO("addon", "加料（可选项，不参与矩阵拆分）", false, Arrays.asList(
            new SpecOptionVO("燕麦奶", 3),
            new SpecOptionVO("浓缩shot", 4),
            new SpecOptionVO("奶油顶", 5),
            new SpecOptionVO("香草糖浆", 3)
        ))
    );

    @Data
    @AllArgsConstructor
    static class SpecTemplateVO {
        private String key;
        private String label;
        private boolean required;
        private List<SpecOptionVO> options;
    }

    @Data
    @AllArgsConstructor
    static class SpecOptionVO {
        private String name;
        private int extra;
    }
}