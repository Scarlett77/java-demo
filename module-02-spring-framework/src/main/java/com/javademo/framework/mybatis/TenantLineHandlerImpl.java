package com.javademo.framework.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

/**
 * 多租户行级隔离处理器
 *
 * <p>技术点：MyBatis-Plus 多租户插件
 * <p>场景背景：SaaS 系统中，多个租户共享同一套数据库表，需要在 SQL 层面自动隔离不同租户数据。
 *            传统方式需要每个 Mapper 手动加 tenant_id 条件，极易遗漏；
 *            MyBatis-Plus 多租户插件可在拦截器层自动拼接 WHERE tenant_id = ? 条件。
 *
 * <p>核心原理：
 *   MyBatis-Plus 使用 JSQLParser 解析 SQL，在 SELECT/INSERT/UPDATE/DELETE 语句中
 *   自动注入租户条件，无需修改业务 Mapper 代码。
 *
 * <p>避坑指南：
 *   1. 不是所有表都需要多租户隔离（如字典表、系统配置表），
 *      通过 ignoreTenantId() 方法返回 true 跳过不需要隔离的表。
 *   2. 多租户插件不支持子表/关联表的自动拼接，复杂 JOIN 查询需手动处理。
 *   3. 租户 ID 要从 ThreadLocal 中获取（在请求拦截器中设置，请求结束后清除），
 *      避免线程污染。
 *   4. 插件顺序很重要：多租户插件应在分页插件之前注册。
 */
@Component
public class TenantLineHandlerImpl implements TenantLineHandler {

    /**
     * 获取当前租户 ID
     * 从 TenantContext（ThreadLocal）中获取，由请求拦截器在每次请求开始时设置
     */
    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            // 如果没有租户上下文（如系统内部任务），使用默认租户或抛出异常
            throw new IllegalStateException("当前请求未设置租户ID，请检查请求头 X-Tenant-Id");
        }
        return new LongValue(tenantId);
    }

    /**
     * 租户字段名（数据库列名）
     */
    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    /**
     * 是否忽略多租户过滤（返回 true 表示跳过该表的租户隔离）
     * 适用于：数据字典、系统配置、全局公共表等
     */
    @Override
    public boolean ignoreTable(String tableName) {
        // 白名单：这些表不需要租户隔离
        return switch (tableName.toLowerCase()) {
            case "sys_dict", "sys_config", "sys_region" -> true;
            default -> false;
        };
    }
}
