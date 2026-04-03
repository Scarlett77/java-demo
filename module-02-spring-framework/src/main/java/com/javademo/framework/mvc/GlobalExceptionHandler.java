package com.javademo.framework.mvc;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * <p>技术点：Spring MVC @RestControllerAdvice 全局异常拦截
 * <p>场景背景：如果不统一处理异常，接口报错时响应格式不一（有时返回 Spring 默认 error 页面，
 *            有时返回原始异常信息），前端无法统一处理。通过全局异常处理器：
 *            1. 所有异常都返回统一的 ApiResponse 结构
 *            2. 服务端内部异常不暴露给客户端（防止信息泄露）
 *            3. 参数校验错误自动提取字段错误信息
 *
 * <p>核心原理：
 *   @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 *   Spring MVC 的 HandlerExceptionResolver 链中，ExceptionHandlerExceptionResolver
 *   负责处理 @ExceptionHandler 方法，优先级最高。
 *
 * <p>异常处理优先级（从精确到通用）：
 *   ConstraintViolationException → MethodArgumentNotValidException →
 *   BusinessException → Exception
 *
 * <p>避坑指南：
 *   1. 不要在 @ExceptionHandler 方法中打印原始异常堆栈到响应体（安全风险）。
 *   2. 区分 @RequestBody 参数校验（MethodArgumentNotValidException）和
 *      路径参数/Query参数校验（ConstraintViolationException）。
 *   3. 生产环境建议将异常链路 ID 记录在日志和响应中，方便排查。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 @RequestBody 参数校验失败（@Valid + @RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        // 提取所有字段校验错误信息，拼接成可读字符串
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[参数校验失败] {}", errorMsg);
        return ApiResponse.error(400, "参数校验失败: " + errorMsg);
    }

    /**
     * 处理 Form 表单绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBindException(BindException e) {
        String errorMsg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[表单绑定失败] {}", errorMsg);
        return ApiResponse.error(400, "参数绑定失败: " + errorMsg);
    }

    /**
     * 处理路径变量/Query参数校验失败（@PathVariable/@RequestParam + @Validated）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException e) {
        String errorMsg = e.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("[约束校验失败] {}", errorMsg);
        return ApiResponse.error(400, "请求参数不合法: " + errorMsg);
    }

    /**
     * 处理业务异常（主动抛出，返回具体业务错误码）
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK) // 业务异常 HTTP 状态码仍为 200，由响应 code 区分
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("[业务异常] code={}, message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理资源不存在（404）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoResourceFound(NoResourceFoundException e) {
        return ApiResponse.error(404, "请求的资源不存在: " + e.getResourcePath());
    }

    /**
     * 兜底异常处理（未被上面捕获的所有异常）
     * 注意：不要将异常详情暴露给客户端（安全风险），内部记录日志即可
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        // 生产环境：使用链路追踪 ID（如 traceId）关联日志
        log.error("[系统异常] {}", e.getMessage(), e);
        return ApiResponse.serverError("系统繁忙，请稍后重试");
    }
}
