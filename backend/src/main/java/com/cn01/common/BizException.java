package com.cn01.common;

public class BizException extends RuntimeException {
    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode, String message) {
        super(message == null ? errorCode.defaultMsg() : message);
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public ErrorCode getErrorCode() { return errorCode; }

    public static BizException badRequest(String msg) { return new BizException(ErrorCode.BAD_REQUEST, msg); }
    public static BizException notFound(String msg) { return new BizException(ErrorCode.NOT_FOUND, msg); }
    public static BizException conflict(String msg) { return new BizException(ErrorCode.CONFLICT, msg); }
}
