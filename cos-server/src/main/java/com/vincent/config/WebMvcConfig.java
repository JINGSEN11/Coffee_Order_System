package com.vincent.config;

import com.vincent.interceptor.AdminAuthInterceptor;
import com.vincent.interceptor.AppAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AdminAuthInterceptor adminAuthInterceptor;

    @Autowired
    private AppAuthInterceptor appAuthInterceptor;

    /**
     * 将空字符串请求参数转为 null，避免 @RequestParam Long/Integer 接收空字符串时报 400 错误。
     * 前端某些下拉框（el-select）未选择时默认值为 ""，会被 axios 原样发送。
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, Long>() {
            @Override
            public Long convert(String source) {
                return (source == null || source.trim().isEmpty()) ? null : Long.valueOf(source);
            }
        });
        registry.addConverter(new Converter<String, Integer>() {
            @Override
            public Integer convert(String source) {
                return (source == null || source.trim().isEmpty()) ? null : Integer.valueOf(source);
            }
        });
    }

    /**
     * 两个身份域各挂各的拦截器，路径前缀与密钥一一对应。
     *
     * 早先是一个拦截器覆盖 /** 并「先试管理端密钥、再试用户端密钥」，
     * 导致小程序会员的令牌可以直接调 /admin/** 的全部接口（删商品、审退款、管员工）。
     * 现在 /admin/** 只认管理端密钥、/api/app/** 只认用户端密钥，跨域令牌签名必然不通过。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 管理端：/admin/** 挂管理端鉴权
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
                        "/admin/auth/login",      // 员工登录
                        "/admin/auth/refresh",    // 令牌刷新
                        "/admin/login"            // 兼容前端旧路径
                );

        // C 端：/api/app/** 挂用户端鉴权
        registry.addInterceptor(appAuthInterceptor)
                .addPathPatterns("/api/app/**")
                .excludePathPatterns(
                        "/api/app/auth/login",    // 小程序微信登录
                        "/api/app/pay/notify"     // 微信支付回调（由微信服务器调用，走回调验签）
                );
    }

}
