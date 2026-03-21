package com.taskflow.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")    // 拦截所有 /api 开头的请求
                .excludePathPatterns("/api/user/login","/api/user/register"); // 放行登录接口
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取刚才设置的硬盘路径
        String path = System.getProperty("user.dir") + "/uploads/";

        // 告诉 Spring Boot：只要遇到以 /uploads/ 开头的网络请求
        // 就去 file:（本地硬盘）的 path 目录里找文件
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + path);
    }
}