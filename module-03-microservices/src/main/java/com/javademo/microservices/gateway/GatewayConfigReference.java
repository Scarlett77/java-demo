package com.javademo.microservices.gateway;

/**
 * Spring Cloud Gateway 网关配置模板
 *
 * <p>技术点：Spring Cloud Gateway 网关路由配置
 * <p>场景背景：微服务架构中，客户端不直接调用各微服务，而是统一通过网关入口，
 *            网关负责：路由转发、认证鉴权、限流熔断、日志记录、跨域处理。
 *
 * <p>核心组件：
 * <pre>
 * 客户端请求
 *    │
 *    ▼
 * Gateway Server (8080)
 *    │  GlobalFilter（全局过滤器：认证/日志/限流）
 *    │  RoutePredicateFactory（断言：匹配路由规则）
 *    │  GatewayFilterFactory（局部过滤器：请求改写）
 *    ▼
 * 目标微服务（通过 Nacos 负载均衡）
 * </pre>
 *
 * <p>注意：Gateway 基于 Spring WebFlux（响应式），不能与 spring-boot-starter-web 共存。
 * 网关应独立部署为一个单独的 Spring Boot 应用。
 * 以下代码仅作为独立网关服务的配置参考。
 *
 * <p>避坑指南：
 *   1. Gateway 不能引入 spring-boot-starter-web（会冲突），只用 spring-cloud-starter-gateway。
 *   2. 使用 lb://service-name 前缀实现负载均衡路由（需要 LoadBalancer）。
 *   3. 全局过滤器（GlobalFilter）的 Ordered 值越小优先级越高。
 *   4. 跨域配置在 Gateway 层统一配置，后端微服务不再配置 CORS。
 */
public class GatewayConfigReference {

    /**
     * 以下为 Gateway 独立服务的 application.yml 配置内容说明（代码注释形式）
     *
     * <pre>
     * # application.yml（网关服务 - 独立部署）
     *
     * spring:
     *   application:
     *     name: api-gateway
     *   cloud:
     *     nacos:
     *       discovery:
     *         server-addr: 127.0.0.1:8848
     *     gateway:
     *       # 全局 CORS 跨域配置
     *       globalcors:
     *         corsConfigurations:
     *           '[/**]':
     *             allowedOriginPatterns: "*"
     *             allowedMethods: ["GET","POST","PUT","DELETE","OPTIONS"]
     *             allowedHeaders: "*"
     *             allowCredentials: true
     *
     *       # 路由规则
     *       routes:
     *         # 用户服务路由
     *         - id: user-service
     *           uri: lb://user-service          # lb:// 表示负载均衡（从Nacos获取实例）
     *           predicates:
     *             - Path=/api/user/**            # 路径匹配断言
     *           filters:
     *             - StripPrefix=1               # 去掉路径前缀 /api
     *             - name: RequestRateLimiter    # 限流过滤器
     *               args:
     *                 redis-rate-limiter.replenishRate: 10   # 每秒10个请求
     *                 redis-rate-limiter.burstCapacity: 20   # 峰值20
     *                 key-resolver: "#{@ipKeyResolver}"
     *
     *         # 订单服务路由
     *         - id: order-service
     *           uri: lb://order-service
     *           predicates:
     *             - Path=/api/order/**
     *             - Header=X-Request-Source, mobile   # 仅匹配来自移动端的请求
     *           filters:
     *             - StripPrefix=1
     *             - AddRequestHeader=X-From-Gateway, true  # 添加请求头标识来源
     *
     *       # Sentinel 网关限流
     *     sentinel:
     *       transport:
     *         dashboard: 127.0.0.1:8080
     * </pre>
     */
    private GatewayConfigReference() {}
}
