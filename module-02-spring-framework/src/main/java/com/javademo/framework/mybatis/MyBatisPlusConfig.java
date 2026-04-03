package com.javademo.framework.mybatis;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 *
 * <p>注册插件链（注意顺序）：多租户 → 分页 → 乐观锁
 *
 * <p>避坑指南：
 *   1. 插件注册顺序影响 SQL 拦截顺序，多租户应在分页插件前面，
 *      否则分页 COUNT 语句可能缺少租户条件。
 *   2. 乐观锁插件（OptimisticLocker）只对 updateById 等 MyBatis-Plus 内置方法有效，
 *      自定义 Mapper XML 的 UPDATE 需手动添加版本号条件。
 */
@Configuration
@MapperScan("com.javademo.framework.mybatis.mapper")
public class MyBatisPlusConfig {

    @Autowired
    private TenantLineHandlerImpl tenantLineHandler;

    /**
     * MyBatis-Plus 拦截器插件链配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 多租户插件（必须在分页插件之前）
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));

        // 2. 分页插件（推荐指定数据库类型）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());

        // 3. 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }
}

/**
 * 多租户基础实体（所有业务表实体继承此类）
 *
 * <p>包含公共字段：租户ID、逻辑删除、创建/更新时间
 */
class BaseTenantEntity {

    /** 租户ID（多租户隔离字段，由多租户插件自动处理）*/
    private Long tenantId;

    /** 逻辑删除（0=正常，1=删除），MyBatis-Plus 自动处理 WHERE deleted=0 */
    @TableLogic
    private Integer deleted;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private java.time.LocalDateTime createTime;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private java.time.LocalDateTime updateTime;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
    public java.time.LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }
}
