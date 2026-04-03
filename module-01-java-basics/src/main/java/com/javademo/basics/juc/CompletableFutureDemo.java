package com.javademo.basics.juc;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * CompletableFuture 异步编排综合演示
 *
 * <p>技术点：CompletableFuture 异步任务编排
 * <p>场景背景：电商系统下单场景，需要并行调用多个远程服务（商品库存、用户优惠券、物流报价），
 *            然后汇总结果。使用同步串行调用耗时是各服务之和；使用 CompletableFuture 并行
 *            调用，总耗时仅为最慢服务的耗时，性能提升显著。
 *
 * <p>核心原理：
 *   CompletableFuture 实现了 Future 和 CompletionStage 接口，支持链式调用和多任务编排。
 *   - thenApply/thenAccept/thenRun：同步回调（在完成该步骤的线程中执行）
 *   - thenApplyAsync/thenAcceptAsync：异步回调（在 ForkJoinPool 或自定义线程池中执行）
 *   - thenCombine/allOf/anyOf：多任务聚合
 *
 * <p>避坑指南：
 *   1. 不指定线程池时，CompletableFuture 使用 ForkJoinPool.commonPool()，
 *      该线程池为守护线程，主线程退出时任务可能未完成。生产中务必指定业务线程池。
 *   2. exceptionally/handle 用于异常处理，不要让异常静默消失。
 *   3. join() 会抛出 CompletionException（unchecked），get() 抛 ExecutionException（checked）。
 *   4. allOf() 返回 CompletableFuture<Void>，结果需从各子 Future 中单独获取。
 *   5. 避免在 CompletableFuture 回调中执行耗时操作，应使用 Async 变体并指定线程池。
 */
public class CompletableFutureDemo {

    // 模拟远程服务调用的线程池（生产中使用独立线程池，避免与主业务混用）
    private static final ExecutorService RPC_POOL = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2,
            r -> {
                Thread t = new Thread(r, "rpc-pool-" + new AtomicInteger().getAndIncrement());
                t.setDaemon(true);
                return t;
            }
    );

    // =========================================================
    // 模拟远程服务（每个服务都有网络延迟）
    // =========================================================

    /** 查询商品库存（耗时 200ms） */
    private static CompletableFuture<Integer> queryStock(Long skuId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(200);
            System.out.printf("  [库存服务] 查询 SKU-%d 库存完成%n", skuId);
            return 50; // 模拟库存50件
        }, RPC_POOL);
    }

    /** 查询用户优惠券（耗时 300ms） */
    private static CompletableFuture<Double> queryCoupon(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(300);
            System.out.printf("  [优惠券服务] 查询用户 %d 优惠券完成%n", userId);
            return 20.0; // 模拟优惠20元
        }, RPC_POOL);
    }

    /** 查询物流报价（耗时 150ms） */
    private static CompletableFuture<Double> queryShipping(Long cityId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(150);
            System.out.printf("  [物流服务] 查询城市 %d 物流报价完成%n", cityId);
            return 12.0; // 模拟运费12元
        }, RPC_POOL);
    }

    // =========================================================
    // 演示1：串行调用 vs 并行调用 性能对比
    // =========================================================

    /** 串行调用（耗时 = 200 + 300 + 150 = 650ms） */
    public static void serialCall() throws Exception {
        long start = System.currentTimeMillis();
        int stock = queryStock(1001L).get();
        double coupon = queryCoupon(2001L).get();
        double shipping = queryShipping(110L).get();
        System.out.printf("串行结果: 库存=%d, 优惠券=%.1f, 运费=%.1f, 耗时=%dms%n",
                stock, coupon, shipping, System.currentTimeMillis() - start);
    }

    /** 并行调用（耗时 ≈ max(200, 300, 150) = 300ms） */
    public static void parallelCall() throws Exception {
        long start = System.currentTimeMillis();
        // 同时发起三个异步调用
        CompletableFuture<Integer> stockFuture = queryStock(1001L);
        CompletableFuture<Double> couponFuture = queryCoupon(2001L);
        CompletableFuture<Double> shippingFuture = queryShipping(110L);

        // 等待全部完成
        CompletableFuture.allOf(stockFuture, couponFuture, shippingFuture).join();

        System.out.printf("并行结果: 库存=%d, 优惠券=%.1f, 运费=%.1f, 耗时=%dms%n",
                stockFuture.get(), couponFuture.get(), shippingFuture.get(),
                System.currentTimeMillis() - start);
    }

    // =========================================================
    // 演示2：链式编排（thenCompose / thenCombine）
    // =========================================================

    /**
     * 下单流程：查库存 → 校验通过后 → 并行 [扣库存 + 创建订单] → 通知物流
     */
    public static void orderFlow() {
        System.out.println("\n=== 下单流程编排 ===");
        long start = System.currentTimeMillis();

        CompletableFuture<String> orderResult = queryStock(1001L)
                // 库存校验（同步，在完成库存查询的线程中执行）
                .thenApply(stock -> {
                    if (stock <= 0) throw new RuntimeException("库存不足");
                    System.out.println("  [校验] 库存充足，开始创建订单...");
                    return stock;
                })
                // 并行扣库存 + 创建订单（使用 thenComposeAsync 异步）
                .thenComposeAsync(stock -> {
                    CompletableFuture<String> deductStock = CompletableFuture.supplyAsync(() -> {
                        sleep(100);
                        return "扣库存成功";
                    }, RPC_POOL);
                    CompletableFuture<String> createOrder = CompletableFuture.supplyAsync(() -> {
                        sleep(200);
                        return "订单ORD-" + System.currentTimeMillis();
                    }, RPC_POOL);
                    // 合并两个结果
                    return deductStock.thenCombine(createOrder, (deduct, orderId) ->
                            deduct + ", " + orderId);
                }, RPC_POOL)
                // 通知物流（依赖订单创建结果）
                .thenApplyAsync(orderInfo -> {
                    sleep(80);
                    System.out.println("  [物流] 通知物流发货，订单信息: " + orderInfo);
                    return "下单成功：" + orderInfo;
                }, RPC_POOL)
                // 异常兜底处理
                .exceptionally(ex -> {
                    System.err.println("  [异常] 下单失败: " + ex.getMessage());
                    return "下单失败";
                });

        System.out.println("最终结果: " + orderResult.join());
        System.out.printf("下单流程总耗时: %dms%n", System.currentTimeMillis() - start);
    }

    // =========================================================
    // 演示3：anyOf 竞速（取最快返回的结果）
    // =========================================================

    /**
     * 多数据源读取，取最快返回的结果（如多个缓存节点）
     */
    public static void anyOfDemo() throws Exception {
        System.out.println("\n=== anyOf 竞速（取最快的缓存节点）===");
        List<CompletableFuture<String>> futures = List.of(
                CompletableFuture.supplyAsync(() -> { sleep(300); return "节点A的数据"; }, RPC_POOL),
                CompletableFuture.supplyAsync(() -> { sleep(100); return "节点B的数据"; }, RPC_POOL),
                CompletableFuture.supplyAsync(() -> { sleep(200); return "节点C的数据"; }, RPC_POOL)
        );

        // anyOf 返回最先完成的任务结果
        Object result = CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0])).get();
        System.out.println("最快结果: " + result);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 串行 vs 并行 性能对比 ===");
        serialCall();
        parallelCall();

        orderFlow();
        anyOfDemo();

        RPC_POOL.shutdown();
    }
}
