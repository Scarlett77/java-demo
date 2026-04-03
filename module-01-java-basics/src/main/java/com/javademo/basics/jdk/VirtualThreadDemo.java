package com.javademo.basics.jdk;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 虚拟线程 (Virtual Threads) 演示 - JDK 21 正式发布
 *
 * <p>技术点：Project Loom 虚拟线程
 * <p>场景背景：传统平台线程（1个线程 ≈ 1MB OS线程栈），高并发场景下线程数受限。
 *            虚拟线程由 JVM 调度，栈极小（KB级），可创建数百万个，非常适合 I/O 密集型场景
 *            （如 Web API、数据库查询、RPC调用）。
 *
 * <p>核心原理：
 *   虚拟线程运行在"载体线程"（平台线程）之上，当虚拟线程执行阻塞 I/O 时，JVM 自动
 *   将其从载体线程上卸载（unmount），让载体线程去执行其他虚拟线程，从而实现高吞吐。
 *   这与响应式编程（Reactor/RxJava）在效果上类似，但代码风格是同步阻塞式，更易读写。
 *
 * <p>注意：虚拟线程 API（Thread.ofVirtual、Executors.newVirtualThreadPerTaskExecutor）
 *         在 JDK 21 中正式发布（GA）。本演示类在 JDK 17 环境中提供文字描述和等效平台线程
 *         演示；在 JDK 21+ 环境中，注释中的代码可直接使用。
 *
 * <p>避坑指南：
 *   1. 不要将虚拟线程与 ThreadLocal 大量数据绑定——虚拟线程数量极大，会导致内存膨胀。
 *   2. synchronized 块会"钉住"(pin)载体线程，降低并发效率。建议改用 ReentrantLock。
 *   3. 虚拟线程不适合 CPU 密集型任务（如图像处理、加解密），仍需平台线程池。
 *   4. Spring Boot 3.2+ 已内置虚拟线程支持，设置 spring.threads.virtual.enabled=true 即可。
 */
public class VirtualThreadDemo {

    /**
     * JDK 21 虚拟线程的 3 种创建方式（代码模板，需 JDK 21+ 运行）
     *
     * <pre>
     * // 方式1：Thread.ofVirtual().start()
     * Thread vt1 = Thread.ofVirtual()
     *         .name("vt-1")
     *         .start(() -> System.out.println("isVirtual: " + Thread.currentThread().isVirtual()));
     *
     * // 方式2：Thread.startVirtualThread()
     * Thread vt2 = Thread.startVirtualThread(
     *         () -> System.out.println("isVirtual: " + Thread.currentThread().isVirtual())
     * );
     *
     * // 方式3：Executors.newVirtualThreadPerTaskExecutor()（推荐，配合 try-with-resources）
     * try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
     *     executor.submit(() -> doTask());
     * }
     * </pre>
     */
    public static void printVirtualThreadCodeTemplate() {
        System.out.println("""
                === JDK 21 虚拟线程代码模板（需 JDK 21+ 才能运行）===
                
                // 方式1：Thread.ofVirtual()
                Thread vt = Thread.ofVirtual().name("vt-1")
                        .start(() -> System.out.println(Thread.currentThread().isVirtual())); // true
                
                // 方式2：Thread.startVirtualThread()（最简单）
                Thread.startVirtualThread(() -> doWork());
                
                // 方式3：推荐 - VirtualThreadPerTaskExecutor（配合 try-with-resources）
                try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                    for (int i = 0; i < 100_000; i++) {
                        executor.submit(() -> { /* I/O 密集型任务 */ });
                    }
                } // 自动等待所有任务完成
                
                // Spring Boot 3.2+ 一行开启虚拟线程：
                // application.yml: spring.threads.virtual.enabled=true
                """);
    }

    /**
     * 对比：平台线程 I/O 密集型场景演示（JDK 17 兼容版）
     * 在 JDK 21 中可将此方法替换为 Executors.newVirtualThreadPerTaskExecutor()
     */
    public static void platformThreadDemo() throws InterruptedException {
        int threadCount = 500; // JDK 17 平台线程演示，数量不宜过大
        long start = System.currentTimeMillis();
        AtomicInteger counter = new AtomicInteger(0);
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                counter.incrementAndGet();
            }, "platform-thread-" + idx);
            threads[i].start();
        }
        for (Thread t : threads) { t.join(); }

        System.out.printf("[平台线程] %d 个线程，各 sleep 50ms，总耗时: %d ms%n",
                threadCount, System.currentTimeMillis() - start);
        System.out.println("  对应 JDK 21 虚拟线程版本，100万个任务耗时仍仅约 50ms");
    }

    /**
     * 锁最佳实践：虚拟线程应使用 ReentrantLock 而非 synchronized
     *
     * <p>原因：JDK 21 中 synchronized 会"钉住"(pin)载体线程，使其无法被其他虚拟线程复用。
     */
    public static void lockBestPractice() throws InterruptedException {
        var lock = new java.util.concurrent.locks.ReentrantLock();
        Thread thread = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("[最佳实践] 使用 ReentrantLock（而非 synchronized）保证虚拟线程可正常卸载");
                try { TimeUnit.MILLISECONDS.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            } finally {
                lock.unlock();
            }
        });
        thread.start();
        thread.join();
    }

    /**
     * 打印虚拟线程 vs 平台线程特性对比
     */
    public static void printComparison() {
        System.out.println("""
                === 虚拟线程 vs 平台线程 对比 ===
                
                维度          平台线程                 虚拟线程 (JDK 21)
                内存占用      ~1MB/个（OS线程栈）        ~KB级别（JVM管理）
                最大数量      通常 <10,000              可达百万级
                创建开销      大（需OS系统调用）          极小（JVM内部）
                调度方式      OS 抢占式调度              JVM 协作式调度
                适用场景      CPU 密集型                I/O 密集型
                代码风格      同步阻塞                  同步阻塞（无需async/await）
                
                性能对比（1万个任务，各 sleep 100ms）：
                  平台线程池(100线程): ~10,000ms
                  虚拟线程(JDK 21):    ~100ms（快 ~100x）
                """);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("当前 JDK 版本: " + Runtime.version());
        printComparison();
        printVirtualThreadCodeTemplate();

        System.out.println("=== ReentrantLock 最佳实践 ===");
        lockBestPractice();

        System.out.println("\n=== 平台线程 I/O 密集型场景演示（JDK 17 兼容版）===");
        platformThreadDemo();
    }
}
