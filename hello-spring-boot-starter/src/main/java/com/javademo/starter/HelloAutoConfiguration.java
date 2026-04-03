package com.javademo.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 自动配置类：HelloAutoConfiguration
 *
 * <p>技术点：Spring Boot 自动配置原理
 *
 * <p>核心原理：
 * <pre>
 * Spring Boot 启动流程（自动配置部分）：
 *
 * 1. @SpringBootApplication
 *       └─ @EnableAutoConfiguration
 *             └─ AutoConfigurationImportSelector
 *                   └─ 读取 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 *                         └─ 加载并实例化所有自动配置类
 *                               └─ 根据 @ConditionalOn* 条件决定是否生效
 *
 * 关键注解说明：
 * - @AutoConfiguration：标记为自动配置类（Spring Boot 3.x；2.x 用 @Configuration）
 * - @ConditionalOnClass：classpath 中存在指定类时生效
 * - @ConditionalOnMissingBean：容器中不存在指定 Bean 时才创建（避免覆盖用户自定义）
 * - @ConditionalOnProperty：配置属性满足条件时生效（hello.enabled=true）
 * - @EnableConfigurationProperties：启用 @ConfigurationProperties 类的绑定
 * </pre>
 *
 * <p>注意（Spring Boot 3.x 变更）：
 *   Spring Boot 2.7+ 废弃了 META-INF/spring.factories 的自动配置注册方式，
 *   改为 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports。
 *   Spring Boot 3.x 完全移除了旧方式的支持。
 *
 * <p>避坑指南：
 *   1. Starter 中不应使用 @ComponentScan，避免扫描使用方的 Bean。
 *   2. 自动配置类必须通过 SPI 文件注册，不能靠包扫描发现。
 *   3. 使用 @ConditionalOnMissingBean 允许用户覆盖默认 Bean。
 *   4. Starter 的依赖应声明为 optional 或仅依赖 spring-boot-autoconfigure。
 */
@AutoConfiguration
@ConditionalOnClass(HelloService.class) // classpath 中有 HelloService 才生效
@ConditionalOnProperty(
        prefix = "hello",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true // 未配置时默认生效
)
@EnableConfigurationProperties(HelloProperties.class) // 启用属性绑定
public class HelloAutoConfiguration {

    /**
     * 注册 HelloService Bean
     * @ConditionalOnMissingBean 保证用户可以自定义 Bean 覆盖此默认实现
     */
    @Bean
    @ConditionalOnMissingBean
    public HelloService helloService(HelloProperties properties) {
        return new HelloService(properties);
    }
}
