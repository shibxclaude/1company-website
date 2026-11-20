package com.cn01.common;

/** 统一返回结构 {code, msg, data}。code=0 表示成功。 */
public record Result<T>(int code, String msg, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static Result<Void> ok() {
        return new Result<>(0, "ok", null);
    }

    public static <T> Result<T> fail(ErrorCode code, String msg) {
        return new Result<>(code.code(), msg == null ? code.defaultMsg() : msg, null);
    }
}
