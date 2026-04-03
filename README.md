# Java 全栈技术实战详解

> 🎓 一份从 **基础环境搭建** 到 **微服务架构** 的全链路 Java 技术教学项目
>
> **技术路线**：JDK 特性 → JVM 调优 → Spring Boot → MyBatis-Plus → Spring Security → Spring Cloud Alibaba → Seata → RocketMQ → Docker 部署

---

## 📚 项目结构

```
java-fullstack-demo/
├── pom.xml                           # 父 POM，统一依赖版本管理（BOM）
│
├── module-01-java-basics/            # 模块一：Java 基础与 JVM
│   └── src/main/java/com/javademo/basics/
│       ├── jdk/
│       │   ├── JdkFeatureComparison.java  # JDK 8 vs JDK 17/21 语法对比
│       │   └── VirtualThreadDemo.java     # 虚拟线程（JDK 21）
│       ├── jvm/
│       │   └── OomSimulation.java         # JVM OOM 模拟与诊断指南
│       └── juc/
│           ├── CustomThreadPoolDemo.java  # 自定义线程池拒绝策略
│           └── CompletableFutureDemo.java # 异步编排
│
├── hello-spring-boot-starter/        # 自定义 Spring Boot Starter
│   └── src/main/java/com/javademo/starter/
│       ├── HelloProperties.java       # @ConfigurationProperties 配置属性
│       ├── HelloService.java          # 核心服务类
│       └── HelloAutoConfiguration.java # 自动配置类
│
├── module-02-spring-framework/       # 模块二：Spring 框架深度解析
│   └── src/main/java/com/javademo/framework/
│       ├── mvc/
│       │   ├── ApiResponse.java       # 统一 API 响应封装
│       │   ├── BusinessException.java # 业务异常
│       │   └── GlobalExceptionHandler.java # 全局异常处理器
│       ├── mybatis/
│       │   ├── TenantContext.java     # 租户上下文（ThreadLocal）
│       │   ├── TenantLineHandlerImpl.java  # 多租户插件处理器
│       │   └── MyBatisPlusConfig.java # MyBatis-Plus 插件配置
│       └── security/
│           ├── config/SecurityConfig.java  # Spring Security + JWT 配置
│           ├── filter/JwtAuthenticationFilter.java # JWT 过滤器
│           └── service/
│               ├── JwtTokenProvider.java   # JWT 生成/解析
│               └── AuthService.java        # 登录认证服务
│
├── module-03-microservices/          # 模块三：微服务组件实战
│   └── src/main/java/com/javademo/microservices/
│       ├── gateway/GatewayConfigReference.java  # Gateway 配置模板
│       ├── seata/OrderSeataService.java          # Seata AT 模式分布式事务
│       └── mq/OrderMessageService.java           # RocketMQ 事务消息
│
└── docker/
    ├── Dockerfile                    # 多阶段构建 Docker 镜像
    └── docker-compose.yml            # 完整中间件环境（MySQL/Nacos/Redis/RocketMQ/Seata）
```

---

## 🚀 快速开始

### 环境要求

| 工具 | 版本要求 |
|------|----------|
| JDK | 17+ (推荐 JDK 21) |
| Maven | 3.8+ |
| Docker | 20+ |
| Docker Compose | 2.x |

### 1. 克隆并构建

```bash
git clone <repo-url>
cd java-demo

# 编译所有模块
mvn clean compile -DskipTests

# 运行测试
mvn test
```

### 2. 启动中间件（Docker Compose）

```bash
cd docker

# 启动全部中间件（MySQL、Nacos、Redis、RocketMQ、Seata）
docker compose up -d

# 查看启动状态
docker compose ps

# 访问 Nacos 控制台（账号：nacos/nacos）
open http://localhost:8848/nacos
```

### 3. 运行演示程序

```bash
# 模块一演示：JDK 特性对比
mvn exec:java -pl module-01-java-basics \
    -Dexec.mainClass="com.javademo.basics.jdk.JdkFeatureComparison"

# 虚拟线程 vs 平台线程性能对比
mvn exec:java -pl module-01-java-basics \
    -Dexec.mainClass="com.javademo.basics.jdk.VirtualThreadDemo"

# JUC 线程池演示
mvn exec:java -pl module-01-java-basics \
    -Dexec.mainClass="com.javademo.basics.juc.CustomThreadPoolDemo"

# CompletableFuture 异步编排演示
mvn exec:java -pl module-01-java-basics \
    -Dexec.mainClass="com.javademo.basics.juc.CompletableFutureDemo"

# 模块二：Spring 框架应用启动
mvn spring-boot:run -pl module-02-spring-framework
# 访问: http://localhost:8080/h2-console
```

---

## 📖 各模块知识点详解

### 模块一：Java 基础与 JVM

#### 1.1 JDK 8 → JDK 17/21 特性演进

