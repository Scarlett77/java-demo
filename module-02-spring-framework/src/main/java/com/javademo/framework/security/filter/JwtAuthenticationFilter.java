package com.javademo.framework.security.filter;

import com.javademo.framework.security.service.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器（每次请求执行一次）
 *
 * <p>技术点：Spring Security 自定义过滤器
 * <p>场景背景：Spring Security 默认使用 Session 进行认证，REST API 场景需改为 JWT 无状态认证。
 *            通过自定义过滤器，在每次请求时解析 Authorization 请求头中的 JWT，
 *            将用户信息注入 SecurityContext，后续鉴权由 Spring Security 自动处理。
 *
 * <p>过滤器执行位置：
 *   UsernamePasswordAuthenticationFilter 之前（在 SecurityConfig 中配置 addFilterBefore）
 *
 * <p>避坑指南：
 *   1. 继承 OncePerRequestFilter 确保每次请求只执行一次（避免转发时重复执行）。
 *   2. 不要在过滤器中抛出异常，而是直接返回 401 响应（Security 的 AuthenticationEntryPoint 也可）。
 *   3. 公开接口（登录、注册）不需要 JWT，在 SecurityConfig 中配置 permitAll 即可，
 *      本过滤器不需要特殊处理（解析失败不设置 SecurityContext 即可）。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                // 解析用户名和角色
                String username = tokenProvider.getUsernameFromToken(token);
                List<String> roles = tokenProvider.getRolesFromToken(token);

                // 将角色字符串转换为 Spring Security GrantedAuthority
                var authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                // 创建认证对象（credentials 设为 null，已通过 JWT 签名验证）
                var authentication = new UsernamePasswordAuthenticationToken(
                        username, null, authorities);

                // 注入到 SecurityContext，后续 @PreAuthorize 鉴权依赖此值
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("[JWT认证] 用户 {} 认证成功，角色: {}", username, roles);
            }
        } catch (Exception e) {
            log.warn("[JWT认证] 令牌解析失败: {}", e.getMessage());
            // 不抛异常，让请求继续走，未认证的请求由 Spring Security 拦截并返回 401
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头提取 JWT 令牌
     * Authorization: Bearer <token>
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
