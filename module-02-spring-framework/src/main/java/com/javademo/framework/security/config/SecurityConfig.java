package com.javademo.framework.security.config;

import com.javademo.framework.security.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security + JWT 安全配置（RBAC 角色权限控制）
 *
 * <p>技术点：Spring Security 3.x（Spring Boot 3.x）JWT + RBAC 最小化配置
 * <p>场景背景：企业 API 服务需要：
 *   1. 无状态认证（JWT 替代 Session）
 *   2. 基于角色的权限控制（ROLE_ADMIN/ROLE_USER）
 *   3. 部分接口公开（登录、注册）
 *
 * <p>RBAC 核心原理：
 * <pre>
 * 用户(User) ──M:N── 角色(Role) ──M:N── 权限(Permission)
 *
 * Spring Security 的角色约定：
 *   - hasRole("ADMIN")   ──对应── GrantedAuthority: "ROLE_ADMIN"
 *   - hasAuthority("user:read")  ──对应── GrantedAuthority: "user:read"（细粒度权限）
 * </pre>
 *
 * <p>避坑指南：
 *   1. Spring Boot 3.x / Spring Security 6.x 废弃了 WebSecurityConfigurerAdapter，
 *      改为基于 SecurityFilterChain Bean 的声明式配置。
 *   2. cors() 和 csrf() 不要简单禁用，生产环境需配置 CORS 允许源白名单。
 *   3. @EnableMethodSecurity 替换了旧的 @EnableGlobalMethodSecurity(prePostEnabled=true)。
 *   4. BCrypt 加密强度（strength）建议 10-12，强度越高越安全但越慢。
 *   5. 认证失败（401）和授权失败（403）要分别配置不同的入口点，返回自定义 JSON 响应。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 启用 @PreAuthorize/@PostAuthorize 方法级鉴权
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * 核心安全过滤链配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（REST API 无状态，不需要 CSRF 保护）
                // 生产环境：如有表单提交，需启用 CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // 无状态会话（不创建 HttpSession，每次请求从 JWT 中还原认证状态）
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 请求授权规则
                .authorizeHttpRequests(auth -> auth
                        // 公开接口：无需认证
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // 管理员接口：需要 ADMIN 角色
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 其他所有接口：需要登录（任意角色）
                        .anyRequest().authenticated()
                )

                // 未认证时返回 401（而非默认的重定向到登录页）
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(401);
                            response.getWriter().write("{\"code\":401,\"message\":\"未登录或令牌已过期\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(403);
                            response.getWriter().write("{\"code\":403,\"message\":\"无权访问该资源\"}");
                        })
                )

                // 在 UsernamePasswordAuthenticationFilter 前插入 JWT 过滤器
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码加密器（BCrypt，单向加密，无法解密）
     * 避坑：不要使用 NoOpPasswordEncoder（明文存储密码），线上必须使用 BCrypt 或 Argon2
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // strength=12，安全性与性能平衡
    }

    /**
     * 暴露 AuthenticationManager Bean，用于登录接口手动触发认证
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
