package com.javademo.framework.mvc;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一 API 响应对象封装
 *
 * <p>技术点：统一响应结构设计
 * <p>场景背景：前后端分离项目中，接口响应格式不统一会增加前端处理成本。
 *            约定统一的 code/message/data 三段式结构，配合全局异常处理，
 *            使所有接口响应格式一致，便于前端统一处理。
 *
 * <p>设计规范：
 *   - code：业务状态码（200成功，4xx客户端错误，5xx服务端错误）
 *   - message：面向用户的提示消息
 *   - data：响应业务数据（失败时为 null，null 字段不序列化到 JSON）
 *
 * @param <T> 响应数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // null 字段不出现在 JSON 中
public class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // =========================================================
    // 工厂方法（推荐使用静态工厂，避免 new ApiResponse）
    // =========================================================

    /** 操作成功，携带数据 */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    /** 操作成功，自定义消息 */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    /** 操作成功，无返回数据 */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "操作成功", null);
    }

    /** 客户端错误（如参数错误、资源不存在） */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /** 服务端错误 */
    public static <T> ApiResponse<T> serverError(String message) {
        return new ApiResponse<>(500, message, null);
    }

    /** 未授权 */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(401, message, null);
    }

    /** 无权限 */
    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(403, message, null);
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }

    @Override
    public String toString() {
        return String.format("ApiResponse{code=%d, message='%s', data=%s}", code, message, data);
    }
}