| 特性 | 引入版本 | 说明 |
|------|---------|------|
| `var` 局部变量推断 | JDK 10 | 减少冗余类型声明 |
| `switch` 表达式 | JDK 14 | switch 可返回值 |
| `instanceof` 模式匹配 | JDK 16 | 无需强制转换 |
| `Record` 记录类 | JDK 16 | 不可变数据载体，自动生成 equals/hashCode/toString |
| `Sealed Classes` 密封类 | JDK 17 | 限制继承层次，配合模式匹配 |
| 文本块 Text Blocks | JDK 15 | 多行字符串，保持缩进 |
| 虚拟线程 Virtual Threads | JDK 21 | 轻量级线程，适合 IO 密集型 |

**代码示例**：[JdkFeatureComparison.java](module-01-java-basics/src/main/java/com/javademo/basics/jdk/JdkFeatureComparison.java)

#### 1.2 虚拟线程（JDK 21）

虚拟线程是 **Project Loom** 的成果，JVM 管理的轻量级线程：
- **内存**：平台线程 ~1MB/个，虚拟线程 ~KB 级别
- **数量**：平台线程受 OS 限制（通常几千），虚拟线程可创建**百万级**
- **适用**：I/O 密集型（数据库查询、HTTP 请求），不适合 CPU 密集型

```java
// 三种创建方式
Thread.ofVirtual().start(() -> { ... });
Thread.startVirtualThread(() -> { ... });
Executors.newVirtualThreadPerTaskExecutor(); // 推荐
```

> **Spring Boot 3.2+ 开启虚拟线程**：`spring.threads.virtual.enabled=true`

**代码示例**：[VirtualThreadDemo.java](module-01-java-basics/src/main/java/com/javademo/basics/jdk/VirtualThreadDemo.java)

#### 1.3 JVM 内存与 OOM 诊断

```
JVM 内存区域：
┌─────────────────────────────────────────────────┐
│  堆 (Heap)          │  方法区/元空间 (Metaspace)  │
│  对象实例            │  类元数据、静态变量          │
│  OOM: Java heap     │  OOM: Metaspace             │
├─────────────────────────────────────────────────┤
│  JVM 栈 (Stack)     │  程序计数器   │ 本地方法栈   │
│  栈帧（线程私有）     │  线程私有     │              │
│  StackOverflowError │               │              │
└─────────────────────────────────────────────────┘
```

**OOM 必备 JVM 参数**：
```bash
-Xms512m -Xmx2g                        # 堆内存
-XX:MaxMetaspaceSize=256m              # 元空间上限
-XX:+HeapDumpOnOutOfMemoryError        # OOM 自动 dump
-XX:HeapDumpPath=/tmp/heap.hprof       # dump 路径
-Xlog:gc*:file=/tmp/gc.log             # GC 日志
```

**代码示例**：[OomSimulation.java](module-01-java-basics/src/main/java/com/javademo/basics/jvm/OomSimulation.java)

#### 1.4 JUC 线程池最佳实践

```
线程池工作流程：
提交任务 → 核心线程未满? → 创建核心线程
              │ NO
              ▼
         等待队列未满? → 入队等待
              │ NO
              ▼
         最大线程未满? → 创建非核心线程
              │ NO
              ▼
         执行拒绝策略
```

> ⚠️ **避坑**：不要使用 `Executors.newFixedThreadPool()`（队列无界）或 `newCachedThreadPool()`（线程数无限）

---

### 模块二：Spring 框架深度解析

#### 2.1 自定义 Spring Boot Starter

**自动配置原理**：
```
@SpringBootApplication
  └─ @EnableAutoConfiguration
       └─ AutoConfigurationImportSelector
            └─ 读取 META-INF/spring/
               org.springframework.boot.autoconfigure.AutoConfiguration.imports
                    └─ 按 @ConditionalOn* 条件判断是否注册 Bean
```

**关键文件**：[HelloAutoConfiguration.java](hello-spring-boot-starter/src/main/java/com/javademo/starter/HelloAutoConfiguration.java)

#### 2.2 全局异常处理 + 统一响应

所有接口统一返回：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

参数校验失败自动响应：
```json
{
  "code": 400,
  "message": "参数校验失败: 用户名不能为空; 邮箱格式不正确"
}
```

**代码示例**：
- [ApiResponse.java](module-02-spring-framework/src/main/java/com/javademo/framework/mvc/ApiResponse.java)
- [GlobalExceptionHandler.java](module-02-spring-framework/src/main/java/com/javademo/framework/mvc/GlobalExceptionHandler.java)

#### 2.3 MyBatis-Plus 多租户插件

```sql
-- 原始 SQL（业务代码写法）
SELECT * FROM orders WHERE status = 1

-- MyBatis-Plus 自动拼接为（无需修改 Mapper）
SELECT * FROM orders WHERE status = 1 AND tenant_id = 100
```

**代码示例**：[TenantLineHandlerImpl.java](module-02-spring-framework/src/main/java/com/javademo/framework/mybatis/TenantLineHandlerImpl.java)

#### 2.4 Spring Security + JWT + RBAC

