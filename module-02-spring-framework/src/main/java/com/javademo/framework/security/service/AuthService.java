package com.javademo.framework.security.service;

import com.javademo.framework.mvc.ApiResponse;
import com.javademo.framework.mvc.BusinessException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 认证服务：登录、注册
 *
 * <p>演示 Spring Security + JWT 登录流程：
 * <pre>
 * 客户端 POST /api/auth/login
 *    │
 *    ▼
 * AuthController.login(username, password)
 *    │
 *    ▼
 * AuthenticationManager.authenticate(token)   ── 触发 UserDetailsService.loadUserByUsername
 *    │                                              ── 密码 BCrypt 校验
 *    ▼ 认证成功
 * JwtTokenProvider.generateToken(userId, username, roles)
 *    │
 *    ▼
 * 返回 JWT 给客户端
 *
 * 后续请求：
 * 客户端 请求头: Authorization: Bearer <JWT>
 *    │
 *    ▼
 * JwtAuthenticationFilter 解析 JWT → 注入 SecurityContext
 *    │
 *    ▼
 * @PreAuthorize("hasRole('ADMIN')") 方法级鉴权
 * </pre>
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户登录，返回 JWT
     */
    public ApiResponse<LoginResponse> login(String username, String password) {
        try {
            // 委托 Spring Security 的 AuthenticationManager 进行认证
            // 内部调用 UserDetailsService.loadUserByUsername + BCrypt 密码校验
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            // 从认证结果中提取角色列表
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            // 生成 JWT（实际项目从 UserDetails 中取 userId）
            String token = tokenProvider.generateToken(1L, username, roles);

            return ApiResponse.success("登录成功", new LoginResponse(token, username, roles));
        } catch (Exception e) {
            throw new BusinessException(401, "用户名或密码错误");
        }
    }

    /**
     * 登录响应 DTO
     */
    public record LoginResponse(String token, String username, List<String> roles) {}
}
