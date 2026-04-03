package com.javademo.basics.jdk;

import java.util.List;
import java.util.Optional;

/**
 * JDK 8 vs JDK 17/21 核心语法特性对比演示
 *
 * <p>技术点：新语法特性演进
 * <p>场景背景：企业升级JDK版本时，需要了解新特性以重构冗余代码、提升可读性。
 * <p>核心原理：JDK 17/21 引入了 Record、Sealed Classes、Pattern Matching、虚拟线程等特性，
 *            极大简化了 DTO/值对象、类型判断、并发模型的编写方式。
 *
 * <p>避坑指南：
 *   1. Record 类字段不可变，不能直接作为 JPA 实体（字段由构造器初始化，无 setter）。
 *   2. var 只能用于局部变量，不能用于方法参数或返回值类型。
 *   3. 虚拟线程适合 I/O 密集型，CPU 密集型任务仍需平台线程。
 *   4. Sealed Classes 的 permits 子类必须与父类在同一包或同一编译单元中。
 */
public class JdkFeatureComparison {

    // =========================================================
    // 特性一：var 局部变量类型推断 (JDK 10+)
    // =========================================================

    /**
     * JDK 8 写法：显式声明类型
     */
    public void jdk8VarStyle() {
        List<String> names = List.of("Alice", "Bob", "Charlie");
        for (String name : names) {
            System.out.println(name);
        }
    }

    /**
     * JDK 10+ 写法：使用 var 推断类型，减少冗余
     * 避坑：var 仅适用于局部变量，且右侧初始化表达式不能为 null
     */
    public void jdk10VarStyle() {
        var names = List.of("Alice", "Bob", "Charlie"); // 编译器推断为 List<String>
        for (var name : names) {
            System.out.println(name);
        }
    }

    // =========================================================
    // 特性二：Record 类 (JDK 16 正式发布)
    // =========================================================

    /**
     * JDK 8 写法：手写 DTO，需要 equals/hashCode/toString
     */
    static class UserDtoJdk8 {
        private final String name;
        private final int age;

        public UserDtoJdk8(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public int getAge() { return age; }

        @Override
        public String toString() {
            return "UserDtoJdk8{name='" + name + "', age=" + age + "}";
        }
    }

    /**
     * JDK 16+ 写法：Record 自动生成构造器、getter、equals、hashCode、toString
     * 避坑：Record 是不可变的，不能作为 Hibernate/JPA 实体（缺少无参构造器和 setter）
     */
    record UserRecord(String name, int age) {
        // 可以添加紧凑构造器进行参数校验
        UserRecord {
            if (age < 0) throw new IllegalArgumentException("年龄不能为负数");
        }
    }

    // =========================================================
    // 特性三：Sealed Classes 密封类 (JDK 17 正式发布)
    // =========================================================

    /**
     * 密封类：严格限制继承层次，配合模式匹配实现类型安全的多态。
     * 场景：定义支付结果类型，只允许固定的几种实现。
     */
    sealed interface PaymentResult permits PaymentResult.Success, PaymentResult.Failure {
        record Success(String transactionId, double amount) implements PaymentResult {}
        record Failure(String errorCode, String message) implements PaymentResult {}
    }

    /**
     * JDK 14+ instanceof 模式匹配：避免强制类型转换
     */
    public String handlePayment(PaymentResult result) {
        // JDK 16+ 模式匹配 instanceof，无需额外 cast
        if (result instanceof PaymentResult.Success s) {
            return "支付成功，交易号：" + s.transactionId() + "，金额：" + s.amount();
        } else if (result instanceof PaymentResult.Failure f) {
            return "支付失败，错误码：" + f.errorCode() + "，原因：" + f.message();
        }
        return "未知状态";
    }

    /**
     * JDK 21 Switch 模式匹配（JDK 21 正式发布；JDK 17 使用 instanceof 替代）
     * 注：在 JDK 17 环境下此方法使用 if-instanceof 实现等效逻辑。
     * 若使用 JDK 21+，可改用 switch 语句：
     *   return switch (result) {
     *     case PaymentResult.Success s -> "交易成功: " + s.transactionId();
     *     case PaymentResult.Failure f -> "交易失败: " + f.errorCode();
     *   };
     */
    public String handlePaymentSwitch(PaymentResult result) {
        if (result instanceof PaymentResult.Success s) {
            return "交易成功: " + s.transactionId();
        } else if (result instanceof PaymentResult.Failure f) {
            return "交易失败: " + f.errorCode();
        }
        return "未知状态";
    }

    // =========================================================
    // 特性四：文本块 Text Blocks (JDK 15 正式发布)
    // =========================================================

    public void textBlockDemo() {
        // JDK 8 写法：字符串拼接 JSON/SQL 极其繁琐
        String jsonJdk8 = "{\n" +
                "  \"name\": \"Alice\",\n" +
                "  \"age\": 30\n" +
                "}";

        // JDK 15+ 文本块：保持缩进和换行，代码更清晰
        String jsonJdk15 = """
                {
                  "name": "Alice",
                  "age": 30
                }
                """;

        System.out.println("JDK8 JSON:\n" + jsonJdk8);
        System.out.println("JDK15 Text Block JSON:\n" + jsonJdk15);
    }

    // =========================================================
    // 特性五：Optional 链式操作 (JDK 8+，JDK 9 增强)
    // =========================================================

    public String getUserEmail(String userId) {
        // JDK 8 风格
        Optional<String> emailOpt = findUserById(userId);
        if (emailOpt.isPresent()) {
            return emailOpt.get();
        }
        return "unknown@example.com";
    }

    public String getUserEmailModern(String userId) {
        // JDK 9+ orElse / ifPresentOrElse 链式调用
        return findUserById(userId).orElse("unknown@example.com");
    }

    private Optional<String> findUserById(String userId) {
        // 模拟数据库查询
        if ("U001".equals(userId)) {
            return Optional.of("alice@example.com");
        }
        return Optional.empty();
    }

    public static void main(String[] args) {
        JdkFeatureComparison demo = new JdkFeatureComparison();

        System.out.println("=== var 类型推断 ===");
        demo.jdk10VarStyle();

        System.out.println("\n=== Record ===");
        var user = new UserRecord("Alice", 30);
        System.out.println(user); // 自动生成 toString

        System.out.println("\n=== Sealed Classes + Pattern Matching ===");
        PaymentResult success = new PaymentResult.Success("TXN-001", 199.99);
        PaymentResult failure = new PaymentResult.Failure("INSUFFICIENT_FUNDS", "余额不足");
        System.out.println(demo.handlePaymentSwitch(success));
        System.out.println(demo.handlePaymentSwitch(failure));

        System.out.println("\n=== Text Block ===");
        demo.textBlockDemo();
    }
}
