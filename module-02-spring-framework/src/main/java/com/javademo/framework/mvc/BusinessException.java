package com.javademo.framework.mvc;

/**
 * 业务异常基类
 *
 * <p>统一业务异常，携带业务状态码，由全局异常处理器捕获并转换为标准响应。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public int getCode() { return code; }
}
