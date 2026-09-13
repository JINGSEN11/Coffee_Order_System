package com.vincent.config;

import com.vincent.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                // 拦截所有请求
                .addPathPatterns("/**")
                // 放行以下路径（不需要登录即可访问）
                .excludePathPatterns(
                        "/admin/auth/login",      // 管理员登录
                        "/admin/auth/refresh",    // 管理员 token 刷新
                        "/user/login",            // 用户端登录
                        "/user/register",         // 用户注册
                        "/doc.html",              // Knife4j 接口文档
                        "/swagger-resources/**",  // Swagger 资源
                        "/v3/api-docs/**",        // OpenAPI 文档
                        "/webjars/**",            // WebJars 静态资源
                        "/favicon.ico"            // 网站图标
                );
    }

}