```
登录流程：
POST /api/auth/login
  └─ AuthenticationManager.authenticate()
       └─ UserDetailsService.loadUserByUsername()
            └─ BCrypt 密码校验
                 └─ 生成 JWT（包含 userId, roles）
                      └─ 返回 token

后续请求：
Authorization: Bearer <token>
  └─ JwtAuthenticationFilter 解析 JWT
       └─ 注入 SecurityContext
            └─ @PreAuthorize("hasRole('ADMIN')") 方法级鉴权
```

**代码示例**：
- [SecurityConfig.java](module-02-spring-framework/src/main/java/com/javademo/framework/security/config/SecurityConfig.java)
- [JwtTokenProvider.java](module-02-spring-framework/src/main/java/com/javademo/framework/security/service/JwtTokenProvider.java)

---

### 模块三：微服务组件实战

#### 3.1 Spring Cloud Alibaba 组件矩阵

| 组件 | 作用 | 类比 |
|------|------|------|
| **Nacos** | 服务注册/发现 + 配置中心 | Eureka + Config |
| **Sentinel** | 熔断限流 | Hystrix + RateLimiter |
| **Gateway** | API 网关 | Zuul |
| **Seata** | 分布式事务 | - |
| **RocketMQ** | 消息队列 | Kafka |

#### 3.2 Seata AT 模式分布式事务

```
两阶段提交：
阶段1：各服务本地提交（保存 undo_log）
阶段2：全部成功 → 提交；任一失败 → 根据 undo_log 回滚
```

**代码示例**：[OrderSeataService.java](module-03-microservices/src/main/java/com/javademo/microservices/seata/OrderSeataService.java)

#### 3.3 RocketMQ 事务消息（最终一致性）

```
发送方 → 半事务消息 → Broker
        ↓
     本地事务
        ↓
     COMMIT/ROLLBACK → Broker → 消费方（幂等处理）
```

**代码示例**：[OrderMessageService.java](module-03-microservices/src/main/java/com/javademo/microservices/mq/OrderMessageService.java)

---

## 🐳 Docker 部署

### 构建应用镜像

```bash
# 多阶段构建（Maven 构建 + JRE 运行）
docker build -f docker/Dockerfile -t java-demo:1.0 .

# 运行
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://mysql:3306/java_demo" \
  java-demo:1.0
```

### Docker Compose 一键启动

```bash
cd docker
docker compose up -d

# 服务访问地址
# Nacos:    http://localhost:8848/nacos  (nacos/nacos)
# 应用:     http://localhost:8080
# H2控制台: http://localhost:8080/h2-console
```

---

## ⚠️ 常见避坑指南

### JDK/JVM
- Record 不能作为 JPA 实体（无 setter / 无参构造器）
- 虚拟线程不适合 CPU 密集型任务
- `synchronized` 会 pin 住虚拟线程的载体线程，改用 `ReentrantLock`
- OOM 必须配置自动 heap dump：`-XX:+HeapDumpOnOutOfMemoryError`

### Spring Boot
- Starter 不要使用 `@ComponentScan`
- Spring Boot 3.x 自动配置 SPI 文件路径变更（不再用 `spring.factories`）

### MyBatis-Plus
- 多租户插件必须在分页插件**之前**注册
- ThreadLocal 使用完必须调用 `remove()`，防止线程池中的租户信息泄漏

### Spring Security
- 使用 `BCryptPasswordEncoder`，不要用 `NoOpPasswordEncoder`
- JWT secret 不要硬编码，从配置中心读取

### 微服务
- Seata 每个参与库必须建 `undo_log` 表
- RocketMQ 消费端必须实现幂等性
- Gateway 不能引入 `spring-boot-starter-web`（响应式与阻塞式冲突）

---

## 📊 技术对比

### MyBatis-Plus vs Spring Data JPA

| 维度 | MyBatis-Plus | Spring Data JPA |
|------|-------------|-----------------|
| **SQL 控制** | 完全掌控 SQL | 自动生成，复杂查询需 JPQL/原生 SQL |
| **学习曲线** | 低（会 MyBatis 即可） | 中（需了解 JPA/Hibernate 特性） |
| **性能调优** | 容易（直接优化 SQL） | 较难（Hibernate 生成 SQL 不透明） |
| **适用场景** | 复杂 SQL、大数据量、强控制需求 | CRUD 为主、快速开发 |
| **国内使用** | ⭐⭐⭐⭐⭐ 主流 | ⭐⭐⭐ |

### 分布式事务方案对比

| 方案 | 一致性 | 性能 | 侵入性 | 适用场景 |
|------|-------|------|--------|---------|
| **Seata AT** | 强一致 | 中 | 低 | 中等并发，数据库操作 |
| **Seata TCC** | 强一致 | 高 | 高（需写 try/confirm/cancel） | 高并发 |
| **RocketMQ 事务消息** | 最终一致 | 最高 | 中 | 异步场景，允许短暂不一致 |
| **本地消息表** | 最终一致 | 高 | 中 | 无 MQ 环境 |