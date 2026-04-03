# 多阶段构建（Multi-Stage Build）
# 阶段1：Maven 构建
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# 先复制 pom 文件，利用 Docker 层缓存，依赖不变时不重新下载
COPY pom.xml .
COPY module-02-spring-framework/pom.xml module-02-spring-framework/
COPY hello-spring-boot-starter/pom.xml hello-spring-boot-starter/

# 预下载依赖（缓存优化）
RUN mvn dependency:go-offline -pl module-02-spring-framework,hello-spring-boot-starter -am -q

# 复制源码并构建
COPY . .
RUN mvn clean package -pl hello-spring-boot-starter,module-02-spring-framework \
    -am -DskipTests -q

# 阶段2：运行时镜像（体积更小）
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 创建非 root 用户（安全最佳实践）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# 从构建阶段复制 jar
COPY --from=builder /app/module-02-spring-framework/target/*.jar app.jar

# JVM 参数优化
# -XX:+UseContainerSupport: 感知容器的 CPU/内存限制（JDK 10+）
# -XX:MaxRAMPercentage=75.0: 使用容器内存的75%作为堆上限
# -XX:+HeapDumpOnOutOfMemoryError: OOM 时自动 dump
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/tmp/heap.hprof \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
