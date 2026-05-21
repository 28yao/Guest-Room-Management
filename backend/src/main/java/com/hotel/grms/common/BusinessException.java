package com.hotel.grms.common;

/**
 * 业务异常，携带对外错误码与消息。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
