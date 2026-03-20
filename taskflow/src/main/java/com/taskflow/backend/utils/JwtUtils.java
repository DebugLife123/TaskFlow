package com.taskflow.backend.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.util.Date;

public class JwtUtils {
    // 秘钥（严禁泄露）
    private static final String SECRET = "taskflow_secret_key_2026";
    // 过期时间：24小时
    private static final long EXPIRE_TIME = 24 * 60 * 60 * 1000;

    /**
     * 生成 Token
     */
    public static String createToken(Long userId) {
        return JWT.create()
                .withAudience(userId.toString()) // 把用户ID存入Token
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRE_TIME)) // 设置有效期
                .sign(Algorithm.HMAC256(SECRET)); // 加密
    }
    // ✨ 新增：从 Token 中解析出用户 ID
    public static Long getUserId(String token) {
        try {
            return Long.parseLong(JWT.decode(token).getAudience().get(0));
        } catch (Exception e) {
            return null;
        }
    }
}