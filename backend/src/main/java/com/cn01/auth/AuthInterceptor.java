package com.cn01.auth;

import com.cn01.admin.AdminUserMapper;
import com.cn01.common.ErrorCode;
import com.cn01.common.Status;
import com.cn01.user.AppUserMapper;
import com.cn01.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 鉴权规则：
 *   /api/admin/**（登录接口除外）必须是管理员 token；
 *   /api/user/** 必须是前台用户 token；
 *   其他接口（/api/portal/**）token 可选，有则解析（例如登录用户下单时关联到账号）。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final AdminUserMapper adminUserMapper;
    private final AppUserMapper appUserMapper;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true;
        Optional<AuthPrincipal> principal = jwtService.parse(bearer(req));
        principal.ifPresent(p -> req.setAttribute(AuthContext.ATTR, p));

        String path = req.getRequestURI();
        if (path.startsWith("/api/admin/") && !path.equals("/api/admin/auth/login")) {
            // 管理员请求量小，每次核对账号仍然有效（被停用后立即失效，不必等 token 过期）
            var admin = principal.filter(p -> p.type() == AuthPrincipal.Type.ADMIN);
            if (admin.isEmpty() || !activeAdmin(admin.get().id())) return deny(resp);
        } else if (path.startsWith("/api/user/")) {
            var user = principal.filter(p -> p.type() == AuthPrincipal.Type.USER);
            if (user.isEmpty() || !activeUser(user.get().id())) return deny(resp);
        }
        return true;
    }

    private boolean activeAdmin(long id) {
        var u = adminUserMapper.selectById(id);
        return u != null && u.getStatus() == Status.ON;
    }

    private boolean activeUser(long id) {
        var u = appUserMapper.selectById(id);
        return u != null && u.getStatus() == Status.ON;
    }

    private static String bearer(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        return h != null && h.startsWith("Bearer ") ? h.substring(7) : null;
    }

    private boolean deny(HttpServletResponse resp) throws Exception {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.getWriter().write(objectMapper.writeValueAsString(Result.fail(ErrorCode.UNAUTHORIZED, null)));
        return false;
    }
}
