package com.javademo.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Starter 配置属性类
 *
 * <p>技术点：Spring Boot @ConfigurationProperties
 * <p>原理：Spring Boot 在启动时会将 application.properties/yml 中以 "hello" 为前缀的配置
 *         自动绑定到此类的字段，实现类型安全的外部化配置。
 *
 * <p>用法（在使用方的 application.yml 中配置）：
 * <pre>
 * hello:
 *   name: "Java全栈"
 *   enabled: true
 *   greeting: "你好"
 * </pre>
 */
@ConfigurationProperties(prefix = "hello")
public class HelloProperties {

    /** 是否启用 Hello 功能，默认 true */
    private boolean enabled = true;

    /** 问候的对象名称，默认 "World" */
    private String name = "World";

    /** 问候语前缀，默认 "Hello" */
    private String greeting = "Hello";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGreeting() { return greeting; }
    public void setGreeting(String greeting) { this.greeting = greeting; }
}
