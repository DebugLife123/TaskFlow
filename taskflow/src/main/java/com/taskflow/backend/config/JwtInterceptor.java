package com.taskflow.backend.config;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.taskflow.backend.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头中获取 Token
        String token = request.getHeader("token");

        // 2. 如果是跨域的预检请求（OPTIONS），直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 3. 校验 Token
        try {
            if (token == null || token.isEmpty()) {
                throw new RuntimeException("无Token，请先登录");
            }
            // 这里我们之前写的 JwtUtils 还没写验证逻辑，建议在这里简单调用验证
            // 或者先默认：只要有 Token 就行（进阶再写解析逻辑）
            return true;
        } catch (Exception e) {
            response.setStatus(401); // 设置返回状态码为 401（未授权）
            response.getWriter().write("Invalid Token");
            return false;
        }
    }
}