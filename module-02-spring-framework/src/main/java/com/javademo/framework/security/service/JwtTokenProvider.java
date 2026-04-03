package com.javademo.framework.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 工具类
 *
 * <p>技术点：JWT（JSON Web Token）生成与解析
 * <p>场景背景：REST API 无状态认证场景，服务端不存储 Session，客户端携带 JWT 令牌请求。
 *
 * <p>核心原理：
 *   JWT = Base64(Header) + "." + Base64(Payload) + "." + Signature
 *   - Header：算法类型（如 HS256）
 *   - Payload：声明信息（userId、roles、过期时间）
 *   - Signature：HMAC-SHA256(secret, header.payload) 防篡改
 *
 * <p>避坑指南：
 *   1. JWT secret 必须足够长（HS256 至少 256 bits = 32字节），并存储在配置中心，不要硬编码。
 *   2. JWT 是无状态的，无法主动吊销（除非维护黑名单）。建议设置较短的过期时间 + RefreshToken。
 *   3. Payload 中不要存储敏感信息（密码、身份证等），因为 Base64 可逆。
 *   4. 验证时必须检查：签名、过期时间、issuer/audience（防止令牌跨服务使用）。
 *   5. 使用 JJWT 0.12.x 时 API 有较大变化，注意版本兼容。
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:JavaFullStackDemoSecretKey2024ForHS256}") String secret,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
        // HS256 要求密钥至少 256 位（32 字节）
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * 生成 JWT 令牌
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param roles    角色列表（如 ["ROLE_ADMIN", "ROLE_USER"]）
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String username, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)                    // 主题（用户标识）
                .claim("userId", userId)              // 自定义声明：用户ID
                .claim("roles", roles)                // 自定义声明：角色列表
                .issuedAt(now)                        // 签发时间
                .expiration(expiry)                   // 过期时间
                .signWith(secretKey)                  // 签名（JJWT 0.12 自动推断算法）
                .compact();
    }

    /**
     * 解析 JWT 令牌，获取声明信息
     *
     * @param token JWT 字符串
     * @return Claims 声明对象
     * @throws JwtException 令牌无效或过期时抛出
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从令牌中提取用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从令牌中提取角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return parseToken(token).get("roles", List.class);
    }

    /**
     * 验证令牌是否有效（签名正确且未过期）
     *
     * @return true 有效，false 无效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
