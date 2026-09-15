package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vincent.entity.SysConfig;
import com.vincent.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * C 端读取 sys_config 的轻量封装（管理端 SysConfigService 面向后台增删改，这里只做读）。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppConfigHelper {

    private final SysConfigMapper sysConfigMapper;

    /** 读字符串配置，缺失或为空时返回默认值 */
    public String stringValue(String key, String defaultValue) {
        SysConfig config = selectOne(key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return defaultValue;
        }
        return config.getConfigValue().trim();
    }

    /** 读整型配置，缺失或非法时返回默认值 */
    public int intValue(String key, int defaultValue) {
        String raw = stringValue(key, null);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            log.warn("sys_config.{} = {} 无法解析为整数，回退默认值 {}", key, raw, defaultValue);
            return defaultValue;
        }
    }

    private SysConfig selectOne(String key) {
        return sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key).last("LIMIT 1")
        );
    }
}
