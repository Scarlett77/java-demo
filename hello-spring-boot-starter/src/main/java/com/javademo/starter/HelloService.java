package com.javademo.starter;

/**
 * Starter 核心功能服务类
 *
 * <p>这个 Bean 由自动配置类（HelloAutoConfiguration）注册到 Spring 容器，
 * 使用方无需手动 @Bean 声明，只需引入 starter 依赖并配置属性即可使用。
 */
public class HelloService {

    private final HelloProperties properties;

    public HelloService(HelloProperties properties) {
        this.properties = properties;
    }

    /**
     * 生成问候语
     *
     * @return 问候字符串，如 "Hello, World!" 或根据配置自定义
     */
    public String sayHello() {
        return String.format("%s, %s!", properties.getGreeting(), properties.getName());
    }

    /**
     * 返回当前配置摘要，方便调试
     */
    public String getConfigSummary() {
        return String.format("HelloService 配置[enabled=%b, name=%s, greeting=%s]",
                properties.isEnabled(), properties.getName(), properties.getGreeting());
    }
}
