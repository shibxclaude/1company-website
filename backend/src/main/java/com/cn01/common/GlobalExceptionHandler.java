package com.cn01.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/** 所有异常统一转成 {code, msg, data}，HTTP 状态码与 code 对应。 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> biz(BizException e) {
        return build(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> bind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining("；"));
        return build(ErrorCode.BAD_REQUEST, msg.isEmpty() ? null : msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> constraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .distinct()
                .collect(Collectors.joining("；"));
        return build(ErrorCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Result<Void>> badRequest(Exception e) {
        return build(ErrorCode.BAD_REQUEST, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> tooLarge(MaxUploadSizeExceededException e) {
        return build(ErrorCode.BAD_REQUEST, "文件不能超过 5MB");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> duplicate(DuplicateKeyException e) {
        return build(ErrorCode.CONFLICT, "数据重复，请检查唯一字段（如编码、URL 标识、账号）");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> noResource(NoResourceFoundException e) {
        return build(ErrorCode.NOT_FOUND, "接口不存在");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> method(HttpRequestMethodNotSupportedException e) {
        return build(ErrorCode.BAD_REQUEST, "请求方法不支持");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> other(Exception e) {
        log.error("未处理异常", e);
        return build(ErrorCode.INTERNAL, null);
    }

    private ResponseEntity<Result<Void>> build(ErrorCode code, String msg) {
        return ResponseEntity.status(code.httpStatus()).body(Result.fail(code, msg));
    }
}
