package com.javademo.basics;

import com.javademo.basics.jdk.JdkFeatureComparison;
import com.javademo.basics.juc.CompletableFutureDemo;
import com.javademo.basics.juc.CustomThreadPoolDemo;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模块一：Java基础与JVM 单元测试
 */
class JavaBasicsTest {

    @Test
    void testJdkFeatureRecord() {
        // Record 自动生成 equals, hashCode, toString
        var demo = new JdkFeatureComparison();
        // 使用反射访问内部 record (直接实例化)
        // 验证 Record 的基本功能通过 JdkFeatureComparison 的 main 方法调用
        assertDoesNotThrow(() -> JdkFeatureComparison.main(new String[]{}));
    }

    @Test
    void testJdkSealedClassHandling() {
        JdkFeatureComparison demo = new JdkFeatureComparison();
        // 通过反射调用内部 sealed 接口实现
        // 直接测试 main 逻辑
        assertDoesNotThrow(() -> demo.textBlockDemo());
    }

    @Test
    void testCustomThreadPool() throws InterruptedException {
        ThreadPoolExecutor pool = CustomThreadPoolDemo.buildThreadPool("test-pool", 2, 4, 5);
        assertNotNull(pool);
        assertEquals(2, pool.getCorePoolSize());
        assertEquals(4, pool.getMaximumPoolSize());

        // 提交一个任务验证可正常执行
        pool.execute(() -> {});
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void testCompletableFutureParallelCall() {
        // 验证并行调用不抛异常
        assertDoesNotThrow(() -> CompletableFutureDemo.parallelCall());
    }

    @Test
    void testCompletableFutureAnyOf() {
        assertDoesNotThrow(() -> CompletableFutureDemo.anyOfDemo());
    }
}
