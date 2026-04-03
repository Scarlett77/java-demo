package com.javademo.framework;

import com.javademo.framework.mvc.ApiResponse;
import com.javademo.framework.mvc.BusinessException;
import com.javademo.framework.mvc.GlobalExceptionHandler;
import com.javademo.framework.mybatis.TenantContext;
import com.javademo.framework.security.service.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring 框架模块测试
 */
@SpringBootTest
class FrameworkTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void testJwtGenerateAndValidate() {
        String token = jwtTokenProvider.generateToken(1L, "admin", List.of("ROLE_ADMIN", "ROLE_USER"));
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("admin", jwtTokenProvider.getUsernameFromToken(token));
        List<String> roles = jwtTokenProvider.getRolesFromToken(token);
        assertTrue(roles.contains("ROLE_ADMIN"));
    }

    @Test
    void testJwtInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
        assertFalse(jwtTokenProvider.validateToken(""));
    }

    @Test
    void testApiResponse() {
        ApiResponse<String> success = ApiResponse.success("test data");
        assertEquals(200, success.getCode());
        assertEquals("test data", success.getData());
        assertNotNull(success.getMessage());

        ApiResponse<Void> error = ApiResponse.error(400, "参数错误");
        assertEquals(400, error.getCode());
        assertNull(error.getData());
    }

    @Test
    void testTenantContext() {
        TenantContext.setTenantId(100L);
        assertEquals(100L, TenantContext.getTenantId());
        TenantContext.clear();
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void testBusinessException() {
        BusinessException ex = new BusinessException(404, "用户不存在");
        assertEquals(404, ex.getCode());
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    void testGlobalExceptionHandlerBusiness() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BusinessException ex = new BusinessException(400, "参数错误");
        ApiResponse<Void> response = handler.handleBusinessException(ex);
        assertEquals(400, response.getCode());
    }
}
