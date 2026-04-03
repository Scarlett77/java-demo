package com.javademo.microservices.seata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seata 分布式事务演示（AT 模式）
 *
 * <p>技术点：Seata AT 模式分布式事务
 * <p>场景背景：电商下单场景跨越多个微服务：订单服务、库存服务、账户服务。
 *            三个服务使用各自的数据库，无法用本地事务保证一致性，需要分布式事务。
 *
 * <p>核心原理（AT 模式两阶段提交）：
 * <pre>
 * 阶段一（执行）：
 *   TM 开启全局事务（向 TC 注册，获取 XID）
 *     │
 *     ├─▶ 订单服务：插入订单（解析SQL→保存 before-image → 执行 → 保存 after-image → 注册分支）
 *     ├─▶ 库存服务：扣减库存（同上）
 *     └─▶ 账户服务：扣减余额（同上）
 *         所有分支本地事务提交（一阶段结束）
 *
 * 阶段二（提交/回滚）：
 *   全部成功 → TC 通知各 RM 提交（清理 undo_log）
 *   任一失败 → TC 通知各 RM 回滚（根据 before-image 反向 SQL 恢复数据）
 *
 * 关键表：undo_log（每个参与服务的数据库都需要此表）
 * </pre>
 *
 * <p>Seata 角色：
 *   TC（Transaction Coordinator）：Seata Server，协调全局事务
 *   TM（Transaction Manager）：发起全局事务的服务（@GlobalTransactional 标注处）
 *   RM（Resource Manager）：参与全局事务的各数据库资源
 *
 * <p>前置准备：
 *   1. 启动 Seata Server：docker run -d -p 8091:8091 seataio/seata-server:latest
 *   2. 每个参与服务的数据库执行建表：undo_log 表（见 seata-undo-log.sql）
 *   3. application.yml 配置 seata.tx-service-group 和 Nacos 注册
 *
 * <p>避坑指南：
 *   1. AT 模式依赖数据库 undo_log 表，务必在所有参与库中创建该表。
 *   2. @GlobalTransactional 方法内的异常不要被 try-catch 吞掉，否则 Seata 无法感知到回滚。
 *   3. AT 模式存在"脏写"问题：非 Seata 管理的业务可能在一阶段提交和二阶段之间读到中间状态。
 *      可用 Seata 的全局锁（select for update）避免脏写。
 *   4. 性能：AT 模式比本地事务多 1-2 次网络往返，适合中等并发场景。
 *      高并发场景考虑 TCC 模式（手动编写 try/confirm/cancel 逻辑）。
 */
@Service
public class OrderSeataService {

    private static final Logger log = LoggerFactory.getLogger(OrderSeataService.class);

    // 在实际项目中注入 OrderMapper、InventoryFeignClient、AccountFeignClient
    // @Autowired OrderMapper orderMapper;
    // @Autowired InventoryFeignClient inventoryClient;
    // @Autowired AccountFeignClient accountClient;

    /**
     * 下单方法：跨服务分布式事务
     *
     * <p>@GlobalTransactional 由 Seata 提供，开启全局事务（TM 角色）
     * <p>@Transactional 为本地事务，两者可以组合使用
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @param count     购买数量
     * @param money     订单金额
     */
    // 实际项目中添加 @GlobalTransactional(rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Long userId, Long productId, Integer count, Double money) {
        log.info("[Seata全局事务] 开始下单，userId={}, productId={}, count={}, money={}",
                userId, productId, count, money);

        // 步骤1：创建订单（本服务本地数据库操作）
        createLocalOrder(userId, productId, count, money);

        // 步骤2：调用库存服务（远程调用，Seata RM 自动注册分支事务）
        deductInventory(productId, count);

        // 步骤3：调用账户服务（远程调用）
        deductAccount(userId, money);

        log.info("[Seata全局事务] 下单成功");
    }

    private void createLocalOrder(Long userId, Long productId, Integer count, Double money) {
        // orderMapper.insertOrder(...)
        log.info("  [订单服务] 创建订单记录");
    }

    private void deductInventory(Long productId, Integer count) {
        // inventoryClient.deduct(productId, count); // OpenFeign 调用
        log.info("  [库存服务] 扣减库存 productId={}, count={}", productId, count);
        // 模拟库存服务偶发异常触发全局回滚
        if (count > 100) {
            throw new RuntimeException("库存不足，触发全局事务回滚");
        }
    }

    private void deductAccount(Long userId, Double money) {
        // accountClient.deduct(userId, money); // OpenFeign 调用
        log.info("  [账户服务] 扣减余额 userId={}, money={}", userId, money);
    }
}
