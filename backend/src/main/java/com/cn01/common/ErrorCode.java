package com.cn01.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    BAD_REQUEST(400, HttpStatus.BAD_REQUEST, "请求参数有误"),
    UNAUTHORIZED(401, HttpStatus.UNAUTHORIZED, "请先登录"),
    FORBIDDEN(403, HttpStatus.FORBIDDEN, "无权访问"),
    NOT_FOUND(404, HttpStatus.NOT_FOUND, "资源不存在"),
    CONFLICT(409, HttpStatus.CONFLICT, "数据状态已变化，请刷新后重试"),
    TOO_MANY_REQUESTS(429, HttpStatus.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试"),
    INTERNAL(500, HttpStatus.INTERNAL_SERVER_ERROR, "服务器开小差了，请稍后再试"),
    PAY_ERROR(502, HttpStatus.BAD_GATEWAY, "支付服务暂不可用，请稍后再试");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMsg;

    ErrorCode(int code, HttpStatus httpStatus, String defaultMsg) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMsg = defaultMsg;
    }

    public int code() { return code; }
    public HttpStatus httpStatus() { return httpStatus; }
    public String defaultMsg() { return defaultMsg; }
}
