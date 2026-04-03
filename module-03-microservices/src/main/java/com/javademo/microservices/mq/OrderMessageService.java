package com.javademo.microservices.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * RocketMQ 最终一致性消息投递模板
 *
 * <p>技术点：RocketMQ 事务消息 + 最终一致性
 * <p>场景背景：分布式系统中，某些操作无法使用强一致性事务，
 *            通过"事务消息"实现最终一致性（BASE 理论）。
 *            典型场景：下单后异步通知积分系统、支付完成后通知物流发货。
 *
 * <p>RocketMQ 事务消息核心原理（两阶段 + 回查）：
 * <pre>
 * Producer（发送方）                    Broker（RocketMQ）        Consumer（消费方）
 *    │                                      │                         │
 *    │── 1. 发送半事务消息（Half Message） ──▶│                         │
 *    │◀── 2. Broker 确认 ──────────────────│                         │
 *    │                                      │                         │
 *    │── 3. 执行本地事务 ───────────────────▶│                         │
 *    │                                      │                         │
 *    │── 4a. COMMIT（本地事务成功）─────────▶│── 5. 投递消息 ──────────▶│
 *    │── 4b. ROLLBACK（本地事务失败）────────▶│  （消息对消费方可见）      │
 *    │── 4c. UNKNOWN（网络异常等）           │                         │
 *    │                                      │── 6. 回查本地事务状态 ──▶│
 *    │◀─ 7. 回查结果 ────────────────────────│                         │
 *
 * 最终一致性保证：
 *   - 消费方需要幂等处理（相同 Message ID 的消息只处理一次）
 *   - 消费失败会重试（默认重试16次，最终进入死信队列）
 * </pre>
 *
 * <p>使用方式（需引入 rocketmq-spring-boot-starter）：
 * <pre>
 * // pom.xml 依赖（版本需与 RocketMQ 服务端匹配）
 * &lt;dependency&gt;
 *   &lt;groupId&gt;org.apache.rocketmq&lt;/groupId&gt;
 *   &lt;artifactId&gt;rocketmq-spring-boot-starter&lt;/artifactId&gt;
 *   &lt;version&gt;2.3.0&lt;/version&gt;
 * &lt;/dependency&gt;
 * </pre>
 *
 * <p>避坑指南：
 *   1. 消费端必须实现幂等性（数据库唯一键 / Redis SETNX），防止重复消费。
 *   2. 消息不要过大（RocketMQ 默认限制 4MB），大文件通过 OSS 传 URL。
 *   3. 事务回查方法不要依赖消息内容，应查询本地数据库的业务状态。
 *   4. 死信队列（DLQ）中的消息需人工或定时任务处理，不能丢失。
 *   5. 消费者的 consumerGroup 必须唯一，同一 group 的消费者是竞争关系。
 */
@Service
public class OrderMessageService {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageService.class);

    // 实际项目中注入 RocketMQTemplate
    // @Autowired RocketMQTemplate rocketMQTemplate;

    /**
     * 下单成功后发送事务消息，异步通知积分系统
     * 使用事务消息保证：本地订单创建 与 积分消息发送 的最终一致性
     *
     * <p>实际实现中，此方法使用 @GlobalTransactional 或本地事务，
     * 通过 RocketMQTemplate.sendMessageInTransaction() 发送事务消息
     *
     * @param orderId 订单ID
     * @param userId  用户ID
     * @param amount  订单金额
     */
    public void sendOrderCreatedMessage(String orderId, Long userId, Double amount) {
        log.info("[MQ] 准备发送下单事务消息，orderId={}, userId={}", orderId, userId);

        // 实际代码示例（需要 RocketMQTemplate 注入）：
        // OrderCreatedEvent event = new OrderCreatedEvent(orderId, userId, amount);
        // Message<OrderCreatedEvent> message = MessageBuilder
        //     .withPayload(event)
        //     .setHeader(RocketMQHeaders.TRANSACTION_ID, UUID.randomUUID().toString())
        //     .build();
        //
        // // 发送事务消息（half message + 本地事务执行器）
        // rocketMQTemplate.sendMessageInTransaction(
        //     "order-topic:tag-created",  // topic:tag
        //     message,
        //     null  // 传给事务监听器的参数
        // );

        // 模拟演示
        log.info("[MQ] 事务消息已发送，消息内容: orderId={}, userId={}, amount={}", orderId, userId, amount);
    }

    /**
     * 消费积分消息（消费端实现幂等）
     *
     * <p>实际实现中添加注解：
     * @RocketMQMessageListener(topic = "order-topic", consumerGroup = "points-consumer-group")
     * public class PointsMessageListener implements RocketMQListener<OrderCreatedEvent>
     *
     * @param orderId 订单ID（幂等键）
     * @param userId  用户ID
     * @param amount  订单金额
     */
    public void consumeAndAddPoints(String orderId, Long userId, Double amount) {
        // 幂等性检查：查询该 orderId 是否已处理（Redis SETNX 或 DB 唯一键）
        boolean alreadyProcessed = checkIfProcessed(orderId);
        if (alreadyProcessed) {
            log.warn("[积分消费] 消息重复，已处理过 orderId={}, 跳过", orderId);
            return;
        }

        // 业务处理：计算并增加积分
        int points = (int) (amount * 10); // 每消费1元获得10积分
        log.info("[积分消费] 用户 {} 下单 {}，增加积分 {}", userId, orderId, points);
        // pointsService.addPoints(userId, points, orderId);

        // 标记为已处理（防止重复消费）
        markAsProcessed(orderId);
    }

    private boolean checkIfProcessed(String orderId) {
        // 实际实现：查询 Redis 或数据库幂等表
        return false;
    }

    private void markAsProcessed(String orderId) {
        // 实际实现：Redis SET orderId "1" EX 86400 / 或插入幂等表
        log.debug("[幂等] 标记 orderId={} 已处理", orderId);
    }

    /**
     * 订单消息事件 DTO
     */
    public record OrderCreatedEvent(String orderId, Long userId, Double amount) {}
}
