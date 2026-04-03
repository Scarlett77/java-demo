package com.javademo.basics.jvm;

import java.util.ArrayList;
import java.util.List;

/**
 * JVM 内存溢出（OOM）模拟与分析指南
 *
 * <p>技术点：JVM 内存溢出模拟与诊断
 * <p>场景背景：生产环境中最常见的 JVM 故障之一是 OutOfMemoryError，定位困难、影响严重。
 *            通过模拟典型 OOM 场景，结合 VisualVM/Arthas 工具，学会快速定位内存泄漏。
 *
 * <p>核心原理（JVM 内存区域）：
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                         JVM 内存结构                             │
 * ├────────────────┬────────────────────┬───────────────────────────┤
 * │   堆 (Heap)    │  方法区/元空间      │  JVM栈/本地方法栈/程序计数器 │
 * │ 对象实例存储   │ 类元数据/静态变量   │   线程私有                   │
 * │ GC 主要区域    │ OOM: Metaspace      │   栈溢出: StackOverflowError │
 * │ OOM: Java heap │                    │                              │
 * └────────────────┴────────────────────┴───────────────────────────┘
 * </pre>
 *
 * <p>诊断工具：
 * <pre>
 * 1. VisualVM（图形界面）：JDK 自带，监控堆内存、线程、CPU，可 Dump 堆快照（.hprof）
 *    启动：jvisualvm（或从 https://visualvm.github.io/ 下载最新版）
 *
 * 2. Arthas（阿里开源诊断工具）：
 *    curl -O https://arthas.aliyun.com/arthas-boot.jar
 *    java -jar arthas-boot.jar
 *    常用命令：
 *      dashboard     -- 查看线程、内存、GC 概览
 *      heapdump /tmp/heap.hprof  -- 导出堆快照
 *      memory        -- 查看内存区域使用
 *      thread -b     -- 找出死锁线程
 *      jad com.example.Demo  -- 反编译运行中的类
 *
 * 3. jmap / jstat / jstack（JDK 命令行工具）：
 *    jmap -histo:live <pid>  -- 查看对象数量统计
 *    jstat -gcutil <pid> 1000 -- 每秒打印 GC 情况
 *    jstack <pid>            -- 打印线程栈，定位死锁
 * </pre>
 *
 * <p>避坑指南：
 *   1. 生产环境务必加 JVM 参数：-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/
 *      这样 OOM 时会自动生成 hprof 文件，用 MAT（Eclipse Memory Analyzer）分析。
 *   2. 元空间 OOM（Metaspace）常见于动态生成类（如 CGLIB、Groovy 脚本），
 *      设置 -XX:MaxMetaspaceSize=256m 限制元空间上限。
 *   3. 直接内存 OOM：使用 NIO ByteBuffer.allocateDirect() 过多，
 *      通过 -XX:MaxDirectMemorySize=256m 限制。
 *   4. 使用内存分析工具时，先 jmap -histo 快速看大对象，再按需 dump 堆快照。
 */
public class OomSimulation {

    /**
     * 场景1：堆内存溢出 (java.lang.OutOfMemoryError: Java heap space)
     * 常见原因：大量对象无法被 GC 回收（如静态集合持有对象引用、缓存无上限增长）
     *
     * <p>运行方式（设置小堆便于快速复现）：
     * java -Xmx32m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/ \
     *      -cp target/classes com.javademo.basics.jvm.OomSimulation heap
     */
    public static void heapOom() {
        System.out.println("开始模拟堆内存溢出...");
        // 使用静态变量持有引用，模拟内存泄漏（GC 无法回收）
        List<byte[]> leakList = new ArrayList<>();
        try {
            while (true) {
                // 每次分配 1MB，持续累积
                leakList.add(new byte[1024 * 1024]);
                System.out.printf("已分配: %d MB%n", leakList.size());
            }
        } catch (OutOfMemoryError e) {
            System.err.println("OOM 发生！堆内存不足: " + e.getMessage());
            System.err.println("堆快照已生成（如开启了 -XX:+HeapDumpOnOutOfMemoryError）");
        }
    }

    /**
     * 场景2：栈溢出 (java.lang.StackOverflowError)
     * 常见原因：无限递归调用，每次方法调用都会在栈帧中分配空间
     *
     * <p>运行方式：
     * java -Xss256k -cp target/classes com.javademo.basics.jvm.OomSimulation stack
     */
    public static void stackOverflow() {
        System.out.println("开始模拟栈溢出...");
        try {
            recursiveMethod(0);
        } catch (StackOverflowError e) {
            System.err.println("栈溢出！递归调用层数过深");
            System.err.println("解决方案：将递归改为循环迭代，或增大 -Xss 参数");
        }
    }

    private static void recursiveMethod(int depth) {
        // 无终止条件的递归，模拟无限递归
        recursiveMethod(depth + 1);
    }

    /**
     * 场景3：元空间溢出 (java.lang.OutOfMemoryError: Metaspace)
     * 常见原因：动态生成过多类（如 CGLIB、字节码增强框架）
     *
     * <p>注意：模拟元空间溢出需要 -XX:MaxMetaspaceSize=32m 参数限制元空间
     * 本示例用于说明场景，不实际运行（避免影响JVM状态）
     */
    public static void describeMetaspaceOom() {
        System.out.println("元空间溢出常见场景：");
        System.out.println("1. 使用 CGLIB 动态代理，每次代理都生成新类（未复用 ClassLoader）");
        System.out.println("2. 使用 Groovy/BeanShell 动态脚本，脚本每次编译生成新类");
        System.out.println("3. 热部署场景下旧 ClassLoader 未被 GC（类加载器泄漏）");
        System.out.println("诊断命令: jstat -gcmetacapacity <pid>");
        System.out.println("解决方案: -XX:MaxMetaspaceSize=256m，并排查类加载器泄漏");
    }

    /**
     * JVM 关键调优参数说明
     */
    public static void printJvmTuningParams() {
        System.out.println("""
                === 常用 JVM 启动参数 ===
                
                # 堆内存
                -Xms512m                          # 初始堆大小（建议与 -Xmx 相同，避免动态扩容开销）
                -Xmx2g                            # 最大堆大小
                -XX:NewRatio=2                    # 老年代:新生代 = 2:1
                
                # 元空间
                -XX:MetaspaceSize=128m            # 元空间初始大小
                -XX:MaxMetaspaceSize=256m         # 元空间最大值（避免无限扩展）
                
                # GC 选择（JDK 17+ 默认 G1GC）
                -XX:+UseG1GC                      # G1 垃圾收集器（均衡吞吐与延迟）
                -XX:MaxGCPauseMillis=200          # 目标最大 STW 停顿时间
                -XX:+UseZGC                       # ZGC（低延迟，JDK 15+ 生产可用）
                
                # 故障诊断
                -XX:+HeapDumpOnOutOfMemoryError   # OOM 时自动 dump 堆
                -XX:HeapDumpPath=/tmp/heap.hprof  # heap dump 文件路径
                -XX:+PrintGCDetails               # 打印 GC 详情日志（JDK 9+ 改为 -Xlog:gc*）
                -Xlog:gc*:file=/tmp/gc.log        # JDK 9+ GC 日志
                
                # 线程
                -Xss512k                          # 每个线程栈大小（默认512k~1m）
                """);
    }

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "info";
        switch (mode) {
            case "heap"  -> heapOom();
            case "stack" -> stackOverflow();
            case "meta"  -> describeMetaspaceOom();
            default      -> {
                printJvmTuningParams();
                describeMetaspaceOom();
            }
        }
    }
}
