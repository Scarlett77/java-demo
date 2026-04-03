package com.javademo.basics.juc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JUC 综合应用：自定义线程池拒绝策略
 *
 * <p>技术点：ThreadPoolExecutor 自定义拒绝策略
 * <p>场景背景：默认的 AbortPolicy 直接抛出 RejectedExecutionException，
 *            在高并发场景下可能导致请求丢失；CallerRunsPolicy 会让提交线程执行任务，
 *            影响主线程性能。企业级场景需要根据业务定制拒绝策略（如降级、入队列、报警）。
 *
 * <p>核心原理（线程池工作流程）：
 * <pre>
 * 提交任务
 *    │
 *    ▼
 * 核心线程数(corePoolSize)未满? ──YES──▶ 创建核心线程执行
 *    │ NO
 *    ▼
 * 队列(workQueue)未满? ──────────YES──▶ 任务入队等待
 *    │ NO
 *    ▼
 * 最大线程数(maximumPoolSize)未满? ──YES──▶ 创建非核心线程执行
 *    │ NO
 *    ▼
 * 执行拒绝策略(RejectedExecutionHandler)
 * </pre>
 *
 * <p>四种内置拒绝策略：
 *   - AbortPolicy（默认）：抛出 RejectedExecutionException
 *   - CallerRunsPolicy：由调用线程执行任务（降速反压）
 *   - DiscardPolicy：静默丢弃
 *   - DiscardOldestPolicy：丢弃队列最旧任务，重新提交
 *
 * <p>避坑指南：
 *   1. 不要使用 Executors.newFixedThreadPool()（队列无界，可能 OOM）。
 *   2. 不要使用 Executors.newCachedThreadPool()（最大线程数 Integer.MAX_VALUE，可能创建大量线程）。
 *   3. 线程池参数要结合业务场景：CPU密集型用 N+1，IO密集型用 2N（N为CPU核数）。
 *   4. 务必为线程命名（ThreadFactory），方便日志排查。
 *   5. 线程池要优雅关闭：先 shutdown()，再 awaitTermination()，最后 shutdownNow()。
 */
public class CustomThreadPoolDemo {

    /**
     * 自定义拒绝策略：任务降级 + 告警日志
     * 生产场景：将被拒绝的任务写入降级队列（如数据库/Redis），后续补偿执行
     */
    static class AlertAndFallbackPolicy implements RejectedExecutionHandler {
        private final AtomicInteger rejectedCount = new AtomicInteger(0);
        // 降级队列（生产环境可替换为 Redis List 或 DB 持久化）
        private final BlockingQueue<Runnable> fallbackQueue = new LinkedBlockingQueue<>(100);

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            int count = rejectedCount.incrementAndGet();
            System.err.printf("[拒绝策略] 任务被拒绝！累计拒绝次数: %d，线程池状态: active=%d, queue=%d%n",
                    count, executor.getActiveCount(), executor.getQueue().size());

            // 尝试写入降级队列
            boolean offered = fallbackQueue.offer(r);
            if (offered) {
                System.err.println("[拒绝策略] 任务已写入降级队列，等待补偿执行");
            } else {
                System.err.println("[拒绝策略] 降级队列已满！任务彻底丢弃，触发告警！");
                // TODO: 生产环境在此处接入 Prometheus 指标上报 / 钉钉/企微告警
            }
        }

        public int getRejectedCount() { return rejectedCount.get(); }
        public BlockingQueue<Runnable> getFallbackQueue() { return fallbackQueue; }
    }

    /**
     * 自定义线程工厂：给线程命名，方便在 jstack/日志中快速定位
     */
    static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(String poolName) {
            this.namePrefix = poolName + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            // 统一设置未捕获异常处理器
            t.setUncaughtExceptionHandler((thread, ex) ->
                    System.err.printf("[线程异常] 线程 %s 发生未捕获异常: %s%n", thread.getName(), ex.getMessage())
            );
            return t;
        }
    }

    /**
     * 构建企业级推荐线程池
     *
     * @param poolName  线程池名称（用于日志和监控）
     * @param coreSize  核心线程数（IO密集型：2*CPU核数；CPU密集型：CPU核数+1）
     * @param maxSize   最大线程数
     * @param queueSize 等待队列大小（有界队列，防止 OOM）
     */
    public static ThreadPoolExecutor buildThreadPool(String poolName, int coreSize, int maxSize, int queueSize) {
        AlertAndFallbackPolicy rejectionPolicy = new AlertAndFallbackPolicy();
        return new ThreadPoolExecutor(
                coreSize,
                maxSize,
                60L, TimeUnit.SECONDS,          // 非核心线程空闲超时时间
                new ArrayBlockingQueue<>(queueSize), // 有界队列，防止 OOM
                new NamedThreadFactory(poolName),
                rejectionPolicy
        );
    }

    public static void main(String[] args) throws InterruptedException {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        System.out.printf("CPU 核数: %d%n", cpuCores);

        // 创建一个核心2线程、最大4线程、队列容量5的小线程池（便于演示拒绝）
        ThreadPoolExecutor pool = buildThreadPool("order-executor", 2, 4, 5);

        System.out.println("\n=== 提交12个任务（核心2 + 队列5 + 非核心2 = 最多9个，3个会被拒绝）===");
        for (int i = 1; i <= 12; i++) {
            final int taskId = i;
            try {
                pool.execute(() -> {
                    System.out.printf("[执行] 任务 %d 由 %s 执行%n", taskId, Thread.currentThread().getName());
                    try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                });
                System.out.printf("[提交] 任务 %d 已提交%n", taskId);
            } catch (RejectedExecutionException e) {
                // 自定义策略下不会抛异常（降级处理），此处仅演示
                System.err.printf("[提交失败] 任务 %d 被拒绝: %s%n", taskId, e.getMessage());
            }
        }

        // 优雅关闭线程池
        pool.shutdown();
        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            System.err.println("线程池未在超时内完成，强制关闭");
            pool.shutdownNow();
        }
        System.out.println("\n线程池已关闭");
    }
}
