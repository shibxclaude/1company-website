package com.cn01.auth;

import com.cn01.common.BizException;
import com.cn01.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/** 读取当前请求的登录身份（由 AuthInterceptor 写入）。 */
public final class AuthContext {
    static final String ATTR = "cn01.auth";

    private AuthContext() {}

    public static Optional<AuthPrincipal> current() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return Optional.empty();
        HttpServletRequest req = attrs.getRequest();
        return Optional.ofNullable((AuthPrincipal) req.getAttribute(ATTR));
    }

    public static Optional<AuthPrincipal> currentUser() {
        return current().filter(p -> p.type() == AuthPrincipal.Type.USER);
    }

    public static AuthPrincipal requireUser() {
        return currentUser().orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
    }

    public static AuthPrincipal requireAdmin() {
        return current().filter(p -> p.type() == AuthPrincipal.Type.ADMIN)
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
    }
}
