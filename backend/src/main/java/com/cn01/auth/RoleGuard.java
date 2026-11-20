package com.cn01.auth;

import com.cn01.common.ErrorCode;
import com.cn01.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.nio.charset.StandardCharsets;

/**
 * 要求当前请求是某类账号（管理员 / 前台用户），否则 401。依赖 AuthInterceptor 先解析出身份。
 *
 * 必须通过 {@link #register} 按路由模式注册：InterceptorRegistry 的路径匹配与 Controller 路由是同一套规则
 * （解码、去掉 ;参数），所以凡是能路由到 /api/admin/** 的请求都一定会经过管理员校验。
 */
public class RoleGuard implements HandlerInterceptor {
    public static final String ADMIN_PATHS = "/api/admin/**";
    public static final String ADMIN_LOGIN = "/api/admin/auth/login";
    public static final String USER_PATHS = "/api/user/**";

    private final AuthPrincipal.Type required;
    private final ObjectMapper objectMapper;

    RoleGuard(AuthPrincipal.Type required, ObjectMapper objectMapper) {
        this.required = required;
        this.objectMapper = objectMapper;
    }

    /** 注册身份解析与两类角色校验（顺序：先解析，再校验） */
    public static void register(InterceptorRegistry registry, AuthInterceptor auth, ObjectMapper objectMapper) {
        registry.addInterceptor(auth).addPathPatterns("/api/**");
        registry.addInterceptor(new RoleGuard(AuthPrincipal.Type.ADMIN, objectMapper))
                .addPathPatterns(ADMIN_PATHS).excludePathPatterns(ADMIN_LOGIN);
        registry.addInterceptor(new RoleGuard(AuthPrincipal.Type.USER, objectMapper)).addPathPatterns(USER_PATHS);
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true;
        if (AuthContext.current().filter(p -> p.type() == required).isPresent()) return true;
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.getWriter().write(objectMapper.writeValueAsString(Result.fail(ErrorCode.UNAUTHORIZED, null)));
        return false;
    }
}
