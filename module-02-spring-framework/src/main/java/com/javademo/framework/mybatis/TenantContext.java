package com.javademo.framework.mybatis;

/**
 * 租户上下文（基于 ThreadLocal）
 *
 * <p>在请求拦截器（HandlerInterceptor / Filter）中设置当前请求的租户 ID，
 * MyBatis-Plus 多租户插件通过此类获取租户 ID 并自动拼接到 SQL 条件中。
 *
 * <p>避坑指南：
 *   务必在请求处理完成后调用 clear()，否则会造成线程池环境下的"租户信息泄漏"。
 *   推荐在 Filter 的 finally 块或 HandlerInterceptor.afterCompletion 中调用。
 */
public class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID_HOLDER = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    /** 请求结束后务必调用，防止线程池中租户信息泄漏 */
    public static void clear() {
        TENANT_ID_HOLDER.remove();
    }
}
